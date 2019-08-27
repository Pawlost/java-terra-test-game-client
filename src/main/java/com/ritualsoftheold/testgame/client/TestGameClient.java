package com.ritualsoftheold.testgame.client;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.jme3.texture.TextureArray;
import com.ritualsoftheold.terra.core.materials.Registry;
import com.ritualsoftheold.terra.core.materials.TerraModule;
import com.ritualsoftheold.terra.core.octrees.OctreeBase;
import com.ritualsoftheold.terra.server.manager.WorldGeneratorInterface;
import com.ritualsoftheold.terra.server.manager.world.OffheapLoadMarker;
import com.ritualsoftheold.testgame.client.generation.TestGameMesher;
import com.ritualsoftheold.testgame.client.materials.PrimitiveResourcePack;
import com.ritualsoftheold.testgame.client.utils.InputHandler;
import com.ritualsoftheold.testgame.client.utils.Picker;
import com.ritualsoftheold.testgame.client.generation.TextureManager;
import com.ritualsoftheold.testgame.server.generation.WeltschmerzWorldGenerator;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class TestGameClient extends SimpleApplication {

    private BitmapText playerPosition;
    private Node terrain;
    private OffheapLoadMarker player;
    private ArrayList<OctreeBase> node;

    private BlockingQueue<Spatial> geomCreateQueue = new ArrayBlockingQueue<>(10000);
    private BlockingQueue<String> geomDeleteQueue = new ArrayBlockingQueue<>(10000);

    public static void main(String... args) {
        TestGameClient app = new TestGameClient();
        app.showSettings = false;
        app.settings = new AppSettings(true);
        app.settings.setResolution(1200, 500);
        app.settings.setTitle("Terra testgame");
        app.settings.setFullscreen(false);
        app.start();
    }

    @Override
    public void simpleInitApp() {
       // setDisplayFps(false);
        //setDisplayStatView(false);

        terrain = new Node("Terrain");
        rootNode.attachChild(terrain);
        rootNode.setCullHint(Spatial.CullHint.Never);

        initUI();
        setupMaterials();

        cam.setLocation(new Vector3f(0, 0, 50));


        player = world.createLoadMarker(cam.getLocation().x, cam.getLocation().y,
                cam.getLocation().z, 16, 16, 0);

        Picker picker = new Picker(rootNode);
        //picker.setGeometry(custom);

        // Some config options
        flyCam.setMoveSpeed(20);

        InputHandler input = new InputHandler(inputManager, picker, rootNode, cam);
        // input.addMaterial(material);

        node = new ArrayList<>(10000000);
    }

    private void setupMaterials() {
        TerraModule mod = new TerraModule("testgame");
        Registry reg = new Registry();
        PrimitiveResourcePack resourcePack = new PrimitiveResourcePack(reg);
        resourcePack.registerObjects(mod, assetManager);

        TextureManager texManager = new TextureManager(assetManager, reg);
        TextureArray atlasTexture = texManager.getTextureArray();
        atlasTexture.setWrap(Texture.WrapMode.Repeat);
        atlasTexture.setMagFilter(Texture.MagFilter.Nearest);
        atlasTexture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);

        TestGameMesher listener = new TestGameMesher(assetManager, geomCreateQueue, geomDeleteQueue, atlasTexture, reg);// geomDeleteQueue);
        WorldGeneratorInterface gen = new WeltschmerzWorldGenerator().setup(reg, mod);
    }

    @Override
    public void simpleUpdate(float tpf) {

        playerPosition.setText("Player position x: " + cam.getLocation().x + " y: " +
                cam.getLocation().y + " z: " + cam.getLocation().z);
        int camX = (int) (cam.getLocation().x / 16f) * 16;
        int playerX = (int) (player.getX() / 16f) * 16;
        int camZ = (int) (cam.getLocation().z / 16f) * 16;
        int playerZ = (int) (player.getZ() / 16f) * 16;

        if (geomCreateQueue.isEmpty() && !player.hasMoved() && geomDeleteQueue.isEmpty()) {
            if (camX != playerX || camZ != playerZ) {

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
              //  new Thread(() -> world.updateLoadMarker(player, false)).start();
            }
        }

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
}