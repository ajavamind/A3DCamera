package com.andymodla.a3dcamera.sketch.photobooth;

/*
    Variables used to position and size GUI elements
 */
public class GuiDimension {
    float x, y, w, h; // location
    float inset;
    float fontSize;
    float xOffset;;

    /*
    Constructor
     */
    GuiDimension() {
    }

    void setDimension(float x, float y, float w, float h, float inset, float fontSize, float xOffset) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.inset = inset;
        this.fontSize  = fontSize;
        this.xOffset = xOffset;
    }

}