package com.ritualsoftheold.testgame.client.utils;

import com.jme3.collision.CollisionResults;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.ritualsoftheold.terra.core.materials.TerraObject;

public class Picker {
    private Vector3f collision;
    private Vector3f normals;
    private TerraObject primaryMaterial;
    private TerraObject emptyMaterial;
    private Node node;
    private Spatial custom;

    public Picker (Node node){
        this.node = node;
    }

    public void prepare(CollisionResults results) {
        Geometry chunkMesh = results.getClosestCollision().getGeometry();
        collision = results.getClosestCollision().getContactPoint();
        normals = results.getClosestCollision().getContactNormal();
    }

    public void pickChunk() {
       /* int x = (int) ((collision.x - Math.abs(chunk.getX()))/0.25);
        int y = (int) ((collision.y - Math.abs(chunk.getY()))/0.25);
        int z = (int) ((collision.z - Math.abs(chunk.getZ()))/0.25);

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
        chunkLoader.loadChunk(chunk);

        */
    }

    public void placeChunk() {
       /* int x = (int) ((collision.x - Math.abs(chunk.getX()))/0.25);
        int y = (int) ((collision.y - Math.abs(chunk.getY()))/0.25);
        int z = (int) ((collision.z - Math.abs(chunk.getZ()))/0.25);

        if (normals.x < 0){
            x--;
        }

        if (normals.y < 0){
            y--;
        }

        if(normals.z < 0){
            z--;
        }

        if(x < 64 && y < 64 && z < 64) {
            y *= 64;
            z *= 4096;

        }*/
    }

    public void placeGeometry(){
        int x = (int)(collision.x/0.25f);
        int y = (int)(collision.y/0.25f);
        int z = (int)(collision.z/0.25f);
        if (normals.x < 0){
            x--;
        }

        if (normals.y < 0){
            y--;
        }

        if(normals.z < 0){
            z--;
        }

        custom.setLocalTranslation(0.25f * x, 0.25f * y, 0.25f * z);
        System.out.println("HERE");
        node.attachChild(custom.clone());
    }

    public void setTerraMaterial(TerraObject terraObject, boolean empty){
        if(empty){
            emptyMaterial = terraObject;
        }else {
            primaryMaterial = terraObject;
        }
    }

    public void changeMaterial(){
        /*int x = (int) ((collision.x - Math.abs(chunk.getX()))/0.25);
        int y = (int) ((collision.y - Math.abs(chunk.getY()))/0.25);
        int z = (int) ((collision.z - Math.abs(chunk.getZ()))/0.25);


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

         */
    }

    public void setGeometry(Spatial custom){
        this.custom = custom;
    }
}
