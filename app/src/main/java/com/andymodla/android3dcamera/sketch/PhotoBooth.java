package com.andymodla.android3dcamera.sketch;

/**
 * The Photo Booth Processing sketch for the Graphic user interface
 *
 */

import android.view.KeyEvent;
import android.graphics.Bitmap;
import com.andymodla.android3dcamera.DisplayMode;
import com.andymodla.android3dcamera.camera.Camera3D;
import com.andymodla.android3dcamera.Parameters;

import processing.core.PApplet;
import processing.core.PImage;
import processing.event.MouseEvent;
import processing.opengl.PGL;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PhotoBooth extends PApplet {
    private static boolean DEBUG = true;
    private static boolean testMode = false;

    int yellow = color(255, 255, 128);
    int black = 0;
    int white = color(255);
    int gray = color(128);

    Camera3D camStereo;  // The stereo camera used with the device
    Parameters parameters; // Application parameters
    PImage imgLeft;
    PImage imgRight;
    PImage splashLeft;
    PImage splashRight;

    int XBP_CAMERA_WIDTH = 1280;
    int XBP_CAMERA_HEIGHT = 960;

    int cameraWidth = XBP_CAMERA_WIDTH;  // default
    int cameraHeight = XBP_CAMERA_HEIGHT;
    int XBP_DISPLAY_WIDTH = 2400;
    int XBP_DISPLAY_HEIGHT = 1080;

    int displayFPS = 60; // display frames per second

    // Parallax and vertical alignment adjustments in pixels for XBP
    public volatile int parallax = 32;
    public volatile int verticalAlignment = 0;
    public volatile boolean mirror = false;
    public volatile int brightness = -6;
    DisplayMode displayMode = DisplayMode.SBS;
    volatile boolean update = false;
    volatile boolean zoom = true;
    volatile boolean blankScreen = false;
    volatile boolean liveView = true;

    String countdown = "";  // default ignore null string
    volatile int lastKeyCode;
    volatile int lastKey;

    float[] magnifyScale = {1.0f, 1.1f, 1.2f, 1.3f, 1.4f, 1.5f, 1.6f, 1.7f, 1.8f, 1.9f, 2.0f, 2.1f,
            2.2f, 2.3f, 2.4f, 2.5f, 2.6f, 2.7f, 2.8f, 2.9f, 3.0f};
    int magnifyIndex = 0;

    float AR = 1.33333333f;  // aspect ratio for Xreal Beam Pro camera image sensor

    // Display frame inside full screen AR 4:3
    int frameWidth = 2048;
    int frameHeight = 1080;
    int frameX = (XBP_DISPLAY_WIDTH-frameWidth) / 2;  // 2400 pixel screen minus frameWidth/2
    int frameY = (XBP_DISPLAY_HEIGHT-frameHeight) / 2;

    public void settings() {
        // set size for XReal Beam Pro full display
        // draw canvas size and render using OpenGL
        //fullScreen(P2D);
        size(2400, 1080, P2D);
    }

    public void setup() {
        if (DEBUG) PApplet.println("PhotoBoothSketch setup");
        orientation(LANDSCAPE);
        background(black);
        smooth();
        frameRate(displayFPS);
        liveView = true;

        splashLeft = loadImage("FlowerPot_l.JPG");
        splashRight = loadImage("FlowerPot_r.JPG");
        if(DEBUG) println("splashLeft width=" + splashLeft.width + " height=" + splashLeft.height);
        if(DEBUG) println("splashRight width=" + splashRight.width + " height=" + splashRight.height);
        float ar = (float) splashLeft.width / (float) splashLeft.height;

        image(splashLeft, frameX, 180, frameWidth/2-parallax, (frameWidth/2-parallax)/ar);
        image(splashRight, frameX + frameWidth/2+parallax, 180, frameWidth/2-parallax, (frameWidth/2-parallax)/ar);

        textSize(72);
        textAlign(CENTER, CENTER);
        fill(yellow);
        text("3D Photo Booth", (float) width / 4, (float) height -200);
        text("3D Photo Booth", (float) 3*width / 4 , (float) height -200);
        if (DEBUG) PApplet.println("StereoCamera setup done");
        reviewSetup();
    }

    public void setCamera(Camera3D camera) {
        camStereo = camera;
        this.parameters = camera.getParameters();
    }

    public void setLiveView(boolean liveView) {
        this.liveView = liveView;
        update = true;
    }

    public void update() {
        update = true;
    }

    public void setDisplayMode(DisplayMode mode) {
        displayMode = mode;
        if (mode == DisplayMode.SBS) {
            println( "Display SBS");
        } else if (mode == DisplayMode.ANAGLYPH) {
            println( "Display ANAGLYPH");
        }   else if (mode == DisplayMode.LEFT) {
            println( "Display LEFT");
        } else if (mode == DisplayMode.RIGHT) {
            println("Display RIGHT");
        }
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

    public void setCountdown(String countdown) {
        this.countdown = countdown;
        update = true;
    }

    void showCountdown(boolean sbs) {
        if (countdown.isEmpty()) {
            return;
        }
        textSize(288);
        fill(yellow);
        textAlign(CENTER, CENTER);

        if (sbs) {
            text(countdown, frameX +frameWidth / 4 , height / 2 );
            text(countdown, frameX +3*frameWidth / 4 -40, height / 2 );
        } else {
            text(countdown, width / 2, height / 2);
        }
    }

    public void draw() {
        if (update) {
            background(black);  // clear screen for draw update
            //if (!liveView) loadImageFileList();
            update = false;
        }
        if (blankScreen) {
            return;
        }
        if (camStereo == null) {
            return;
        }

        if (liveView) {
            drawLiveView();
        } else {
            drawReview();
        }
        if (magnifyScale[magnifyIndex] > 1.0f) {
            textSize(48);
            fill(yellow);
            textAlign(CENTER);
            text("Zoom "+magnifyScale[magnifyIndex], width/2, height-48);
        }
        // camera and review mode display test mode for debug
        if (DEBUG && testMode) {
            textSize(48);
            fill(yellow);
            textAlign(LEFT);
            text("parallax = " + (parallax) + " mirror = " + mirror + " zoom = " + zoom + " w = "+imgLeft.width + " h ="+ imgLeft.height, 50, height - 96);
            text("vertical = " + (verticalAlignment) +" magnify = " + magnifyScale[magnifyIndex], 50, height - 48);
        }

    }

    private void drawLiveView() {
        if (camStereo.available) {
            camStereo.available = false;
            imgLeft = camStereo.leftImage;
            imgRight = camStereo.rightImage;
            AR = (float) imgLeft.width / (float) imgLeft.height;
        }
        if (imgLeft != null && imgRight != null) {
            if (displayMode == DisplayMode.ANAGLYPH) {
                drawAnaglyph(imgLeft, imgRight);
            } else if (displayMode == DisplayMode.SBS) {
                drawSBS(imgLeft, imgRight);
            } else if (displayMode == DisplayMode.LEFT) {
                drawPhoto(imgLeft);
            } else if (displayMode == DisplayMode.RIGHT) {
                drawPhoto(imgRight);
             }
            showCountdown(false);
        }
    }

    public void drawSBS(PImage imgLeft, PImage imgRight) {
        float offsetX = 0;
        float offsetY = 0;

        // Calculate base image dimensions - each image gets half the frame width
        float imgWidth = (float) frameWidth / 2;
        float imgHeight = imgWidth / AR;

        // Center vertically within frame
        float baseVerticalOffset = frameY + (frameHeight - imgHeight) / 2;

        // Calculate zoom offsets - these keep the zoomed image centered in its half-frame
        if (zoom) {
            offsetX = (imgWidth * (1 - 1 / magnifyScale[magnifyIndex])) / 2;
            offsetY = (imgHeight * (1 - 1 / magnifyScale[magnifyIndex])) / 2;
        }

        // LEFT IMAGE (left half of frame)
        pushMatrix();
        // Clip to left half - use imgHeight for vertical bounds
        clip(frameX, baseVerticalOffset, imgWidth, imgHeight);

        translate(frameX, baseVerticalOffset);
        translate(-(float)parallax / 2, -(float)verticalAlignment / 2);

        if (mirror) {
            translate(imgWidth, 0);
            scale(-1, 1);
        }

        if (zoom) {
            scale(magnifyScale[magnifyIndex], magnifyScale[magnifyIndex]);
        }

        image(imgLeft, -offsetX, -offsetY, imgWidth, imgHeight);
        noClip();
        popMatrix();

        // RIGHT IMAGE (right half of frame)
        pushMatrix();
        // Clip to right half - use imgHeight for vertical bounds
        clip(frameX + imgWidth, baseVerticalOffset, imgWidth, imgHeight);

        translate(frameX + imgWidth, baseVerticalOffset);
        translate((float)parallax / 2, (float)verticalAlignment / 2);

        if (mirror) {
            translate(imgWidth, 0);
            scale(-1, 1);
        }

        if (zoom) {
            scale(magnifyScale[magnifyIndex], magnifyScale[magnifyIndex]);
        }

        image(imgRight, -offsetX, -offsetY, imgWidth, imgHeight);
        noClip();
        popMatrix();

        drawGrid(false);
    }

    public void drawAnaglyph(PImage imgLeft, PImage imgRight) {
        float offsetX = 0;
        float offsetY = 0;
        float anaglyphW = 0;

        // Calculate the display area dimensions
        anaglyphW = (float) height * AR;
        float displayX = ((float) width - anaglyphW) / 2;

        // Calculate zoom offsets
        if (zoom) {
            offsetX = (anaglyphW * (1 - 1 / magnifyScale[magnifyIndex])) / 2;
            offsetY = (height * (1 - 1 / magnifyScale[magnifyIndex])) / 2;
        }

        PGL pgl;  // Processing Open GL library
        pgl = beginPGL();
        pgl.viewport(0, 0, width, height);
        pgl.colorMask(true, false, false, true);  // Red channel only

        // Add clipping to constrain the image to the display area
        clip(displayX, 0, anaglyphW, height);

        pushMatrix();
        translate(displayX, 0);
        translate(-(float)parallax / 2, -(float)verticalAlignment / 2);

        if (mirror) {
            translate(anaglyphW, 0);
            scale(-1, 1); // Mirror - flip horizontally
        }

        if (zoom) {
            scale(magnifyScale[magnifyIndex], magnifyScale[magnifyIndex]);
        }

        if (mirror) {
            image(imgRight, -offsetX, -offsetY, anaglyphW, height);
        } else {
            image(imgLeft, -offsetX, -offsetY, anaglyphW, height);
        }
        popMatrix();
        noClip();
        endPGL();

        pgl = beginPGL();
        pgl.colorMask(false, true, true, true);  // Blue and Green channels only
        pgl.viewport(0, 0, width, height);

        // Add clipping for second layer too
        clip(displayX, 0, anaglyphW, height);

        pushMatrix();
        translate(displayX, 0);
        translate((float)parallax / 2, (float)verticalAlignment / 2);

        if (mirror) {
            translate(anaglyphW, 0);
            scale(-1, 1); // Mirror - flip horizontally
        }

        if (zoom) {
            scale(magnifyScale[magnifyIndex], magnifyScale[magnifyIndex]);
        }

        if (mirror) {
            image(imgLeft, -offsetX, -offsetY, anaglyphW, height);
        } else {
            image(imgRight, -offsetX, -offsetY, anaglyphW, height);
        }
        popMatrix();
        noClip();
        endPGL();

        // for drawing over anaglyph image
        // change colorMask back before filling with rectangles on edges
        pgl = beginPGL();
        pgl.colorMask(true, true, true, true);  // Restore color channels
        pgl.viewport(0, 0, width, height);
        endPGL();

        drawGrid(true);
    }

    public void drawPhoto(PImage img) {
        float offsetX = 0;
        float offsetY = 0;
        float anaglyphW = 0;

        // Calculate the display area dimensions
        anaglyphW = (float) height * AR;
        float displayX = ((float) width - anaglyphW) / 2;

        // Calculate zoom offsets
        if (zoom) {
            offsetX = (anaglyphW * (1 - 1 / magnifyScale[magnifyIndex])) / 2;
            offsetY = (height * (1 - 1 / magnifyScale[magnifyIndex])) / 2;
        }

        // Add clipping to constrain the image to the display area
        clip(displayX, 0, anaglyphW, height);

        pushMatrix();
        translate(displayX, 0);
        translate(-(float)parallax / 2, -(float)verticalAlignment / 2);

        if (mirror) {
            translate(anaglyphW, 0);
            scale(-1, 1); // Mirror - flip horizontally
        }

        if (zoom) {
            scale(magnifyScale[magnifyIndex], magnifyScale[magnifyIndex]);
        }

        image(img, -offsetX, -offsetY, anaglyphW, height);

        popMatrix();
        noClip();

        drawGrid(true);
    }

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
            //case KeyEvent.KEYCODE_A:
            //    toggleDisplayMode();
            //    break;
            case KeyEvent.KEYCODE_B:
                toggleBlankScreen();
                break;
            case KeyEvent.KEYCODE_LEFT_BRACKET:
                if (magnifyIndex > 0) {
                    magnifyIndex--;
                    update = true;
                    zoom = true;
                }
                //if (DEBUG) println("magnifyScale = " + magnifyScale[magnifyIndex] + " magnifyIndex");
                break;
            case KeyEvent.KEYCODE_RIGHT_BRACKET:
                if (magnifyIndex < magnifyScale.length - 1) {
                    magnifyIndex++;
                    update = true;
                    zoom = true;
                }
                //if (DEBUG) println("magnifyScale = " + magnifyScale[magnifyIndex] + " magnifyIndex");
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
            default:
                if (!liveView) {
                    keyPressedReview(lastKeyCode, lastKey);
                }
                break;
        }
    }

// // For reference not used
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

    /**=================================================================================================
     * Review Code
     */
    // Constants
    final String FOLDER_PATH = "/DCIM/A3DCamera/LR";
    final int SLIDESHOW_DELAY = 2500; // 2 seconds

    // Review Global variables
    ArrayList<String> leftImageFiles;
    ArrayList<String> rightImageFiles;
    int currentIndex = 0;
    boolean slideshowActive = false;
    int lastSlideshowTime = 0;

    PImage currentLeft;
    PImage currentRight;
    boolean imagesLoaded = false;

    public void review(DisplayMode mode) {
        displayMode = mode;
        liveView = false;
        update = true;
    }

    void reviewSetup() {
        // Load image file list
        loadImageFileList();

        // Load the first image if available
        if (!leftImageFiles.isEmpty()) {
            loadCurrentImage();
        }
    }

    void drawReview() {
        if (imagesLoaded && currentLeft != null && currentRight != null) {
            imagesLoaded = false;
            if (displayMode == DisplayMode.SBS) {
                drawSBS(currentLeft, currentRight);
            } else if (displayMode == DisplayMode.ANAGLYPH) {
                drawAnaglyph(currentLeft, currentRight);
            } else if (displayMode == DisplayMode.LEFT) {
                drawPhoto(currentLeft);
            } else if (displayMode == DisplayMode.RIGHT) {
                drawPhoto(currentRight);
            }

        } else {
            // Display message if no images
            fill(255);
            textAlign(CENTER, CENTER);
            textSize(96);
            text("No images found", width/2, height/2);
        }

        // Handle slideshow
        if (slideshowActive && millis() - lastSlideshowTime > SLIDESHOW_DELAY) {
            nextImage();
            lastSlideshowTime = millis();
        }
    }

    /**
     * Load image file list from external storage
     *
     * @return true if images loaded, false if failed to load any image
     */

    public boolean loadImageFileList() {
        leftImageFiles = new ArrayList<String>();
        rightImageFiles = new ArrayList<String>();

        // Get the external storage directory
        File externalStorage = Environment.getExternalStorageDirectory();
        File lrFolder = new File(externalStorage, FOLDER_PATH);

        if (!lrFolder.exists() || !lrFolder.isDirectory()) {
            println("Folder not found: " + lrFolder.getAbsolutePath());
            return false;
        }

        // Temporary lists to collect all left and right files
        ArrayList<String> tempLeftFiles = new ArrayList<String>();
        ArrayList<String> tempRightFiles = new ArrayList<String>();

        // Get all JPG/JPEG files
        File[] files = lrFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String name = file.getName().toLowerCase();
                    String fullPath = file.getAbsolutePath();

                    if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                        // Check if it's a left or right image
                        String nameWithoutExt = name.substring(0, name.lastIndexOf('.'));

                        if (nameWithoutExt.endsWith("_l")) {
                            tempLeftFiles.add(fullPath);
                        } else if (nameWithoutExt.endsWith("_r")) {
                            tempRightFiles.add(fullPath);
                        }
                    }
                }
            }
        }

        // Sort both lists in ascending order
        Collections.sort(tempLeftFiles);
        Collections.sort(tempRightFiles);

        // Match pairs: for each left file, find corresponding right file
        for (String leftPath : tempLeftFiles) {
            String leftFilename = new File(leftPath).getName();
            String leftBase = getBaseName(leftFilename);

            // Look for matching right file
            for (String rightPath : tempRightFiles) {
                String rightFilename = new File(rightPath).getName();
                String rightBase = getBaseName(rightFilename);

                if (leftBase.equals(rightBase)) {
                    // Found a matching pair
                    leftImageFiles.add(leftPath);
                    rightImageFiles.add(rightPath);
                    break;
                }
            }
        }

        println("Found " + leftImageFiles.size() + " matching image pairs");
        return true;
    }

    // Helper function to get base filename without _l/_r suffix and extension
    String getBaseName(String filename) {
        // Remove extension
        String nameWithoutExt = filename.substring(0, filename.lastIndexOf('.'));

        // Remove _l or _r suffix
        if (nameWithoutExt.endsWith("_l")) {
            return nameWithoutExt.substring(0, nameWithoutExt.length() - 2);
        } else if (nameWithoutExt.endsWith("_r")) {
            return nameWithoutExt.substring(0, nameWithoutExt.length() - 2);
        }

        return nameWithoutExt;
    }

//    public void loadCurrentImage() {
//        if (imageFiles.size() == 0 || currentIndex < 0 || currentIndex >= imageFiles.size()) {
//            imagesLoaded = false;
//            return;
//        }
//
//        String filepath = imageFiles.get(currentIndex);
//        println("Loading: " + filepath);
//
//        PImage img = loadImage(filepath);
//        if (img == null) {
//            println("Failed to load image");
//            imagesLoaded = false;
//            return;
//        }
//
//        // Check if image should be split
//        boolean shouldSplit = false;
//        String filename = new File(filepath).getName();
//
//        // Check aspect ratio > 2
//        float aspectRatio = (float)img.width / (float)img.height;
//        if (aspectRatio > 2.0) {
//            shouldSplit = true;
//        }
//
//        // Check filename ends with _2x1
//        String nameWithoutExt = filename.substring(0, filename.lastIndexOf('.'));
//        if (nameWithoutExt.endsWith("_2x1")) {
//            shouldSplit = true;
//        }
//
//        //if (currentLeft != null) ((Bitmap)currentLeft.getNative()).recycle();
//        //if (currentRight != null) ((Bitmap)currentRight.getNative()).recycle();
//
//        if (shouldSplit) {
//            // Split into left and right
//            int halfWidth = img.width / 2;
//            currentLeft = img.get(0, 0, halfWidth, img.height);
//            currentRight = img.get(halfWidth, 0, halfWidth, img.height);
//        } else {
//            // Use same image for both
//            currentLeft = img;
//            currentRight = img;
//        }
//        ((Bitmap)img.getNative()).recycle();
//        imagesLoaded = true;
//        println("Loaded: " + filepath + " width=" + currentLeft.width + " height=" + currentLeft.height);
//    }

    /**
     *
     * @return true if images loaded, false if failed to load any image
     */
//    public boolean loadCurrentImage() {
//        if (imageFiles.isEmpty() || currentIndex < 0 || currentIndex >= imageFiles.size()) {
//            imagesLoaded = false;
//            return false;
//        }
//
//        String filepath = imageFiles.get(currentIndex);
//        println("Loading: " + filepath);
//
//        PImage img = loadImage(filepath);
//        if (img == null) {
//            println("Failed to load image");
//            imagesLoaded = false;
//            return false;
//        }
//
//
//        // Check if image should be split
////        boolean shouldSplit = false;
////        String filename = new File(filepath).getName();
////
////        // Check aspect ratio > 2
////        float aspectRatio = (float)img.width / (float)img.height;
////        if (aspectRatio > 2.0) {
////            shouldSplit = true;
////        }
//
////        // Check filename ends with _2x1
////        String nameWithoutExt = filename.substring(0, filename.lastIndexOf('.'));
////        if (nameWithoutExt.endsWith("_2x1")) {
////            shouldSplit = true;
////        }
//
//        //if (currentLeft != null) ((Bitmap)currentLeft.getNative()).recycle();
//        //if (currentRight != null) ((Bitmap)currentRight.getNative()).recycle();
//
//        if (shouldSplit) {
//            // Split into left and right
//            int halfWidth = img.width / 2;
//            currentLeft = img.get(0, 0, halfWidth, img.height);
//            currentRight = img.get(halfWidth, 0, halfWidth, img.height);
//        } else {
//            // Use same image for both
//            currentLeft = img;
//            currentRight = img;
//        }
//        ((Bitmap)img.getNative()).recycle();
//        imagesLoaded = true;
//        println("Loaded: " + filepath + " width=" + currentLeft.width + " height=" + currentLeft.height);
//    }

    void loadCurrentImage() {
        if (leftImageFiles.size() == 0 || currentIndex < 0 || currentIndex >= leftImageFiles.size()) {
            imagesLoaded = false;
            return;
        }

        String leftPath = leftImageFiles.get(currentIndex);
        String rightPath = rightImageFiles.get(currentIndex);

        println("Loading pair " + (currentIndex + 1) + "/" + leftImageFiles.size());
        println("  Left: " + leftPath);
        println("  Right: " + rightPath);

        // Load left image
        currentLeft = loadImage(leftPath);
        if (currentLeft == null) {
            println("Failed to load left image");
            imagesLoaded = false;
            return;
        }

        // Load right image
        currentRight = loadImage(rightPath);
        if (currentRight == null) {
            println("Failed to load right image");
            imagesLoaded = false;
            return;
        }
        update = true;
        imagesLoaded = true;
    }

    void nextImage() {
        if (currentIndex < leftImageFiles.size() - 1) {
            currentIndex++;
            loadCurrentImage();
        } else {
            // Stop slideshow at end
            slideshowActive = false;
        }
    }

    void previousImage() {
        if (currentIndex > 0) {
            currentIndex--;
            loadCurrentImage();
        }
    }

    void firstImage() {
        if (leftImageFiles.size() > 0) {
            currentIndex = 0;
            loadCurrentImage();
        }
    }

    void lastImage() {
        if (leftImageFiles.size() > 0) {
            currentIndex = leftImageFiles.size() - 1;
            loadCurrentImage();
        }
    }

    void startSlideshow() {
        slideshowActive = true;
        lastSlideshowTime = millis();
    }

    void stopSlideshow() {
        slideshowActive = false;
    }

    public void keyPressedReview(int lastKeyCode, int lastKey) {
        if (DEBUG) println("keyCode="+lastKeyCode);

        // Handle keyboard key
        if (lastKeyCode == KeyEvent.KEYCODE_DPAD_RIGHT || lastKeyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            stopSlideshow();
            if (lastKeyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                nextImage();
            } else {
                previousImage();
            }
        } else if (lastKeyCode == 24 || lastKeyCode== KeyEvent.KEYCODE_DPAD_UP) { // VOLUME_UP
            stopSlideshow();
            nextImage();
        } else if (lastKeyCode == 25) { // VOLUME_DOWN
            stopSlideshow();
            previousImage();
        } else if (lastKeyCode == 122) { // HOME key
            stopSlideshow();
            firstImage();
        } else if (lastKeyCode == 123) { // END key
            stopSlideshow();
            lastImage();
        } else if (lastKeyCode == 126 || lastKeyCode == KeyEvent.KEYCODE_BUTTON_R1) { // MEDIA_PLAY key
            startSlideshow();
        } else if (lastKeyCode == 23 || lastKeyCode == CENTER || lastKeyCode == KeyEvent.KEYCODE_BUTTON_A) { // DPAD_CENTER or OK
            stopSlideshow();
        } else if (lastKeyCode == ESC || lastKeyCode == 4) { // ESC or BACK
            setLiveView(true);
        } else if (key == 'm' || key == 'M') {
            // Toggle mode (for testing)
            stopSlideshow();
            if (displayMode == DisplayMode.SBS) {
                displayMode = DisplayMode.ANAGLYPH;
            } else {
                displayMode = DisplayMode.SBS;
            }
        }
    }

//    // Override the default ESC behavior
//    void exit() {
//        super.exit();
//    }
}

