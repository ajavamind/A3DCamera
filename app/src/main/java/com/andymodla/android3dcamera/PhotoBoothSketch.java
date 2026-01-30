package com.andymodla.android3dcamera;

import android.view.KeyEvent;

import processing.core.PApplet;
import processing.core.PImage;
import processing.opengl.PGL;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import processing.core.*;
import processing.opengl.*;

public class PhotoBoothSketch extends PApplet {

        private static final boolean DEBUG = true; //true;
        private static final boolean test = true; // test various alternate camera classes

        Camera camStereo;

        // image sensor size and display fixed for testing
// assumes same dimensions can be used for both front and back cameras for testing
//
        int CAMERA_WIDTH = 960;  // Samsung Galaxy S25 for test
        int CAMERA_HEIGHT = 720;
        int XBP_CAMERA_WIDTH = 1280;//4080;
        int XBP_CAMERA_HEIGHT = 960;//3072
        int cameraIdStereo = 3;
        int cameraIdLeft = 0;
        int cameraIdRight = 2;

        int cameraWidth = XBP_CAMERA_WIDTH;  // default
        int cameraHeight = XBP_CAMERA_HEIGHT;
        int FPS = 30; // camera frames per second
        int displayFPS = 30; // display frames per second

        // Parallax and vertical alignment adjustments in pixels for XBP
        public volatile int parallax = 114;
        public volatile int verticalAlignment = 8;
        public volatile boolean mirror = true;
        public volatile int brightness = -6;
        boolean initial = true;
        boolean anaglyph = false;
        boolean update = true;
        float AR = 1.33333333f;  // aspect ratio for Xreal Beam Pro camera image sensor
        int setupDone = 0;

        // Display frame
        int frameX = 176;  // 2400 pixel screen minus frameWidth
        int frameY = 0;
        int frameWidth = 2048;
        int frameHeight = 1080;

        public void settings() {
            // set size for XReal Beam Pro full display
            // draw canvas size and render using OpenGL
            fullScreen(P2D);
            //size(2048, 1080, P2D);  // actual dimensions of screen
        }

        public void setup() {
            if (DEBUG) PApplet.println("StereoCamera setup");
            orientation(LANDSCAPE);
            background(0);
            smooth();

            frameRate(displayFPS);

//            String manufacturer = android.os.Build.MANUFACTURER;
//            String modelName = android.os.Build.MODEL;
//            String deviceName = manufacturer + " " + modelName;
//            if (DEBUG) println("Device Manufacturer and Model: " + deviceName);
//
//            if (manufacturer.equals("XREAL") && modelName.equals("X4000")) {
//                cameraWidth = XBP_CAMERA_WIDTH;
//                cameraHeight = XBP_CAMERA_HEIGHT;
//                cameraIdLeft = 0;
//                cameraIdRight = 2;
//                brightness = 12; // override
//            } else {
//                cameraWidth = CAMERA_WIDTH;
//                cameraHeight = CAMERA_HEIGHT;
//                parallax = 0;  // override
//                verticalAlignment = 0;  // override
//                cameraIdLeft = 0;
//                cameraIdRight = 1;
//            }
//            if (DEBUG) println("cameraIdLeft: " + cameraIdLeft + " cameraIdRight: " + cameraIdRight);
//            camStereo = null; //new SCamera2(this, cameraIdStereo, cameraWidth, cameraHeight, FPS);  // stereo camera
//
//            setupDone = 1;
//            thread("startCameras");
            textSize(96);
            textAlign(CENTER, CENTER);
            text("3D Photo Booth", width / 2, height / 2);
            if (DEBUG) PApplet.println("StereoCamera setup done");
        }

        public void setCamera(Camera camera) {
            camStereo = camera;
        }

//        public void startCameras() {
//            if (setupDone == 1) {
//                if (DEBUG) println("StereoCamera startCameras()");
//                // Start capturing the images from the camera
//                boolean success = false;
//                if (camStereo != null) {
//                    //camStereo.start();
//                    success = true;
//                    //camStereo.brightnessValue = brightness;
//                    //camStereo.setCameraExposureCompensation(camStereo.brightnessValue);
//                }
//
//                if (success) {
//                    if (DEBUG) println("setup() Cameras started successfully");
//                } else {
//                    if (DEBUG) println("setup() Cameras failed to start");
//                    exit();
//                }
//                setupDone = 2;
//            }
//        }

//        public void start() {
//            if (DEBUG) println("StereoCamera start");
//            if (camStereo != null) camStereo.start();
//        }
//
//        public void pause() {
//            if (DEBUG) println("StereoCamera pause");
//            if (camStereo != null) camStereo.pause();
//        }
//
//        public void resume() {
//            if (DEBUG) println("StereoCamera onResume");
//            if (camStereo != null) {
//                if (!camStereo.isStarted()) {
//                    if (DEBUG) println("StereoCamera onResume() camStereo.resume()");
//                    camStereo.resume();
//                }
//            }
//        }
//
//        public void onDestroy() {
//            if (camStereo != null) camStereo.dispose();
//        }

        public void draw() {
            if (update) {
                background(0);  // clear screen for update
                update = false;
            }

            //if (camStereo != null && camStereo.isStarted() && camStereo.available) {
            if (camStereo != null  && camStereo.available) {
                camStereo.available = false;
                if (anaglyph) {
                    drawAnaglyph(camStereo.self, camStereo.self2);
                } else {
                    drawSBS(camStereo.self, camStereo.self2);
                }
            }
        }

        public void drawSBS(PImage imgLeft, PImage imgRight) {
            if (imgLeft == null || imgRight == null) return;

            PGL pgl;
            pgl = beginPGL();
            pgl.viewport(0, 0, width, height);
            pgl.colorMask(true, true, true, true);
            endPGL();

            if (mirror) {
                pushMatrix();
                translate(frameX+ (float) frameWidth / 4, frameY);
                scale(-1, 1); // Mirror horizontally
                image(imgLeft, (float) -frameWidth / 4, 180,
                        (float) frameWidth / 2, ((float) frameWidth/2) / AR); // Draw at adjusted position
                popMatrix();

                pushMatrix();
                translate( frameX + (float) frameWidth / 4, 0);
                scale(-1, 1); // Mirror horizontally
                image(imgRight,  -(float)frameWidth/2 - (float) frameWidth / 4, frameY+180 ,
                        (float) frameWidth/2, ((float) frameWidth/2) / AR); // Draw at adjusted position
                popMatrix();
            } else {
                //pushMatrix();
                //image(imgLeft, 0, 180, (float) width / 2, ((float) width / 2) / AR);
                //image(imgRight, (float) width / 2, 180, (float) width / 2, ((float) width / 2) / AR);
                image(imgLeft, frameX, frameY+180, (float) frameWidth / 2, ((float) frameWidth / 2) / AR);
                image(imgRight, (float) frameX+frameWidth / 2, frameY+180, (float) frameWidth / 2, ((float) frameWidth / 2) / AR);
                //popMatrix();
            }

        }

        public void drawAnaglyph(PImage imgLeft, PImage imgRight) {

            PGL pgl;
            pgl = beginPGL();
            pgl.viewport(0, 0, width, height);
            pgl.colorMask(true, false, false, true);
            pushMatrix();

            if (mirror) {
                translate(width/2 -parallax / 2, -verticalAlignment / 2);
                scale(-1, 1); // Mirror horizontally
                image(imgLeft, -width/2 + ((float) width - (float) height * AR) / 2, 0, (float) height * AR, height);
            } else {
                translate(-parallax / 2, -verticalAlignment / 2);
                image(imgLeft, ((float) width - (float) height * AR) / 2, 0, (float) height * AR, height);
            }
            popMatrix();
            endPGL();

            pgl = beginPGL();
            pgl.colorMask(false, true, true, true);
            pgl.viewport(0, 0, width, height);
            pushMatrix();
            if (mirror) {
                translate(width/2 + parallax / 2, verticalAlignment / 2);
                scale(-1, 1); // Mirror horizontally
                image(imgRight, -width/2 + ((float) width - (float) height * AR) / 2, 0, (float) height * AR, height);
            } else {
                translate(parallax / 2, verticalAlignment / 2);
                image(imgRight, ((float) width - (float) height * AR) / 2, 0, (float) height * AR, height);
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

//        // toggle display between SBS and anaglyph images
//        public void mousePressed() {
//            anaglyph = !anaglyph;
//            update = true;
//        }
//
//        public void keyPressed() {
//            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
//                return;
//            }
//            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
//                return;
//            }
//        }
//
//        static public void main(String[] passedArgs) {
//            String[] appletArgs = new String[]{"StereoCamera"};
//            if (passedArgs != null) {
//                PApplet.main(concat(appletArgs, passedArgs));
//            } else {
//                PApplet.main(appletArgs);
//            }
//        }
    }

