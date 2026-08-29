package com.andymodla.android3dcamera;

import static android.Manifest.permission.CAMERA;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import androidx.core.content.ContextCompat;
import com.andymodla.android3dcamera.camera.Camera3D;
import com.andymodla.android3dcamera.camera.CameraInfoUtil;
import com.andymodla.android3dcamera.sketch.photobooth.PhotoBooth;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import processing.android.CompatUtils;
import processing.android.PFragment;

// README:   https://github.com/ajavamind/A3DCamera/blob/main/README.md

/**
 * A3DCamera app
 * Copyright 2025-2026, Andy Modla All Rights Reserved
 */
public class MainActivity extends AppCompatActivity {

    public static final String TAG = "A3DCamera";

    private static final int MY_CAMERA_REQUEST_CODE = 100;
    private static final String PREFS_NAME = "Parameters";

    volatile boolean allPermissionsGranted = false;
    private volatile boolean cameraInitialized = false;

    // aspect ratio
    int aspectRatioIndex = 0;  // default

    private AIvision aiVision;  // local network small multimodal vision AI model server (Google Gemma 3 8B 4_K_M GGUF)
    private Media media;
    private Camera3D camera;
    public static Parameters parameters;

    private boolean aiVisionEnabled = false;
    public boolean isAiEdit = false;
    private UdpRemoteControl udpRemoteControl;

    // stereoscope camera and photo booth state definitions
    public static final int LIVE_VIEW_STATE = 0;
    public static final int REVIEW_PHOTO_STATE = 1;
    public static final int REVIEW_AI_EDIT_STATE = 2;
    public volatile int state = LIVE_VIEW_STATE;
    public static final String[] stateName = {"LIVE_VIEW_STATE", "REVIEW_PHOTO_STATE", "REVIEW_AI_EDIT_STATE"};
    public volatile DisplayMode displayMode = DisplayMode.SBS;

    // function modes definitions - changes GUI key labels and key function
    public static final int FUNCTION_MODE_LIVEVIEW = 0; //
    public static final int FUNCTION_MODE_REVIEW = 1;
    public static final int FUNCTION_MODE_PARALLAX = 2; // runs with both live view and review states
    public static final int FUNCTION_MODE_ZOOM = 3; // runs with both live view and review states
    public static final int NUMBER_OF_FUNCTIONS = 4;
    public int functionMode = FUNCTION_MODE_LIVEVIEW;

    private boolean exitApp = false; // exit app flag with back or esc button

    // Photo booth variables
    public boolean isSimpleCamera = false;  // Force app to be either stereoscope or photo booth - no Simple Camera
    public PhotoBooth photoBooth;  // photo booth sketch
    PFragment photoBoothFragment;  // processing library photo booth fragment
    View decorView; // screen window view for camera app

    public String hostIpAddr = "";
    public int hostPort = 8333;
    public ImageSender imageSender;  // camera sends last picture URL link to an android 3D display device

    Timer countdownTimer = null;
    int countdownStart = 0;
    int countdownDigit = -1;

    volatile boolean continuousModeFeature = true;
    volatile boolean continuousMode = false;  // continuous capture is active
    public volatile int continuousCounter = 0;
    public volatile int labelContinuousCounter = 0;
    public static int CONTINUOUS_COUNT_PHOTO_BOOTH = 3; //(one less 4 photos captured for a strip collection)
    public static int CONTINUOUS_COUNT_DEFAULT = (24 * 60 * 60 / 2) - 1; // second count is one less - one day
    public int CONTINUOUS_COUNT = 0;

    // Key codes for Photo Booth Buzzer Box, Beam Pro device: Camera, Volume up and down functionality
//    static final int PB_SHUTTER_KEY = KeyEvent.KEYCODE_CAMERA;  // Camera shutter
//    static final int PB_PRINT_KEY = KeyEvent.KEYCODE_CAMERA;    // Review Photo Function and launch print
//    static final int PB_AI_EDITOR_KEY = KeyEvent.KEYCODE_CAMERA; // Review Photo Function and launch AI Editor
//    static final int PB_STATE_TOGGLE_KEY = KeyEvent.KEYCODE_VOLUME_UP; // Toggle through states round-robin
//    static final int PB_IMAGE_TOGGLE_KEY = KeyEvent.KEYCODE_VOLUME_DOWN;  // Toggle through photo views: SBS, Anaglyph, Left Eye, Right Eye

    // Key codes for 8BitDo Micro Bluetooth Keyboard controller (Android mode)

    // Key codes for ShanWan Q36 Bluetooth Mini Game Controller
    // S switch set for Android mode the center position on the controller
    public static final int SHUTTER_KEY = KeyEvent.KEYCODE_BUTTON_R1; // 103
    public static final int ANAGLYPH_KEY = KeyEvent.KEYCODE_BUTTON_R2; // 105
    public static final int MODE_KEY = KeyEvent.KEYCODE_BUTTON_L1; // 102
    public static final int SETTINGS_KEY = KeyEvent.KEYCODE_BUTTON_L2; // 104

    public static final int UP_ARROW_KEY = KeyEvent.KEYCODE_DPAD_UP; // 19 up arrow
    public static final int DOWN_ARROW_KEY = KeyEvent.KEYCODE_DPAD_DOWN; // 20 down arrow
    public static final int LEFT_ARROW_KEY = KeyEvent.KEYCODE_DPAD_LEFT; // 21 left arrow
    public static final int RIGHT_ARROW_KEY = KeyEvent.KEYCODE_DPAD_RIGHT; // 22 right arrow

    public static final int BUTTON_PLUS_KEY = KeyEvent.KEYCODE_BUTTON_START; // 108 "+" button
    public static final int BUTTON_MINUS_KEY = KeyEvent.KEYCODE_BUTTON_SELECT; // 109 "-" button

    // game controller key labels do not match Android key codes it outputs!
    public static final int BUTTON_X_KEY = KeyEvent.KEYCODE_BUTTON_Y; //  100 up
    public static final int BUTTON_Y_KEY = KeyEvent.KEYCODE_BUTTON_X; //  99 up
    public static final int BUTTON_B_KEY = KeyEvent.KEYCODE_BUTTON_A;  //  96 up
    public static final int BUTTON_A_KEY = KeyEvent.KEYCODE_BUTTON_B;  //  97 up

    static final int BACK_KEY = KeyEvent.KEYCODE_BACK;  // KEYCODE_BACK = 04
    static final int DPAD_CENTER_KEY = KeyEvent.KEYCODE_DPAD_CENTER;  // 23 up

// Key codes generated by 8BitDo Micro Bluetooth Keyboard controller (in Keyboard mode)
// FOR DOCUMENTATION ONLY - NOT USED
//    static final int SHUTTER_KB_KEY = KeyEvent.KEYCODE_M;
//    static final int FOCUS_DISTANCE_KB_KEY = KeyEvent.KEYCODE_R;
//    static final int MODE_KB_KEY = KeyEvent.KEYCODE_L;
//    static final int CONTINUOUS_KB_KEY = KeyEvent.KEYCODE_K;
//    static final int DISP_KB_KEY = KeyEvent.KEYCODE_C;
//    static final int ISO_KB_KEY = KeyEvent.KEYCODE_D;
//    static final int TIMER_KB_KEY = KeyEvent.KEYCODE_E;
//    static final int SHUTTER_SPEED_KB_KEY = KeyEvent.KEYCODE_F;
//    static final int PRINT_KB_KEY = KeyEvent.KEYCODE_N;
//    static final int ANAGLYPH_KB_KEY = KeyEvent.KEYCODE_O; // "+" button
//    static final int FN_KB_KEY = KeyEvent.KEYCODE_H;
//    static final int MENU_KB_KEY = KeyEvent.KEYCODE_I;
//    static final int REVIEW_KB_KEY = KeyEvent.KEYCODE_G;
//    static final int OK_KB_KEY = KeyEvent.KEYCODE_G;
//    static final int BACK_KB_KEY = KeyEvent.KEYCODE_J;
//    static final int SHARE_KB_KEY = KeyEvent.KEYCODE_S;

    // Key codes for ASCII Bluetooth Keyboard controller
    static final int FOCUS_DISTANCE_KB_KEY = KeyEvent.KEYCODE_Q;
    static final int FN_KB_KEY = KeyEvent.KEYCODE_U;
    static final int CONTINUOUS_KB_KEY = KeyEvent.KEYCODE_C;
    static final int MIRROR_KB_KEY = KeyEvent.KEYCODE_M;

    public static final int HIDDEN_SHUTTER_BUTTON_X = 2040;
    public static final int HIDDEN_SHUTTER_BUTTON_Y = 140;
    public static final int HIDDEN_MODE_BUTTON_X = 360;
    public static final int HIDDEN_MODE_BUTTON_Y = 140;

    private TextView countdownTextView;
    private CommandLine commandLine;
    private String splashMessage = "Welcome to A3DCamera - Prototype 3D Camera - Andy Modla";

    public DisplayMode getDisplayMode() {
        return displayMode;
    }

    /**
     * Preload libyuv to initialize native library
     */
    private void initLibYuv() {
        try {
            // Trigger class loading which will load the native library
            // This is a lightweight operation that forces the static initializer to run
            Class.forName("io.github.crow_misia.libyuv.Yuv");
        } catch (ClassNotFoundException e) {
            android.util.Log.e(TAG, "Failed to preload libyuv", e);
        }
    }

    private final ActivityResultLauncher<Intent> storageActivityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                // Check if the user granted the permission after returning to the app
                if (Environment.isExternalStorageManager()) {
                    Toast.makeText(this, "All Files Access Granted!", Toast.LENGTH_SHORT).show();
                    // Proceed with your file operations
                } else {
                    Toast.makeText(this, "Permission Denied. Cannot access files.", Toast.LENGTH_SHORT).show();
                }
            });

    /*==================================================================
     * Activity Lifecycle methods
     ===================================================================*/

    /**
     * Create MainActivity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "A3DCamera onCreate()");
        initLibYuv(); // Initialize native yuvlib library

        // initialize Parameters from storage
        // shared preferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        parameters = new Parameters(prefs, this);
        parameters.init();
        isSimpleCamera = parameters.isSimpleCameraMode();
        isAiEdit = parameters.getIsAiEdit();
        //aiVisionEnabled = parameters.getAiVisionEnabled();

        checkPermissions();
//        private static String title1 = "3D/AI Photo Booth by Andy Modla";
//        private static String title2 = "Philadelphia Maker Faire - April 19, 2026";
//        private static String instruction1 = "Look at Camera";
//        private static String instruction2 = "";

        // set parameters for my XReal Beam Pro stereo window adjustment
        // Stereo Image Alignment parameters (same values as StereoPhotoMaker for alignment)
        // 100  left/right parallax horizontal offset for stereo window placement
        // -1  left/right camera alignment vertical offset for camera correction
        // testing only:
        //parameters.writeParallaxOffset(100); // photo booth parallax offset
        //parameters.writeVerticalOffset(-1);

        // Establish media storage folders for saving photos
        media = new Media(this, parameters, aiVision);
        media.createMediaFolder();

        // set up UDP remote control for WIFI local network broadcast message transmit or receive
        udpRemoteControl = new UdpRemoteControl(this);
        hostIpAddr = udpRemoteControl.getHostnameAddress();
        Log.d(TAG, "Host IP Address = " + hostIpAddr);

        // setup AI vision connection to local network vision small multimodal LLM
        if (aiVisionEnabled) {
            aiVision = new AIvision(this);
        }

        if (!isSimpleCamera) {
            // all camera modes use photo booth sketch except simple camera
            FrameLayout frame = new FrameLayout(this);
            frame.setId(CompatUtils.getUniqueViewId());
            setContentView(frame, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            photoBooth = new PhotoBooth();
            photoBooth.setMedia(media);
            photoBooth.setParameters(parameters);
            photoBoothFragment = new PFragment(photoBooth);
            photoBoothFragment.setView(frame, this);
            media.setupApplet(photoBooth);

            photoBooth.setMainActivity(this);
            photoBooth.setMirror(parameters.getIsMirror());
            photoBooth.setParallax(parameters.getParallaxOffset());
            photoBooth.setVerticalAlignment(parameters.getVerticalOffset());
        } else {
            setContentView(R.layout.layout);
        }

        camera = new Camera3D(this, media, parameters, photoBooth);
        media.setCamera(camera);
        if (parameters.getUdpControlEnabled()) {
            if (parameters.getUdpTransmit()) { // transmitter and receiver mutually exclusive
                udpRemoteControl.setUdpTransmitter(camera, hostIpAddr);
            } else {
                udpRemoteControl.setUdpReceiver(camera, hostIpAddr);
            }
        }
        imageSender = new ImageSender(this, parameters, udpRemoteControl);
        if (photoBooth != null) { // we are using processing in stereoscope or photo booth mode
            // set photo booth countdown
            countdownDigit = -1;
            if (parameters.getCountDownEnabled()) {
                countdownStart = parameters.getCountdownTimer();
            } else {
                countdownStart = 0;
            }
        }

        // countdownTextView will be null for photo booth
        // because photo booth uses sketch graphics
        countdownTextView = findViewById(R.id.overlay_text);

        // This is a crucial step: we need to wait for the view to be laid out
        // before we can get its dimensions.
        if (countdownTextView != null) {
            countdownTextView.post(new Runnable() {
                @Override
                public void run() {
                    // Get the total height of the parent FrameLayout
                    int parentHeight = ((RelativeLayout) countdownTextView.getParent()).getHeight();

                    // Calculate one-third of the parent height
                    int countdownHeight = parentHeight / 3;

                    // Set the TextView's height to one-third of the parent height
                    countdownTextView.getLayoutParams().height = countdownHeight;

                    // Adjust the font size to fit within this new height
                    // You can use a library or a helper method to do this dynamically
                    // For a simpler approach, you can set a large fixed value
                    // and let the TextView handle scaling, but dynamic sizing is better
                    float newTextSize = (float) (countdownHeight * 0.75); // Use 75% of the height as a good starting point for the font size
                    countdownTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);
                    Log.d(TAG, "countdownTextView height=" + countdownTextView.getHeight());
                    Log.d(TAG, "parent height=" + parentHeight);
                    countdownTextView.setY(parentHeight / 3.0f);
                    countdownTextView.setVisibility(View.GONE);
                    countdownTextView.requestLayout();

                }
            });
        }

        decorView = getWindow().getDecorView();
        // Set the pointer icon to null (invisible)
        decorView.setPointerIcon(PointerIcon.getSystemIcon(this, PointerIcon.TYPE_NULL));
        decorView.setOnGenericMotionListener((view, motionEvent) -> {
            // Handle the event here
            return handleMouseEvent(motionEvent);
        });

        // Start the camera after the first layout pass (as before), but only
        // if permission was ALREADY granted. On first launch (dialog pending)
        // the open is deferred to onPermissionsResult()/onResume().
        startCamera();

        decorView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // 3. Get the coordinates
                float x = event.getX();
                float y = event.getY();
                //Log.d(TAG, "onTouch -> X: " + x + " | Y: " + y);
                // 4. Determine the action (Down, Move, Up)
                int action = event.getAction();

                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        // Log when the finger/mouse first touches the screen
                        //Log.d(TAG, "ACTION_DOWN detected at -> X: " + x + " | Y: " + y);
                        break;

                    case MotionEvent.ACTION_MOVE:
                        // Log when the finger/mouse is sliding/moving
                        // Note: This will spam your Logcat very fast!
                        //Log.d(TAG, "ACTION_MOVE detected at -> X: " + x + " | Y: " + y);
                        break;

                    case MotionEvent.ACTION_UP:
                        // upper right corner is hidden shutter release button
                        // ignore for photo booth based camera modes
                        if (isSimpleCamera) {
                            if (x > HIDDEN_SHUTTER_BUTTON_X && y < HIDDEN_SHUTTER_BUTTON_Y) {
                                capturePhoto();
                                // upper left corner is hidden launch settings button
                            } else if (x < HIDDEN_MODE_BUTTON_X && y < HIDDEN_MODE_BUTTON_Y) {
                                processModeChange(); // liveview/review
                            }
                        }
                        // Log when the finger/mouse is lifted
                        Log.d(TAG, "ACTION_UP detected at -> X: " + x + " | Y: " + y);
                        break;
                }

                // Return true to indicate we have handled the event.
                // Return false if you want the event to pass through to other views.
                return true;
            }
        });
        if (MyDebug.DEBUG) CameraInfoUtil.displayCameraInfo(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");
        setVisibility();

        if (commandLine == null) {
            commandLine = new CommandLine(this, parameters, splashMessage + " Version: " + BuildConfig.VERSION_NAME + " Alpha");
        }

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
        if (isFinishing()) {
            Log.d(TAG, "isFinishing");
        }
        camera.closeCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");

        // Re-evaluate the ACTUAL runtime permission state on every resume.
        // Covers returning from the permission dialog AND returning from
        // system Settings (no result callback fires in that case).
        allPermissionsGranted = hasRuntimePermissions();

        if (allPermissionsGranted) {
            if (camera == null) {
                Log.e(TAG, "Internal error - camera is null");
                return;
            }

            camera.shutterSound();
            startCamera();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
        camera.destroy();
        if (photoBoothFragment != null) {
            photoBoothFragment.onDestroy();
        }
        if (udpRemoteControl != null) {
            udpRemoteControl.destroy();
        }
    }


     /*==================================================================
     * OnGenericMotionListener implementation methods
     * Mouse Events
     ===================================================================*/

    private boolean handleMouseEvent(MotionEvent motionEvent) {
        // Check if the event is from a mouse
        if (motionEvent.isFromSource(InputDevice.SOURCE_MOUSE)) {
            switch (motionEvent.getActionMasked()) {
                case MotionEvent.ACTION_BUTTON_PRESS:
                    // Button pressed
                    handleButtonPress(motionEvent.getButtonState());
                    return true;
                case MotionEvent.ACTION_BUTTON_RELEASE:
                    // Button released
                    handleButtonRelease(motionEvent.getButtonState());
                    return true; // Event handled
                case MotionEvent.ACTION_MOVE:
                    // Mouse movement (use getX(), getY()) not used and consumed
                    return true; // Event handled
                // You can also handle ACTION_HOVER_MOVE for hover events
            }

            if (motionEvent.getAction() == MotionEvent.ACTION_SCROLL) {
                // AXIS_VSCROLL provides the vertical scroll delta
                // Negative values mean scrolling down, positive mean up
                float delta = motionEvent.getAxisValue(MotionEvent.AXIS_VSCROLL);
                handleMouseWheel(delta);
                return true; // Event handled
            }
        }

        return super.onGenericMotionEvent(motionEvent); //return false;
    }

    private void handleButtonPress(int buttonState) {
        if ((buttonState & MotionEvent.BUTTON_PRIMARY) != 0) { // left mouse button
            // Left mouse button pressed (Large Shutter button on Buzzer Box)
            Log.d(TAG, "Left button pressed");
            if (!isSimpleCamera) {
                if (!camera.captureInProgress.get()) {
                    processShutterKey();
                }
            } else {
                if (!camera.captureInProgress.get()) {
                    capturePhoto();
                }
            }
        } else if ((buttonState & MotionEvent.BUTTON_TERTIARY) != 0) { // middle mouse button
            // mouse button pressed (SBS/Anaglyph/L/R button on Buzzer Box)
            Log.d(TAG, "Middle button pressed");
            if (!isSimpleCamera) {
                processDisplayToggle();
            } else { // Not photo booth - send to printer immediately
                media.printImageType();
            }
        } else if ((buttonState & MotionEvent.BUTTON_SECONDARY) != 0) { // right mouse button
            // mouse button pressed (Review button on Buzzer Box)
            // handles toggle state changes in photo booth sketch
            Log.d(TAG, "Right button pressed");

            if (!isSimpleCamera) {
                if (parameters.getIsAiEdit()) {
                    processPrintStateToggle();
                } else {
                    processStateToggle();
                }
            } else {
                media.reviewPhotos(displayMode);
            }
        }
        // Other mouse buttons like BUTTON_BACK, BUTTON_FORWARD can also be checked here
    }

    private void handleButtonRelease(int buttonState) {
        // Handle button release events similarly
    }

    private void handleMouseWheel(float delta) {
        // Handle mouse wheel events
        if (delta > 0) {
            // Scrolled Up (away from user)
            if (!isSimpleCamera) {
                photoBooth.setKeyCode(KeyEvent.KEYCODE_RIGHT_BRACKET, 0, true);
            }
        } else if (delta < 0) {
            // Scrolled Down (toward user)
            if (!isSimpleCamera) {
                photoBooth.setKeyCode(KeyEvent.KEYCODE_LEFT_BRACKET, 0, true);
            }
        }
    }

    public void simulateClick(View targetView, float x, float y) {
        Log.d(TAG, "simulateClick x=" + x + " y=" + y);
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();

        // 1. Create the DOWN event
        MotionEvent downEvent = MotionEvent.obtain(
                downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0);

        // 2. Create the UP event
        MotionEvent upEvent = MotionEvent.obtain(
                downTime, eventTime + 100, MotionEvent.ACTION_UP, x, y, 0);

        // 3. Send them to the view
        targetView.dispatchTouchEvent(downEvent);
        targetView.dispatchTouchEvent(upEvent);

        // Clean up
        downEvent.recycle();
        upEvent.recycle();
    }


    private void processShutterKey() {
        Log.d(TAG, "processShutterKey");
        //if (!(functionMode == FUNCTION_MODE_PARALLAX || functionMode == FUNCTION_MODE_ZOOM)) {
        if (isLiveView()) {
            capturePhoto();
        } else if (isReview()) {
            media.printImageType();
        } else if (isReviewEdit()) {
            File mediaFile = media.getMediaFile();
            if (mediaFile == null) {
                Toast.makeText(this, "Nothing To Edit or Share", Toast.LENGTH_SHORT).show();
            } else if (parameters.getIsAiEdit()) {
                Toast.makeText(this, "Entering AI Edit", Toast.LENGTH_LONG).show();
                media.shareImage2(mediaFile, Media.APP_AIEDIT_PACKAGE);
            } else {
                Toast.makeText(this, "Entering Stereo Edit", Toast.LENGTH_LONG).show();
                media.shareImage2(mediaFile, Media.APP_REVIEW_PACKAGE);
            }
            // TODO add print only option and label right side edit or print type

        }

    }

    private void processDisplayToggle() {
        // toggle through photo types for display
        displayMode = displayMode.next();
        photoBooth.setDisplayMode(displayMode);

    }

    // save this unused code
    private void processPrintStateToggle() {
        // toggle through photo types for display
        if (state == LIVE_VIEW_STATE) {
            setReview();
        } else if (state == REVIEW_PHOTO_STATE) {
            setAiEditReview();
        } else if (state == REVIEW_AI_EDIT_STATE) {
            setLiveView();
        }
    }

    private void processStateToggle() {
        // toggle through photo types for display
        Log.d(TAG, "processStateToggle state=" + stateName[state]);
        if (state == LIVE_VIEW_STATE) {
            setReview();
        } else if (state == REVIEW_PHOTO_STATE && !parameters.getIsAiEdit()) {
            setLiveView();
        } else if (state == REVIEW_AI_EDIT_STATE) {
            setLiveView();
        }
    }

    public boolean isLiveView() {
        if (state == MainActivity.LIVE_VIEW_STATE) return true;
        return false;
    }

    public boolean isReview() {
        if (state == MainActivity.REVIEW_PHOTO_STATE) return true;
        return false;
    }

    public boolean isReviewEdit() {
        if (state == MainActivity.REVIEW_AI_EDIT_STATE) return true;
        return false;
    }

    public void setLiveView() {
        state = LIVE_VIEW_STATE;
        setFunctionMode(FUNCTION_MODE_LIVEVIEW);
        photoBooth.clearImageLabelTimeout();
        wakeUpSketch(state);
    }

    public void setReview() {
        state = REVIEW_PHOTO_STATE;
        setFunctionMode(FUNCTION_MODE_REVIEW);
        wakeUpSketch(state);
    }

    public void setAiEditReview() {
        state = REVIEW_AI_EDIT_STATE;
        wakeUpSketch(state);
    }

    public boolean isLiveviewFunction() {
        return (functionMode == FUNCTION_MODE_LIVEVIEW);
    }

    public boolean isReviewFunction() {
        return (functionMode == FUNCTION_MODE_REVIEW);
    }

    public boolean isParallaxFunction() {
        return (functionMode == FUNCTION_MODE_PARALLAX);
    }

    public boolean isZoomFunction() {
        return (functionMode == FUNCTION_MODE_ZOOM);
    }

    public void setFunctionMode(int func) {
        functionMode = func;
        photoBooth.setMenuKeyLabels(func);
    }

    public int getFunctionMode() {
        return functionMode;
    }

    public void wakeUpSketch(int theState) {
        Log.d(TAG, "wakeUpSketch state=" + stateName[theState]);
        if (!isSimpleCamera && isReview()) {
            camera.pauseCameraPreviewSession();
            photoBooth.setImageLabelTimeout();
        } else if (!isSimpleCamera && isLiveView()) {
            camera.resumeCameraPreviewSession();
        }

        photoBooth.loop();
        Log.d(TAG, "wakeUpSketch isLooping=" + photoBooth.isLooping());

    }

    /*==================================================================
     * Key Events
     ===================================================================*/

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown " + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_ENTER:
            case SHUTTER_KEY:
                exitApp = false;
                return true;
            case KeyEvent.KEYCODE_3D_MODE: // ignore so that this key does not launch XReal camera app
                exitApp = false;
                return true;
            //case BACK_KB_KEY:
            case BACK_KEY:
            case KeyEvent.KEYCODE_ESCAPE:
            case KeyEvent.KEYCODE_SLASH:
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (!isSimpleCamera) {
                    if (!photoBooth.isReady())
                        return true;  // ignore keystrokes until sketch is ready
                    photoBooth.setKeyCode(keyCode, 0, false);
                }
                return true;
            default:
                exitApp = false;
                return super.onKeyDown(keyCode, event);
        }

    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        char ch = 0;
        if (event != null) {
            ch = (char) event.getUnicodeChar();
            if (ch == 65535 && keyCode == 0) { // special case all other keys
                // ignore key
                return true;
            }
        }
        Log.d(TAG, "onKeyUp " + keyCode + " " + ch);
        if (commandLine != null && ch != 0 && commandLine.processCommandLineKey(keyCode, ch)) {
            return true;
        }
        if (!isSimpleCamera) {
            if (!photoBooth.isReady()) return true;  // ignore keystrokes until sketch is ready

            // up keys to ignore due to multiple codes output from game controller key press
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_MENU:
                case KeyEvent.KEYCODE_SPACE:
                case KeyEvent.KEYCODE_BACK:
                    return true;
            }
            // send to photo booth sketch to process on its next draw() frame
            photoBooth.setKeyCode(keyCode, ch, true);

            // select keys not processed by sketch are handled here
            // these keys must be mutually exclusive between here and the sketch
            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                case MODE_KEY:
                    processModeChange();
//                    if (isAiEdit) {
//                        processPrintStateToggle();
//                    } else {
//                        processStateToggle();
//                    }
                    return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                case ANAGLYPH_KEY:
                    processDisplayToggle();
                    return true;
                case KeyEvent.KEYCODE_3D_MODE:
                case KeyEvent.KEYCODE_ENTER:
                case SHUTTER_KEY:
                    if (!camera.captureInProgress.get()) {
                        processShutterKey();
                    }
                    return true;
            }
        }

        // Only for Basic Camera Configuration Setting
        // Basic camera does not use Processing sketch PhotoBooth
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_3D_MODE: // camera key - first turn off auto launch of native camera app
            case SHUTTER_KEY:
                if (continuousMode) {
                    //Log.d(TAG, "onKeyUp - ignore shutter key in continuous mode");
                    return true; // ignore shutter key in continuous shutter
                } else {
                    if (state == LIVE_VIEW_STATE) {
                        capturePhoto();
                    } else if (state == REVIEW_PHOTO_STATE) {
                        media.printImageType();
                    }
                    return true;
                }

                //case CONTINUOUS_KEY:
            case CONTINUOUS_KB_KEY: // start continuous capture mode
                if (continuousModeFeature) {
                    if (continuousMode) {
                        continuousMode = false;
                        countdownDigit = -1;
                        Toast.makeText(this, "Continuous Mode Canceled ", Toast.LENGTH_SHORT).show();
                        camera.closeCamera();
                        camera.captureInProgress.set(false);
                        camera.openCamera();
                        return true;

                        //return true;  // ignore key when already in continuous mode
                    } else {
                        continuousMode = true;
                        startContinuousCapturePhoto();
                    }
                }
                return true;

            //case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
                //case BUTTON_A_KEY:
                if (continuousMode) {
                    continuousMode = false;
                    countdownDigit = -1;
                    Toast.makeText(this, "Continuous Mode Canceled ", Toast.LENGTH_SHORT).show();
                    camera.closeCamera();
                    camera.captureInProgress.set(false);
                    camera.openCamera();
                    return true;
                }
                if (state == REVIEW_PHOTO_STATE) {
                    // turn on camera for entering live view state
                    //state = LIVE_VIEW_STATE;
                    camera.openCamera();
                    setLiveView();
                    return true;
                }
                if (exitApp) {
                    //finish();
                    //System.exit(0);
                    exitApp = false;
                    return true;
                } else {
                    //Toast.makeText(this, "Exit?", Toast.LENGTH_SHORT).show();
                    exitApp = true;
                }
                return true;

            case BUTTON_Y_KEY:
                //case SHARE_KB_KEY:
                if (!isSimpleCamera) {
                    // ignore share key in photo booth
                    return true;
                } else {
                    boolean ok = media.shareReviewImage();
                    if (!ok) {
                        Toast.makeText(this, "Nothing to Share", Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
//            case EV_KEY:
//            //case FN_KB_KEY:
//            case KeyEvent.KEYCODE_F:
//                if (state != LIVE_VIEW_STATE) {
//                    return true;
//                }
//                camera.changeFunction();
//                //camera.closeCamera();
//                //camera.openCamera();
//                return true;
            //case FOCUS_DISTANCE_KEY: // change focus distance, should be sub menu
            case FOCUS_DISTANCE_KB_KEY: // change focus distance, should be sub menu
                if (state != LIVE_VIEW_STATE) {
                    return true;
                }
                camera.closeCamera();
                camera.setFocusDistance();
                camera.openCamera();
                return true;
//            case KeyEvent.KEYCODE_ENTER:
//            case OK_KEY:
            //           case OK_KB_KEY:
//                Toast.makeText(this, " OK/Review - not implemented", Toast.LENGTH_SHORT).show();
//                closeCamera();
//                openCamera();
//                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE: // 85 not used with 8BitDo
                Toast.makeText(this, "Not implemented", Toast.LENGTH_SHORT).show();
//                closeCamera();
//                openCamera();
                return true;
//            case MODE_KEY:
//                //case MODE_KB_KEY:
//                Toast.makeText(this, "Mode - not implemented", Toast.LENGTH_SHORT).show();
//                closeCamera();
//                openCamera();
//                return true;
//            case SHUTTER_SPEED_KEY:
//                //case SHUTTER_SPEED_KB_KEY:
//                if (!isSimpleCamera) {
//                    //photoBooth.keyPressedReview(keyCode, ch);
//                    return true;
//                }
//                Toast.makeText(this, "Shutter Speed - not implemented", Toast.LENGTH_SHORT).show();
//                closeCamera();
//                openCamera();
//                return true;

//            case KeyEvent.KEYCODE_N:
//                //case TIMER_KB_KEY:
//                if (state != LIVE_VIEW_STATE) {
//                    if (!isSimpleCamera) {
//                        //photoBooth.keyPressedReview(keyCode, ch);
//                    }
//                    return true;
//                } else {
//                    if (CONTINUOUS_COUNT > 0) {
//                        CONTINUOUS_COUNT = 0;
//                    } else {
//                        if (parameters.isPhotoBoothCameraMode()) {
//                            if (parameters.getCountDownEnabled())
//                                CONTINUOUS_COUNT = parameters.getCountdownTimer(); //CONTINUOUS_COUNT_PHOTO_BOOTH;
//                            else
//                                CONTINUOUS_COUNT = 0;
//                        } else {
//                            CONTINUOUS_COUNT = CONTINUOUS_COUNT_DEFAULT;
//                        }
//                    }
//                    Toast.makeText(this, "Set Timer Countdown=" + Integer.toString(CONTINUOUS_COUNT), Toast.LENGTH_SHORT).show();
//                    countdownDigit = -1;
//                    countdownStart = CONTINUOUS_COUNT;
//                }
//                return true;
//
////            case ISO_KEY:
            //case ISO_KB_KEY:
//                Toast.makeText(this, "ISO - not implemented", Toast.LENGTH_SHORT).show();
//                closeCamera();
//                openCamera();
//                return true;
            case ANAGLYPH_KEY:
            case KeyEvent.KEYCODE_A:
                displayMode = displayMode.next();
                if (!isSimpleCamera) {
                    photoBooth.setDisplayMode(displayMode);
                }
//                if (displayMode == DisplayMode.SBS) {
//                    Toast.makeText(this, "Display SBS", Toast.LENGTH_SHORT).show();
//                } else if (displayMode == DisplayMode.ANAGLYPH) {
//                    Toast.makeText(this, "Display ANAGLYPH", Toast.LENGTH_SHORT).show();
//                } else if (displayMode == DisplayMode.LEFT) {
//                    Toast.makeText(this, "Display LEFT", Toast.LENGTH_SHORT).show();
//                } else if (displayMode == DisplayMode.RIGHT) {
//                    Toast.makeText(this, "Display RIGHT", Toast.LENGTH_SHORT).show();
//                }
//                closeCamera();
//                openCamera();
                return true;
            case SETTINGS_KEY:
            case KeyEvent.KEYCODE_J:
                // Launch Settings Activity
                launchSettings();
                return true;
            case MIRROR_KB_KEY:
                boolean amirror = parameters.getIsMirror();
                amirror = !amirror;
                parameters.setIsMirror(amirror);
                Toast.makeText(this, "Mirror=" + Boolean.toString(amirror), Toast.LENGTH_SHORT).show();
                return true;
//            case KeyEvent.KEYCODE_D:
//                if (!isSimpleCamera) {
//                    simulateClick(decorView, 1500, 540);
//                    return true;
//                }
//                return true;

//            case VIDEO_RECORD_KEY:
//            case VIDEO_RECORD_KB_KEY:
//                Toast.makeText(this, "Video Record - not implemented", Toast.LENGTH_SHORT).show();
//                closeCamera();
//                openCamera();
//                return true;
            //case BLANK_SCREEN_KEY:
            //case BLANK_SCREEN_KB_KEY:
            //Toast.makeText(this, "Blank Screen - not implemented", Toast.LENGTH_SHORT).show();
            //blankScreen = !blankScreen;

            //String id = String.valueOf(mCameraCaptureSession.getDevice().getId());
            //Toast.makeText(this, (blankScreen ? "Id: " + id + " Blank Screen" : "UnBlank Screen"), Toast.LENGTH_SHORT).show();
//                if (blankScreen) {
//                    //mSurfaceView0.setVisibility(View.GONE);
//                    //mSurfaceView2.setVisibility(View.GONE);
//                    countdownTextView.setVisibility(View.GONE);
//                } else {
//                    //mSurfaceView0.setVisibility(View.VISIBLE);
//                    //mSurfaceView2.setVisibility(View.VISIBLE);
//                    //closeCamera();
//                    //openCamera();
//                    countdownTextView.setVisibility(View.VISIBLE);
//                }

            //    return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    public void processModeChange() {
        if (isAiEdit) {
            processPrintStateToggle();
        } else {
            processStateToggle();
        }

    }

    public CommandLine getCommandLine() {
        return commandLine;
    }

    public void setContinuousModeFeature(boolean continuousModeFeature) {
        this.continuousModeFeature = continuousModeFeature;
    }

    public void setContinuousMode(boolean continuousMode) {
        this.continuousMode = continuousMode;
    }

    public boolean getContinuousMode() {
        return continuousMode;
    }

    public int getContinuousCounter() {
        return continuousCounter;
    }

    public int nextLabelContinuousCounter() {
        return ++labelContinuousCounter;
    }

    public void setContinuousCounter(int continuousCounter) {
        this.continuousCounter = continuousCounter;
    }

    public void launchSettings() {
        // Launch Settings Activity
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        startActivity(settingsIntent);
    }

    public void capturePhoto() {
        Log.d(TAG, "capturePhoto() captureInProgress=" + camera.captureInProgress.get());
        if (camera.captureInProgress.get()) return;
        if (!isSimpleCamera && !isLiveView()) {
            setLiveView();
            return;
        }
        if (countdownTimer != null) return;
        //Log.d(TAG, "countdownDigit=" + countdownDigit);
        if (parameters.getCountDownEnabled()) {
            countdownStart = parameters.getCountdownTimer();
            startCountdownSequence(countdownStart);
        } else {
            if (countdownTextView != null) countdownTextView.setVisibility(View.GONE);
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    camera.createCameraCaptureSession();
                }
            });
        }
    }

    public void startContinuousCapturePhoto() {
        if (continuousModeFeature) {
            if (continuousMode) {
                Toast.makeText(this, "Start Continuous Mode ", Toast.LENGTH_SHORT).show();
                if ((countdownDigit < 0) && parameters.getCountDownEnabled()) {
                    Log.d(TAG, "startContinuousCapturePhoto countdownDigit=" + countdownDigit);
                    startCountdownSequence(countdownStart);  // calls createCameraCaptureSession() after count down finished
                    continuousCounter = parameters.getCountdownTimer();
                } else {
                    if (parameters.isPhotoBoothCameraMode()) {
                        continuousCounter = CONTINUOUS_COUNT_PHOTO_BOOTH;
                    } else {
                        continuousCounter = CONTINUOUS_COUNT_DEFAULT;
                    }
                    labelContinuousCounter = 0;
                    CONTINUOUS_COUNT = 0;
                    camera.createCameraCaptureSession();
                }
            }
        } else {
            Toast.makeText(this, "Continuous Mode Not Enabled", Toast.LENGTH_SHORT).show();
        }
    }

    public void nextContinuousCapturePhoto() {
        Log.d(TAG, "nextContinuousCapturePhoto()");
        if (continuousModeFeature && continuousMode) {
            if (continuousCounter > 0) {
                continuousCounter--;
                Log.d(TAG, "nextContinuousCapturePhoto continuousCounter=" + continuousCounter);
                camera.createCameraCaptureSession();
                if (continuousCounter <= 0) {
                    continuousMode = false;
                    //showToast("Continuous Mode Completed "); java.lang.NullPointerException: Can't toast on a thread that has not called Looper.prepare()
                }
            }
        }
    }

    /*
     * Start countdown sequence logic for camera app (not photo booth)
     */
    void startCountdownSequence(int startCount) {
        Log.d(TAG, "startCountdownSequence startCount=" + startCount);

        if (startCount == 0) {
            camera.createCameraCaptureSession(); // take a picture
            return;
        }

        if (countdownTimer == null) {
            countdownTimer = new Timer();
            countdownDigit = startCount + 1;  // for display correct
            if (!isSimpleCamera) {
                photoBooth.setCountdown(Integer.toString(countdownDigit));
            } else {
                countdownTextView.setText(Integer.toString(countdownDigit));
                countdownTextView.setVisibility(View.VISIBLE);
            }
            // define a task to decrement the countdown digit every second
            TimerTask task = new TimerTask() {
                public void run() {
                    countdownDigit--;
                    if (countdownDigit < 0) {
                        // stop the timer when the countdown reaches 0
                        countdownTimer.cancel();
                        countdownTimer = null;
                        MainActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                // hide digit display
                                if (!isSimpleCamera) {
                                    photoBooth.setCountdown("");
                                } else {
                                    countdownTextView.setText("");
                                    countdownTextView.setVisibility(View.GONE);
                                }
                                camera.createCameraCaptureSession(); // take a picture
                            }
                        });
                    } else {
                        MainActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                if (countdownDigit == 0) {
                                    if (!isSimpleCamera) {
                                        photoBooth.setCountdown("");
                                    } else {
                                        countdownTextView.setText("");
                                        countdownTextView.setVisibility(View.GONE);
                                    }
                                    remoteFocus(); // send broadcast focus command over the network
                                } else {
                                    if (!isSimpleCamera) {
                                        photoBooth.setCountdown(Integer.toString(countdownDigit));
                                    } else {
                                        countdownTextView.setText(Integer.toString(countdownDigit));
                                        countdownTextView.setVisibility(View.VISIBLE);
                                    }
                                }
                            }
                        });

                        Log.d(TAG, "countdown=" + countdownDigit); // show digit display
                    }
                }
            };

            countdownTimer.schedule(task, 0, 1000);
        }
    }

    // send remote control shutter command on local network
    public void remoteShutter() {
        if (parameters.getUdpControlEnabled()) {
            if (parameters.getUdpTransmit()) {
                udpRemoteControl.sendShutterPushRelease();
            }
        }
    }

    // send remote control focus command on local network
    public void remoteFocus() {
        if (parameters.getUdpControlEnabled()) {
            if (parameters.getUdpTransmit()) {
                udpRemoteControl.sendFocusReleasePush();
            }
        }
    }

    /**
     * Show half second Toast message
     *
     * @param message Text message to display
     */
    public void showToast(String message) {
        //    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        ToastHelper.showToast(this, message);
    }

    public final ActivityResultLauncher<Intent> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Uri contentUri = result.getData().getData();
                            if (contentUri != null) {
                                // Start the share intent with the URI from the Photo Picker
                                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                                shareIntent.setType("image/*");
                                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                shareIntent.setPackage(Media.APP_PHOTO_REVIEW_PACKAGE); //  actual package name
                                this.startActivity(shareIntent);
                                //startActivity(Intent.createChooser(shareIntent, "Share image..."));
                            }
                        }
                    });


    private void setVisibility() {
        runOnUiThread(new Runnable() {
                          @Override
                          public void run() {
                              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                  // Android 11 (API 30) and above - use WindowInsetsController
                                  WindowInsetsController controller = getWindow().getInsetsController();
                                  if (controller != null) {
                                      // Hide status bar and navigation bar
                                      controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());

                                      // Set behavior for when user swipes to show system bars
                                      controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

                                      // Optional: Set light status bar (uncomment if needed)
                                      // controller.setSystemBarsAppearance(
                                      //     WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                                      //     WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                      // );
                                  }

                                  // Enable edge-to-edge layout
                                  getWindow().setDecorFitsSystemWindows(false);
                              } else {
                                  // Fallback for older Android versions (API 29 and below)
                                  int newVis = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                          | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                          | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                          | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                          | View.SYSTEM_UI_FLAG_FULLSCREEN
                                          //  | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                                          | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

                                  final View decorView = getWindow().getDecorView();
                                  decorView.setSystemUiVisibility(newVis);
                              }
                          }
                      }
        );
    }

    public void restartApp() {
        showToast("Restarting A3DCamera");
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (intent != null) {
            // Clear the back stack and start as a new task
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            // Kill the current process to ensure a fresh start
            Runtime.getRuntime().exit(0);
        }
    }

    public void updateParameters() {
        isAiEdit = parameters.getIsAiEdit();
        isSimpleCamera = parameters.isSimpleCameraMode();
        //parameters.getCountDownEnabled();
        CONTINUOUS_COUNT = parameters.getCountdownTimer();
        camera.focusDistanceIndex = parameters.getFocusDistanceIndex();
        //parameters.getUdpControlEnabled();
        //parameters.getUdpTransmit();

    }
    /*==================================================================
     * Permissions
     ===================================================================*/

    /*==================================================================
     * Permissions — fixed, single launcher, camera + storage in one flow
     *
     * Requests:
     *   - CAMERA                        (all API levels)
     *   - READ_MEDIA_IMAGES / VIDEO     (API 33+)
     *   - READ_MEDIA_VISUAL_USER_SELECTED is NOT requested directly —
     *     it is granted automatically when the user picks
     *     "Select photos and videos" (partial access) on API 34+.
     *   - READ_EXTERNAL_STORAGE         (for API 31–32 only, since minSdk = 34)
     *==================================================================*/

// --- AndroidManifest.xml (debug AND main) ---
//
// <uses-feature android:name="android.hardware.camera" android:required="false" />
// <uses-permission android:name="android.permission.CAMERA" />
// <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
// <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
// <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
//                     android:maxSdkVersion="32" />
//
// NOTE: do NOT declare MANAGE_EXTERNAL_STORAGE for a camera app —
// use MediaStore to save pictures and READ_MEDIA_* to read the gallery.


        /*==================================================================
         * Single permission launcher for everything
         ===================================================================*/
        private final ActivityResultLauncher<String[]> permissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                        this::onPermissionsResult);

        /*==================================================================
         * Build the exact permission set for this device's API level
         ===================================================================*/
        private String[] requiredPermissions() {
            List<String> perms = new ArrayList<>();
            perms.add(Manifest.permission.CAMERA);            // camera

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {  // API 33+
                perms.add(Manifest.permission.READ_MEDIA_IMAGES);
                perms.add(Manifest.permission.READ_MEDIA_VIDEO);
            } else {                                          // API 31–32
                perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            return perms.toArray(new String[0]);
        }

        /*==================================================================
         * Entry point — call this from onCreate() AFTER super.onCreate()
         ===================================================================*/
        private void checkPermissions() {
            Log.d(TAG, "checkPermissions");

            List<String> missing = new ArrayList<>();
            for (String permission : requiredPermissions()) {
                if (ContextCompat.checkSelfPermission(this, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    missing.add(permission);
                }
            }

            if (missing.isEmpty()) {
                allPermissionsGranted = true;
                Log.d(TAG, "All permissions already granted");
                onAllPermissionsGranted();                    // start camera here
                return;
            }

            Log.d(TAG, "Requesting missing permissions: " + missing);
            // ONE request, ONE dialog flow. Never launch a second
            // permission request while one is pending — API 30+ drops it.
            permissionLauncher.launch(missing.toArray(new String[0]));
        }

        /*==================================================================
         * Result callback — replaces the old onRequestPermissionsResult
         ===================================================================*/
        private void onPermissionsResult(Map<String, Boolean> result) {
            allPermissionsGranted = true;
            List<String> denied = new ArrayList<>();

            for (Map.Entry<String, Boolean> entry : result.entrySet()) {
                Log.d(TAG, entry.getKey() + " = " + entry.getValue());
                if (!entry.getValue()) {
                    denied.add(entry.getKey());
                    allPermissionsGranted = false;
                }
            }

            // API 34+ partial media access: user chose "Select photos and videos"
            boolean partialMediaAccess =
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                            && ContextCompat.checkSelfPermission(this,
                            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                            == PackageManager.PERMISSION_GRANTED;

            if (allPermissionsGranted) {
                Log.d(TAG, "All permissions granted");
                onAllPermissionsGranted();
            } else if (partialMediaAccess) {
                Log.d(TAG, "Partial media access granted (user-selected photos)");
                // Acceptable for many apps; if you truly need full gallery access:
                Toast.makeText(this,
                        "Only selected photos are accessible. Enable full access in Settings.",
                        Toast.LENGTH_LONG).show();
                allPermissionsGranted = true;                 // partial is enough to run
                onAllPermissionsGranted();
            } else {
                handleDeniedPermissions(denied);
            }
        }

        /*==================================================================
         * Denial handling — including "Don't ask again" (dialog never
         * shows again on API 30+ after two denials)
         ===================================================================*/
        private void handleDeniedPermissions(List<String> denied) {
            Log.d(TAG, "Denied: " + denied);

            // Anything permanently denied can only be fixed in Settings.
            boolean permanentlyDenied = false;
            for (String permission : denied) {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    permanentlyDenied = true;                 // "Don't allow" chosen
                }
            }

            if (permanentlyDenied) {
                new AlertDialog.Builder(this)
                        .setTitle("Permissions required")
                        .setMessage("Camera and media access are required to use this app. "
                                + "Enable them in Settings.")
                        .setPositiveButton("Open Settings", (d, w) -> openAppSettings())
                        .setNegativeButton("Cancel", (d, w) ->
                                Toast.makeText(this, "App cannot work without permissions",
                                        Toast.LENGTH_LONG).show())
                        .show();
            } else {
                Toast.makeText(this,
                        "Camera and media permissions are required",
                        Toast.LENGTH_LONG).show();
                // Optionally retry: permissionLauncher.launch(requiredPermissions());
            }
        }

        /*==================================================================
         * Success hook — wire your camera start-up here
         ===================================================================*/
        private void onAllPermissionsGranted() {
            Log.d(TAG, "onAllPermissionsGranted — starting camera");
            startCamera();
        }

        /*==================================================================
         * Camera start helper — defers init/open to the first layout pass
         * (same as the old decorView.post), is idempotent (no double
         * init/open), and only opens when permission is actually granted.
         * Called from: onCreate, onPermissionsResult, and onResume.
         ===================================================================*/
        private void startCamera() {
            if (camera == null || decorView == null) {
                Log.e(TAG, "startCamera: camera or decorView not ready yet");
                return;
            }
            decorView.post(new Runnable() {
                @Override
                public void run() {
                    if (!cameraInitialized) {
                        camera.init(isSimpleCamera);
                        cameraInitialized = true;
                    }
                    if (photoBooth != null) {
                        photoBooth.setCamera(camera);
                    }
                    if (allPermissionsGranted && !camera.isCameraOpen()) {
                        Log.d(TAG, "startCamera: opening camera");
                        camera.openCamera();
                    }
                }
            });
        }

        /*==================================================================
         * Check the real runtime permission state (not the stale flag).
         * Full grant, or (API 34+) partial media access with camera granted.
         ===================================================================*/
        private boolean hasRuntimePermissions() {
            for (String permission : requiredPermissions()) {
                if (ContextCompat.checkSelfPermission(this, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    // API 34+ partial media access ("Select photos and videos")
                    // together with camera permission is enough to run.
                    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                            && ContextCompat.checkSelfPermission(this,
                            Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                            && ContextCompat.checkSelfPermission(this,
                            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                            == PackageManager.PERMISSION_GRANTED;
                }
            }
            return true;
        }

        private void openAppSettings() {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        }

        /*==================================================================
         * Heap check
         ===================================================================*/
        void checkHeap() {
            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            int standardHeapSize = am.getMemoryClass();
            int largeHeapSize = am.getLargeMemoryClass();
            System.out.println("Standard: " + standardHeapSize + "MB, Large: " + largeHeapSize + "MB");
        }

}

