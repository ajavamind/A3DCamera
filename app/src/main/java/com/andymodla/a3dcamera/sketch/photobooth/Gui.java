package com.andymodla.a3dcamera.sketch.photobooth;

import processing.core.PApplet;


// The GUI assumes the sketch screen is at (0,0) top left corner of the display
public class Gui {
    static final boolean DEBUG = true;

    PApplet pApplet;  // parent PApplet sketch reference for drawing
    HorzMenuBar menuBar;

    // information zone touch coordinates
    // screen boundaries for click zone use
    float menuWidth; // menu width
    float menuHeight; // menu height
    float menuX;
    float menuY;

    Gui() {  // default constructor

    }

    void setup(PApplet pApplet) {
        if (DEBUG) pApplet.println("createGui()");
        this.pApplet = pApplet;
        menuWidth = ((PhotoBooth)pApplet).XBP_DISPLAY_FRAME_WIDTH;
        menuHeight = 156;//pApplet.height / 6 - 20;
        menuX = ((PhotoBooth)pApplet).frameX;
        menuY = 0;
        menuBar = new HorzMenuBar(pApplet, menuX, menuY, menuWidth, menuHeight);

    }

    public HorzMenuBar getMenuBar() {
        return menuBar;
    }

    void displayMenuBar() {
        //if (DEBUG) pApplet.println("displayMenuBar() menuX="+menuX+" menuY="+menuY+" menuWidth="+menuWidth+" menuHeight="+menuHeight);
        menuBar.display();
    }


    int mousePressed(int x, int y) {
        return menuBar.mousePressed(x, y);
    }
}
