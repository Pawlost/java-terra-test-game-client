package com.ritualsoftheold.testgame.generation;

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
        weltschmerz.setBlocksID(
                reg.getMaterial(mod, "grass").getWorldId(),
                reg.getMaterial(mod,"dirt").getWorldId(),
                reg.getMaterial(mod, "Tall_Grass-mesh_variant01-01").getWorldId());

        TerraObject tree =  reg.getMaterial(mod, "birch-02_baked");
        weltschmerz.setObject(tree.getMesh().getDefaultDistanceX(), tree.getMesh().getDefaultDistanceY(),
                tree.getMesh().getDefaultDistanceZ(), tree.getMesh().getId());
        return this;
    }

    public void generate(ChunkLArray chunk) {
        weltschmerz.getChunk((int)chunk.x, (int)chunk.y, (int)chunk.z, chunk.getChunkVoxelData());
        chunk.setDifferent(weltschmerz.isDifferent());
    }
}