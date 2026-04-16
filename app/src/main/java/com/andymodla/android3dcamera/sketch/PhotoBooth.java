package com.andymodla.android3dcamera.sketch;

/**
 * The Photo Booth Processing sketch for the Graphic user interface
 *
 */

import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.view.KeyEvent;
import android.graphics.Bitmap;

import com.andymodla.android3dcamera.DisplayMode;
import com.andymodla.android3dcamera.Media;
import com.andymodla.android3dcamera.camera.Camera3D;
import com.andymodla.android3dcamera.Parameters;
import com.andymodla.android3dcamera.MainActivity;

import processing.core.PApplet;
import processing.core.PImage;
import processing.opengl.PGL;

import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;


public class PhotoBooth extends PApplet {
    private static boolean DEBUG = true;
    private static boolean testMode = false;

    private static String title1 = "3D/AI Photo Booth by Andy Modla";
    private static String title2 = "Philadelphia Maker Faire - April 19, 2026";
    private static String instruction1 = "Look at Camera";
    private static String instruction2 = "";

    int black = color(0);
    int white = color(255);
    int yellow = color(255, 255, 128);
    int magenta = color(255, 0, 128);
    int green = color(0, 255, 128);

    int gray = color(128);

    MainActivity mainActivity;
    Camera3D camStereo;  // The stereo camera used with the device
    Parameters parameters; // Application parameters
    Media media;

    PImage imgLeft;
    PImage imgRight;
    //PImage splashLeft;
    //PImage splashRight;

    int XBP_CAMERA_WIDTH = 1280;
    int XBP_CAMERA_HEIGHT = 960;

    int cameraWidth = XBP_CAMERA_WIDTH;  // default
    int cameraHeight = XBP_CAMERA_HEIGHT;
    int XBP_DISPLAY_WIDTH = 2400;
    int XBP_DISPLAY_HEIGHT = 1080;

    int displayFPS = 30; // display frames per second

    // Parallax and vertical alignment adjustments in pixels for XBP photo booth
    public volatile int parallax = 100;
    public volatile int verticalAlignment = -1;
    public volatile boolean mirror = false;
    public volatile boolean crossEye = false;
    public volatile int brightness = -6;
    DisplayMode displayMode = DisplayMode.SBS;
    volatile boolean update = false;
    volatile boolean zoom = false;
    volatile boolean blankScreen = false;
    boolean screenshot = false;
    int debugHelp = 0;
    String[] help;

    String[] help2 = {
            "Photo Booth Key Functions:",
            "Cycle Display Mode: A",
            "Decrease Parallax: Minus (-)",
            "Increase Parallax: Plus (+) or Equals (=)",
            "Save Parallax: P",
            "Screenshot: C",
            "Toggle Blank Screen: B",
            "Toggle Cross-Eye: X",
            "Toggle Mirror: M",
            "Toggle Zoom State: Z",
            "Zoom In: Right Bracket (])",
            "Zoom Out: Left Bracket ([)",
            "View Help/Parameters: H",
            "Toggle Test Mode: Space",
            "Toggle Debug Logging: Period (.)",
            "Toggle Focus Distance: Q",
            "Toggle Exposure Metering: T"
    };

    private boolean loadPrevious = true;
    private int captureFrameCount = 0;
    String countdown = "";  // default ignore null string

    float[] magnifyScale = {1.0f, 1.1f, 1.2f, 1.3f, 1.4f, 1.5f, 1.6f, 1.7f, 1.8f, 1.9f, 2.0f, 2.5f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f};
    int magnifyIndex = 0;

    float AR = 1.33333333f;  // aspect ratio for XReal Beam Pro camera image sensor

    // Display frame inside full screen AR 4:3
    int frameWidth = 2048;
    int frameHeight = 1080;
    int frameX = (XBP_DISPLAY_WIDTH - frameWidth) / 2;  // 2400 pixel screen minus frameWidth/2
    int frameY = (XBP_DISPLAY_HEIGHT - frameHeight) / 2;

    //Gui gui;
    public void setMainActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public void settings() {
        // set size for XReal Beam Pro full display
        // draw canvas size and render using OpenGL
        //fullScreen(P2D);
        size(2400, 1080, P2D);
    }

    public void setup() {
        if (DEBUG) PApplet.println("Photo Booth Sketch setup");
        orientation(LANDSCAPE);
        background(black);
        smooth();
        frameRate(displayFPS);
        mainActivity.state = MainActivity.LIVE_VIEW_STATE;

        //gui = new Gui();
        //gui.setup(this);

//        splashLeft = loadImage("FlowerPot_l.JPG");
//        splashRight = loadImage("FlowerPot_r.JPG");
//        if (DEBUG)
//            PApplet.println("splashLeft width=" + splashLeft.width + " height=" + splashLeft.height);
//        if (DEBUG)
//            PApplet.println("splashRight width=" + splashRight.width + " height=" + splashRight.height);
//        float ar = (float) splashLeft.width / (float) splashLeft.height;
//
//        image(splashLeft, frameX, 180, frameWidth / 2 - parallax, (frameWidth / 2 - parallax) / ar);
//        image(splashRight, frameX + frameWidth / 2 + parallax, 180, frameWidth / 2 - parallax, (frameWidth / 2 - parallax) / ar);

        textSize(72);
        textAlign(CENTER, CENTER);
        fill(yellow);
        text("3D Photo Booth", (float) width / 4, (float) height - 200);
        text("3D Photo Booth", (float) 3 * width / 4, (float) height - 200);
        if (DEBUG) PApplet.println("PhotoBooth setup done");
        update = true;
    }

    public void setCamera(Camera3D camera) {
        camStereo = camera;
        this.parameters = camera.getParameters();
        this.media = camera.getMedia();
    }

    public boolean isLiveView() {
        if (mainActivity.state == MainActivity.LIVE_VIEW_STATE) return true;
        return false;
    }

    public boolean isCapture() {
        if (mainActivity.state == MainActivity.CAPTURE_STATE) return true;
        return false;
    }

    public boolean isReview() {
        if (mainActivity.state == MainActivity.REVIEW_PHOTO_STATE) return true;
        return false;
    }

    public boolean isReviewEdit() {
        if (mainActivity.state == MainActivity.REVIEW_AI_EDIT_STATE) return true;
        return false;
    }

    public void update() {
        update = true;
    }

    public void setDisplayMode(DisplayMode mode) {
        displayMode = mode;
        if (mode == DisplayMode.SBS) {
            if (DEBUG) PApplet.println("Display SBS");
        } else if (mode == DisplayMode.ANAGLYPH) {
            if (DEBUG) PApplet.println("Display ANAGLYPH");
        } else if (mode == DisplayMode.LEFT) {
            if (DEBUG) PApplet.println("Display LEFT");
        } else if (mode == DisplayMode.RIGHT) {
            if (DEBUG) PApplet.println("Display RIGHT");
        }
        update = true;
    }

    public void toggleBlankScreen() {
        blankScreen = !blankScreen;
        update = true;
    }

    public void toggleMirror() {
        mirror = !mirror;
        update = true;
    }

    public void setMirror(boolean mirror) {
        if (DEBUG) PApplet.println("setMirror(" + mirror + ")");
        this.mirror = mirror;
        update = true;
    }

    void zoomToggle() {
        zoom = !zoom;
        if (!zoom) magnifyIndex = 0;
        update = true;
    }

    void toggleCrossEye() {
        crossEye = !crossEye;
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
            text(countdown, frameX + frameWidth / 4, height / 2);
            text(countdown, frameX + 3 * frameWidth / 4 - 40, height / 2);
        } else {
            text(countdown, width / 2, height / 2);
        }
    }

    public void draw() {

        if (loadPrevious) {
            loadPrevious = false;
            thread("reviewSetup");
        }
        background(black);

        mirror = parameters.getIsMirror();

        if (blankScreen) {
            return;
        }
        if (camStereo == null) {
            return;
        }

        if (isLiveView()) {
            drawLiveView();
        } else if (isCapture()) {
            drawCapture();
        } else if (isReview()) {
            drawReview();
        } else if (isReviewEdit()) {
            drawReview();
        }
        if (magnifyScale[magnifyIndex] > 1.0f) {
            textSize(48);
            fill(yellow);
            textAlign(LEFT);
            text("+" + magnifyScale[magnifyIndex] + "    ", width - 200, height - 4);
        }
        // camera and review mode display test mode for debug
        if (DEBUG && testMode) {
            textSize(48);
            fill(yellow);
            textAlign(LEFT);
            text("parallax = " + (parallax) + " mirror = " + mirror + " zoom = " + zoom + " w = " + imgLeft.width + " h =" + imgLeft.height, 50, height - 96);
            text("vertical = " + (verticalAlignment) + " magnify = " + magnifyScale[magnifyIndex], 50, height - 48);
        }

        // draw text on screen
        textSize(48);
        fill(yellow);
        textAlign(LEFT);
        String sMode = "";
        if (displayMode == DisplayMode.SBS) {
            sMode = "SBS";
        } else if (displayMode == DisplayMode.ANAGLYPH) {
            sMode = "Anaglyph";
        } else if (displayMode == DisplayMode.LEFT) {
            sMode = "Left";
        } else if (displayMode == DisplayMode.RIGHT) {
            sMode = "Right";
        }

        if (isLiveView()) {
            text("Live", 50, height - 48);
        } else if (isReview()) {
            text("Review Print", 50, height - 48);
        } else if (isReviewEdit()) {
            text("Review Edit", 50, height - 48);
        }
        text(sMode, 50, height - 96);

        if (mainActivity.state == MainActivity.LIVE_VIEW_STATE) {
            textAlign(CENTER);
            text(instruction1, width / 2, 50);
            if (displayMode == DisplayMode.SBS) {
                text(title1, width / 2, height - 96);
                text(title2, width / 2, height - 48);
            }
            if (displayMode == DisplayMode.ANAGLYPH) {
                text("P=" + (parallax) + "   ", width - 50, height - 96);
                text("V=" + (verticalAlignment) + "   ", width - 50, height - 48);
            }
        } else if (mainActivity.state == MainActivity.REVIEW_PHOTO_STATE) {
            textAlign(RIGHT);
            fill(green);
            text("Print", width - 50, height - 48);
        } else if (mainActivity.state == MainActivity.REVIEW_AI_EDIT_STATE) {
            textAlign(RIGHT);
            fill(magenta);
            text("AI Edit", width - 50, height - 48);

        }

        switch (debugHelp) {
            // overlays everything on screen
            case 1:
                fill(255);
                textAlign(LEFT);
                textSize(48);
                for (int i = 0; i < help.length; i++) {
                    text(help[i], 100, 50 + i * 50);
                }
                break;
            case 2:
                fill(255);
                textAlign(LEFT);
                textSize(48);
                for (int i = 0; i < help2.length; i++) {
                    text(help2[i], 100, 50 + i * 50);
                }

                break;
            default:
                break;
        }

        // last thing to check is screenshot
        if (screenshot) {
            saveScreenshot();
            screenshot = false;
        }
    }

    void drawCapture() {
        // PApplet.println("drawReview()");
//        if (imagesLoaded && currentLeft != null && currentRight != null) {
//            boolean saveMirror = mirror;  // review does not display mirror image
//            mirror = false;
//            if (displayMode == DisplayMode.SBS) {
//                drawSBS(currentLeft, currentRight);
//            } else if (displayMode == DisplayMode.ANAGLYPH) {
//                drawAnaglyph(currentLeft, currentRight);
//            } else if (displayMode == DisplayMode.LEFT) {
//                drawPhoto(currentLeft);
//            } else if (displayMode == DisplayMode.RIGHT) {
//                drawPhoto(currentRight);
//            }
//            mirror = saveMirror;
//        } else {
        // Display message if no images
        fill(255);
        textAlign(CENTER, CENTER);
        textSize(96);
        int animate = captureFrameCount / displayFPS;
        text("Please Wait for Photos to Develop ", width / 2, height / 2);
        loop();
        //    }
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
        // Clip to left half - use imgHeight for vertical bounds
        clip(frameX, baseVerticalOffset, imgWidth, imgHeight);
        // LEFT IMAGE (left half of frame)
        push();
        translate(frameX, baseVerticalOffset);
        translate(-(float) parallax / 2, -(float) verticalAlignment / 2);

        if (mirror) {
            translate(imgWidth, 0);
            scale(-1, 1);
        }

        if (zoom) {
            scale(magnifyScale[magnifyIndex], magnifyScale[magnifyIndex]);
        }

        image(imgLeft, -offsetX, -offsetY, imgWidth, imgHeight);
        pop();
        noClip();

        // RIGHT IMAGE (right half of frame)
        // Clip to right half - use imgHeight for vertical bounds
        clip(frameX + imgWidth, baseVerticalOffset, imgWidth, imgHeight);
        push();
        translate(frameX + imgWidth, baseVerticalOffset);
        translate((float) parallax / 2, (float) verticalAlignment / 2);

        if (mirror) {
            translate(imgWidth, 0);
            scale(-1, 1);
        }

        if (zoom) {
            scale(magnifyScale[magnifyIndex], magnifyScale[magnifyIndex]);
        }

        image(imgRight, -offsetX, -offsetY, imgWidth, imgHeight);
        pop();
        noClip();

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

        push();
        translate(displayX, 0);
        translate(-(float) parallax / 2, -(float) verticalAlignment / 2);

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
        pop();

        noClip();
        endPGL();

        pgl = beginPGL();
        pgl.colorMask(false, true, true, true);  // Blue and Green channels only
        pgl.viewport(0, 0, width, height);

        // Add clipping for second layer too
        clip(displayX, 0, anaglyphW, height);

        push();
        translate(displayX, 0);
        translate((float) parallax / 2, (float) verticalAlignment / 2);

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
        pop();
        noClip();
        endPGL();

        // for drawing over anaglyph image
        // change colorMask back before filling with rectangles on edges
        pgl = beginPGL();
        pgl.colorMask(true, true, true, true);  // Restore color channels
        pgl.viewport(0, 0, width, height);
        endPGL();

        // cover anaglyph alignment edges
//        fill(black);
//        if (verticalAlignment != 0) {
//            rect(0, 0, width, abs(verticalAlignment));  // top of image
//            rect(0, height - abs(verticalAlignment), width, abs(verticalAlignment));  // bottom of image
//        }

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

        push();
        translate(displayX, 0);
        translate(-(float) parallax / 2, -(float) verticalAlignment / 2);

        if (mirror) {
            translate(anaglyphW, 0);
            scale(-1, 1); // Mirror - flip horizontally
        }

        if (zoom) {
            scale(magnifyScale[magnifyIndex], magnifyScale[magnifyIndex]);
        }
        //if (DEBUG) PApplet.println("drawPhoto()");
        image(img, -offsetX, -offsetY, anaglyphW, height);

        pop();
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

    // called by MainActivty onKeyUp to process key events for photo booth exclusively
    public boolean processKeyCode(int lastKeyCode, int lastKey) {
        switch (lastKeyCode) {
            case KeyEvent.KEYCODE_A:
                displayMode = displayMode.next();
                break;
            case KeyEvent.KEYCODE_B:
                toggleBlankScreen();
                break;
            case KeyEvent.KEYCODE_C:
                screenshot = true;
                break;
            case KeyEvent.KEYCODE_LEFT_BRACKET:
                if (magnifyIndex > 0) {
                    magnifyIndex--;
                    update = true;
                }
                //if (DEBUG) PApplet.println("magnifyScale = " + magnifyScale[magnifyIndex] + " magnifyIndex");
                break;
            case KeyEvent.KEYCODE_RIGHT_BRACKET:
                if (magnifyIndex < magnifyScale.length - 1) {
                    magnifyIndex++;
                    update = true;
                }
                //if (DEBUG) PApplet.println("magnifyScale = " + magnifyScale[magnifyIndex] + " magnifyIndex");
                break;
//            case KeyEvent.KEYCODE_M:
//                toggleMirror();
//                break;
            case KeyEvent.KEYCODE_X:
                toggleCrossEye();
                break;
            case KeyEvent.KEYCODE_P:
                // save current parallax in shared preferences
                parameters.setParallaxOffset(parallax);
                break;
//            case KeyEvent.KEYCODE_FORWARD:  // 125 forward media button on mouse: mirror toggle
//                File mediaFile = media.getMediaFile();
//                if (mediaFile == null) {
//                    if (DEBUG) PApplet.println("Nothing for AI Edit");
//                }
//                media.shareImage2(media.getMediaFile(), Media.APP_AIEDIT_PACKAGE);
//                break;
            case KeyEvent.KEYCODE_Z:
                zoomToggle();
                break;
            case KeyEvent.KEYCODE_SPACE:
                testMode = !testMode;
                update = true;
                break;
            case KeyEvent.KEYCODE_PERIOD:
                DEBUG = !DEBUG;
                break;
            case KeyEvent.KEYCODE_MINUS:
                setParallax(parallax - 2);
                if (DEBUG) PApplet.println("parallax = " + parallax);
                break;
            case KeyEvent.KEYCODE_PLUS:
            case KeyEvent.KEYCODE_EQUALS:
                setParallax(parallax + 2);
                if (DEBUG) PApplet.println("parallax = " + parallax);
                break;
            case KeyEvent.KEYCODE_H:  // help screens for debug
            case KeyEvent.KEYCODE_HELP:
                debugHelp++;
                if (debugHelp > 2) {
                    debugHelp = 0;
                } else if (debugHelp == 1) {
                    help = parameters.getParameterDetails();
                    for (String s : help) {
                        if (DEBUG) PApplet.println(s);
                    }
                } else if (debugHelp == 2) {
                    //help = parameters.getKeyDetails();  TODO
                    for (String s : help) {
                        if (DEBUG) PApplet.println(s);
                    }
                }
                break;
            default:
                return false;
        }
        update = true;
        return true;
    }

    void saveScreenshot() {
        String dateTime = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = "Screenshot_" + dateTime + ".png";
        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures/Screenshots/" + filename;
        saveFrame(filePath);
        MediaScannerConnection.scanFile(getContext(), new String[]{filePath},
                new String[]{"image/*"}, null);
        if (DEBUG) PApplet.println("Screenshot saved to " + filePath);
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

    /**
     * =================================================================================================
     * Review Code
     */
    // Constants
    final String FOLDER_PATH = "/DCIM/A3DCamera/LR";
    final int SLIDESHOW_DELAY = 2500; // 2 seconds

    // Review Global variables
    ArrayList<String> leftImageFiles;
    ArrayList<String> rightImageFiles;
    int currentIndex = 0;

    // Review photos for display;
    volatile PImage currentLeft;
    volatile PImage currentRight;
    volatile boolean imagesLoaded = false;

    // Review photo for print
    volatile PImage currentSBS;

    // reviewSetup is run as a thread using Processing's thread() function
    public void reviewSetup() {
        if (DEBUG) PApplet.println("reviewSetup()");
        // Load image file list
        boolean listAvailable = loadImageFileList();

        // Load the first image if available
        if (listAvailable) {
            loadCurrentImage();
            update = true;
        }

    }


    public void setReviewImages(PImage left, PImage right) {
        //if (currentLeft != null) ((Bitmap) currentLeft.getNative()).recycle();
        //if (currentRight != null) ((Bitmap) currentRight.getNative()).recycle();
        currentLeft = left;
        currentRight = right;
        imagesLoaded = true;
    }

    void drawReview() {
        // PApplet.println("drawReview()");
        if (imagesLoaded && currentLeft != null && currentRight != null) {
            boolean saveMirror = mirror;  // review does not display mirror image
            mirror = false;
            if (displayMode == DisplayMode.SBS) {
                drawSBS(currentLeft, currentRight);
            } else if (displayMode == DisplayMode.ANAGLYPH) {
                drawAnaglyph(currentLeft, currentRight);
            } else if (displayMode == DisplayMode.LEFT) {
                drawPhoto(currentLeft);
            } else if (displayMode == DisplayMode.RIGHT) {
                drawPhoto(currentRight);
            }
            mirror = saveMirror;
        } else {
            // Display message if no images
            fill(255);
            textAlign(CENTER, CENTER);
            textSize(48);
            text("Waiting for Photo", width / 2, height / 2);
        }
//        fill(255);
//        textAlign(CENTER, CENTER);
//        textSize(48);
//        text("Review Photo", width/2, height/2);

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
            if (DEBUG) PApplet.println("Folder not found: " + lrFolder.getAbsolutePath());
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

                    if (name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".jpeg")) {
                        // Check if it's a left or right image
                        String nameWithoutExt = name.substring(0, name.lastIndexOf('.'));

                        if (nameWithoutExt.toLowerCase().endsWith("_l")) {
                            tempLeftFiles.add(fullPath);
                        } else if (nameWithoutExt.toLowerCase().endsWith("_r")) {
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
                    //if (DEBUG) PApplet.println("Pairs: " + leftPath + " " + rightPath);
                    break;
                }
            }
        }
        currentIndex = leftImageFiles.size() - 1;
        //if (DEBUG) PApplet.println("Found " + leftImageFiles.size() + " matching image pairs");
        //if (DEBUG) PApplet.println("First pair: " + leftImageFiles.get(0) + " " + rightImageFiles.get(0));
        //if (DEBUG) PApplet.println("Last pair: " + leftImageFiles.get(leftImageFiles.size() - 1) + " " + rightImageFiles.get(rightImageFiles.size() - 1));
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

    void loadCurrentImage() {
        if (DEBUG) PApplet.println("loadCurrentImage() state=" + mainActivity.state);
        if (leftImageFiles.isEmpty() || currentIndex < 0 || currentIndex >= leftImageFiles.size()) {
            imagesLoaded = false;
            if (DEBUG) PApplet.println("loadCurrentImage failed");
            return;
        }

        String leftPath = leftImageFiles.get(currentIndex);
        String rightPath = rightImageFiles.get(currentIndex);

        //if (DEBUG) PApplet.println("Loading pair " + (currentIndex + 1) + "/" + leftImageFiles.size());
        //if (DEBUG) PApplet.println("  Left: " + leftPath);
        //if (DEBUG) PApplet.println("  Right: " + rightPath);

        // Load left image
        currentLeft = loadImage(leftPath);
        if (currentLeft == null) {
            if (DEBUG) PApplet.println("Failed to load left image");
            imagesLoaded = false;
            return;
        }

        // Load right image
        currentRight = loadImage(rightPath);
        if (currentRight == null) {
            if (DEBUG) PApplet.println("Failed to load right image");
            imagesLoaded = false;
            return;
        }
        update = true;
        imagesLoaded = true;
        if (DEBUG) PApplet.println("loadCurrentImage success.");
    }

    // copied from PApplet.java Processing-Android
    public PImage loadImage(String filename) {
        System.out.println("loadImage " + filename);
        InputStream stream = createInput(filename);
        if (stream == null) {
            System.err.println("Could not find the image " + filename + ".");
            return null;
        } else {
            Bitmap bitmap = null;

            try {
                bitmap = BitmapFactory.decodeStream(stream);
            } finally {
                try {
                    stream.close();
                    //InputStream var12 = null;
                } catch (IOException var10) {
                }

            }

            if (bitmap == null) {
                System.err.println("Could not load the image because the bitmap was empty.");
                return null;
            } else {
                PImage image = new PImage(bitmap);
                image.parent = this;
                image.loadPixels();
                image.updatePixels();
                return image;
            }
        }
    }

//    void nextImage() {
//        if (currentIndex < leftImageFiles.size() - 1) {
//            currentIndex++;
//            loadCurrentImage();
//        } else {
//            // Stop slideshow at end
//            slideshowActive = false;
//        }
//    }
//
//    void previousImage() {
//        if (currentIndex > 0) {
//            currentIndex--;
//            loadCurrentImage();
//        }
//    }
//
//    void firstImage() {
//        if (!leftImageFiles.isEmpty()) {
//            currentIndex = 0;
//            loadCurrentImage();
//        }
//    }
//
//    void lastImage() {
//        if (!leftImageFiles.isEmpty()) {
//            currentIndex = leftImageFiles.size() - 1;
//            loadCurrentImage();
//        }
//    }
//
//    void startSlideshow() {
//        slideshowActive = true;
//        lastSlideshowTime = millis();
//    }
//
//    void stopSlideshow() {
//        slideshowActive = false;
//    }
//
//    public void keyPressedReview(int lastKeyCode, int lastKey) {
//        if (DEBUG) PApplet.println("keyPressedReview keyCode="+lastKeyCode);
//
//        // Handle keyboard key
//        if (lastKeyCode == KeyEvent.KEYCODE_DPAD_RIGHT || lastKeyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
//            stopSlideshow();
//            if (lastKeyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
//                nextImage();
//            } else {
//                previousImage();
//            }
//        } else if (lastKeyCode == KeyEvent.KEYCODE_VOLUME_UP || lastKeyCode== KeyEvent.KEYCODE_DPAD_UP) {
//            stopSlideshow();
//            nextImage();
//        } else if (lastKeyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
//            stopSlideshow();
//            previousImage();
//        } else if (lastKeyCode == KeyEvent.KEYCODE_MOVE_HOME) {
//            stopSlideshow();
//            firstImage();
//        } else if (lastKeyCode == KeyEvent.KEYCODE_MOVE_END) {
//            stopSlideshow();
//            lastImage();
//        } else if (lastKeyCode == KeyEvent.KEYCODE_MEDIA_PLAY || lastKeyCode == KeyEvent.KEYCODE_BUTTON_R1) { // MEDIA_PLAY key
//            startSlideshow();
//        } else if (lastKeyCode == KeyEvent.KEYCODE_DPAD_CENTER || lastKeyCode == KeyEvent.KEYCODE_BUTTON_A) { // DPAD_CENTER or OK
//            stopSlideshow();
//        } else if (lastKeyCode == ESC || lastKeyCode == KeyEvent.KEYCODE_BACK) { // ESC or BACK
//            setLiveView(true);
//        } else if (key == 'm' || key == 'M') {
//            // Toggle mode (for testing)
//            stopSlideshow();
//            if (displayMode == DisplayMode.SBS) {
//                displayMode = DisplayMode.ANAGLYPH;
//            } else {
//                displayMode = DisplayMode.SBS;
//            }
//        }
//    }

// unused code for reference from previous photo booth project
//    PImage getPhoto(String name) {
//        //String name;
//        String filename = "";
//        String filenameUrl = "";
//        PImage lastPhoto = null;
//        boolean showPhoto = false;
//        String aFilename = "IMG_" + getFilename(SAME, PHOTO_MODE) + "_" + name + ".jpg";
//        filename = aFilename;
//        String afilenameUrl = "http://" + ipAddress + ":" + HTTPport + "/" + aFilename;
//        afilenameUrl.trim();
//        afilenameUrl = afilenameUrl.replaceAll("(\\r|\\n)", "");
//        String afilename = filename.replaceAll("(\\r|\\n)", "");
//        Log.d(TAG, "result filename = " + afilename + " filenameURL= " + afilenameUrl);
//        //if (!afilenameUrl.equals(filenameUrl)) {
//        if (!afilenameUrl.equals(filenameUrl) || lastPhoto == null || lastPhoto.width <= 0 || lastPhoto.height <= 0) {
//            filename = afilename.substring(afilename.lastIndexOf('/') + 1);
//            filenameUrl = afilenameUrl;
//            lastPhoto = loadImage(filenameUrl, "jpg");
//            Log.d(TAG, "OCR getFilename loadImage " + filenameUrl);
//            if (lastPhoto == null || lastPhoto.width == -1 || lastPhoto.height == -1) {
//                showPhoto = false;
//            } else {
//                showPhoto = true;
//            }
//        }
//        return lastPhoto;
//    }
}

