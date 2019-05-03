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
        weltschmerz.setSector(0, (int)task.getY(), 0);
        for(int i = 0; i < DataConstants.CHUNK_MAX_BLOCKS; i++){
            int x = i%64;
            int z = i/4096;
            int y = (i - 4096 * z) / 64;
            buf.write(registry.getForWorldId(weltschmerz.generateVoxel(x, y , z)));
            buf.next();
        }
    }
}