package com.ritualsoftheold.testgame;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.jme3.texture.TextureArray;
import com.ritualsoftheold.terra.core.TerraModule;
import com.ritualsoftheold.terra.offheap.WorldGeneratorInterface;
import com.ritualsoftheold.terra.core.material.MaterialRegistry;
import com.ritualsoftheold.terra.core.material.TerraTexture;
import com.ritualsoftheold.terra.mesher.resource.TextureManager;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.io.ChunkLoader;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyOctreeLoader;
import com.ritualsoftheold.terra.offheap.memory.MemoryPanicHandler;
import com.ritualsoftheold.terra.offheap.world.OffheapLoadMarker;
import com.ritualsoftheold.terra.offheap.world.OffheapWorld;
import com.ritualsoftheold.terra.offheap.world.WorldLoadListener;
import com.ritualsoftheold.testgame.utils.InputHandler;
import com.ritualsoftheold.testgame.generation.MeshListener;
import com.ritualsoftheold.testgame.generation.WeltschmerzWorldGenerator;
import com.ritualsoftheold.weltschmerz.core.Weltschmerz;
import com.ritualsoftheold.weltschmerz.landmass.Zone;
import com.ritualsoftheold.weltschmerz.noise.generators.WorldNoise;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ForkJoinPool;

public class TestGame extends SimpleApplication {

    private OffheapWorld world;
    private Material mat;
    private BitmapText playerPosition;
    private WorldLoadListener listener;
    private ChunkLoader chunkLoader;
    private MaterialRegistry reg;
    private Node terrain;
    private TerraModule mod;
    private OffheapLoadMarker player;

    private BlockingQueue<Geometry> geomCreateQueue = new ArrayBlockingQueue<>(10000);
    private BlockingQueue<String> geomDeleteQueue = new ArrayBlockingQueue<>(10000);

    public static void main(String... args) {
        TestGame app = new TestGame();
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

        initUI();
        setupMaterials();
        setupWorld();
        cam.setLocation(new Vector3f(50,50,50));

        player = world.createLoadMarker(cam.getLocation().x, cam.getLocation().y,
                cam.getLocation().z, 2, 2, 0);

        //Picker picker = new Picker(chunkLoader, player, reg.getMaterial(mod, "grass"), reg.getMaterial("base:air"));

        // Some config options
        flyCam.setMoveSpeed(40);

        new InputHandler(inputManager, null, terrain, mat, cam);

        new Thread(() -> world.initialChunkGeneration(player)).start();
    }

    private void setupWorld() {
        listener = new MeshListener(mat, reg, geomCreateQueue, geomDeleteQueue);
        WorldGeneratorInterface gen = new WeltschmerzWorldGenerator().setup(reg, mod);
        chunkLoader = new ChunkLoader(listener);

        world = new OffheapWorld(gen, reg, 8 , listener);
    }

    private void setupMaterials() {
        mod = new TerraModule("testgame");
        mod.newMaterial().name("dirt").texture(new TerraTexture("NorthenForestDirt256px.png"));
        mod.newMaterial().name("grass").texture(new TerraTexture("NorthenForestGrass256px.png"));

        reg = new MaterialRegistry();
        mod.registerMaterials(reg);

        TextureManager texManager = new TextureManager(assetManager, reg);
        TextureArray atlasTexture = texManager.getTextureArray();
        atlasTexture.setWrap(Texture.WrapMode.Repeat);
        atlasTexture.setMagFilter(Texture.MagFilter.Nearest);
        atlasTexture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);

        mat = new Material(assetManager, "/shaders/terra/TerraArray.j3md");
        mat.setTexture("ColorMap", atlasTexture);
    }

    @Override
    public void simpleUpdate(float tpf) {

        playerPosition.setText("Player position x: " + cam.getLocation().x + " y: " +
                cam.getLocation().y + " z: " + cam.getLocation().z);
        int camX = (int) (cam.getLocation().x / 16f)*16;
        int playerX = (int) (player.getX() / 16f)*16;
        int camZ = (int) (cam.getLocation().z / 16f)*16;
        int playerZ = (int) (player.getZ() / 16f)*16;

        if(geomCreateQueue.isEmpty() && !player.hasMoved() && geomDeleteQueue.isEmpty()) {
            if (camX != playerX  || camZ != playerZ) {

                if (camX > playerX) {
                    playerX += 16;
                } else if (camX < playerX) {
                    playerX -= 16;
                }

                if (camZ > playerZ) {
                    playerZ += 16;
                } else if (camZ < playerZ) {
                    playerZ -= 16;
                }

                player.move(playerX, (int) cam.getLocation().y, playerZ);
                new Thread(() -> {world.updateLoadMarker(player, false); }).start();
            }
        }

        while (!geomCreateQueue.isEmpty()) {
            Geometry geom = geomCreateQueue.poll();
            if (terrain.getChild(geom.getName()) != null) {
                terrain.detachChildNamed(geom.getName());
            }

            terrain.attachChild(geom);
        }

        while (!geomDeleteQueue.isEmpty()) {
            String name = geomDeleteQueue.poll();
            terrain.detachChildNamed(name);
        }
    }

    /**
     * A centred plus sign to help the player aim.
     */
    private void initUI() {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText ch = new BitmapText(guiFont, false);
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        ch.setText("+"); // crosshairs
        ch.setLocalTranslation( // center
                settings.getWidth() / 2f - ch.getLineWidth() / 2,
                settings.getHeight() / 2f + ch.getLineHeight() / 2, 0);
        guiNode.attachChild(ch);

        playerPosition = new BitmapText(guiFont, false);
        playerPosition.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        playerPosition.setLocalTranslation(0, 800, 0);
        guiNode.attachChild(playerPosition);
    }
}