package com.ritualsoftheold.testgame.client.network;

import com.ritualsoftheold.terra.core.octrees.OctreeBase;

import java.util.ArrayList;

public interface Server {
    ArrayList<OctreeBase> init(Client client);
    void update();
}
