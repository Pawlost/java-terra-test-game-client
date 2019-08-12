package com.ritualsoftheold.testgame.generation;

import com.ritualsoftheold.terra.core.material.TerraModule;
import com.ritualsoftheold.terra.offheap.WorldGeneratorInterface;
import com.ritualsoftheold.terra.core.material.Registry;
import com.ritualsoftheold.terra.offheap.chunk.ChunkLArray;
import com.ritualsoftheold.weltschmerz.core.Weltschmerz;

public class WeltschmerzWorldGenerator implements WorldGeneratorInterface {

    private Weltschmerz weltschmerz;
    private Registry reg;

    @Override
    public  WorldGeneratorInterface setup(Registry reg, TerraModule mod) {
        this.reg = reg;
        weltschmerz = new Weltschmerz();
        weltschmerz.setMaterialID(reg.getMaterial(mod, "grass").getWorldId(),
                reg.getMaterial(mod,"dirt").getWorldId(),
                reg.getMaterial(mod, "Tall_grass").getWorldId());
        return this;
    }

    public void generate(ChunkLArray chunk) {
        weltschmerz.getChunk((int)chunk.x, (int)chunk.y, (int)chunk.z, chunk.getChunkVoxelData());
        chunk.setDifferent(weltschmerz.isDifferent());
    }
}