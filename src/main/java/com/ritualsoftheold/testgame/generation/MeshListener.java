package com.ritualsoftheold.testgame.generation;

import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.ritualsoftheold.terra.core.gen.objects.LoadMarker;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;
import com.ritualsoftheold.terra.offheap.world.WorldLoadListener;

import java.util.concurrent.BlockingQueue;


public class MeshListener implements WorldLoadListener {
    private Material mat;
    private BlockingQueue<Geometry> geomCreateQueue;
    private BlockingQueue<String>  geomDeleteQueue;

    public MeshListener(Material mat, BlockingQueue<Geometry> geomCreateQueue, BlockingQueue<String>  geomDeleteQueue) {
        this.mat = mat;
        this.geomCreateQueue = geomCreateQueue;
        this.geomDeleteQueue = geomDeleteQueue;
    }

    @Override
    public void octreeLoaded(long addr, long groupAddr, int id, float x, float y, float z, float scale, LoadMarker trigger) {
        // For now, just ignore octrees
    }

    @Override
    public void chunkUnloaded(OffheapChunk chunk) {

        float x = chunk.getX();
        float y = chunk.getY();
        float z = chunk.getZ();

        // Create geometry
        String name = "chunk:" + x + "," + y + "," + z;

        // Place geometry in queue for main thread
        geomDeleteQueue.add(name);
    }

    @Override
    public void chunkLoaded(OffheapChunk chunk) {

        Mesh mesh = JMEMesherWrapper.createMesh(chunk.getBuffer());

        mesh.updateBound();

        float x = chunk.getX();
        float y = chunk.getY();
        float z = chunk.getZ();

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
