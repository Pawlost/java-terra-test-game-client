package com.ritualsoftheold.testgame.generation;

import com.ritualsoftheold.terra.core.TerraModule;
import com.ritualsoftheold.terra.core.buffer.BlockBuffer;
import com.ritualsoftheold.terra.core.gen.interfaces.GeneratorControl;
import com.ritualsoftheold.terra.core.gen.interfaces.world.WorldGeneratorInterface;
import com.ritualsoftheold.terra.core.gen.tasks.GenerationTask;
import com.ritualsoftheold.terra.core.gen.tasks.Pipeline;
import com.ritualsoftheold.terra.core.material.MaterialRegistry;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.weltschmerz.core.Weltschmerz;

public class WorldGenerator implements WorldGeneratorInterface<Void> {

    private Weltschmerz weltschmerz;
    private MaterialRegistry reg;

    @Override
    public void setup(long seed, MaterialRegistry reg, TerraModule mod) {
        this.reg = reg;
        weltschmerz = new Weltschmerz(reg.getMaterial(mod, "grass").getWorldId(), reg.getMaterial(mod,"dirt").getWorldId());
    }

    @Override
    public Void initialize(GenerationTask task, Pipeline<Void> pipeline) {
        pipeline.addLast(this::generate);
        return null;
    }

    public void generate(GenerationTask task, GeneratorControl control, Void nothing) {
        BlockBuffer buf = control.getBuffer();
        control.canGenerate(weltschmerz.setChunk((int)task.getX(), (int) task.getY(), (int)task.getZ()));
        if (control.isGenerated()) {
            for (int i = 0; i < DataConstants.CHUNK_MAX_BLOCKS; i++) {
                int x = i % 64;
                int z = i / 4096;
                int y = (i - 4096 * z) / 64;
                buf.write(reg.getForWorldId(weltschmerz.generateVoxel(x, y, z)));
                buf.next();
            }
        }
    }
}