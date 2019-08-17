package com.ritualsoftheold.testgame.generation;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.*;
import com.jme3.texture.Texture;
import com.jme3.texture.TextureArray;
import com.jme3.util.BufferUtils;
import com.ritualsoftheold.loader.ModelLoader3D;
import com.ritualsoftheold.terra.core.gen.objects.LoadMarker;
import com.ritualsoftheold.terra.core.material.Registry;
import com.ritualsoftheold.terra.core.material.TerraObject;
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
    private Registry reg;
    private HashMap<Integer, Integer> unsualMeshSize;

    public MeshListener(AssetManager manager, BlockingQueue<Spatial> geomCreateQueue,
                        BlockingQueue<String>  geomDeleteQueue, TextureArray array, Registry reg) {
        this.array = array;
        this.geomCreateQueue = geomCreateQueue;
        this.geomDeleteQueue = geomDeleteQueue;
        this.assetManager =  manager;
        this.reg = reg;
        greedyMesher = new GreedyMesher();
        modelLoader3D = new ModelLoader3D(manager);
        unsualMeshSize = new HashMap<>();
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

            mesh.updateBound();

            Geometry geom = new Geometry("chunk:" + chunk.x + "," + chunk.y + "," + chunk.z, mesh);
            geom.setCullHint(Spatial.CullHint.Never);
            Material mat = new Material(assetManager, "shaders/terra/voxel/TerraArray.j3md");
            mat.setTexture("ColorMap", array);
            geom.setMaterial(mat);

            geom.setLocalTranslation(chunk.x, chunk. y, chunk.z);

            Node node = new Node();
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
                            (posX*0.25f) + chunk.x + (object.getMesh().getDefaultDistanceX() * 0.25f)/2f,
                            (posY*0.25f) + chunk.y,
                            (posZ*0.25f) + chunk.z + (object.getMesh().getDefaultDistanceZ() * 0.25f)/2f);
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
                            asset.setLocalTranslation(object.getX(), object.getY(), object.getZ());
                            asset.setCullHint(Spatial.CullHint.Never);

                            if (object.getTexture().hasTexture()) {
                                Texture texture = modelLoader3D.getTexture(object.getTexture().getAsset());
                                mat = new Material(assetManager, "shaders/transparency/TransparencyShader.j3md");
                                mat.setTexture("ColorMap", texture);
                                mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
                            } else {
                                mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
                                mat.setColor("Color", ColorRGBA.White);
                            }

                            asset.setMaterial(mat);
                            node.attachChild(asset);
                        }
                    }
                }
            }
            side.clear();

            // Place geometry in queue for main thread
            geomCreateQueue.add(node);
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
