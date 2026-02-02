package com.andymodla.android3dcamera.sketch;

/**
 * The Photo Booth Processing sketch for the Graphic user interface
 *
 */

import android.view.KeyEvent;

import com.andymodla.android3dcamera.Camera;
import com.andymodla.android3dcamera.Parameters;

import processing.core.PApplet;
import processing.core.PImage;
import processing.event.MouseEvent;
import processing.opengl.PGL;
import android.view.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PhotoBoothSketch extends PApplet {
    private static boolean DEBUG = true;
    private static boolean testMode = false;

    int yellow = color(255, 255, 128);
    int black = 0;
    int white = color(255);
    int gray = color(128);

    Camera camStereo;  // The stereo camera used with the device
    Parameters parameters; // Application parameters

    int XBP_CAMERA_WIDTH = 1280;
    int XBP_CAMERA_HEIGHT = 960;

    int cameraWidth = XBP_CAMERA_WIDTH;  // default
    int cameraHeight = XBP_CAMERA_HEIGHT;
    int displayFPS = 30; // display frames per second

    // Parallax and vertical alignment adjustments in pixels for XBP
    public volatile int parallax = 32;
    public volatile int verticalAlignment = 0;
    public volatile boolean mirror = false;
    public volatile int brightness = -6;
    volatile boolean anaglyph = false;
    volatile boolean update = true;
    volatile boolean zoom = true;
    volatile boolean blankScreen = false;
    volatile int lastKeyCode;
    volatile int lastKey;

    float[] magnifyScale = {1.0f, 1.1f, 1.2f, 1.3f, 1.4f, 1.5f, 1.6f, 1.7f, 1.8f, 1.9f, 2.0f, 2.1f,
            2.2f, 2.3f, 2.4f, 2.5f, 2.6f, 2.7f, 2.8f, 2.9f, 3.0f};
    int magnifyIndex = 0;

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
        background(black);
        smooth();
        frameRate(displayFPS);

        textSize(96);
        textAlign(CENTER, CENTER);
        text("3D Photo Booth", (float) width / 2, (float) height / 2);
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

    public void toggleBlankScreen() {
        blankScreen = !blankScreen;
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
            background(black);  // clear screen for draw update
            update = false;
        }
        if (blankScreen) {
            return;
        }

        if (camStereo != null && camStereo.available) {
            camStereo.available = false;
            PImage imgLeft = camStereo.leftImage;
            PImage imgRight = camStereo.rightImage;
            if (imgLeft != null && imgRight != null) {
                if (anaglyph) {
                    drawAnaglyph(imgLeft, imgRight);
                } else {
                    drawSBS(imgLeft, imgRight);
                }
            }

            if (DEBUG && testMode) {
                textSize(48);
                fill(yellow);
                textAlign(LEFT);
                text("parallax = " + (parallax) + " mirror = " + mirror + " zoom = " + zoom, 50, height - 96);
                text("vertical = " + (verticalAlignment) +" magnify = " + magnifyScale[magnifyIndex], 50, height - 48);
            }

        }

    }

    public void drawSBS(PImage imgLeft, PImage imgRight) {

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
            pushMatrix();
            image(imgLeft, frameX, frameY + 180, (float) frameWidth / 2, ((float) frameWidth / 2) / AR);
            image(imgRight, (float) frameX + (float)frameWidth / 2, frameY + 180, (float) frameWidth / 2, ((float) frameWidth / 2) / AR);
            popMatrix();
        }
        drawGrid(false);
    }

    public void drawAnaglyph(PImage imgLeft, PImage imgRight) {
        float offsetX = 0;
        float offsetY = 0;
        float anaglyphX = 0;
        float anaglyphXM = 0;
        float anaglyphW = 0;
        float fillX = 0;

        PGL pgl;  // Processing Open GL library
        pgl = beginPGL();
        pgl.viewport(0, 0, width, height);
        pgl.colorMask(true, false, false, true);  // Red channel only
        pushMatrix();
        if (mirror) {
            translate(-(float)parallax / 2, -(float)verticalAlignment / 2);
            scale(-1, 1); // Mirror - flip horizontally
            if (zoom) {
                scale(magnifyScale[magnifyIndex], magnifyScale[magnifyIndex]);
                offsetX = (width * (1 - 1 / magnifyScale[magnifyIndex])) / 2;
                offsetY = (height * (1 - 1 / magnifyScale[magnifyIndex])) / 2;
            }
            anaglyphXM = ((float) -width + 2 * offsetX - (float) height * AR) / 2;
            anaglyphW = (float) height * AR;
            fillX = anaglyphXM;
            image(imgRight, anaglyphXM, -offsetY, anaglyphW, height);
        } else {
            translate(-(float)parallax / 2, -(float)verticalAlignment / 2);
            if (zoom) {
                scale(magnifyScale[magnifyIndex], magnifyScale[magnifyIndex]);
                offsetX = (width * (1 - 1 / magnifyScale[magnifyIndex])) / 2;
                offsetY = (height * (1 - 1 / magnifyScale[magnifyIndex])) / 2;
            }
            anaglyphW = (float) height * AR;
            anaglyphX = ((float) width - 2 * offsetX - anaglyphW) / 2;
            fillX = anaglyphX;
            image(imgLeft, anaglyphX, -offsetY, anaglyphW, height);
        }
        popMatrix();
        endPGL();
        offsetX = 0;
        offsetY = 0;

        pgl = beginPGL();
        pgl.colorMask(false, true, true, true);  // Blue and Green channels only
        pgl.viewport(0, 0, width, height);
        pushMatrix();
        if (mirror) {
            translate((float)parallax / 2, (float)verticalAlignment / 2);
            scale(-1, 1); // Mirror - flip horizontally
            if (zoom) {
                scale(magnifyScale[magnifyIndex], magnifyScale[magnifyIndex]);
                offsetX = (width * (1 - 1 / magnifyScale[magnifyIndex])) / 2;
                offsetY = (height * (1 - 1 / magnifyScale[magnifyIndex])) / 2;
            }
            anaglyphW = (float) height * AR;
            anaglyphXM = ((float) -width + 2 * offsetX - anaglyphW) / 2;
            image(imgLeft, anaglyphXM, -offsetY, anaglyphW, height);
        } else {
            translate((float)parallax / 2, (float)verticalAlignment / 2);
            if (zoom) {
                scale(magnifyScale[magnifyIndex], magnifyScale[magnifyIndex]);
                offsetX = (width * (1 - 1 / magnifyScale[magnifyIndex])) / 2;
                offsetY = (height * (1 - 1 / magnifyScale[magnifyIndex])) / 2;
            }
            anaglyphW = (float) height * AR;
            anaglyphX = ((float) width - 2 * offsetX - anaglyphW) / 2;
            image(imgRight, anaglyphX, -offsetY, anaglyphW, height);
        }
        popMatrix();
        endPGL();

        // used to draw over anaglyph image for hiding overlay areas outside anaglyph image
        // needed to change colorMask back before filling with rectangles on edges
        pgl = beginPGL();
        pgl.colorMask(true, true, true, true);  // Restore color channels
        pgl.viewport(0, 0, width, height);
        endPGL();

        // cover anaglyph alignment edges
        fill(black);
        if (verticalAlignment != 0) {
            rect(0, 0, width, abs(verticalAlignment));  // top of image
            rect(0, height - abs(verticalAlignment), width, abs(verticalAlignment));  // bottom of image
        }
//        if (testMode) {
//            fill(yellow);
//            if (parallax != 0) {
//                if (mirror) {
//                    rect(fillX - offsetX, 0, abs(parallax), height); // left side
//                    rect(fillX - offsetX + anaglyphW - abs(parallax), 0, abs(parallax), height); // right side
//
//                } else {
//                    rect(fillX - offsetX, 0, abs(parallax), height); // left side
//                    rect(fillX + -offsetX + anaglyphW - abs(parallax), 0, abs(parallax), height); // right side
//                }
//            }
//        }
        drawGrid(true);
    }

    //    void drawGridLine(boolean full) {
//        strokeWeight(2);
//        if (full) {
//            line(0, height / 2, width, height / 2);
//            line(width / 2, 0, width / 2, height);
//        } else {
//            line(0, frameHeight / 2, width, frameHeight / 2);
//            line(frameX + frameWidth / 4, 0, frameX + frameWidth / 4, frameHeight);
//            line(frameX + 3 * frameWidth / 4, 0, frameX + 3 * frameWidth / 4, frameHeight);
//        }
//    }

    void drawGrid(boolean full) {
        if (!testMode) return;
        fill(yellow);
        int s = 2;
        if (full) {
            // Horizontal line (center of canvas)
            rect(0, height / 2 - s / 2, width, s);
            // Vertical line (center of canvas)
            rect(width / 2 - s / 2, 0, s, height);
        } else {
            // Horizontal line (center of frame)
            rect(0, frameHeight / 2 - s / 2, width, s);
            // First vertical line (1/4 of frame width)
            rect(frameX + frameWidth / 4 - s / 2, 0, s, frameHeight);
            // Second vertical line (3/4 of frame width)
            rect(frameX + 3 * frameWidth / 4 - s / 2, 0, s, frameHeight);
        }
    }

    // debug keys
    public void keyPressed() {
        lastKey = key;
        lastKeyCode = keyCode;
        processKeyCode(lastKeyCode, lastKey);
    }

    public void processKeyCode(int lastKeyCode, int lastKey) {
        switch (lastKeyCode) {
            case KeyEvent.KEYCODE_A:
                toggleAnaglyph();
                break;
            case KeyEvent.KEYCODE_B:
                toggleBlankScreen();
                break;
            case KeyEvent.KEYCODE_LEFT_BRACKET:
                if (magnifyIndex > 0) {
                    magnifyIndex--;
                    update = true;
                    zoom = true;
                }
                if (DEBUG)
                    println("magnifyScale = " + magnifyScale[magnifyIndex] + " magnifyIndex");
                break;
            case KeyEvent.KEYCODE_RIGHT_BRACKET:
                if (magnifyIndex < magnifyScale.length - 1) {
                    magnifyIndex++;
                    update = true;
                    zoom = true;
                }
                if (DEBUG)
                    println("magnifyScale = " + magnifyScale[magnifyIndex] + " magnifyIndex");
                break;
            case KeyEvent.KEYCODE_Q:
            case KeyEvent.KEYCODE_FORWARD:  // 125 forward media button on mouse: mirror toggle
                toggleMirror();
                break;
            case KeyEvent.KEYCODE_Z:
                toggleZoom();
                break;
            case KeyEvent.KEYCODE_SPACE:
                testMode = !testMode;
                update = true;
                break;
            case KeyEvent.KEYCODE_PERIOD:
                DEBUG = !DEBUG;
                break;
            case KeyEvent.KEYCODE_MINUS:
                setParallax(parallax - 4);
                if (DEBUG) println("parallax = " + parallax);
                break;
            case KeyEvent.KEYCODE_PLUS:
            case KeyEvent.KEYCODE_EQUALS:
                setParallax(parallax + 4);
                if (DEBUG) println("parallax = " + parallax);
                break;

//            case KeyEvent.:
//                setVerticalAlignment(verticalAlignment + 1);
//                break;
            default:
                break;
        }





    }
}

