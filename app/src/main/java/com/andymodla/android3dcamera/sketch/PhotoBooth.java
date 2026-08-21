package com.andymodla.android3dcamera.sketch;

/**
 * The Photo Booth Processing sketch for the Graphic user interface
 *
 */

import static android.graphics.BitmapFactory.decodeStream;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.view.KeyEvent;
import android.graphics.Bitmap;

import com.andymodla.android3dcamera.DisplayMode;
import com.andymodla.android3dcamera.Media;
import com.andymodla.android3dcamera.MyDebug;
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
    private static final boolean DEBUG = MyDebug.DEBUG;
    private static final boolean testMode = false;
    private static boolean testCheckDraw = false;

    int black = color(0);
    int white = color(255);
    int yellow = color(255, 255, 128);
    int magenta = color(255, 0, 128);
    int green = color(0, 255, 128);

    int gray = color(128);

    MainActivity mainActivity;
    Camera3D stereoCamera;  // The stereo camera used with the device
    Parameters parameters; // Application parameters
    Media media;
    Gui gui;

    private volatile int state; // updated from MainActivity
    private boolean initial = true;

    PImage imgLeft;
    PImage imgRight;

    // Constants
    final int SLIDESHOW_DELAY = 2500; // 2 seconds

    // Review Global variables
    ArrayList<String> sbsImageFiles; // List of links to image files stored in folder /DCIM/A3DCamera
    int currentIndex = 0;  // current index into sbsImageFiles for review

    // Review photos for display;
    volatile PImage currentLeft;
    volatile PImage currentRight;

    int XBP_CAMERA_DISPLAY_WIDTH = 1280;
    int XBP_CAMERA_DISPLAY_HEIGHT = 960;

    int cameraWidth = XBP_CAMERA_DISPLAY_WIDTH;  // default
    int cameraHeight = XBP_CAMERA_DISPLAY_HEIGHT;
    int XBP_DISPLAY_WIDTH = 2400;
    int XBP_DISPLAY_HEIGHT = 1080;

    int displayFPS = 30; // display frames per second
    int reviewTimeout = 0;  // frame count for review timeout
    int MAX_REVIEW_TIMEOUT_SECONDS = 90;  // for photo booth camera mode
    int REVIEW_TIMEOUT_SECONDS = 3;  // for stereoscope camera mode

    // Parallax and vertical alignment adjustments in pixels for XBP photo booth display
    private volatile int parallax = 0;  // display parallax - converted from camera sensor parallax
    private static final int DELTA_PARALLAX = 2;
    private volatile int verticalAlignment = 0;
    private volatile boolean mirror = false;
    private volatile boolean crossEye = false;
    private volatile boolean grid = false;
    volatile boolean zoom = false;
    private volatile boolean showZoom = false;
    private volatile boolean showMenu = false;
    private volatile boolean showEv = false;
    private volatile boolean showParallax = false;
    //private volatile boolean showZoom = false;
    private volatile boolean showPhotoBoothTitle = false;
    volatile boolean update = false;
    boolean screenshot = false;

    private float shiftOffsetX = 0;
    private float shiftOffsetY = 0;
    private int DISPLAY_OFFSET_Y = 180;  // status display line for filename, EV, parallax, zoom

    private volatile int lastKeyCode; // for processKeyCode()
    private volatile int lastKey; // for processKeyCode()

    private static final int STEREO_OFFSET = -10; // right image shift used for stereo depth
    private static final int TITLE_STEREO_OFFSET = -160; // right image shift used for stereo depth
    private String imageLabel;
    private int labelFrameCount = 0;
    private static final int IMAGE_LABEL_TIMEOUT_FRAMES = 90;

    private String[] rotatingText = {"-", "\\", "|", "/"};
    private int rotatingIndex = 0;
    private volatile boolean rotating = false;  // for test to show frame rate

//    // menu mode functions
//    private static final String NO_OPERATION_MODE = "";
//    private static final String PARALLAX_MODE = "Parallax";
//    private static final String ZOOM_MODE = "Zoom";
//    private static final int NO_OPERATION_MODE_INDEX = 0;
//    private static final int PARALLAX_MODE_INDEX = 1;
//    private static final int ZOOM_MODE_INDEX = 2;
//    private int modeIndex = NO_OPERATION_MODE_INDEX;
//    private String[] menuModeLabels = {NO_OPERATION_MODE, PARALLAX_MODE, ZOOM_MODE};

    DisplayMode displayMode = DisplayMode.SBS;
    int debugHelp = 0;
    String[] help;

    String[] help2 = {
            "Photo Booth Bluetooth Android Keyboard Functions:",
            "Debug Toggle Display Frame Counter: D",
            "Cycle Display Mode: A",
            "Decrease Parallax: Minus (-)",
            "Increase Parallax: Plus (+) or Equals (=)",
            "Screenshot: P",
            "Toggle Blank Screen: B",
            "Toggle Photo Booth Title: T",
            "Toggle Grid: G",
            "Toggle Cross-Eye: X",
            "Toggle Mirror: M",
            "Toggle Zoom ON/OFF: Z",
            "Zoom In: Right Bracket (])",
            "Zoom Out: Left Bracket ([)",
            "View Help/Parameters: H",
            "Toggle Show Menu: U",
            "Settings: J",
            "Toggle Focus Distance: Q",
            "Toggle Auto Exposure: F",
            "Continuous Shutter: C",
            "Increment Exposure Compensation: Period (.)",
            "Decrement Exposure Compensation: Comma (,)"
    };

    String countdown = "";  // default ignore null string

    private int magnifyIndex = 0;
    private static final float[] magnifyScale = {1.0f, 1.1f, 1.2f, 1.3f, 1.4f, 1.5f, 1.6f, 1.7f, 1.8f, 1.9f, 2.0f, 2.5f, 3.0f, 4.0f, 6.0f, 8.0f};
    private static final float[] shiftOffsetDelta = {0.0f, 10.0f, 9.0f, 8.0f, 7.0f, 6.0f, 5.0f, 4.0f, 3.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
    //private static final float[] magnifyScale =     {1.0f, 1.1f, 1.2f, 1.3f, 1.4f, 1.5f, 1.6f, 1.7f, 1.8f, 1.9f, 2.0f, 2.5f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f};
    //private static final float[] shiftOffsetDelta = {0.0f, 10.0f, 9.0f, 8.0f, 7.0f, 6.0f, 5.0f, 4.0f, 3.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};

    float AR = 1.33333333f;  // aspect ratio for XReal Beam Pro camera image sensor

    // Display frame inside full screen AR 4:3
    private static final int XBP_DISPLAY_FRAME_WIDTH = 2048;
    private static final int XBP_DISPLAY_FRAME_HEIGHT = 1080;
    private static final int XBP_DISPLAY_BOTTOM_H = XBP_DISPLAY_FRAME_HEIGHT - 144;
    // calculate position of frame in full screen
    int frameX = (XBP_DISPLAY_WIDTH - XBP_DISPLAY_FRAME_WIDTH) / 2;
    int frameY = (XBP_DISPLAY_HEIGHT - XBP_DISPLAY_FRAME_HEIGHT) / 2;

    private static final int HIDDEN_LEFT_ARROW_BUTTON_X = 0;
    private static final int HIDDEN_LEFT_ARROW_BUTTON_Y = 141;
    private static final int HIDDEN_LEFT_ARROW_BUTTON_W = 360;
    private static final int HIDDEN_LEFT_ARROW_BUTTON_H = XBP_DISPLAY_FRAME_HEIGHT - 140;
    private static final int HIDDEN_RIGHT_ARROW_BUTTON_X = 360;
    private static final int HIDDEN_RIGHT_ARROW_BUTTON_Y = 140;
    private static final int HIDDEN_RIGHT_ARROW_BUTTON_W = 360;
    private static final int HIDDEN_RIGHT_ARROW_BUTTON_H = 140;

    public void setMainActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public void setMedia(Media media) {
        this.media = media;
    }

    public void settings() {
        if (DEBUG) System.out.println("Photo Booth Sketch settings()");
        // set processing sketch size for XReal Beam Pro full display
        // draw canvas size and render using OpenGL
        //fullScreen(P2D);
        size(XBP_DISPLAY_WIDTH, XBP_DISPLAY_HEIGHT, P2D);
        if (gui == null) {
            gui = new Gui();
            gui.setup(this);
        }
        if (DEBUG) System.out.println("Photo Booth Sketch settings() done");

    }

    public void setMenuKeyLabels(int func) {
        if (state == MainActivity.REVIEW_PHOTO_STATE) {
            resetZoom();
        }
        gui.getMenuBar().setMenuKeyLabels(func);
    }

    private void reloadReviewImage(int index) {
        if (DEBUG) PApplet.println("PhotoBooth reloadImage() index=" + index);
        PImage sbsImage = loadImage(sbsImageFiles.get(index));
        if (sbsImage != null) {
            //((Bitmap)media.leftReview.getNative()).recycle();
            //((Bitmap)media.rightReview.getNative()).recycle();
            PImage[] split = splitImageLR(sbsImage, Camera3D.CAMERA_WIDTH_DEFAULT, Camera3D.CAMERA_HEIGHT_DEFAULT);
            media.leftReview = split[0];
            media.rightReview = split[1];
        } else {
            media.leftReview = loadImage("Image_l.JPG");
            media.rightReview = loadImage("Image_r.JPG");
        }

        currentLeft = media.leftReview;
        currentRight = media.rightReview;
        setReviewLabel("");
    }

    public void setup() {
        if (DEBUG) System.out.println("Photo Booth Sketch setup()");
        long t0 = System.nanoTime();
        orientation(LANDSCAPE);
        background(black);
        smooth();
        frameRate(displayFPS);
        initial = true;
        if (media.leftReview == null && media.rightReview == null) {
            boolean success = loadImageFileList();
            if (DEBUG) PApplet.println("PhotoBooth loadImageFileList() = " + success);
            if (success) {
                PImage sbsImage = loadImage(sbsImageFiles.get(currentIndex));
                if (sbsImage != null) {
                    PImage[] split = splitImageLR(sbsImage, Camera3D.CAMERA_WIDTH_DEFAULT, Camera3D.CAMERA_HEIGHT_DEFAULT);
                    media.leftReview = split[0];
                    media.rightReview = split[1];
                }
            } else {
                media.leftReview = loadImage("Image_l.JPG");
                media.rightReview = loadImage("Image_r.JPG");
            }
            currentLeft = media.leftReview;
            currentRight = media.rightReview;

        }

        textSize(36);
        textAlign(CENTER, CENTER);
        fill(yellow);
        int level = 72;
        if (parameters.isPhotoBoothCameraMode()) {
            text("3D Photo Booth", (float) width / 4, (float) height / 2); // left
            text("3D Photo Booth", ((float) 3 * width / 4) + STEREO_OFFSET, (float) height / 2); // right
        } else {
            text("3D Stereoscope", (float) width / 4, (float) height / 2);  // left
            text("3D Stereoscope", ((float) 3 * width / 4) + TITLE_STEREO_OFFSET, (float) height / 2); // right
            text("Camera", (float) width / 4, (float) (height / 2) + level);  // left
            text("Camera", ((float) 3 * width / 4) + TITLE_STEREO_OFFSET, (float) (height / 2) + level); // right
        }
        textSize(24);
        text("Please wait ...", (float) width / 4, (float) (height / 2) + 4 * level);  // left
        text("Please wait ...", ((float) 3 * width / 4) + TITLE_STEREO_OFFSET, (float) (height / 2) + 4 * level); // right

        if (DEBUG)
            PApplet.println("PhotoBooth setup done in " + n2s(System.nanoTime() - t0) + " seconds");

        update = true;
    }

    public boolean isReady() {
        return !initial;
    }

    public void onBackPressed() {
        if (DEBUG) println("onBackPressed()");
    }

    public void backPressed() {
        if (DEBUG) println("backPressed()");
    }

    public void setCamera(Camera3D camera) {
        stereoCamera = camera;
    }

    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }

    public void update() {
        update = true;
    }

    public void setDisplayMode(DisplayMode mode) {
        displayMode = mode;
        // set next key press label
        if (mode == DisplayMode.SBS) {
            if (DEBUG) PApplet.println("Display SBS Parallel Image");
            gui.menuBar.setMenuKeyLabel(5, "ANAGLYPH");
        } else if (mode == DisplayMode.ANAGLYPH) {
            if (DEBUG) PApplet.println("Display ANAGLYPH Image");
            gui.menuBar.setMenuKeyLabel(5, "LEFT\nEYE");
        } else if (mode == DisplayMode.LEFT) {
            if (DEBUG) PApplet.println("Display LEFT Image");
            gui.menuBar.setMenuKeyLabel(5, "RIGHT\nEYE");
        } else if (mode == DisplayMode.RIGHT) {
            if (DEBUG) PApplet.println("Display RIGHT Image");
            gui.menuBar.setMenuKeyLabel(5, "SBS");
        }
        update = true;
    }

    public void toggleBlankScreen() {
        parameters.setIsBlankScreen(!parameters.getIsBlankScreen());
        update = true;
    }

    public void toggleGrid() {
        grid = !grid;
        if (DEBUG) PApplet.println("grid = " + grid);
        update = true;
    }

    public void toggleShowPhotoBoothTitle() {
        showPhotoBoothTitle = !showPhotoBoothTitle;
        update = true;
    }

    public void toggleShowMenu() {
        if (parameters.isStereoscopeCameraMode()) {
            showMenu = !showMenu;
            update = true;
        }
    }

    public void toggleEv() {
        showEv = !showEv;
        update = true;
    }

    public void setMirror(boolean mirror) {
        if (DEBUG) PApplet.println("setMirror(" + mirror + ")");
        this.mirror = mirror;
        update = true;
    }

//    void toggleZoom() {
//        zoom = !zoom;
//        update = true;
//    }

    void toggleShowZoom() {
        if (DEBUG) println("toggleShowZoom");
        showZoom = !showZoom;
        update = true;
    }

    void resetZoom() {
        magnifyIndex = 0;
        shiftOffsetX = 0;
        shiftOffsetY = 0;
    }

    void toggleParallax() {
        showParallax = !showParallax;
        update = true;
    }

    void toggleCrossEye() {
        crossEye = !crossEye;
        update = true;
    }

    public void setParallax(int parallax) {
        this.parallax = toDisplayPixels(parallax);
        update = true;
    }

    public void setVerticalAlignment(int verticalAlignment) {
        this.verticalAlignment = verticalAlignment;
        update = true;
    }

    /**
     * Convert a display-pixel offset to camera-pixel offset.
     */
    public int toCameraPixels(int displayPixels) {
        float dispImgWidth = (float) XBP_DISPLAY_FRAME_WIDTH / 2;
        return (int) Math.round(displayPixels * (float) Camera3D.CAMERA_WIDTH_DEFAULT / dispImgWidth);
    }

    /**
     * Convert a camera-pixel offset to display-pixel offset (inverse of toCameraPixels).
     */
    public int toDisplayPixels(int cameraPixels) {
        float dispImgWidth = (float) XBP_DISPLAY_FRAME_WIDTH / 2;
        int displayPixels = (int) Math.round(cameraPixels * dispImgWidth / (float) Camera3D.CAMERA_WIDTH_DEFAULT);
        if (DEBUG) println("toDisplayPixels(" + cameraPixels + ") = " + displayPixels);
        return displayPixels;
    }

    // reference not used:
    public int REFtoCameraPixels(int displayPixels) {
        int camWidth;
        if (imgLeft != null && imgLeft.width > 0) {
            camWidth = imgLeft.width; // live camera image width
        } else {
            camWidth = XBP_DISPLAY_FRAME_WIDTH; // fallback: known sensor width
        }
        float dispImgWidth = (float) camWidth / 2;
        return (int) Math.round(displayPixels * (float) camWidth / dispImgWidth);
    }

    /**
     * Convert a camera-pixel offset to display-pixel offset (inverse of toCameraPixels).
     */
    //reference not used:
    public int REFtoDisplayPixels(int cameraPixels) {
        int camWidth;
        if (imgLeft != null && imgLeft.width > 0) {
            camWidth = imgLeft.width;
        } else {
            camWidth = XBP_DISPLAY_FRAME_WIDTH;
        }
        float dispImgWidth = (float) camWidth / 2;
        return (int) Math.round(cameraPixels * dispImgWidth / (float) camWidth);
    }

    public void setCountdown(String countdown) {
        this.countdown = countdown;
        update = true;
    }

    public void setReviewTimeout(int reviewTimeout) {
        if (parameters.getAutoReview() && parameters.isBasicCameraMode()) {
            this.reviewTimeout = MAX_REVIEW_TIMEOUT_SECONDS * displayFPS;
        } else if (reviewTimeout == 0) {
            this.reviewTimeout = 2;  // review timeout in frames
        } else {
            this.reviewTimeout = (REVIEW_TIMEOUT_SECONDS + 1) * displayFPS;
        }
    }

    void showCountdown(boolean sbs) {
        if (countdown.isEmpty()) {
            return;
        }
        textSize(288);
        fill(yellow);
        textAlign(CENTER, CENTER);

        if (sbs) {
            text(countdown, frameX + XBP_DISPLAY_FRAME_WIDTH / 4, height / 2);
            text(countdown, frameX + 3 * XBP_DISPLAY_FRAME_WIDTH / 4 - 40, height / 2);
        } else {
            text(countdown, width / 2, height / 2);
        }
    }

    // Convert nanoseconds to seconds
    static String n2s(long nanoseconds) {
        double seconds = nanoseconds / 1000000000.0;
        return String.format("%.3f", seconds);
    }

//    public void createTextures() {
//        if (DEBUG) PApplet.println("PhotoBooth createTextures()");
//        long t0 = System.nanoTime();
//        if (g instanceof processing.opengl.PGraphicsOpenGL) {
//            ((processing.opengl.PGraphicsOpenGL)g).getTexture(media.leftReview);
//            ((processing.opengl.PGraphicsOpenGL)g).getTexture(media.rightReview);
//        }
//        initial = false;
//        if (DEBUG) PApplet.println("PhotoBooth draw() initial done in " + n2s(System.nanoTime() - t0)  + " seconds");
//
//    }

    private void setReviewLabel(String prefix) {
        if (DEBUG) PApplet.println("PhotoBooth setReviewLabel()");
        if (currentIndex >= 0) {
            String fullPath = sbsImageFiles.get(currentIndex);
            media.setReviewFilePath(fullPath);
            String name = fullPath.substring(0, fullPath.lastIndexOf("_2x1."));
            String nameWithoutExt = name.substring(name.lastIndexOf("/") + 5);
            //if (DEBUG) println("nameWithoutExt: " + nameWithoutExt);
            if (prefix.isEmpty()) {
                setImageLabel(nameWithoutExt);
            } else {
                setImageLabel(prefix + ": " + nameWithoutExt);
            }
        }
    }

    public File getCurrentSbsFile() {
        File reviewSbs = null;
        if (sbsImageFiles.size() > 0) {
            String fullPath = sbsImageFiles.get(currentIndex);
            if (fullPath != null) {
                reviewSbs = new File(fullPath);
            }
        }
        return reviewSbs;
    }

    public void draw() {
        if (initial) {
            long t0 = System.nanoTime();
            // Force OpenGL texture creation/upload now, on the GL thread
            // assumes review images are the default size

            media.leftReview.resize(Camera3D.CAMERA_WIDTH_DEFAULT, Camera3D.CAMERA_HEIGHT_DEFAULT);
            media.rightReview.resize(Camera3D.CAMERA_WIDTH_DEFAULT, Camera3D.CAMERA_HEIGHT_DEFAULT);

            // this code has to run here on the processing draw() GL thread to create the textures
            if (g instanceof processing.opengl.PGraphicsOpenGL) {
                ((processing.opengl.PGraphicsOpenGL) g).getTexture(media.leftReview);
                ((processing.opengl.PGraphicsOpenGL) g).getTexture(media.rightReview);
            }
            initial = false;
            if (DEBUG)
                PApplet.println("PhotoBooth draw() initialization done in " + n2s(System.nanoTime() - t0) + " seconds");
            setReviewLabel("Previous");

        }

        state = mainActivity.state;
        mirror = parameters.getIsMirror();
        processKeyCode();

        background(black);

        if (reviewTimeout > 0) {
            //if (DEBUG) println("reviewTimeout = " + reviewTimeout + "  ");
            reviewTimeout--;
            if (reviewTimeout == 0) {
                mainActivity.state = MainActivity.LIVE_VIEW_STATE;
                state = mainActivity.state;
                update = true;
            } else {
                mainActivity.state = MainActivity.REVIEW_PHOTO_STATE;
                state = mainActivity.state;
                update = true;
            }
        } else if (reviewTimeout == 0) {
            update = true;
        }

        if (parameters.getIsBlankScreen()) {
            return;
        }

        if (stereoCamera == null) {
            return;
        }

        if (state == MainActivity.LIVE_VIEW_STATE) {
            drawLiveView();
        } else if (state == MainActivity.REVIEW_PHOTO_STATE) {
            drawReview();
        } else if (state == MainActivity.REVIEW_AI_EDIT_STATE) {
            drawReview();
        }
//        if (parameters.isStereoscopeCameraMode()) {
//            if (zoom) {
//                textSize(48);
//                fill(yellow);
//                textAlign(LEFT);
//                text("zoom +" + magnifyScale[magnifyIndex] + "    ", width - 400, height - 4);
//            }
//        }

        if (parameters.isPhotoBoothCameraMode()) {
//            if (zoom && magnifyScale[magnifyIndex] > 1.0f) {
//                textSize(48);
//                fill(yellow);
//                textAlign(LEFT);
//                text("+" + magnifyScale[magnifyIndex] + "    ", width - 200, height - 4);
//            }

            // camera and review mode display test mode for debug
            if (DEBUG && testMode) {
                textSize(48);
                fill(yellow);
                textAlign(LEFT);
                text("mirror=" + mirror + " zoom=" + zoom + " w=" + imgLeft.width + " h=" + imgLeft.height, width / 8, height - 96);
                text("parallax=" + (parameters.getParallaxOffset()) + " vertical=" + (parameters.getVerticalOffset()) + " magnify=" + magnifyScale[magnifyIndex], width / 8, height - 48);
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

            if (state == MainActivity.LIVE_VIEW_STATE) {
                text("Live", 50, height - 48);
            } else if (state == MainActivity.REVIEW_PHOTO_STATE) {
                text("Review", 50, height - 48);
            } else if (state == MainActivity.REVIEW_AI_EDIT_STATE) {
                text("Review", 50, height - 48);
            }
            text(sMode, 50, height - 96);

            //if (mainActivity.state == MainActivity.LIVE_VIEW_STATE) {
            if (state == MainActivity.LIVE_VIEW_STATE) {
                textAlign(CENTER);
                if (parameters.isPhotoBoothCameraMode()) {
                    text(parameters.getInst1(), width / 2, 50);
                    text(parameters.getInst2(), width / 2, 100);
                    if (displayMode == DisplayMode.SBS) {
                        text(parameters.getTitle1(), width / 2, height - 96);
                        text(parameters.getTitle2(), width / 2, height - 48);
                    }
                }
                textAlign(LEFT);
                if (crossEye) {
                    text("X Eye", (float) (9 * width) / 10, height - 96);
                }
                if (displayMode == DisplayMode.ANAGLYPH) {
                    text("px=" + (parameters.getParallaxOffset()) + "   ", (float) (9 * width) / 10, height - 96);
                    text("vt=" + (parameters.getVerticalOffset()) + "   ", (float) (9 * width) / 10, height - 48);
                }
            } else if (state == MainActivity.REVIEW_PHOTO_STATE) {
                textAlign(RIGHT);
                fill(green);
                text("Print", width - 50, height - 48);
                if (parameters.getSbsCropPrint() && displayMode == DisplayMode.SBS) {
                    text("Crop", width - 50, height - 96);
                }
            } else if (state == MainActivity.REVIEW_AI_EDIT_STATE) {
                textAlign(RIGHT);
                fill(magenta);
                text("AI Edit", width - 50, height - 48);

            }
        }
        switch (debugHelp) {
            // overlays everything on screen
            case 1:
                fill(255);
                textAlign(LEFT);
                textSize(48);
                for (int i = 0; i < 20; i++) {
                    text(help[i], 100, 36 + i * 50);
                }
                for (int i = 20; i < help.length; i++) {
                    text(help[i], width / 2 + 100, 36 + (i - 20) * 50);
                }
                break;
            case 2:
                fill(255);
                textAlign(LEFT);
                textSize(48);
                for (int i = 0; i < help2.length; i++) {
                    text(help2[i], 100, 36 + i * 50);
                }

                break;
            default:
                break;
        }
//        if (parameters.isStereoscopeCameraMode()) {
//            if (stereoCamera.getFunctionMode() == Camera3D.FUNCTION_MODE_EV) {
//                drawEv();
//            } else if (stereoCamera.getFunctionMode() == Camera3D.FUNCTION_MODE_PARALLAX) {
//                drawParallax();
//            }
//        }
        if (showMenu) {
            gui.displayMenuBar();
        }

        if (mainActivity.isLiveviewFunction()) {
            drawEv();
        } else if (mainActivity.isParallaxFunction()) {
            drawParallax();
        } else if (mainActivity.isZoomFunction()) {
            drawZoom();
        }

        if (labelFrameCount > 0) {
            labelFrameCount--;
            drawImageLabel();
        }
//        else if (state == MainActivity.REVIEW_PHOTO_STATE) {
//            drawImageLabel();
//        }

        // display draw frame counter for debug
        if (testCheckDraw) {
            fill(yellow);
            textSize(48);
            text(""+frameCount, width/2, height/3);
        }

        // last thing to check is screenshot
        if (screenshot) {
            saveScreenshot();
            screenshot = false;
        }
    }

    private void drawLiveView() {

        // Synchronize with ImageReader thread: grab reference only when pixels are stable
        if (stereoCamera.available.get()) {
            synchronized (stereoCamera.imageLock) {
                if (stereoCamera.available.compareAndSet(true, false)) {
                    imgLeft = stereoCamera.leftImage;
                    imgRight = stereoCamera.rightImage;
                    AR = (float) imgLeft.width / (float) imgLeft.height;
                }
            }
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
        float imgWidth = (float) XBP_DISPLAY_FRAME_WIDTH / 2;
        float imgHeight = imgWidth / AR;

        // Center vertically within frame
        float baseVerticalOffset = frameY + (XBP_DISPLAY_FRAME_HEIGHT - imgHeight) / 2;

        // Calculate zoom offsets - these keep the zoomed image centered in its half-frame
        if (zoom) {
            offsetX = ((imgWidth * (1 - 1 / magnifyScale[magnifyIndex])) / 2) + shiftOffsetX;
            offsetY = ((imgHeight * (1 - 1 / magnifyScale[magnifyIndex])) / 2) + shiftOffsetY;
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

        if (crossEye) {
            image(imgRight, -offsetX, -offsetY, imgWidth, imgHeight);
        } else {
            image(imgLeft, -offsetX, -offsetY, imgWidth, imgHeight);
        }
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

        if (crossEye) {
            image(imgLeft, -offsetX, -offsetY, imgWidth, imgHeight);
        } else {
            image(imgRight, -offsetX, -offsetY, imgWidth, imgHeight);
        }

        pop();
        noClip();

        if (grid) {
            drawGrid(false);
        }
        // show any display pause with rotating text
        if (rotating) {
            textSize(48);
            fill(IGui.dimyellow);
            textAlign(CENTER, CENTER);
            rotatingIndex = (rotatingIndex + 1) % rotatingText.length;
            text(rotatingText[rotatingIndex], width / 2, height / 2);
        }
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
            offsetX = ((anaglyphW * (1 - 1 / magnifyScale[magnifyIndex])) / 2) + shiftOffsetX;
            offsetY = ((height * (1 - 1 / magnifyScale[magnifyIndex])) / 2) + shiftOffsetY;
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

        if (grid) {
            drawGrid(true);
        }
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
            offsetX = ((anaglyphW * (1 - 1 / magnifyScale[magnifyIndex])) / 2) + shiftOffsetX;
            offsetY = ((height * (1 - 1 / magnifyScale[magnifyIndex])) / 2) + shiftOffsetY;
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

        if (grid) {
            drawGrid(true);
        }
    }

    // experimental noClip() option
    public void drawPhotoNoClip(PImage img) {
        float offsetX = 0;
        float offsetY = 0;
        float anaglyphW = (float) height * AR;
        float displayX = ((float) width - anaglyphW) / 2;

        push();
        // Center the rendering origin to the display box on screen
        translate(displayX, 0);
        translate(-(float) parallax / 2, -(float) verticalAlignment / 2);

        if (mirror) {
            translate(anaglyphW, 0);
            scale(-1, 1);
        }

        if (zoom) {
            // Calculate crop boundaries relative to the source image size
            float scaleFactor = magnifyScale[magnifyIndex];
            int srcW = (int) (((float) img.width) / scaleFactor);
            int srcH = (int) (((float) img.height) / scaleFactor);
            int srcX = (img.width - srcW) / 2;
            int srcY = (img.height - srcH) / 2;

            // Draw only the zoomed sub-section stretched to the destination size
            image(img, 0, 0, anaglyphW, height, srcX, srcY, srcX + srcW, srcY + srcH);
        } else {
            // Draw the full image normally
            image(img, 0, 0, anaglyphW, height);
        }
        pop();

        if (grid) {
            drawGrid(true);
        }
    }

    void drawGrid(boolean full) {
        fill(yellow);
        int thickness = 2;
        int top = MainActivity.HIDDEN_SHUTTER_BUTTON_Y + 10;
        int bottom = 96 + top;
        int leftMargin = frameX;
        int rightMargin = frameX;
        if (full) {
            // Horizontal line (center of canvas)
            rect(leftMargin, height / 2 - thickness / 2, width - leftMargin - rightMargin, thickness);
            // Vertical line (center of canvas)
            rect(width / 2 - thickness / 2, top, thickness, height - bottom);
        } else {
            // Horizontal line (center of frame)
            rect(leftMargin, XBP_DISPLAY_FRAME_HEIGHT / 2 - thickness / 2, width - leftMargin - rightMargin, thickness);
            // First vertical line (1/4 of frame width)
            rect(frameX + XBP_DISPLAY_FRAME_WIDTH / 4 - thickness / 2, top, thickness, XBP_DISPLAY_FRAME_HEIGHT - bottom);
            // Second vertical line (3/4 of frame width)
            rect(frameX + 3 * XBP_DISPLAY_FRAME_WIDTH / 4 - thickness / 2, top, thickness, XBP_DISPLAY_FRAME_HEIGHT - bottom);
        }
    }

    void drawEv() {
        if (!showEv) return;
        String ev = stereoCamera.getEv();
//        if (stereoCamera.getFunctionMode() == Camera3D.FUNCTION_MODE_EV) {
//            ev = Camera3D.METERING_NAMES[parameters.getExposureMeteringIndex()]  + ev;
//        }

        int level = DISPLAY_OFFSET_Y;
        DisplayMode position = displayMode.get();
        if (position == DisplayMode.SBS) { // for stereoscope
            showString(ev, LEFT, 0, level);
            showString(ev, RIGHT, STEREO_OFFSET, level);
        } else { //monoscopic
            showString(ev, CENTER, 0, level);
        }
    }

    void drawParallax() {
        if (!showParallax) return;
        String spx = "PX ";
        int px = parameters.getParallaxOffset();
        if (px >= 0) spx += "+" + px;
        else spx += px;
//        if (stereoCamera.getFunctionMode() == Camera3D.FUNCTION_MODE_PARALLAX) {
//            px = "= " + Camera3D.FOCUS_DISTANCE_NAMES[parameters.getFocusDistanceIndex()] + " " + px;
//        }

        int level = DISPLAY_OFFSET_Y;
        DisplayMode position = displayMode.get();
        if (position == DisplayMode.SBS) { // for stereoscope
            showString(spx, LEFT, 0, level);
            showString(spx, RIGHT, STEREO_OFFSET, level);
        } else { //monoscopic
            showString(spx, CENTER, 0, level);
        }
    }

    void drawZoom() {
        if (!showZoom) return;
        String sZoom = "+" + magnifyScale[magnifyIndex] + "  x: " + (int)shiftOffsetX + "  y: " + (int)shiftOffsetY;
//        if (stereoCamera.getFunctionMode() == Camera3D.FUNCTION_MODE_PARALLAX) {
//            px = "= " + Camera3D.FOCUS_DISTANCE_NAMES[parameters.getFocusDistanceIndex()] + " " + px;
//        }

        int level = DISPLAY_OFFSET_Y;
        DisplayMode position = displayMode.get();
        if (position == DisplayMode.SBS) { // for stereoscope
            showString(sZoom, LEFT, 0, level);
            showString(sZoom, RIGHT, STEREO_OFFSET, level);
        } else { //monoscopic
            showString(sZoom, CENTER, 0, level);
        }
    }

    public void setImageLabel(String imageLabel) {
        this.imageLabel = imageLabel;
        setImageLabelTimeout();
        update = true;
    }

    public void setImageLabelTimeout() {
        this.labelFrameCount = IMAGE_LABEL_TIMEOUT_FRAMES;
    }

    void drawImageLabel() {
        if (imageLabel == null) return;
        String label = imageLabel;
        if (label.isEmpty()) return;

        int level = DISPLAY_OFFSET_Y + 100;
        DisplayMode position = displayMode.get();
        if (position == DisplayMode.SBS) { // stereoscope
            showString(label, LEFT, 0, level);
            showString(label, RIGHT, STEREO_OFFSET, level);
        } else { //monoscopic
            showString(label, CENTER, 0, level);
        }
    }


    void showString(String ev, int position, int offset, int bottom) {
        int x;
        if (position == LEFT) {
            x = frameX + XBP_DISPLAY_FRAME_WIDTH / 4 + offset;
        } else if (position == RIGHT) {
            x = frameX + 3 * XBP_DISPLAY_FRAME_WIDTH / 4 + offset;
        } else {
            x = width / 2; // CENTER
        }
        textSize(24);
        fill(yellow);
        textAlign(CENTER, CENTER);
        text(ev, x, height - bottom);
    }

    // called by MainActivity onKeyUp to process key events for the photo booth exclusively
    public boolean processKeyCode() {
        if (lastKeyCode == 0) return true;
        if (DEBUG) println("processKeyCode() lastKeyCode=" + lastKeyCode);

        int iParallax;
        switch (lastKeyCode) {

            case KeyEvent.KEYCODE_A:
                displayMode = displayMode.next();
                break;
            case KeyEvent.KEYCODE_B:
                toggleBlankScreen();
                break;
            case KeyEvent.KEYCODE_C: // continuous capture (handled in MainActivity)
                lastKeyCode = 0;
                lastKey = 0;
                return false;
            case KeyEvent.KEYCODE_M:  // toggle mirror display (handled in MainActivity)
                lastKeyCode = 0;
                lastKey = 0;
                return false;
            case KeyEvent.KEYCODE_P:
                screenshot = true;
                break;
            case KeyEvent.KEYCODE_G:
                toggleGrid();
                break;
            case KeyEvent.KEYCODE_X:
                toggleCrossEye();
                break;

//            case KeyEvent.KEYCODE_FORWARD:  // 125 forward media button on mouse: mirror toggle
//                File mediaFile = media.getMediaFile();
//                if (mediaFile == null) {
//                    if (DEBUG) PApplet.println("Nothing for AI Edit");
//                }
//                media.shareImage2(media.getMediaFile(), Media.APP_AIEDIT_PACKAGE);
//                break;

            case KeyEvent.KEYCODE_Z:
            case MainActivity.BUTTON_Y_KEY:  // ZOOM
                if (state != MainActivity.LIVE_VIEW_STATE) {
                    toggleShowZoom();
                    if (showZoom) {
                        zoom = true;
                        int xfunction = MainActivity.FUNCTION_MODE_ZOOM;
                        mainActivity.setFunctionMode(xfunction);
                        gui.menuBar.setMenuKeyLabels(xfunction);
                        showMenu = true;
                    } else {
                        if (state == MainActivity.LIVE_VIEW_STATE) {
                            mainActivity.setFunctionMode(MainActivity.FUNCTION_MODE_LIVEVIEW);
                            gui.menuBar.setMenuKeyLabels(MainActivity.FUNCTION_MODE_LIVEVIEW);
                        } else {
                            mainActivity.setFunctionMode(MainActivity.FUNCTION_MODE_REVIEW);
                            gui.menuBar.setMenuKeyLabels(MainActivity.FUNCTION_MODE_REVIEW);

                        }
                    }
                }
                break;

            case MainActivity.BUTTON_X_KEY:  // PARALLAX
                toggleParallax();
                if (showParallax) {
                    int yfunction = MainActivity.FUNCTION_MODE_PARALLAX;
                    mainActivity.setFunctionMode(yfunction);
                    gui.menuBar.setMenuKeyLabels(yfunction);
                    showMenu = true;
                } else {
                    if (state == MainActivity.LIVE_VIEW_STATE) {
                        mainActivity.setFunctionMode(MainActivity.FUNCTION_MODE_LIVEVIEW);
                        gui.menuBar.setMenuKeyLabels(MainActivity.FUNCTION_MODE_LIVEVIEW);
                    } else {
                        mainActivity.setFunctionMode(MainActivity.FUNCTION_MODE_REVIEW);
                        gui.menuBar.setMenuKeyLabels(MainActivity.FUNCTION_MODE_REVIEW);

                    }
                }

                break;

            case MainActivity.BUTTON_B_KEY:
                if (DEBUG) println("B key " + (mainActivity.isZoomFunction()) + " " + zoom);
                if (mainActivity.isLiveviewFunction()) {
                    toggleEv();
                } else if (mainActivity.isReviewFunction()) {
                    setImageLabelTimeout();
                } else if (mainActivity.isParallaxFunction()) {
                    toggleParallax();
                } else if (mainActivity.isZoomFunction()) {
                    toggleShowZoom();
                }
                break;

            case MainActivity.BUTTON_A_KEY:
                if (DEBUG) println("button A");
                if (mainActivity.getFunctionMode() == MainActivity.FUNCTION_MODE_ZOOM) {
                    //resetZoom();
                } else if (mainActivity.getFunctionMode() == MainActivity.FUNCTION_MODE_PARALLAX) {
                    //showParallax = !showParallax;
                    //mainActivity.setFunctionMode(MainActivity.FUNCTION_MODE_LIVEVIEW);
                    //gui.menuBar.setMenuKeyLabels(MainActivity.FUNCTION_MODE_LIVEVIEW);
                } else if (state == MainActivity.REVIEW_PHOTO_STATE) {
                    mainActivity.setFunctionMode(MainActivity.FUNCTION_MODE_REVIEW);
                    gui.menuBar.setMenuKeyLabels(MainActivity.FUNCTION_MODE_REVIEW);
                } else {
                    mainActivity.setFunctionMode(MainActivity.FUNCTION_MODE_LIVEVIEW);
                    gui.menuBar.setMenuKeyLabels(MainActivity.FUNCTION_MODE_LIVEVIEW);
                }

                toggleShowMenu();
                break;

            case MainActivity.UP_ARROW_KEY:
                if (state == MainActivity.REVIEW_PHOTO_STATE && mainActivity.isReviewFunction()) {

                    if (currentIndex != sbsImageFiles.size() - 1) {
                        currentIndex = sbsImageFiles.size() - 1;
                        reloadReviewImage(currentIndex);
                    } else {
                        setReviewLabel("End");
                    }
                } else if (mainActivity.isParallaxFunction()) {
//                    if (showParallax) {
//                        iParallax = parameters.getParallaxOffset() + DELTA_PARALLAX;
//                        parameters.setParallaxOffset(iParallax);
//                        parallax = toDisplayPixels(iParallax);
//                        if (DEBUG) PApplet.println("iParallax = " + iParallax);
//                    }
                } else if (mainActivity.isZoomFunction() && zoom) {
                    shiftOffsetY -= shiftOffsetDelta[magnifyIndex];
                }
                break;

            case MainActivity.DOWN_ARROW_KEY:
                if (state == MainActivity.LIVE_VIEW_STATE && mainActivity.isLiveviewFunction()) {
                    //resetZoom();  removed - TODO replace with crop??
                } else if (state == MainActivity.REVIEW_PHOTO_STATE && mainActivity.isReviewFunction()) {
                    if (currentIndex != 0) {
                        currentIndex = 0;
                        reloadReviewImage(currentIndex);
                    } else {
                        setReviewLabel("Begin");
                    }
                } else if (mainActivity.isParallaxFunction()) {
//                    if (showParallax) {
//                        iParallax = parameters.getParallaxOffset() - DELTA_PARALLAX;
//                        parameters.setParallaxOffset(iParallax);
//                        parallax = toDisplayPixels(iParallax);
//                        if (DEBUG) PApplet.println("iParallax = " + iParallax);
//                    }
                } else if (mainActivity.isZoomFunction() && zoom) {
                    shiftOffsetY += shiftOffsetDelta[magnifyIndex];
                }
                break;

            case KeyEvent.KEYCODE_PERIOD:
            case MainActivity.RIGHT_ARROW_KEY:
                //if (showMenu) {
//                if (mainActivity.isLiveviewFunction()) {
//                    int index = stereoCamera.incrementExposureCompensation(1);
//                    parameters.setExposureCompensationIndex(index);
//                } else
                if (mainActivity.isParallaxFunction()) {
                    if (showParallax) {
                        iParallax = parameters.getParallaxOffset() + DELTA_PARALLAX;
                        parameters.setParallaxOffset(iParallax);
                        parallax = toDisplayPixels(iParallax);
                        if (DEBUG) PApplet.println("iParallax = " + iParallax);
                    }

                } else if (mainActivity.isZoomFunction()) {
                    shiftOffsetX -= shiftOffsetDelta[magnifyIndex];
                } else if (mainActivity.isReviewFunction()) {
                    //               if (state == MainActivity.REVIEW_PHOTO_STATE) {
                    currentIndex++;
                    if (currentIndex >= sbsImageFiles.size()) {
                        currentIndex--;
                        setReviewLabel("End");
                    } else {
                        reloadReviewImage(currentIndex);
                    }
                }
                //}
                break;
            case KeyEvent.KEYCODE_COMMA:
            case MainActivity.LEFT_ARROW_KEY:
                //if (showMenu) {
//                if (mainActivity.isLiveviewFunction()) {
//                    int index = stereoCamera.decrementExposureCompensation(1);
//                    parameters.setExposureCompensationIndex(index);
//                } else
                if (mainActivity.isParallaxFunction()) {
                    if (showParallax) {
                        iParallax = parameters.getParallaxOffset() - DELTA_PARALLAX;
                        parameters.setParallaxOffset(iParallax);
                        parallax = toDisplayPixels(iParallax);
                        //if (DEBUG) PApplet.println("iParallax = " + iParallax);
                    }
                } else if (mainActivity.isZoomFunction()) {
                    shiftOffsetX += shiftOffsetDelta[magnifyIndex];
                } else if (state == MainActivity.REVIEW_PHOTO_STATE) {
                    currentIndex--;
                    if (currentIndex < 0) {
                        currentIndex = 0;
                    } else {
                        reloadReviewImage(currentIndex);
                    }
                }

                break;

            case KeyEvent.KEYCODE_MINUS:
            case MainActivity.BUTTON_MINUS_KEY:
            case KeyEvent.KEYCODE_LEFT_BRACKET:
                if (mainActivity.isLiveviewFunction()) {
                    if (showEv) {
                        int index = stereoCamera.decrementExposureCompensation(1);
                        parameters.setExposureCompensationIndex(index);
                    }

                } else if (mainActivity.isReviewFunction()) {
//                    currentIndex--;
//                    if (currentIndex < 0) {
//                        currentIndex = 0;
//                    } else {
//                        reloadReviewImage(currentIndex);
//                    }

                } else if (mainActivity.isParallaxFunction()) {
                    if (showParallax) {
                        iParallax = parameters.getParallaxOffset() - 1;
                        parameters.setParallaxOffset(iParallax);
                        parallax = toDisplayPixels(iParallax);
                        if (DEBUG) PApplet.println("parallax = " + parallax);
                    }
                } else if (mainActivity.isZoomFunction()) {
                    if (zoom) {
                        if (magnifyIndex > 0) {
                            magnifyIndex--;
                            update = true;
                        }
                    }
                }
                break;

            case KeyEvent.KEYCODE_PLUS:
            case KeyEvent.KEYCODE_EQUALS:
            case MainActivity.BUTTON_PLUS_KEY:
            case KeyEvent.KEYCODE_RIGHT_BRACKET:
                if (mainActivity.isLiveviewFunction()) {
                    if (showEv) {
                        int index = stereoCamera.incrementExposureCompensation(1);
                        parameters.setExposureCompensationIndex(index);
                    }

                } else if (mainActivity.isReviewFunction()) {
//                    currentIndex++;
//                    if (currentIndex >= sbsImageFiles.size()) {
//                        currentIndex--;
//                    } else {
//                        reloadReviewImage(currentIndex);
//                    }

                } else if (mainActivity.isParallaxFunction()) {
                    if (showParallax) {
                        iParallax = parameters.getParallaxOffset() + 1;
                        parameters.setParallaxOffset(iParallax);
                        parallax = toDisplayPixels(iParallax);
                        //if (DEBUG) PApplet.println("parallax = " + parallax);
                    }
                } else if (mainActivity.isZoomFunction()) {
                    if (zoom) {
                        if (magnifyIndex < magnifyScale.length - 1) {
                            magnifyIndex++;
                            update = true;
                        }
                    }
                }
                break;

            case MainActivity.MODE_KEY:  // processing in MainActivity
                break;

            case KeyEvent.KEYCODE_U:
            case KeyEvent.KEYCODE_SPACE:
                toggleShowMenu();
                break;
            case KeyEvent.KEYCODE_T:
                toggleShowPhotoBoothTitle();
                break;
            case KeyEvent.KEYCODE_H:  // help screens for debug
            case KeyEvent.KEYCODE_I:
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
                    for (String s : help) {
                        if (DEBUG) PApplet.println(s);
                    }
                }
                break;
            case KeyEvent.KEYCODE_D:
                // toggle show frame count
                testCheckDraw = !testCheckDraw;
                break;
            default:
                lastKeyCode = 0;
                lastKey = 0;
                return false;
        }
        update = true;
        lastKeyCode = 0;
        lastKey = 0;
        return true;
    }

    void saveScreenshot() {
        String dateTime = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        String filename = Media.SCREENSHOT_PREFIX + dateTime + Media.SCREENSHOT_FILETYPE;
        //Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures/Screenshots/" + filename;
        String filePath = Environment.getExternalStorageDirectory() + File.separator + Media.BASE_FOLDER
                + File.separator + Media.SAVE_FOLDER + File.separator + Media.SAVE_SCREENSHOT_FOLDER + File.separator + filename;
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
//            line(0, XBP_DISPLAY_FRAME_HEIGHT / 2, width, XBP_DISPLAY_FRAME_HEIGHT / 2);
//            line(frameX + XBP_DISPLAY_FRAME_WIDTH / 4, 0, frameX + XBP_DISPLAY_FRAME_WIDTH / 4, XBP_DISPLAY_FRAME_HEIGHT);
//            line(frameX + 3 * XBP_DISPLAY_FRAME_WIDTH / 4, 0, frameX + 3 * XBP_DISPLAY_FRAME_WIDTH / 4, XBP_DISPLAY_FRAME_HEIGHT);
//        }
//    }

    /**
     * =================================================================================================
     * Review Code
     */

    // TODO exit and restore take care of last image on application start up
    public void exit() {
        if (DEBUG) println("exit PhotoBooth .........");

    }

    public void setReviewImages(PImage left, PImage right, File reviewSBS) {
        update = false;
        //if (DEBUG) PApplet.println("setReviewImages() left=" + left + " right=" + right);
        currentLeft = left;
        currentRight = right;
        if (currentLeft != null && currentRight != null) {
            update = true;
        }
        if (reviewSBS != null) {
            sbsImageFiles.add(reviewSBS.getAbsolutePath());
            currentIndex = sbsImageFiles.size() - 1;
        }
        //if (DEBUG) PApplet.println("setReviewImages() added: " + sbsImageFiles.get(currentIndex));
    }

    void drawReview() {
        // PApplet.println("drawReview()");
        synchronized (media.reviewLock) {
            if (currentLeft != null && currentRight != null && currentLeft.width > 0 && currentLeft.height > 0 && currentRight.width > 0 && currentRight.height > 0) {
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
        }
    }

    /**
     * Load image file list from external storage
     *
     * @return true if images loaded, false if failed to load any image
     */
    public boolean loadImageFileList() {
        sbsImageFiles = new ArrayList<String>();

        // Get the external storage directory
        File externalStorage = Environment.getExternalStorageDirectory();
        File saveFolder = new File(externalStorage, Media.BASE_FOLDER + File.separator + Media.SAVE_FOLDER);
        if (DEBUG) println("saveFolder=" + saveFolder.getAbsolutePath());
        if (!saveFolder.exists() || !saveFolder.isDirectory()) {
            if (DEBUG) PApplet.println("Folder not found: " + saveFolder.getAbsolutePath());
            return false;
        }

        // Get all JPG/JPEG files
        File[] files = saveFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String name = file.getName().toLowerCase();
                    if (DEBUG) println("sbs file name: " + name);
                    String fullPath = file.getAbsolutePath();
                    if (DEBUG) println("sbs file path: " + fullPath);
                    if (fullPath.toLowerCase().endsWith(".jpg") || fullPath.toLowerCase().endsWith(".jpeg")) {
                        String nameWithoutExt = fullPath.substring(0, fullPath.lastIndexOf('.'));
                        if (DEBUG) println("nameWithoutExt: " + nameWithoutExt);
                        if (nameWithoutExt.toLowerCase().endsWith("_2x1")) {
                            boolean sbsFilesAdded = sbsImageFiles.add(fullPath);
                            if (sbsFilesAdded) println("sbs file added: " + fullPath);
                        }
                    }
                }
            }
        }

        // Sort list in ascending order
        Collections.sort(sbsImageFiles);

        currentIndex = sbsImageFiles.size() - 1;
        if (DEBUG) PApplet.println("Found " + sbsImageFiles.size());
        if (currentIndex < 0) {
            return false;
        }
        if (DEBUG) PApplet.println("Last: " + sbsImageFiles.get(currentIndex));

        return true;
    }

    // Helper method to get base filename without _l/_r suffix and extension
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

//    private boolean loadCurrentImage() {
//        boolean imagesLoaded = false;
//        if (DEBUG)
//            PApplet.println("loadCurrentImage() state=" + MainActivity.stateName[mainActivity.state]);
//        if (sbsImageFiles.isEmpty() || currentIndex < 0 || currentIndex >= sbsImageFiles.size()) {
//            if (DEBUG) PApplet.println("loadCurrentImage failed");
//            return imagesLoaded;
//        }
//
//        String sbsPath = sbsImageFiles.get(currentIndex);
//        if (DEBUG) PApplet.println("Loading pair " + (currentIndex) + "/" + sbsImageFiles.size());
//
//        // Load sbs image
//        currentLeft = loadImage(sbsPath);
//        if (currentLeft == null) {
//            if (DEBUG) PApplet.println("Failed to load left image");
//            return imagesLoaded;
//        }
//
//        update = true;
//        imagesLoaded = true;
//        if (DEBUG) PApplet.println("loadCurrentImage success.");
//        return imagesLoaded;
//    }

    // copied from PApplet.java Processing-Android
    public PImage loadImage(String filename) {
        if (DEBUG) System.out.println("loadImage " + filename);
        InputStream stream = createInput(filename);
        if (stream == null) {
            System.err.println("Could not find the image " + filename + ".");
            return null;
        } else {
            Bitmap bitmap = null;

            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888; // specify a config

                // 1. Crucial: Tell the decoder to reuse our existing memory allocation buffer
                options.inBitmap = bitmap;
                // 2. Ensure mutable status so it can be safely written over
                options.inMutable = true;
                options.inSampleSize = 1;

                bitmap = decodeStream(stream, null, options);
            } finally {
                try {
                    stream.close();
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

    private PImage[] splitImageLR(PImage original, int imageWidth, int imageHeight) {
        if (DEBUG) println("splitImageLR");
        // Create an array to hold the two resulting images
        PImage[] result = new PImage[2];

        // Calculate the width of each half, using integer division
        int halfWidth = original.width / 2;

        // Create the left half image
        if (media.leftReview == null) {
            result[0] = createImage(imageWidth, imageHeight, ARGB);
            result[0].copy(original, 0, 0, halfWidth, original.height, 0, 0, halfWidth, original.height);
        } else {
            media.leftReview.copy(original, 0, 0, halfWidth, original.height, 0, 0, halfWidth, original.height);
            result[0] = media.leftReview;
        }
        // Create the right half image
        if (media.rightReview == null) {
            result[1] = createImage(imageWidth, imageHeight, ARGB);
            result[1].copy(original, halfWidth, 0, halfWidth, original.height, 0, 0, halfWidth, original.height);
        } else {
            media.rightReview.copy(original, halfWidth, 0, halfWidth, original.height, 0, 0, halfWidth, original.height);
            result[1] = media.rightReview;
        }
        ((Bitmap) original.getNative()).recycle();

//        if (DEBUG) {
//            println("leftReview bitmap=" + ((Bitmap) result[0].getNative()));
//            println("rightReview bitmap=" + ((Bitmap) result[1].getNative()));
//        }
        return result;
    }

    public File make6x4ImageFile(String filename) {
        return null;
    }

    /**
     * resize image to 6x4 aspect ratio pixels
     * Handles all input aspect ratios without distortion using letterbox/pillarbox
     * PImage input image
     * returns resized image padded to 1620x1080 with white borders
     */
    public PImage resizeToPrint6x4(PImage img) {
        if (DEBUG)
            println("resizeToPrint6x4() convert image to 6x4 aspect ratio img.width=" + img.width + " img.height=" + img.height);
        float printWidth = 1800;//1620;//1680;
        float printHeight = 1200;//1080;//1120;
        PImage resizeImage = createImage((int) printWidth, (int) printHeight, ARGB);

        // Fill background white
        resizeImage.loadPixels();
        for (int i = 0; i < resizeImage.pixels.length; i++) {
            resizeImage.pixels[i] = color(0xFFFFFFFF); // white, fully opaque
        }
        resizeImage.updatePixels();

        // Calculate scale factor to fit img inside 1800x1200 preserving aspect ratio
        float scaleX = printWidth / (float) img.width;
        float scaleY = printHeight / (float) img.height;
        float scale = min(scaleX, scaleY);

        int scaledW = (int) printWidth;
        int scaledH = (int) (img.height * scale);

        // Center the scaled image
        int offsetX = ((int) printWidth - scaledW) / 2;
        int offsetY = ((int) printHeight - scaledH) / 2;

        if (DEBUG)
            println("resize6x4() scale=" + scale + " scaledW=" + scaledW + " scaledH=" + scaledH + " offsetX=" + offsetX + " offsetY=" + offsetY);

        resizeImage.copy(img, 0, 0, img.width, img.height, offsetX, offsetY, scaledW, scaledH);

        return resizeImage;
    }

    /**
     * Processing mouseReleased event handler
     * This code overrides main activity's decorView.setOnTouchListener(new View.OnTouchListener()
     * Currently this implements the same features but is subject to change
     */
    public void mouseReleased() {
        int x = mouseX;
        int y = mouseY;
        if (gui.menuBar.isOutside(x, y)) {
            toggleShowMenu();
            return;
        }
        if (showMenu) {
            int keyCode = gui.mousePressed(x, y);
            if (keyCode > 0)
                mainActivity.onKeyUp(keyCode, null);
            return;
        }
        // upper right corner is shutter release when menu button is not visible
        if (x > MainActivity.HIDDEN_SHUTTER_BUTTON_X && y < MainActivity.HIDDEN_SHUTTER_BUTTON_Y) {
            if (DEBUG) PApplet.println("mouseReleased shutter release");
            mainActivity.capturePhoto();
            // upper left corner is settings menu invisible button
        } else if (x < MainActivity.HIDDEN_MODE_BUTTON_X && y < MainActivity.HIDDEN_MODE_BUTTON_Y) {
            if (DEBUG) PApplet.println("mouseReleased photo booth mode change");
            mainActivity.processModeChange(); // liveview/review
        }
    }

    public void setKeyCode(int lastKeyCode, int lastKey, boolean keyUp) {
        if (lastKeyCode == 0) return;
        if (keyUp) {
            this.lastKey = lastKey;
            this.lastKeyCode = lastKeyCode;
            println("setKeyCode " + lastKeyCode);
        } else {  // key down
            switch (lastKeyCode) {
                case MainActivity.LEFT_ARROW_KEY:
                case MainActivity.RIGHT_ARROW_KEY:
                case MainActivity.UP_ARROW_KEY:
                case MainActivity.DOWN_ARROW_KEY:
                    this.lastKey = lastKey;
                    this.lastKeyCode = lastKeyCode;
                    break;
            }
        }
    }

////////////////////////////////////////////////////////////////////////////////////////////////
    // NOT used for reference
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

