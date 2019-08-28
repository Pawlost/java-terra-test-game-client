package com.ritualsoftheold.testgame.client.network;

import com.ritualsoftheold.terra.core.chunk.ChunkLArray;

public interface Client {
    float getPosX();
    float getPosY();
    float getPosZ();

    void sendChunk(ChunkLArray chunk);
}
