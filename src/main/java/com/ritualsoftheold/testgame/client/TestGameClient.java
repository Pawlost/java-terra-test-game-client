package com.ritualsoftheold.testgame.client;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;

import com.jme3.texture.Texture;
import com.jme3.texture.TextureArray;
import com.ritualsoftheold.loader.config.PrimitiveResourcePack;
import com.ritualsoftheold.terra.core.chunk.ChunkLArray;
import com.ritualsoftheold.terra.core.markers.Marker;
import com.ritualsoftheold.terra.core.markers.Type;
import com.ritualsoftheold.terra.core.materials.Registry;
import com.ritualsoftheold.terra.core.materials.TerraModule;
import com.ritualsoftheold.terra.core.octrees.OffheapOctree;
import com.ritualsoftheold.testgame.client.generation.TestGameMesher;
import com.ritualsoftheold.testgame.client.generation.TextureManager;
import com.ritualsoftheold.testgame.client.network.Client;
import com.ritualsoftheold.testgame.client.network.Server;
import com.ritualsoftheold.testgame.client.utils.InputHandler;
import com.ritualsoftheold.testgame.client.utils.Picker;
import com.ritualsoftheold.testgame.materials.BarrelDistortion;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class TestGameClient extends SimpleApplication implements Client {

    private BitmapText playerPosition;
    private Node terrain;
    private Server server;
    private TestGameMesher mesher;
    private TextureManager texManager;
    private Registry registry;

    private FilterPostProcessor fpp;
    private BarrelDistortion barrel;

    private BlockingQueue<Spatial> geomCreateQueue = new ArrayBlockingQueue<>(1000000);
    private BlockingQueue<String> geomDeleteQueue = new ArrayBlockingQueue<>(10000);

    public TestGameClient(Server server) {
        super();
        this.server = server;
        this.showSettings = false;
        this.settings = new AppSettings(true);
        this.settings.setResolution(1200, 500);
        this.settings.setTitle("Terra testgame client");
        this.settings.setFullscreen(false);
        this.start();
    }

    @Override
    public void simpleInitApp() {
        // setDisplayFps(false);
        //setDisplayStatView(false);

        terrain = new Node("Terrain");
        rootNode.attachChild(terrain);
        rootNode.setCullHint(Spatial.CullHint.Never);

        registry = new Registry();
        TerraModule mod = new TerraModule("testgame");
        PrimitiveResourcePack resourcePack = new PrimitiveResourcePack(assetManager);
        resourcePack.registerObjects(mod);
        mod.registerMaterials(registry);

        initTextures();
        initUI();

        Picker picker = new Picker(rootNode);
        //picker.setGeometry(custom);

        // Some config options
        flyCam.setMoveSpeed(20);

        InputHandler input = new InputHandler(inputManager, picker, rootNode, cam);
        // input.addMaterial(material);
        server.init(this);

        setUpLight();

        //Barrel distortion setup
        fpp = new FilterPostProcessor(assetManager);
        barrel = new BarrelDistortion();
        fpp.addFilter(barrel);
        viewPort.addProcessor(fpp);
    }

    private void initTextures() {
        texManager = new TextureManager(assetManager, registry);

        TextureArray atlasTexture = texManager.getTextureArray();
        atlasTexture.setWrap(Texture.WrapMode.Repeat);
        atlasTexture.setMagFilter(Texture.MagFilter.Nearest);
        atlasTexture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);

        mesher = new TestGameMesher(assetManager, geomCreateQueue, geomDeleteQueue, atlasTexture, registry);
    }

    @Override
    public void simpleUpdate(float tpf) {

        playerPosition.setText("Player position x: " + cam.getLocation().x + " y: " +
                cam.getLocation().y + " z: " + cam.getLocation().z);

        while (!geomCreateQueue.isEmpty()) {
            Spatial geom = geomCreateQueue.poll();
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

    private void setUpLight() {
        // We add light so we see the scene
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(1.3f));
        rootNode.addLight(al);

        DirectionalLight dl = new DirectionalLight();
        dl.setColor(ColorRGBA.White);
        dl.setDirection(new Vector3f(2.8f, -2.8f, -2.8f).normalizeLocal());
        rootNode.addLight(dl);
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
        playerPosition.setSize(guiFont.getCharSet().getRenderedSize());
        playerPosition.setLocalTranslation(0, (settings.getHeight() / 4f) * 3, 0);
        guiNode.attachChild(playerPosition);
    }

    @Override
    public float getPosX() {
        return cam.getLocation().x;
    }

    @Override
    public float getPosY() {
        return cam.getLocation().y;
    }

    @Override
    public float getPosZ() {
        return cam.getLocation().z;
    }

    @Override
    public void sendChunk(ChunkLArray chunk) {
        chunk.setReg(this.registry);
        mesher.chunkLoaded(chunk);
    }

    @Override
    public void sendOctree(Marker octree) {
        if (octree.getType() == Type.OCTREE) {
            System.out.println("Octree generation started");
            for (Marker marker : ((OffheapOctree) octree).getOctreeNodes()) {
                if (marker.getType() == Type.LEAF_OCTANT) {
                    Box box = new Box(8, 8, 8);
                    box.setMode(Mesh.Mode.Lines);
                    System.out.println(" x " + marker.getPosX() + " y " + marker.getPosY() + " z " + marker.getPosZ());
                    Geometry geom = new Geometry("Box:" + marker.getPosX() + " " + marker.getPosY() + " " + marker.getPosZ(), box);
                    Material mat1 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
                    mat1.setColor("Color", ColorRGBA.Green);
                    geom.setMaterial(mat1);
                    geom.updateGeometricState();
                    geom.setLocalTranslation(marker.getPosX() + 8, marker.getPosY() + 8, marker.getPosZ() + 8);
                    geom.updateModelBound();
                    geomCreateQueue.add(geom);
                }
            }
        }
    }
}