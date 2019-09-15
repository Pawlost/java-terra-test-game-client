package com.ritualsoftheold.testgame.client.generation;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.scene.*;
import com.jme3.texture.TextureArray;
import com.jme3.util.BufferUtils;

import com.ritualsoftheold.loader.ModelLoader3D;
import com.ritualsoftheold.terra.core.chunk.ChunkLArray;
import com.ritualsoftheold.terra.core.materials.Registry;
import com.ritualsoftheold.terra.core.materials.TerraObject;
import com.ritualsoftheold.terra.client.mesher.Face;
import com.ritualsoftheold.terra.client.mesher.GreedyMesher;
import jme3tools.optimize.GeometryBatchFactory;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;


public class TestGameMesher {
    private BlockingQueue<Spatial> geomCreateQueue;
    private BlockingQueue<String> geomDeleteQueue;
    private GreedyMesher greedyMesher;
    private ModelLoader3D modelLoader3D;
    private Registry reg;
    private HashMap<Integer, Integer> unsualMeshSize;
    private Material mat;

    public TestGameMesher(AssetManager manager, BlockingQueue<Spatial> geomCreateQueue,
                          BlockingQueue<String> geomDeleteQueue, TextureArray array, Registry reg) {
        this.geomCreateQueue = geomCreateQueue;
        this.geomDeleteQueue = geomDeleteQueue;
        this.reg = reg;
        greedyMesher = new GreedyMesher();
        modelLoader3D = new ModelLoader3D(manager);
        unsualMeshSize = new HashMap<>();
        mat = new Material(manager, "shaders/terra/voxel/TerraArray.j3md");
        mat.setTexture("ColorMap", array);
        mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
    }

    public void chunkLoaded(ChunkLArray chunk) {

        int verticeIndex = 0;

        HashMap<Integer, HashMap<Integer, Face>> sector = greedyMesher.cull(chunk);
        // Reset buffer to starting position

        if (sector.size() > 0) {
            int verticeSize = 0;
            int indexSize = 0;
            int texCoordSize = 0;
            int normalSize = 0;

            Integer[] keySet = new Integer[sector.keySet().size()];
            sector.keySet().toArray(keySet);

            for (Integer key : keySet) {
                if (key != 6) {
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
                if (key != 6) {
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

            Node node = new Node();

            Geometry geom = new Geometry("chunk:" + chunk.getPosX() + "," + chunk.getPosY() + "," + chunk.getPosZ(), mesh);
            geom.setMaterial(mat);
            geom.setLocalTranslation(chunk.getPosX(), chunk.getPosY(), chunk.getPosZ());
            geom.updateModelBound();

            node.attachChild(geom);

            HashMap<Integer, Face> side = sector.get(6);
            for (Integer i : side.keySet()) {
                Face face = side.get(i);
                Integer id = face.getObject().getWorldId();

                if (unsualMeshSize.get(id) == null) {
                    unsualMeshSize.put(id, 1);
                    TerraObject object = reg.getForWorldId(id);
                    int posZ = i / 4096;
                    int posY = (i - (4096 * posZ)) / 64;
                    int posX = i % 64;
                    object.position(
                            (posX * 0.25f) + chunk.getPosX() + (object.getMesh().getDefaultDistanceX() * 0.25f) / 2f,
                            (posY * 0.25f) + chunk.getPosY(),
                            (posZ * 0.25f) + chunk.getPosZ() + (object.getMesh().getDefaultDistanceZ() * 0.25f) / 2f);
                } else {
                    int s = unsualMeshSize.get(id);
                    s += 1;
                    unsualMeshSize.replace(id, s);
                }

                Integer[] currentKeys = new Integer[unsualMeshSize.keySet().size()];
                unsualMeshSize.keySet().toArray(currentKeys);
                for (Integer objectId : currentKeys) {
                    if (objectId != null) {
                        TerraObject object = reg.getForWorldId(objectId);
                        if (object.getMesh().getSize() == unsualMeshSize.get(objectId)) {
                            unsualMeshSize.remove(objectId);

                            Spatial asset = modelLoader3D.getMesh(object.getMesh().getAsset());

                            while (asset instanceof Node) {
                                asset = ((Node) asset).getChild(0);
                            }

                            Geometry assetGeom = (Geometry) asset;

                            FloatBuffer normalBuffer = FloatBuffer.allocate(6 * assetGeom.getTriangleCount());

                            for (int n = 0; n < normalBuffer.capacity(); n++) {
                                normalBuffer.put(0);
                            }

                            texCoordsBuffer = BufferUtils.createFloatBuffer(
                                    ((assetGeom.getMesh().getFloatBuffer(VertexBuffer.Type.TexCoord).capacity()
                                            / 2) * 3));
                            for (int t = 0; t < assetGeom.getMesh().getFloatBuffer(VertexBuffer.Type.TexCoord).capacity();
                                 t += 2) {
                                texCoordsBuffer.put(assetGeom.getMesh().getFloatBuffer(VertexBuffer.Type.TexCoord).get(t));
                                texCoordsBuffer.put(assetGeom.getMesh().getFloatBuffer(VertexBuffer.Type.TexCoord).get(t + 1));
                                texCoordsBuffer.put(object.getTexture().getPosition());
                            }

                            Mesh newMash = new Mesh();
                            newMash.setBuffer(assetGeom.getMesh().getBuffer(VertexBuffer.Type.Position));
                            newMash.setBuffer(assetGeom.getMesh().getBuffer(VertexBuffer.Type.Index));
                            newMash.setBuffer(VertexBuffer.Type.TexCoord, 3, texCoordsBuffer);
                            newMash.setBuffer(VertexBuffer.Type.Normal, 3, normalBuffer);

                            assetGeom.setMesh(newMash);
                            assetGeom.setLocalTranslation(object.getX(), object.getY(), object.getZ());
                            assetGeom.setMaterial(mat);
                            assetGeom.updateModelBound();
                            node.attachChild(assetGeom);
                        }
                    }
                }
            }
            side.clear();

            // Place geometry in queue for main thread
            geomCreateQueue.add(GeometryBatchFactory.optimize(node));
        }
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
