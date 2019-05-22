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

public class TestWorldGenerator implements WorldGeneratorInterface<Void> {

    private MaterialRegistry reg;

    @Override
    public void setup(long seed, MaterialRegistry reg, TerraModule mod) {
        this.reg = reg;
    }

    @Override
    public Void initialize(GenerationTask task, Pipeline<Void> pipeline) {
        pipeline.addLast(this::generate);
        return null;
    }

    public void generate(GenerationTask task, GeneratorControl control, Void nothing) {
        BlockBuffer buf = control.getBuffer();
        control.canGenerate(true);
        if (control.isGenerated()) {
            for (int i = 0; i < DataConstants.CHUNK_MAX_BLOCKS; i++) {
                buf.write(reg.getForWorldId(2));
                buf.next();
            }
        }
    }
}