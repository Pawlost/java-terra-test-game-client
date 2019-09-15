package com.ritualsoftheold.testgame.client.generation;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import com.ritualsoftheold.terra.core.chunk.ChunkLArray;
import com.ritualsoftheold.terra.client.mesher.SplatMesher;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

public class SplatListener {
    private SplatMesher mesher;
    private Material mat;
    private BlockingQueue<Geometry> geomCreateQueue;
    private BlockingQueue<String> geomDeleteQueue;

    public SplatListener(Material mat, BlockingQueue<Geometry> geomCreateQueue) {
        mesher = new SplatMesher();
        this.mat = mat;
        this.geomCreateQueue = geomCreateQueue;
    }

    public void chunkLoaded(ChunkLArray chunk) {
        //System.out.println("Loaded chunk: " + chunk.memoryAddress());
        ArrayList<Vector3f> vector3fsArray = new ArrayList<>();
        ArrayList<ColorRGBA> colorsArray = new ArrayList<>();

        mesher.chunk(chunk, vector3fsArray, colorsArray);

        // Create mesh
        Mesh mesh = new Mesh();
        mesh.setMode(Mesh.Mode.Points);

        //Set coordinates
        Vector3f[] vector3fs = new Vector3f[vector3fsArray.size()];
        vector3fsArray.toArray(vector3fs);
        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vector3fs));
        //Connects triangles
        ColorRGBA[] colorRGBAs = new ColorRGBA[colorsArray.size()];
        colorsArray.toArray(colorRGBAs);
        mesh.setBuffer(VertexBuffer.Type.Color,4,BufferUtils.createFloatBuffer(colorRGBAs));

        //Update mesh
        mesh.updateBound();

        // Create geometry
        Geometry geom = new Geometry("chunk:" + chunk.getPosX() + "," + chunk.getPosY() + "," + chunk.getPosZ(), mesh);

        // Create material
        geom.setMaterial(mat);

        //Set chunk position in world
        geom.setShadowMode(RenderQueue.ShadowMode.Cast);
        geom.setLocalTranslation(chunk.getPosX(), chunk.getPosY(), chunk.getPosX());
        geom.setCullHint(Spatial.CullHint.Never);

        // Place geometry in queue for main thread
        geomCreateQueue.add(geom);
    }

    public void chunkUnloaded(ChunkLArray chunk) {
        float x = chunk.getPosX();
        float y = chunk.getPosY();
        float z = chunk.getPosZ();

        // Create geometry
        String name = "chunk:" + x + "," + y + "," + z;

        // Place geometry in queue for main thread
        geomDeleteQueue.add(name);
    }
}
