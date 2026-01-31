package com.andymodla.android3dcamera;

/**
 * The Photo Booth Processing sketch for the Graphic user interface
 *
 */

import android.view.KeyEvent;

import processing.core.PApplet;
import processing.core.PImage;
import processing.opengl.PGL;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PhotoBoothSketch extends PApplet {
    private static boolean DEBUG = true;

    Camera camStereo;  // The stereo camera used with the device
    Parameters parameters; // Application parameters

    int XBP_CAMERA_WIDTH = 1280;
    int XBP_CAMERA_HEIGHT = 960;

    int cameraWidth = XBP_CAMERA_WIDTH;  // default
    int cameraHeight = XBP_CAMERA_HEIGHT;
    int FPS = 30; // camera frames per second
    int displayFPS = 30; // display frames per second

    // Parallax and vertical alignment adjustments in pixels for XBP
    public volatile int parallax = 240;
    public volatile int verticalAlignment = 8;
    public volatile boolean mirror = false;
    public volatile int brightness = -6;
    volatile boolean anaglyph = false;
    volatile boolean update = true;
    volatile boolean zoom = false;
    int offsetX = 0;
    int offsetY = 0;
    float zoomPercent = 50f/100f;

    float AR = 1.33333333f;  // aspect ratio for Xreal Beam Pro camera image sensor

    // Display frame inside full screen
    int frameX = 176;  // 2400 pixel screen minus frameWidth
    int frameY = 0;
    int frameWidth = 2048;
    int frameHeight = 1080;

    public void settings() {
        // set size for XReal Beam Pro full display
        // draw canvas size and render using OpenGL
        fullScreen(P2D);
        //size(2048, 1080, P2D);  // actual dimensions of XReal BP screen
    }

    public void setup() {
        if (DEBUG) PApplet.println("PhotoBoothSketch setup");
        orientation(LANDSCAPE);
        background(0);
        smooth();
        frameRate(displayFPS);

        textSize(96);
        textAlign(CENTER, CENTER);
        text("3D Photo Booth", width / 2, height / 2);
        if (DEBUG) PApplet.println("StereoCamera setup done");
    }

    public void setCamera(Camera camera, Parameters parameters) {
        camStereo = camera;
        this.parameters = parameters;
    }

    public void update() {
        update = true;
    }

    public void toggleAnaglyph() {
        anaglyph = !anaglyph;
        update = true;
    }

    void toggleMirror() {
        mirror = !mirror;
        update = true;
    }

    void toggleZoom() {
        zoom = !zoom;
        update = true;
    }
    public void setParallax(int parallax) {
        this.parallax = parallax;
        update = true;
    }

    public void setVerticalAlignment(int verticalAlignment) {
        this.verticalAlignment = verticalAlignment;
        update = true;
    }

    public void draw() {
        if (update) {
            background(0);  // clear screen for draw update
//            parallax = parameters.getParallaxOffset();
//            verticalAlignment = parameters.getVerticalOffset();

            update = false;
        }

        if (camStereo != null && camStereo.available) {
            camStereo.available = false;
            if (anaglyph) {
                drawAnaglyph(camStereo.self, camStereo.self2);
            } else {
                drawSBS(camStereo.self, camStereo.self2);
            }
            if (DEBUG) {
                textSize(48);
                fill(color(255,255,128)); //fill(128);
                textAlign(LEFT);
                text("parallax = "+(parallax) + " mirror = " + mirror + " zoom = " + zoom, 50, height -96);
                text("vertical = "+(verticalAlignment), 50, height -48);
            }

        }

    }

    public void drawSBS(PImage imgLeft, PImage imgRight) {
        if (imgLeft == null || imgRight == null) return;

        PGL pgl;  // Processing Open GL library
        pgl = beginPGL();
        pgl.viewport(0, 0, width, height);
        pgl.colorMask(true, true, true, true);
        endPGL();

        if (mirror) {
            pushMatrix();
            translate(frameX + (float) frameWidth / 4, frameY);
            scale(-1, 1); // Mirror horizontally
            image(imgLeft, (float) -frameWidth / 4, 180,
                    (float) frameWidth / 2, ((float) frameWidth / 2) / AR); // Draw at adjusted position
            popMatrix();

            pushMatrix();
            translate(frameX + (float) frameWidth / 4, 0);
            scale(-1, 1); // Mirror horizontally
            image(imgRight, -(float) frameWidth / 2 - (float) frameWidth / 4, frameY + 180,
                    (float) frameWidth / 2, ((float) frameWidth / 2) / AR); // Draw at adjusted position
            popMatrix();
        } else {
            //pushMatrix();
            //image(imgLeft, 0, 180, (float) width / 2, ((float) width / 2) / AR);
            //image(imgRight, (float) width / 2, 180, (float) width / 2, ((float) width / 2) / AR);
            image(imgLeft, frameX, frameY + 180, (float) frameWidth / 2, ((float) frameWidth / 2) / AR);
            image(imgRight, (float) frameX + frameWidth / 2, frameY + 180, (float) frameWidth / 2, ((float) frameWidth / 2) / AR);
            //popMatrix();
        }

    }

    public void drawAnaglyph(PImage imgLeft, PImage imgRight) {
        PGL pgl;  // Processing Open GL library
        pgl = beginPGL();
        pgl.viewport(0, 0, width, height);
        pgl.colorMask(true, false, false, true);
        pushMatrix();
        float offsetX= 0;
        float offsetY = 0;
        if (mirror) {
            translate(width / 2 - parallax / 2, -verticalAlignment / 2);
            scale(-1, 1); // Mirror - flip horizontally
            if (zoom) {
                scale((1.0f + zoomPercent), 1.0f + zoomPercent);
                offsetX = ((float)width/2)*zoomPercent - (float)width/4;
                offsetY = ((float)height/2)*zoomPercent;
            }
            image(imgRight, -width / 2 - offsetX + ((float) width - (float) height * AR) / 2, -offsetY, (float) height * AR, height);
        } else {
            translate(-parallax / 2, -verticalAlignment / 2);
            if (zoom) {
                scale(1.0f + zoomPercent, 1.0f + zoomPercent);
                offsetX = ((float)width/2)*zoomPercent;
                offsetY = ((float)height/2)*zoomPercent;
            }
            image(imgLeft, ((float) width -offsetX - (float) height * AR) / 2, - offsetY, (float) height * AR, height);
        }
        popMatrix();
        endPGL();
        offsetX = 0;
        offsetY = 0;

        pgl = beginPGL();
        pgl.colorMask(false, true, true, true);
        pgl.viewport(0, 0, width, height);
        pushMatrix();
        if (mirror) {
            translate(width / 2 + parallax / 2, verticalAlignment / 2);
            scale(-1, 1); // Mirror - flip horizontally
            if (zoom) {
                scale((1.0f + zoomPercent), 1.0f + zoomPercent);
                offsetX = ((float)width/2)*zoomPercent - (float)width/4;
                offsetY = ((float)height/2)*zoomPercent;
            }
            image(imgLeft, -width / 2 - offsetX + ((float) width - (float) height * AR) / 2, -offsetY, (float) height * AR, height);
        } else {
            translate(parallax / 2, verticalAlignment / 2);
            if (zoom) {
                scale(1.0f + zoomPercent, 1.0f + zoomPercent);
                offsetX = ((float)width/2)*zoomPercent;
                offsetY = ((float)height/2)*zoomPercent;
            }
            image(imgRight, ((float) width -offsetX - (float) height * AR) / 2, - offsetY, (float) height * AR, height);
        }
        popMatrix();
        endPGL();

        // used to draw over anaglyph image for hiding overlay areas outside anaglyph image
        // need to change colorMask back before filling with rectangles on edges
        pgl = beginPGL();
        pgl.colorMask(true, true, true, true);
        pgl.viewport(0, 0, width, height);
        endPGL();

        fill(0);
        rect(0, 0, width, verticalAlignment);
        rect(0, height - verticalAlignment, width, verticalAlignment);
        rect(0, 0, 2 * parallax, height);
        rect(width - 2 * parallax, 0, 2 * parallax, height);
    }

    public void keyPressed() {
        if (key == ' ') {
            toggleMirror();
        } else if (key == 'a') {
            toggleAnaglyph();
        } else if (key == '+') {
            setParallax(parallax + 10);
        } else if (key == '=') {
            setParallax(parallax - 10);
        } else if (key == '_') {
            setVerticalAlignment(verticalAlignment + 1);
        } else if (key == '-') {
            setVerticalAlignment(verticalAlignment - 1);
        } else if (key == '.') {
            toggleMirror();
        } else if (key == '?') {
            DEBUG = !DEBUG;
        } else if (key == 'z') {
            toggleZoom();
        } else if (key == 'v') {
            setVerticalAlignment(verticalAlignment + 10);
        }

    }
}

