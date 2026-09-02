package com.andymodla.a3dcamera.sketch.photobooth;


import processing.core.PApplet;
import processing.core.PImage;

public class StereoMenuKey extends MenuKey implements IGui {
//    float x, y, w, h; // location
//    float inset;
//    String text;
//    PImage img;
//    int keyColor;
//    int keyBackgroundColor;
//    int keyCode;
//    float fontSize;
//    boolean visible = false;
//    boolean highlight = false;
//    boolean active = true;
//    boolean corner = true;
//    boolean textOnly = false;
//    int value;
//    PApplet base;
    int parallaxShift = 0;

    /**
     * Constructor
     */
    StereoMenuKey() {
    }

    StereoMenuKey(PApplet base, int keyCode, String text, float fontSize, int keyColor, int keyBackgroundColor) {
        this.base = base;
        this.keyCode = keyCode;
        this.text = text;
        this.keyColor = keyColor;
        this.keyBackgroundColor = keyBackgroundColor;
        this.fontSize = fontSize;
        this.img = null;
    }

    StereoMenuKey(PApplet base, int keyCode, PImage img, int keyColor) {
        this.base = base;
        this.keyCode = keyCode;
        this.img = img;
        this.keyColor = keyColor;
    }

    void setStereoPosition(float x, float y, float w, float h, float inset) {
        this.x = x/2;
        this.y = y;
        this.w = w/2;
        this.h = h/2;
        this.inset = inset/2;
    }

    void stereoDraw() {
        float sx = x; // save configured x position
        draw();  // draw left side of display
        x = x + (float)(parallaxShift + XBP_DISPLAY_WIDTH/2);
        draw();  // draw right side of display
        // restore for left side of display
        x = sx;
    }

    /**
     * @param mx mouse x full screen coordinate
     * @param my mouse y full screen coordinate
     * @return boolean true if mouse in key area
     */
    boolean isPressed(int mx, int my) {
        mx = mx % XBP_DISPLAY_WIDTH; // convert mouse to left side of display
        boolean hit = false;
        if (my >= y && my <= (y + h)
                && mx >= x && mx <= (x + w)) {
            hit = true;
        }
        return hit;
    }


}