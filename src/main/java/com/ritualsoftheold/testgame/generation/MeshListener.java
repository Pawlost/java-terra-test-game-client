package com.ritualsoftheold.testgame.generation;

import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.ritualsoftheold.terra.core.gen.objects.LoadMarker;
import com.ritualsoftheold.terra.core.material.MaterialRegistry;
import com.ritualsoftheold.terra.offheap.chunk.ChunkLArray;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;
import com.ritualsoftheold.terra.offheap.world.WorldLoadListener;

import java.util.concurrent.BlockingQueue;


public class MeshListener implements WorldLoadListener {
    private Material mat;
    private MaterialRegistry reg;
    private BlockingQueue<Geometry> geomCreateQueue;
    private BlockingQueue<String>  geomDeleteQueue;

    public MeshListener(Material mat, MaterialRegistry reg, BlockingQueue<Geometry> geomCreateQueue, BlockingQueue<String>  geomDeleteQueue) {
        this.mat = mat;
        this.geomCreateQueue = geomCreateQueue;
        this.geomDeleteQueue = geomDeleteQueue;
        this.reg = reg;
    }

    @Override
    public void octreeLoaded(long addr, long groupAddr, int id, float x, float y, float z, float scale, LoadMarker trigger) {
        // For now, just ignore octrees
    }

    @Override
    public void chunkLoaded(ChunkLArray chunk) {

        Mesh mesh = JMEMesherWrapper.createMesh(chunk);

        if(mesh != null) {
            mesh.updateBound();

            float x = chunk.x;
            float y = chunk.y;
            float z = chunk.z;

            // Create geometry
            Geometry geom = new Geometry("chunk:" + x + "," + y + "," + z, mesh);

            // Create material
            geom.setMaterial(mat);

            //Set chunk position in world
            geom.setLocalTranslation(x, y, z);
            geom.setCullHint(Spatial.CullHint.Never);

            // Place geometry in queue for main thread
            geomCreateQueue.add(geom);
        }
    }

    @Override
    public void chunkUnloaded(ChunkLArray chunk) {

        float x = chunk.x;
        float y = chunk.y;
        float z = chunk.z;

        // Create geometry
        String name = "chunk:" + x + "," + y + "," + z;

        // Place geometry in queue for main thread
        geomDeleteQueue.add(name);
    }
}
