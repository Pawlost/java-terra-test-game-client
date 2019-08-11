package com.ritualsoftheold.testgame;

import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.*;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.jme3.texture.TextureArray;
import com.jme3.util.BufferUtils;
import com.ritualsoftheold.terra.core.TerraModule;
import com.ritualsoftheold.terra.core.gen.interfaces.world.WorldGeneratorInterface;
import com.ritualsoftheold.terra.core.gen.objects.LoadMarker;
import com.ritualsoftheold.terra.core.material.MaterialRegistry;
import com.ritualsoftheold.terra.core.material.TerraTexture;
import com.ritualsoftheold.terra.mesher.GreedyMesher;
import com.ritualsoftheold.terra.mesher.resource.MeshContainer;
import com.ritualsoftheold.terra.mesher.SplatMesher;
import com.ritualsoftheold.terra.mesher.VoxelMesher;
import com.ritualsoftheold.terra.mesher.resource.TextureManager;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.io.ChunkLoader;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyOctreeLoader;
import com.ritualsoftheold.terra.offheap.memory.MemoryPanicHandler;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;
import com.ritualsoftheold.terra.offheap.world.OffheapWorld;
import com.ritualsoftheold.terra.offheap.world.WorldLoadListener;
import com.ritualsoftheold.testgame.generation.WeltschmerzListener;
import com.ritualsoftheold.testgame.generation.WeltschmerzListenerSplat;
import com.ritualsoftheold.testgame.generation.WorldGenerator;
import com.ritualsoftheold.testgame.utils.Picker;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

public class SplatTestGame extends SimpleApplication implements ActionListener {

    private OffheapWorld world;
    private boolean wireframe = false;
    private Material mat;
    private int loadMarkersUpdated;
    private WorldLoadListener listener;
    private ChunkLoader chunkLoader;
    private Picker picker;
    private TextureManager texManager;
    private MaterialRegistry reg;
    private Node terrain;
    private TerraModule mod;

    private BlockingQueue<Geometry> geomCreateQueue = new ArrayBlockingQueue<>(10000);

    public static void main(String... args) {
        SplatTestGame app = new SplatTestGame();
        app.showSettings = false;
        app.settings = new AppSettings(true);
        app.settings.setResolution(1024, 768);
        app.settings.setTitle("Terra testgame");
        app.settings.setFullscreen(false);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        //setDisplayFps(false);
        //setDisplayStatView(false);


        terrain = new Node("Terrain");
        rootNode.attachChild(terrain);
        rootNode.addLight(new AmbientLight());
        rootNode.setCullHint(Spatial.CullHint.Never);

        initKeyMapping();
        setupMaterials();
        listener = new WeltschmerzListenerSplat(texManager, mat, geomCreateQueue);
        setupWorld();

        picker = new Picker(chunkLoader, world,  reg.getMaterial(mod, "grass"), reg.getMaterial("base:air"));

        world.setLoadListener(listener);
        LoadMarker player = world.createLoadMarker(0, 0, 0, 250, 250, 0);
        world.addLoadMarker(player);
        world.updateLoadMarkers();
    }

    private void setupWorld(){
        WorldGeneratorInterface<?> gen = new WorldGenerator();
        chunkLoader = new ChunkLoader(listener);
        gen.setup(0, reg, mod);

        ChunkBuffer.Builder bufferBuilder = new ChunkBuffer.Builder()
                .maxChunks(128)
                .queueSize(4);

        world = new OffheapWorld.Builder()
                .chunkLoader(chunkLoader)
                .octreeLoader(new DummyOctreeLoader(322768))
                .storageExecutor(ForkJoinPool.commonPool())
                .chunkStorage(bufferBuilder, 10000000)
                .octreeStorage(322768)
                .generator(gen)
                .generatorExecutor(ForkJoinPool.commonPool())
                .materialRegistry(reg)
                .memorySettings(10000000, 10000000, new MemoryPanicHandler() {

                    @Override
                    public PanicResult outOfMemory(long max, long used, long possible) {
                        return PanicResult.CONTINUE;
                    }

                    @Override
                    public PanicResult goalNotMet(long goal, long possible) {
                        return PanicResult.CONTINUE;
                    }
                }).build();
    }

    private void setupMaterials(){
        mod = new TerraModule("testgame");
        mod.newMaterial().name("dirt").texture(new TerraTexture(256, 256, "NorthenForestDirt256px.png"));
        mod.newMaterial().name("grass").texture(new TerraTexture(256, 256, "NorthenForestGrass256px.png"));

        reg = new MaterialRegistry();
        mod.registerMaterials(reg);

        texManager = new TextureManager(assetManager, reg);
        TextureArray atlasTexture = texManager.getTextureArray();
        atlasTexture.setWrap(Texture.WrapMode.Repeat);
        atlasTexture.setMagFilter(Texture.MagFilter.Nearest);
        atlasTexture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);

        mat = new Material(assetManager, "/shaders/terra/SplatShader.j3md");
        mat.setFloat("VoxelSize",DataConstants.SMALLEST_BLOCK);
    }

    private void initKeyMapping(){
        inputManager.addMapping("RELOAD", new KeyTrigger(KeyInput.KEY_G));
        inputManager.addListener(this, "RELOAD");
        inputManager.addMapping("toggle wireframe", new KeyTrigger(KeyInput.KEY_T));
        inputManager.addListener(this, "toggle wireframe");

        inputManager.addMapping("Pick",
                new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(this, "Pick");

        inputManager.addMapping("Place",
                new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addListener(this, "Place");

        inputManager.addMapping("Change",
                new MouseButtonTrigger(MouseInput.BUTTON_MIDDLE));
        inputManager.addListener(this, "Change");

        // Some config options
        flyCam.setMoveSpeed(10);
    }

    @Override
    public void simpleUpdate(float tpf) {
        loadMarkersUpdated += tpf;
        if (loadMarkersUpdated > 1) {
            loadMarkersUpdated = 0;
            //   Vector3f camLoc = cam.getLocation();

        }

        while (!geomCreateQueue.isEmpty()) {
            Geometry geom = geomCreateQueue.poll();
            if(terrain.getChild(geom.getName()) != null){
                terrain.detachChild(terrain.getChild(geom.getName()));
            }

            terrain.attachChild(geom);
        }
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals("toggle wireframe") && !isPressed) {
            wireframe = !wireframe; // toggle boolean
            mat.getAdditionalRenderState().setWireframe(wireframe);
        }

    }

}
