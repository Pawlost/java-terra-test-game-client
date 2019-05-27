package com.ritualsoftheold.testgame.generation;

import com.jme3.font.BitmapText;
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
    private BitmapText text;

    public WorldGeneratorInterface<Void> setup(MaterialRegistry reg, TerraModule mod, BitmapText text) {
        this.text = text;
        setup(reg, mod);
        return this;
    }

    @Override
    public void setup(MaterialRegistry reg, TerraModule mod) {
        this.reg = reg;
        weltschmerz = new Weltschmerz();
        weltschmerz.changeSector();
        text.setText("Sector, name: " + weltschmerz.getSectorName()+ ", position x: " +
                weltschmerz.getPostionX() + " , z:" + weltschmerz.getPostionZ());
        weltschmerz.setMaterialID(reg.getMaterial(mod, "grass").getWorldId(), reg.getMaterial(mod,"dirt").getWorldId());
    }

    @Override
    public Void initialize(GenerationTask task, Pipeline<Void> pipeline) {
        pipeline.addLast(this::generate);
        return null;
    }

    public void generate(GenerationTask task, GeneratorControl control, Void nothing) {
        BlockBuffer buf = control.getBuffer();
        weltschmerz.setChunk((int)task.getX(), (int)task.getZ());
        task.setY((float) weltschmerz.getY());
        for (int i = 0; i < DataConstants.CHUNK_MAX_BLOCKS; i++) {
            int x = i % 64;
            int z = i / 4096;
            int y = (i - 4096 * z) / 64;
            buf.write(reg.getForWorldId(weltschmerz.generateVoxel(x, y, z)));
            buf.next();
        }
    }
}