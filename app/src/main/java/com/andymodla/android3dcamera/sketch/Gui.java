package com.andymodla.android3dcamera.sketch;

import processing.core.PApplet;


// The GUI assumes the sketch screen is at (0,0)
public class Gui {
    static final boolean DEBUG = false;

    PApplet base;  // base sketch reference
    MainHorzMenuBar menuBar;

    // information zone touch coordinates
    // screen boundaries for click zone use
    float menuWidth; // menu width
    float menuHeight; // menu height
    float menuX;
    float menuY;

    Gui() {  // default constructor

    }

    void setup(PApplet base) {
        if (DEBUG) base.println("createGui()");
        this.base = base;
        menuWidth = base.width;
        menuHeight = base.height / 6;
        menuX = 0;
        menuY = 0;
        menuBar = new MainHorzMenuBar(base, menuX, menuY, menuWidth, menuHeight);

    }

    void displayMenuBar() {
        if (DEBUG) base.println("displayMenuBar()");
        menuBar.display();
    }

    int mousePressed(int x, int y) {
        return menuBar.mousePressed(x, y);
    }
}
