package com.ritualsoftheold.testgame.generation;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import com.ritualsoftheold.terra.core.material.MaterialRegistry;
import com.ritualsoftheold.terra.mesher.Face;
import com.ritualsoftheold.terra.mesher.GreedyMesher;
import com.ritualsoftheold.terra.offheap.chunk.ChunkLArray;
import xerial.larray.LByteArray;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.HashMap;

public class JMEMesherWrapper {

    //Moving all values to MeshContainer
    public static Mesh createMesh(ChunkLArray array) {

        GreedyMesher greedyMesher = new GreedyMesher();
        int verticeIndex = 0;
        HashMap<Integer, HashMap<Integer, Face>> sector = greedyMesher.cull(array);
        // Reset buffer to starting position

        if(sector.size() > 0) {
            int verticeSize = 0;
            int indexSize = 0;
            int texCoordSize = 0;
            int normalSize = 0;

            Integer[] keySet = new Integer[sector.keySet().size()];
            sector.keySet().toArray(keySet);

            for (Integer key : keySet) {
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

            FloatBuffer verticeBuffer = BufferUtils.createFloatBuffer(verticeSize);
            IntBuffer indexBuffer = BufferUtils.createIntBuffer(indexSize);
            FloatBuffer texCoordsBuffer = BufferUtils.createFloatBuffer(texCoordSize);
            FloatBuffer normalsBuffer = BufferUtils.createFloatBuffer(normalSize);

            for (Integer key : keySet) {

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

                sector.remove(key);
            }

            Mesh mesh = new Mesh();

            mesh.setBuffer(VertexBuffer.Type.Position, 3, verticeBuffer);

            mesh.setBuffer(VertexBuffer.Type.Index, 2, indexBuffer);

            mesh.setBuffer(VertexBuffer.Type.Normal, 3, normalsBuffer);

            mesh.setBuffer(VertexBuffer.Type.TexCoord, 3, texCoordsBuffer);
            return mesh;
        }
        return null;
    }
}
