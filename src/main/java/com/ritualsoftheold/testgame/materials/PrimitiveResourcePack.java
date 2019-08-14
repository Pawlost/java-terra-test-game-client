package com.ritualsoftheold.testgame.materials;

import com.jme3.asset.AssetManager;
import com.jme3.scene.Spatial;
import com.ritualsoftheold.loader.BlockMaker;
import com.ritualsoftheold.loader.ModelLoader3D;
import com.ritualsoftheold.terra.core.material.TerraMesh;
import com.ritualsoftheold.terra.core.material.TerraModule;
import com.ritualsoftheold.terra.core.material.Registry;
import com.ritualsoftheold.terra.core.material.TerraTexture;

public class PrimitiveResourcePack {
    private Registry reg;

    public PrimitiveResourcePack(Registry reg){
        this.reg = reg;
    }

    public void registerObjects(TerraModule mod, AssetManager manager){
        ModelLoader3D modelLoader3D = new ModelLoader3D(manager);
        //Textures
        mod.newMaterial().name("dirt").texture(new TerraTexture("NorthenForestDirt256px.png"));
        mod.newMaterial().name("grass").texture(new TerraTexture("NorthenForestGrass256px.png"));

        Spatial asset =  modelLoader3D.getMesh("Tall_Grass-mesh_variant01-01");
        //Custom meshes
        mod.newMaterial().name("Tall_grass").model(new TerraMesh("Tall_Grass-mesh_variant01-01", true,
                BlockMaker.getBlocks(asset))).texture(new TerraTexture("Tall_grass-texture-2.png",
                true));

        Spatial spatial = modelLoader3D.getMesh("birch-01");
        mod.newMaterial().name("birch-01").model(new TerraMesh("birch-01", true,
                BlockMaker.getBlocks(spatial))).texture(
                        new TerraTexture("birch_bark_ambient_occlusion-01-4096x4096.dds", true));

        mod.registerMaterials(reg);
    }

}
