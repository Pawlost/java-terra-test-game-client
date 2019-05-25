package com.ritualsoftheold.testgame.utils;

import com.jme3.collision.CollisionResults;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.ritualsoftheold.terra.core.material.TerraMaterial;
import com.ritualsoftheold.terra.offheap.data.BufferWithFormat;
import com.ritualsoftheold.terra.offheap.io.ChunkLoader;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;
import com.ritualsoftheold.terra.offheap.world.OffheapLoadMarker;

public class Picker {
    private ChunkLoader chunkLoader;
    private OffheapChunk chunk;
    private OffheapLoadMarker player;
    private Vector3f collision;
    private Vector3f normals;
    private TerraMaterial primaryMaterial;
    private TerraMaterial emptyMaterial;
    private float x;
    private float y;
    private float z;

    public Picker (ChunkLoader loader, OffheapLoadMarker player, TerraMaterial primaryMaterial, TerraMaterial emptyMaterial){
        chunkLoader = loader;
        this.player = player;
        this.primaryMaterial = primaryMaterial;
        this.emptyMaterial = emptyMaterial;
    }

    public void prepare(CollisionResults results) {
        Geometry chunkMesh = results.getClosestCollision().getGeometry();
        x = chunkMesh.getWorldMatrix().m03;
        y = chunkMesh.getWorldMatrix().m13;
        z = chunkMesh.getWorldMatrix().m23;
        chunk = chunkLoader.getChunk(x, y, z, player);
        collision = results.getClosestCollision().getContactPoint();
        normals = results.getClosestCollision().getContactNormal();
    }

    public void pick() {
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
        chunkBuffer.write(emptyMaterial);
        chunkLoader.loadChunk(this.x, this.y, this.z, chunk, player);
    }

    public void place() {
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

        BufferWithFormat chunkBuffer = chunk.getBuffer();
        if(x < 64 && y < 64 && z < 64) {
            y *= 64;
            z *= 4096;
            chunkBuffer.seek(x + y + z);
            if(chunkBuffer.read().getWorldId() == 1) {
                chunkBuffer.write(primaryMaterial);
                chunkLoader.loadChunk(this.x, this.y, this.z, chunk, player);
            }
        }
    }

    public void changeMaterial(){
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
        primaryMaterial = chunkBuffer.read();
    }
}
