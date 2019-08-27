package com.ritualsoftheold.testgame.client.generation;

import com.jme3.asset.AssetManager;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.TextureArray;
import com.ritualsoftheold.loader.AssetIO;
import com.ritualsoftheold.loader.ModelLoader3D;
import com.ritualsoftheold.terra.core.materials.Registry;
import com.ritualsoftheold.terra.core.materials.TerraObject;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Manages textures of materials. Creates texture atlases.
 */
public class TextureManager{

    private int maxHeight;
    private int maxWidth;
    private static final Image.Format DEFAULT_IMAGE_FORMAT = Image.Format.ARGB8;

    //Size of the cube
    private TextureArray textureArray;

    public TextureManager(AssetManager assetManager, Registry registry) {
        HashMap<Integer, Image> atlas = new HashMap<>();
        ModelLoader3D modelLoader3D = new ModelLoader3D(assetManager);
        for (int i = 2; i <= registry.getAllMaterials().size(); i++) {
            TerraObject object = registry.getForWorldId(i);
            if (object.getTexture() != null) {
                object.getTexture().setPosition(i);

                Texture tex;
                if(object.hasMesh()){
                    tex = modelLoader3D.getTexture(object.getTexture().getAsset());
                }else {
                    tex = assetManager.loadTexture(object.getTexture().getAsset());
                }

                Image image = tex.getImage();

                if (image.getHeight() > maxHeight) {
                    maxHeight = image.getHeight();
                }

                if (image.getWidth() > maxWidth) {
                    maxWidth = image.getWidth();
                }
                atlas.put(i, image);
            }
        }

        for (TerraObject material : registry.getAllMaterials()) {
            if (material.getTexture() != null) {
                material.getTexture().setSize(maxWidth, maxHeight);
            }
        }

        for (Integer key: new ArrayList<>(atlas.keySet())) {
            Image image = atlas.get(key);
            ByteBuffer atlasBuf = AssetIO.resizeImage(image.getData(0), maxWidth, maxHeight,
                    image.getWidth(), image.getHeight());
            image = new Image(DEFAULT_IMAGE_FORMAT, maxWidth, maxHeight, atlasBuf, null, com.jme3.texture.image.ColorSpace.Linear);
            atlas.replace(key, image);
        }

        textureArray = new TextureArray(new ArrayList<>(atlas.values()));
    }

    public TextureArray getTextureArray() {
        return textureArray;
    }
}