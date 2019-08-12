package com.ritualsoftheold.testgame.generation;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.scene.*;
import com.jme3.texture.TextureArray;
import com.jme3.util.BufferUtils;
import com.ritualsoftheold.loader.ModelLoader3D;
import com.ritualsoftheold.terra.core.gen.objects.LoadMarker;
import com.ritualsoftheold.terra.mesher.Face;
import com.ritualsoftheold.terra.mesher.GreedyMesher;
import com.ritualsoftheold.terra.offheap.chunk.ChunkLArray;
import com.ritualsoftheold.terra.offheap.world.WorldLoadListener;
import jme3tools.optimize.GeometryBatchFactory;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;


public class MeshListener implements WorldLoadListener {
    private BlockingQueue<Spatial> geomCreateQueue;
    private BlockingQueue<String>  geomDeleteQueue;
    private GreedyMesher greedyMesher;
    private ModelLoader3D modelLoader3D;
    private AssetManager assetManager;
    private TextureArray array;

    public MeshListener(AssetManager manager, BlockingQueue<Spatial> geomCreateQueue,
                        BlockingQueue<String>  geomDeleteQueue, TextureArray array) {
        this.array = array;
        this.geomCreateQueue = geomCreateQueue;
        this.geomDeleteQueue = geomDeleteQueue;
        this.assetManager =  manager;
        greedyMesher = new GreedyMesher();
        modelLoader3D = new ModelLoader3D(manager);
    }

    @Override
    public void octreeLoaded(long addr, long groupAddr, int id, float x, float y, float z, float scale, LoadMarker trigger) {
        // For now, just ignore octrees
    }

    @Override
    public void chunkLoaded(ChunkLArray chunk) {

        int verticeIndex = 0;

        HashMap<Integer, HashMap<Integer, Face>> sector = greedyMesher.cull(chunk);
        // Reset buffer to starting position

        if(sector.size() > 0) {
            int verticeSize = 0;
            int indexSize = 0;
            int texCoordSize = 0;
            int normalSize = 0;

            Integer[] keySet = new Integer[sector.keySet().size()];
            sector.keySet().toArray(keySet);

            for (Integer key : keySet) {
                if(key != 6) {
                    HashMap<Integer, Face> faces = sector.get(key);
                    Integer[] keys = new Integer[faces.keySet().size()];
                    faces.keySet().toArray(keys);
                    Arrays.sort(keys);
                    for (int i = keys.length - 1; i >= 0; i--) {
                        int index = keys[i];
                        greedyMesher.joinReversed(faces, index, key);
                    }

                    greedyMesher.setTextureCoords(faces.values(), key);
                    verticeSize += faces.values().size() * 12;
                    indexSize += faces.values().size() * 6;
                    texCoordSize += faces.values().size() * 12;
                    normalSize += faces.values().size() * 12;
                }
            }

            FloatBuffer verticeBuffer = BufferUtils.createFloatBuffer(verticeSize);
            IntBuffer indexBuffer = BufferUtils.createIntBuffer(indexSize);
            FloatBuffer texCoordsBuffer = BufferUtils.createFloatBuffer(texCoordSize);
            FloatBuffer normalsBuffer = BufferUtils.createFloatBuffer(normalSize);

            for (Integer key : keySet) {
                if(key != 6) {
                    Integer[] faceSet = new Integer[sector.get(key).keySet().size()];
                    sector.get(key).keySet().toArray(faceSet);

                    for (Integer faceKey : faceSet) {
                        Face completeFace = sector.get(key).get(faceKey);
                        verticeBuffer.put(BufferUtils.createFloatBuffer(completeFace.getVector3fs()));
                        indexBuffer.put(BufferUtils.createIntBuffer(greedyMesher.getIndexes(verticeIndex)));
                        texCoordsBuffer.put(BufferUtils.createFloatBuffer(completeFace.getTextureCoords()));
                        normalsBuffer.put(BufferUtils.createFloatBuffer(completeFace.getNormals()));
                        verticeIndex += 4;
                        sector.get(key).remove(faceKey);
                    }
                }
            }

            Mesh mesh = new Mesh();

            mesh.setBuffer(VertexBuffer.Type.Position, 3, verticeBuffer);

            mesh.setBuffer(VertexBuffer.Type.Index, 2, indexBuffer);

            mesh.setBuffer(VertexBuffer.Type.Normal, 3, normalsBuffer);

            mesh.setBuffer(VertexBuffer.Type.TexCoord, 3, texCoordsBuffer);

            mesh.updateBound();

            Geometry geom = new Geometry("chunk:" + chunk.x + "," + chunk.y + "," + chunk.z, mesh);
            geom.setCullHint(Spatial.CullHint.Never);
            geom.setLocalTranslation(0, 0, 0);
            Material mat = new Material(assetManager, "shaders/terra/voxel/TerraArray.j3md");
            mat.setTexture("ColorMap", array);
            geom.setMaterial(mat);

            Node node = new Node();
            node.attachChild(geom);

            HashMap<Integer, Face> side = sector.get(6);
            for(Integer i: side.keySet()){
                int posZ = i / 4096;
                int posY = (i - (4096 * posZ)) / 64;
                int posX = i % 64;

                float x = posX * 0.25f;
                float y = posY * 0.25f;
                float z = posZ * 0.25f;

                z += (0.25f/2f);
                y -= (0.25f/2f);
                x += (0.25f/2f);

                Face face = side.get(i);
                Spatial asset =  modelLoader3D.getMesh(face.getObject().getMesh().getAsset());
                asset.setLocalTranslation(x, y, z);
                asset.setCullHint(Spatial.CullHint.Never);

                mat = new Material(assetManager, "shaders/transparency/TransparencyShader.j3md");
                mat.setTexture("ColorMap", modelLoader3D.getTexture(face.getObject().getTexture().getAsset()));
                asset.setMaterial(mat);
                node.attachChild(asset);
            }
            side.clear();

            Spatial spatial = GeometryBatchFactory.optimize(node);
            spatial.setLocalTranslation(chunk.x, chunk.y, chunk.z);
            // Place geometry in queue for main thread
            geomCreateQueue.add(spatial);
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
