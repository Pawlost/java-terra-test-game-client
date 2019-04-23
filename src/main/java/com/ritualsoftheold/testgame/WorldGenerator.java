package com.ritualsoftheold.testgame;

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
    private MaterialRegistry registry;
    private int index;

    @Override
    public void setup(long seed, MaterialRegistry registry) {
        this.registry = registry;
        weltschmerz = new Weltschmerz();
    }

    @Override
    public Void initialize(GenerationTask task, Pipeline<Void> pipeline) {
        pipeline.addLast(this::generate);
        return null;
    }

    public void generate(GenerationTask task, GeneratorControl control, Void nothing) {
        BlockBuffer buf = control.getBuffer();
        System.out.println("x:" + task.getX() + " y:" + task.getY() + " z:" + task.getZ());
        int[] ids = weltschmerz.getChunk((int)task.getX(), (int)task.getY(), (int)task.getZ(), DataConstants.CHUNK_MAX_BLOCKS);
        for(int i = 0; i < DataConstants.CHUNK_MAX_BLOCKS; i++){
            buf.write(registry.getForWorldId(ids[i]));
            buf.next();
        }
        index++;
        System.out.println(index);
    }
}