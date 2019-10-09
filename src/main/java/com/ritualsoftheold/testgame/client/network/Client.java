package com.ritualsoftheold.testgame.client.network;

import com.ritualsoftheold.terra.core.chunk.ChunkLArray;
import com.ritualsoftheold.terra.core.markers.Marker;

public interface Client {
    float getPosX();
    float getPosY();
    float getPosZ();

    void sendChunk(ChunkLArray chunk);

    void sendOctree(Marker octree);
}
