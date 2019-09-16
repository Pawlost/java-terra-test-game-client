package com.ritualsoftheold.testgame.materials;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.post.Filter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;

public class BarrelDistortion extends Filter {
    private float horizontalFOV;
    private float strength;
    private float cylindricalRatio;
    private float height;
    private Vector2f screenSize;
    private float aspect;

    private Material mat;


    public BarrelDistortion(){
        super("Barrel Distortion");
    }

    private float focusDistance = 50f;
    private float focusRange = 10f;
    private float blurScale = 1f;
    private float blurThreshold = 0.2f;
    @Override
    protected void initFilter(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h) {
        var cam = vp.getCamera();
        System.out.println("W: "+w+" H:"+h);
        horizontalFOV = 114f;
        strength = 1.0f;
        cylindricalRatio = 2.0f;

        screenSize = new Vector2f(w,h);
        aspect = w/h;
        height = FastMath.tan((FastMath.DEG_TO_RAD * horizontalFOV)/2.0f)/aspect;
        cam.setFrustumPerspective(horizontalFOV,aspect,0.001f,500f);
//        height = cam.getFrustumTop();
        mat = new Material(manager,"Shaders/BarrelDistortion.j3md");
        mat.setFloat("strength",strength);
        mat.setFloat("height",height);
        mat.setFloat("cylindricalRatio",cylindricalRatio);
        mat.setFloat("aspect",aspect);
//        material = new Material(manager, "Common/MatDefs/Post/DepthOfField.j3md");
//        material.setFloat("FocusDistance", focusDistance);
//        material.setFloat("FocusRange", focusRange);
//        material.setFloat("BlurThreshold", blurThreshold);
//        material.setBoolean("DebugUnfocus", false);

//        var xScale = 1.0f / w;
//        var yScale = 1.0f / h;

//        material.setFloat("XScale", blurScale * xScale);
//        material.setFloat("YScale", blurScale * yScale);

    }

    @Override
    protected Material getMaterial() {
//        return material;
        return mat;
    }
}
