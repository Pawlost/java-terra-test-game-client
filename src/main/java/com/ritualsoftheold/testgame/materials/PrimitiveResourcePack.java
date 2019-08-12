package com.ritualsoftheold.testgame.materials;

import com.ritualsoftheold.terra.core.material.TerraMesh;
import com.ritualsoftheold.terra.core.material.TerraModule;
import com.ritualsoftheold.terra.core.material.Registry;
import com.ritualsoftheold.terra.core.material.TerraTexture;

public class PrimitiveResourcePack {
    private Registry reg;

    public PrimitiveResourcePack(Registry reg){
        this.reg = reg;
    }

    public void registerObjects(TerraModule mod){
        //Textures
        mod.newMaterial().name("dirt").texture(new TerraTexture("NorthenForestDirt256px.png"));
        mod.newMaterial().name("grass").texture(new TerraTexture("NorthenForestGrass256px.png"));

        //Custom meshes
        mod.newMaterial().name("Tall_grass").setModel(new TerraMesh("Tall_grass", true))
                .texture(new TerraTexture("Tall_grass-texture-2.png", true));

        mod.registerMaterials(reg);
    }

}
