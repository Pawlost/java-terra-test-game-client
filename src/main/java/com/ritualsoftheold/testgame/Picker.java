package com.ritualsoftheold.testgame;

import com.jme3.collision.CollisionResults;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.ritualsoftheold.terra.core.material.TerraMaterial;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.data.BufferWithFormat;
import com.ritualsoftheold.terra.offheap.io.ChunkLoader;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;
import com.ritualsoftheold.terra.offheap.world.OffheapLoadMarker;
import com.ritualsoftheold.terra.offheap.world.OffheapWorld;

public class Picker {
    private ChunkLoader chunkLoader;
    private OffheapWorld world;
    private OffheapChunk chunk;
    private OffheapLoadMarker loadMarker;
    private Vector3f collision;
    private Vector3f normals;
    private float x;
    private float y;
    private float z;

    public Picker (ChunkLoader loader, OffheapWorld world){
        chunkLoader = loader;
        this.world = world;
    }

    public void prepare(CollisionResults results) {
        Geometry chunkMesh = results.getClosestCollision().getGeometry();
        x = chunkMesh.getWorldMatrix().m03;
        y = chunkMesh.getWorldMatrix().m13;
        z = chunkMesh.getWorldMatrix().m23;
        loadMarker = world.getLoadMarker(x, y, z);
        chunk = chunkLoader.getChunk(x, y, z, loadMarker);
        collision = results.getClosestCollision().getContactPoint();
        normals = results.getClosestCollision().getContactNormal();
    }

    public void pick(TerraMaterial material) {
        int x = (int) ((collision.x - chunk.getX())/0.25);
        int y = (int) ((collision.y - chunk.getY())/0.25);
        int z = (int) ((collision.z - chunk.getZ())/0.25);

        if(normals.x > 0){
            x--;
        }

        if(normals.y > 0){
            y--;
        }

        if(normals.z > 0){
            z--;
        }

        y *= 64;
        z *= 4096;

        BufferWithFormat chunkBuffer = chunk.getBuffer();
        chunkBuffer.seek(x + y + z);
        chunkBuffer.write(material);
        chunkLoader.loadChunk(this.x, this.y, this.z, chunk, loadMarker);
    }

    public void place(TerraMaterial material) {
        int x = (int) ((collision.x - chunk.getX())/0.25);
        int y = (int) ((collision.y - chunk.getY())/0.25);
        int z = (int) ((collision.z - chunk.getZ())/0.25);

        if (normals.x < 0){
            x--;
        }

        if (normals.y < 0){
            y--;
        }

        if(normals.z < 0){
            z--;
        }

        y *= 64;
        z *= 4096;

        BufferWithFormat chunkBuffer = chunk.getBuffer();
        if(x+y+z < DataConstants.CHUNK_MAX_BLOCKS) {
            chunkBuffer.seek(x + y + z);
            if(chunkBuffer.read().getWorldId() == 1) {
                chunkBuffer.write(material);
                chunkLoader.loadChunk(this.x, this.y, this.z, chunk, loadMarker);
            }
        }
    }
}
