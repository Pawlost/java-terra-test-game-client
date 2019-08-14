package com.ritualsoftheold.testgame.generation;

import com.jme3.asset.AssetManager;
import com.jme3.scene.Spatial;
import com.ritualsoftheold.loader.BlockMaker;
import com.ritualsoftheold.loader.ModelLoader3D;
import com.ritualsoftheold.terra.core.material.TerraModule;
import com.ritualsoftheold.terra.core.material.TerraObject;
import com.ritualsoftheold.terra.offheap.WorldGeneratorInterface;
import com.ritualsoftheold.terra.core.material.Registry;
import com.ritualsoftheold.terra.offheap.chunk.ChunkLArray;
import com.ritualsoftheold.weltschmerz.core.Weltschmerz;

public class WeltschmerzWorldGenerator implements WorldGeneratorInterface {

    private Weltschmerz weltschmerz;

    @Override
    public  WorldGeneratorInterface setup(Registry reg, TerraModule mod) {
        weltschmerz = new Weltschmerz();
        weltschmerz.setBlocksID(reg.getMaterial(mod, "grass").getWorldId(),
                reg.getMaterial(mod,"dirt").getWorldId(),
                reg.getMaterial(mod, "Tall_Grass-mesh_variant01-01").getWorldId());

        TerraObject tree =  reg.getMaterial(mod, "birch-01");
        weltschmerz.setObject(tree.getMesh().getVoxels());
        return this;
    }

    public void generate(ChunkLArray chunk) {
        weltschmerz.getChunk((int)chunk.x, (int)chunk.y, (int)chunk.z, chunk.getChunkVoxelData());
        chunk.setDifferent(weltschmerz.isDifferent());
    }
}