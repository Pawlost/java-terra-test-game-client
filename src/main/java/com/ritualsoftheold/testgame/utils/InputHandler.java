package com.ritualsoftheold.testgame.utils;

import com.jme3.collision.CollisionResults;
import com.jme3.input.FlyByCamera;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.Ray;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.ritualsoftheold.testgame.utils.Picker;

public class InputHandler implements ActionListener {

    private Picker picker;
    private Node terrain;
    private boolean wireframe = false;
    private Material mat;
    private Camera cam;

    public InputHandler(InputManager inputManager, Picker picker, Node terrain, Material mat, Camera cam){

        this.mat = mat;
        this.picker = picker;
        this.terrain = terrain;
        this.cam = cam;

        inputManager.addMapping("RELOAD", new KeyTrigger(KeyInput.KEY_G));
        inputManager.addListener(this, "RELOAD");
        inputManager.addMapping("toggle wireframe", new KeyTrigger(KeyInput.KEY_T));
        inputManager.addListener(this, "toggle wireframe");

        inputManager.addMapping("Pick",
                new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(this, "Pick");

        inputManager.addMapping("Place",
                new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addListener(this, "Place");

        inputManager.addMapping("Change",
                new MouseButtonTrigger(MouseInput.BUTTON_MIDDLE));
        inputManager.addListener(this, "Change");

    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals("toggle wireframe") && !isPressed) {
            wireframe = !wireframe; // toggle boolean
            mat.getAdditionalRenderState().setWireframe(wireframe);
        }

        if (name.equals("Pick") && !isPressed) {
            // 1. Reset results list.
            CollisionResults results = new CollisionResults();
            // 2. Aim the ray from cam loc to cam direction.
            Ray ray = new Ray(cam.getLocation(), cam.getDirection());
            // 3. Collect intersections between Ray and Shootables in results list.
            // DO NOT check collision with the root node, or else ALL collisions will hit the
            // skybox! Always make a separate node for objects you want to collide with.
            terrain.collideWith(ray, results);
            // For each hit, we know distance, impact point, name of geometry.
            if(results.size() > 0) {
              /*  picker.prepare(results);
                picker.pick();
               */
            }
        }

        if (name.equals("Place") && !isPressed) {
            // 1. Reset results list.
            CollisionResults results = new CollisionResults();
            // 2. Aim the ray from cam loc to cam direction.
            Ray ray = new Ray(cam.getLocation(), cam.getDirection());
            // 3. Collect intersections between Ray and Shootables in results list.
            // DO NOT check collision with the root node, or else ALL collisions will hit the
            // skybox! Always make a separate node for objects you want to collide with.
            terrain.collideWith(ray, results);
            // For each hit, we know distance, impact point, name of geometry.
            if(results.size() > 0) {
                picker.prepare(results);
                picker.place();
            }
        }

        if (name.equals("Change") && !isPressed) {
            CollisionResults results = new CollisionResults();
            Ray ray = new Ray(cam.getLocation(), cam.getDirection());
            terrain.collideWith(ray, results);
            if(results.size() > 0) {
                picker.prepare(results);
                picker.changeMaterial();
            }
        }
    }
}
