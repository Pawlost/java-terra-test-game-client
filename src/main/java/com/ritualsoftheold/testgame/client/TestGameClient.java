package com.ritualsoftheold.testgame.client;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;

import com.jme3.texture.Texture;
import com.jme3.texture.TextureArray;
import com.ritualsoftheold.loader.config.PrimitiveResourcePack;
import com.ritualsoftheold.terra.core.chunk.ChunkLArray;
import com.ritualsoftheold.terra.core.materials.Registry;
import com.ritualsoftheold.terra.core.materials.TerraModule;
import com.ritualsoftheold.terra.core.octrees.OctreeBase;
import com.ritualsoftheold.testgame.client.generation.TestGameMesher;
import com.ritualsoftheold.testgame.client.generation.TextureManager;
import com.ritualsoftheold.testgame.client.network.Client;
import com.ritualsoftheold.testgame.client.network.Server;
import com.ritualsoftheold.testgame.client.utils.InputHandler;
import com.ritualsoftheold.testgame.client.utils.Picker;
import com.ritualsoftheold.testgame.materials.BarrelDistortion;

import java.util.ArrayList;
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

    private BlockingQueue<Spatial> geomCreateQueue = new ArrayBlockingQueue<>(10000);
    private BlockingQueue<String> geomDeleteQueue = new ArrayBlockingQueue<>(10000);

    private ArrayList<OctreeBase> octrees;

    public TestGameClient(Server server){
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

        cam.setLocation(new Vector3f(0, 0, 50));

        Picker picker = new Picker(rootNode);
        //picker.setGeometry(custom);

        // Some config options
        flyCam.setMoveSpeed(20);

        InputHandler input = new InputHandler(inputManager, picker, rootNode, cam);
        // input.addMaterial(material);
        octrees = server.init(this);

        setUpLight();

        fpp = new FilterPostProcessor(assetManager);
        barrel = new BarrelDistortion();
        fpp.addFilter(barrel);
        viewPort.addProcessor(fpp);
    }

    private void initTextures(){
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
        mesher.chunkLoaded(chunk);
    }
}