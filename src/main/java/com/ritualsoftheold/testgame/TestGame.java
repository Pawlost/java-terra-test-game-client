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
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.VertexBuffer.Type;
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
import com.ritualsoftheold.terra.mesher.VoxelMesher;
import com.ritualsoftheold.terra.mesher.resource.MeshContainer;
import com.ritualsoftheold.terra.mesher.resource.TextureManager;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.io.ChunkLoader;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyOctreeLoader;
import com.ritualsoftheold.terra.offheap.memory.MemoryPanicHandler;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;
import com.ritualsoftheold.terra.offheap.world.OffheapWorld;
import com.ritualsoftheold.terra.offheap.world.WorldLoadListener;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ForkJoinPool;

public class TestGame extends SimpleApplication implements ActionListener {

    private OffheapWorld world;
    private boolean wireframe = false;
    private Material mat;
    private int loadMarkersUpdated;
    private LoadMarker[][] sectors;
    private LoadMarker primarySector;
    private WorldLoadListener listener;
    private ChunkLoader chunkLoader;
    private Picker picker;
    private TextureManager texManager;
    private MaterialRegistry reg;
    private Node terrain;

    private BlockingQueue<Geometry> geomCreateQueue = new ArrayBlockingQueue<>(10000);

    public static void main(String... args) {
        TestGame app = new TestGame();
        app.showSettings = false;
        app.settings = new AppSettings(true);
        app.settings.setResolution(1600, 900);
        app.settings.setTitle("Terra testgame");
        app.settings.setFullscreen(false);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        //setDisplayFps(false);
        //setDisplayStatView(false);
        initCrossHairs();
        sectors = new LoadMarker[3][3];

        terrain = new Node("Terrain");
        rootNode.attachChild(terrain);
        rootNode.addLight(new AmbientLight());
        rootNode.setCullHint(CullHint.Never);

        initKeyMapping();
        setupMaterials();
        setupGenerator();
        setupWorld();

        picker = new Picker(chunkLoader, world);

        world.setLoadListener(listener);
        primarySector = world.createLoadMarker(0, 0, 0, 1, 1, 0);
        world.addLoadMarker(primarySector);
        updateSectors();
    }

    private void updateSectors() {
        if (sectors[1][1] != primarySector) {
            sectors[1][1] = primarySector;
        }

   /*     for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                if (sectors[x][y] == null) {
                    if (x == 0 && y == 0) {
                        sectors[x][y] = world.createLoadMarker(primarySector.getX() - primarySector.getHardRadius() * 16 * 2, primarySector.getY(),
                                primarySector.getZ() - primarySector.getHardRadius() * 16 * 2, 1, 1, 0);
                    } else if (x == 0 && y == 1) {
                        sectors[x][y] = world.createLoadMarker(primarySector.getX() - primarySector.getHardRadius() * 16 * 2, primarySector.getY(),
                                primarySector.getZ(), 1, 1, 0);
                    } else if (x == 0) {
                        sectors[x][y] = world.createLoadMarker(primarySector.getX() - primarySector.getHardRadius() * 16 * 2, primarySector.getY(),
                                primarySector.getZ() + primarySector.getHardRadius() * 16 * 2, 1, 1, 0);
                    } else if (x == 1 && y == 0) {
                        sectors[x][y] = world.createLoadMarker(primarySector.getX(), primarySector.getY(),
                                primarySector.getZ() - primarySector.getHardRadius() * 16 * 2, 1, 1, 0);
                    } else if (x == 1) {
                        sectors[x][y] = world.createLoadMarker(primarySector.getX(), primarySector.getY(),
                                primarySector.getZ() + primarySector.getHardRadius() * 16 * 2, 1, 1, 0);
                    } else if (y == 0) {
                        sectors[x][y] = world.createLoadMarker(primarySector.getX() + primarySector.getHardRadius() * 16 * 2, primarySector.getY(),
                                primarySector.getZ() - primarySector.getHardRadius() * 16 * 2, 1, 1, 0);
                    } else if (y == 1) {
                        sectors[x][y] = world.createLoadMarker(primarySector.getX() + primarySector.getHardRadius() * 16 * 2, primarySector.getY(),
                                primarySector.getZ(), 1, 1, 0);
                    } else {
                        sectors[x][y] = world.createLoadMarker(primarySector.getX() + primarySector.getHardRadius() * 16 * 2, primarySector.getY(),
                                primarySector.getZ() + primarySector.getHardRadius() * 16 * 2, 1, 1, 0);
                    }
                    world.addLoadMarker(sectors[x][y]);
                }
            }
        }*/
        world.updateLoadMarkers();
    }

    private void setupWorld(){
        WorldGeneratorInterface<?> gen = new WorldGenerator();
        chunkLoader = new ChunkLoader(listener);
        gen.setup(0, reg);

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

    private void setupGenerator(){
        VoxelMesher mesher = new GreedyMesher();

        listener = new WorldLoadListener() {

            @Override
            public void octreeLoaded(long addr, long groupAddr, int id, float x,
                                     float y, float z, float scale, LoadMarker trigger) {
                // For now, just ignore octrees
            }

            @Override
            public void chunkLoaded(OffheapChunk chunk, float x, float y, float z, LoadMarker trigger) {
                    //System.out.println("Loaded chunk: " + chunk.memoryAddress());
                MeshContainer container = new MeshContainer();
                mesher.chunk(chunk.getBuffer(), texManager, container);

                // Create mesh
                Mesh mesh = new Mesh();

                //Set coordinates
                Vector3f[] vector3fs = new Vector3f[container.getVertice().toArray().length];
                container.getVertice().toArray(vector3fs);
                mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vector3fs));
                //Connects triangles
                Integer[] integers = new Integer[container.getIndices().toArray().length];
                container.getIndices().toArray(integers);
                int[] indices = new int[container.getIndices().size()];
                for (int i = 0; i < container.getIndices().size(); i++) {
                    indices[i] = integers[i];
                }
                mesh.setBuffer(Type.Index, 2, BufferUtils.createIntBuffer(indices));

                //Normals
                Vector3f[] norm = new Vector3f[container.getNormals().toArray().length];
                container.getNormals().toArray(norm);
                mesh.setBuffer(Type.Normal,3,BufferUtils.createFloatBuffer(norm));

                //Set texture scale and type
                Vector3f[] vector2fs = new Vector3f[container.getTextureCoordinates().toArray().length];
                container.getTextureCoordinates().toArray(vector2fs);
                mesh.setBuffer(Type.TexCoord, 3, BufferUtils.createFloatBuffer(vector2fs));

                //Update mesh
                mesh.updateBound();

                // Create geometry
                Geometry geom = new Geometry("chunk:" + x + "," + y + "," + z, mesh);

                // Create material
                geom.setMaterial(mat);

                //Set chunk position in world
                geom.setShadowMode(RenderQueue.ShadowMode.Cast);
                geom.setLocalTranslation(x, y, z);
                geom.setCullHint(CullHint.Never);

                container.clear();

                // Place geometry in queue for main thread
                geomCreateQueue.add(geom);
            }
        };
    }

    private void setupMaterials(){
        TerraModule mod = new TerraModule("testgame");
        mod.newMaterial().name("dirt").texture(new TerraTexture(256, 256, "NorthenForestDirt256px.png"));
        mod.newMaterial().name("grass").texture(new TerraTexture(256, 256, "NorthenForestGrass256px.png"));

        reg = new MaterialRegistry();
        mod.registerMaterials(reg);

        texManager = new TextureManager(assetManager, reg);
        TextureArray atlasTexture = texManager.getTextureArray();
        atlasTexture.setWrap(Texture.WrapMode.Repeat);
        atlasTexture.setMagFilter(Texture.MagFilter.Nearest);
        atlasTexture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);

        mat = new Material(assetManager, "/shaders/terra/TerraArray.j3md");
        mat.setTexture("ColorMap", atlasTexture);
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

        if (name.equals("Pick") && !isPressed) {
            // 1. Reset results list.
            CollisionResults results = new CollisionResults();
            // 2. Aim the ray from cam loc to cam direction.
            Ray ray = new Ray(cam.getLocation(), cam.getDirection());
            // 3. Collect intersections between Ray and Shootables in results list.
            // DO NOT check collision with the root node, or else ALL collisions will hit the
            // skybox! Always make a separate node for objects you want to collide with.
            terrain.collideWith(ray, results);
            // For each hit, we know distance, impact point, name of geometry.
            if(results.size() > 0) {
                picker.prepare(results);
                picker.pick(reg.getForWorldId(1));
            }
        }

        if (name.equals("Place") && !isPressed) {
            // 1. Reset results list.
            CollisionResults results = new CollisionResults();
            // 2. Aim the ray from cam loc to cam direction.
            Ray ray = new Ray(cam.getLocation(), cam.getDirection());
            // 3. Collect intersections between Ray and Shootables in results list.
            // DO NOT check collision with the root node, or else ALL collisions will hit the
            // skybox! Always make a separate node for objects you want to collide with.
            terrain.collideWith(ray, results);
            // For each hit, we know distance, impact point, name of geometry.
            if(results.size() > 0) {
                picker.prepare(results);
                picker.place(reg.getForWorldId(2));
            }
        }
    }
    /** A centred plus sign to help the player aim. */
    private void initCrossHairs() {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText ch = new BitmapText(guiFont, false);
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        ch.setText("+"); // crosshairs
        ch.setLocalTranslation( // center
                settings.getWidth() / 2 - ch.getLineWidth()/2,
                settings.getHeight() / 2 + ch.getLineHeight()/2, 0);
        guiNode.attachChild(ch);
    }
}