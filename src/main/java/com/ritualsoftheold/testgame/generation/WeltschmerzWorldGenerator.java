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

public class WeltschmerzWorldGenerator implements WorldGeneratorInterface<Void> {

    private Weltschmerz weltschmerz;
    private MaterialRegistry reg;

    @Override
    public  WorldGeneratorInterface<?> setup(MaterialRegistry reg, TerraModule mod) {
        this.reg = reg;
        weltschmerz = new Weltschmerz();
        weltschmerz.setMaterialID(reg.getMaterial(mod, "grass").getWorldId(), reg.getMaterial(mod,"dirt").getWorldId());
        return this;
    }

    @Override
    public Void initialize(GenerationTask task, Pipeline<Void> pipeline) {
        pipeline.addLast(this::generate);
        return null;
    }

    private void generate(GenerationTask task, GeneratorControl control, Void nothing) {
        weltschmerz.getChunk((int)task.getX(), (int)task.getY(), (int)task.getZ(), control.getLArray());
    }
}