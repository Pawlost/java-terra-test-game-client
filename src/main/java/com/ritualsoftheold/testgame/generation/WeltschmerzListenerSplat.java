package com.ritualsoftheold.testgame.generation;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import com.ritualsoftheold.terra.core.gen.objects.LoadMarker;
import com.ritualsoftheold.terra.mesher.GreedyMesher;
import com.ritualsoftheold.terra.mesher.SplatMesher;
import com.ritualsoftheold.terra.mesher.VoxelMesher;
import com.ritualsoftheold.terra.mesher.resource.MeshContainer;
import com.ritualsoftheold.terra.mesher.resource.TextureManager;
import com.ritualsoftheold.terra.offheap.Pointer;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;
import com.ritualsoftheold.terra.offheap.world.WorldLoadListener;

import java.util.concurrent.BlockingQueue;

public class WeltschmerzListenerSplat implements WorldLoadListener {
    private VoxelMesher mesher;
    private TextureManager texManager;
    private Material mat;
    private BlockingQueue<Geometry> geomCreateQueue;

    public WeltschmerzListenerSplat(TextureManager texManager, Material mat, BlockingQueue<Geometry> geomCreateQueue) {
        mesher = new SplatMesher();
        this.texManager = texManager;
        this.mat = mat;
        this.geomCreateQueue = geomCreateQueue;
    }

    @Override
    public void octreeLoaded(long addr, long groupAddr, int id, float x, float y, float z, float scale, LoadMarker trigger) {
        // For now, just ignore octrees
    }

    @Override
    public void chunkLoaded(OffheapChunk chunk, float x, float y, float z, LoadMarker trigger) {
        //System.out.println("Loaded chunk: " + chunk.memoryAddress());
        MeshContainer container = new MeshContainer();
        mesher.chunk(chunk.getBuffer(), texManager, container);

        // Create mesh
        Mesh mesh = new Mesh();
        mesh.setMode(Mesh.Mode.Points);

        //Set coordinates
        Vector3f[] vector3fs = new Vector3f[container.getVertice().toArray().length];
        container.getVertice().toArray(vector3fs);
        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vector3fs));
        //Connects triangles
        ColorRGBA[] colorRGBAs = new ColorRGBA[container.getColors().toArray().length];
        container.getColors().toArray(colorRGBAs);
        mesh.setBuffer(VertexBuffer.Type.Color,4,BufferUtils.createFloatBuffer(colorRGBAs));

        //Update mesh
        mesh.updateBound();

        // Create geometry
        Geometry geom = new Geometry("chunk:" + x + "," + y + "," + z, mesh);

        // Create material
        geom.setMaterial(mat);

        //Set chunk position in world
        geom.setShadowMode(RenderQueue.ShadowMode.Cast);
        geom.setLocalTranslation(x*4f, y*4f, z*4f);
        geom.setCullHint(Spatial.CullHint.Never);

        container.clear();

        // Place geometry in queue for main thread
        geomCreateQueue.add(geom);
    }
}
