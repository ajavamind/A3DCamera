package com.andymodla.android3dcamera;

import static android.Manifest.permission.CAMERA;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.hardware.camera2.params.TonemapCurve;
import android.media.AudioManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraInfo;
import androidx.core.app.ActivityCompat;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;


// README:   https://github.com/ajavamind/A3DCamera/blob/main/README.md

/**
 * A3DCamera app
 * Copyright 2025 Andy Modla All Rights Reserved
 */
public class MainActivity extends AppCompatActivity {

    public static final String TAG = "A3DCamera";
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    private static final String PREFS_NAME = "Parameters";
    private Parameters parameters;

    volatile boolean allPermissionsGranted = false;
    volatile boolean shutterSound = true;
    private static final int MAX_NUM_CAMERAS = 6;

    volatile int focusDistanceIndex = 0;  // default HYPERFOCAL
    //
    static final float MACRO_FOCUS_DISTANCE = 10.0f;  // 100mm
    static final float HYPERFOCAL_FOCUS_DISTANCE = 0.60356647f;  // 1.66 meters
    //static final float PHOTO_BOOTH_FOCUS_DISTANCE = 1.43f;  // 700mm  1 meter
    static final float PHOTO_BOOTH_FOCUS_DISTANCE = 2.0f;  // 500mm
    static final float AUTO_FOCUS_DISTANCE = 0.0f;
    static final float[] FOCUS_DISTANCE = {HYPERFOCAL_FOCUS_DISTANCE, PHOTO_BOOTH_FOCUS_DISTANCE, MACRO_FOCUS_DISTANCE, AUTO_FOCUS_DISTANCE};
    static final String[] FOCUS_DISTANCE_NAMES = {"HYPERFOCAL FOCUS DISTANCE", "PHOTO BOOTH FOCUS DISTANCE", "MACRO FOCUS DISTANCE", "AUTO FOCUS"};

    volatile boolean burstModeFeature = true;
    volatile boolean burstMode = false;  //
    public static final int BURST_COUNT_DEFAULT = 60;
    public static final int BURST_COUNT_PHOTO_BOOTH = 4;
    public int BURST_COUNT = BURST_COUNT_DEFAULT;  // approximately 1 capture per second
    public volatile int burstCounter = 0;

    // aspect ratio
    int aspectRatioIndex = 0;  // default
    final String[] ASPECT_RATIO_NAMES = {"DEFAULT", "4:3", "16:9", "1:1"};
    int CAMERA_WIDTH_AR_DEFAULT = 4080;
    int CAMERA_HEIGHT_AR_DEFAULT = 3072;
    int CAMERA_WIDTH_AR_4_3 = 4000;
    int CAMERA_HEIGHT_AR_4_3 = 3000;
    int CAMERA_WIDTH_AR_16_9 = 3840;
    int CAMERA_HEIGHT_AR_16_9 = 2160;
    int CAMERA_WIDTH_AR_1_1 = 3072;
    int CAMERA_HEIGHT_AR_1_1 = 3072;
    int CAMERA_WIDTH_AR_SMALL = 1080;
    int CAMERA_HEIGHT_AR_SMALL = 1080;

    // Maximum camera sensor image dimensions
    //private int cameraWidth = 1024;//1440;
    //private int cameraHeight = 768;//1080;
    //private int cameraWidth = 1920;
    //private int cameraHeight = 1080;
    //private int cameraWidth = 4080;  // results in 1920x1440 images
    //private int cameraHeight = 3060; // results in 1920x1440 images
    private int cameraWidth = CAMERA_WIDTH_AR_DEFAULT; // camera width lens pixels
    private int cameraHeight = CAMERA_HEIGHT_AR_DEFAULT;// camera height lens pixels

    private static final float[] curve_srgb = { // sRGB curve
            0.0000f, 0.0000f, 0.0667f, 0.2864f, 0.1333f, 0.4007f, 0.2000f, 0.4845f,
            0.2667f, 0.5532f, 0.3333f, 0.6125f, 0.4000f, 0.6652f, 0.4667f, 0.7130f,
            0.5333f, 0.7569f, 0.6000f, 0.7977f, 0.6667f, 0.8360f, 0.7333f, 0.8721f,
            0.8000f, 0.9063f, 0.8667f, 0.9389f, 0.9333f, 0.9701f, 1.0000f, 1.0000f};

    private static final CaptureRequest.Key<Integer> EXPOSURE_METERING = new CaptureRequest.Key<>("org.codeaurora.qcamera3.exposure_metering.exposure_metering_mode", Integer.TYPE);
    private static final int FRAME_AVERAGE = 0; // normal behavior
    private static final int CENTER_WEIGHTED = 1;
    private static final int SPOT_METERING = 2;
    int meteringIndex = 0;  // default
    static final int[] METERING = {FRAME_AVERAGE, CENTER_WEIGHTED, SPOT_METERING};
    String[] METERING_NAMES = {"FRAME AVERAGE", "CENTER WEIGHTED", "SPOT METERING"};

    // Saturation 0 - 10, default 5
    private static final CaptureRequest.Key<Integer> SATURATION = new CaptureRequest.Key<>("org.codeaurora.qcamera3.saturation.use_saturation", Integer.class);
    //    captureRequestBuilder.set(SATURATION, 5);
    //    captureBuilder.set(SATURATION, 5);

    // Sharpness 0 - 6, default 2
    private static final CaptureRequest.Key<Integer> SHARPNESS = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sharpness.strength", Integer.class);

    // Camera Ids for Xreal Beam Pro
    private String leftCameraId = "0";
    private String frontCameraId = "1";
    private String rightCameraId = "2";
    private String stereoCameraId = "3";
    private CameraDevice mCameraDevice;
    private CameraManager mCameraManager;
    private CameraCaptureSession mCameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;

    private HandlerThread mCameraThread;
    private Handler mCameraHandler;
    private Executor cameraExecutor;

    // Display surfaces
    private volatile SurfaceView mSurfaceView0;
    private volatile SurfaceView mSurfaceView2;
    private volatile SurfaceHolder mSurfaceHolder0;
    private volatile SurfaceHolder mSurfaceHolder2;

    private AIvision aiVision;  // local network small multimodal vision AI model server (Google Gemma 3 8B 4_K_M GGUF)
    private Media media;
    private Camera cameraController;

    private boolean aiVisionEnabled = false;

    private boolean isWiFiRemoteEnabled = false; //true;
    private UdpRemoteControl udpRemoteControl;

    // Image capture
    private volatile ImageReader mImageReader0;
    private volatile ImageReader mImageReader2;  // for SBS display

    // states - work in progress
    private static final int STATE_LIVEVIEW = 0;
    private static final int STATE_REVIEW = 1;
    private int state = STATE_LIVEVIEW;

    volatile Image imageL;
    volatile Image imageR;
    volatile byte[] leftBytes;
    volatile byte[] rightBytes;

    // Stereo Image Alignment parameters (same values as StereoPhotoMaker)
    //public int parallaxOffset = 0; // 212; // left/right parallax horizontal offset for stereo window placement
    //public int verticalOffset = 0; // -12; // left/right camera alignment vertical offset for camera correction

    public int displayMode = DisplayMode.SBS.ordinal();

    private boolean exitApp = false;
    private boolean isPhotobooth = false;  // work in progress
    Timer countdownTimer;
    int countdownStart = 3;
    int countdownDigit = -1;

    // Key codes for 8BitDo Micro Bluetooth Keyboard controller (Android mode)
    static final int SHUTTER_KEY = KeyEvent.KEYCODE_BUTTON_R1;
    static final int FOCUS_KEY = KeyEvent.KEYCODE_BUTTON_R2;
    static final int MODE_KEY = KeyEvent.KEYCODE_BUTTON_L2;
    static final int BURST_KEY = KeyEvent.KEYCODE_BUTTON_L1;
    static final int DISP_KEY = KeyEvent.KEYCODE_DPAD_UP;
    static final int ISO_KEY = KeyEvent.KEYCODE_DPAD_DOWN;
    static final int TIMER_KEY = KeyEvent.KEYCODE_DPAD_LEFT;
    static final int SHUTTER_SPEED_KEY = KeyEvent.KEYCODE_DPAD_RIGHT;
    static final int PRINT_KEY = KeyEvent.KEYCODE_BUTTON_SELECT; // 109-82 KeyEvent.KEYCODE_MENU;
    static final int ANAGLYPH_KEY = KeyEvent.KEYCODE_BUTTON_START; // 108 "+" button
    static final int FN_KEY = KeyEvent.KEYCODE_BUTTON_X; //  99 KeyEvent.KEYCODE_DEL = 67
    static final int MENU_KEY = KeyEvent.KEYCODE_BUTTON_Y;  // 100  KeyEvent.KEYCODE_SPACE = 62
    static final int REVIEW_KEY = KeyEvent.KEYCODE_BUTTON_A;  // 96 KEYCODE_DPAD_CENTER = 23
    static final int OK_KEY = KeyEvent.KEYCODE_BUTTON_A;  // 96 KEYCODE_DPAD_CENTER = 23
    static final int BACK_KEY = KeyEvent.KEYCODE_BACK;  // KeyEvent.KEYCODE_BUTTON_B = 97 KEYCODE_BACK = 04
    static final int SHARE_KEY = KeyEvent.KEYCODE_BUTTON_MODE;  // 110

    // Key codes for 8BitDo Micro Bluetooth Keyboard controller (Keyboard mode)
    static final int SHUTTER_KB_KEY = KeyEvent.KEYCODE_M;
    static final int FOCUS_KB_KEY = KeyEvent.KEYCODE_R;
    static final int MODE_KB_KEY = KeyEvent.KEYCODE_L;
    static final int BURST_KB_KEY = KeyEvent.KEYCODE_K;
    static final int DISP_KB_KEY = KeyEvent.KEYCODE_C;
    static final int ISO_KB_KEY = KeyEvent.KEYCODE_D;
    static final int TIMER_KB_KEY = KeyEvent.KEYCODE_E;
    static final int SHUTTER_SPEED_KB_KEY = KeyEvent.KEYCODE_F;
    static final int PRINT_KB_KEY = KeyEvent.KEYCODE_N;
    static final int ANAGLYPH_KB_KEY = KeyEvent.KEYCODE_O; // "+" button
    static final int FN_KB_KEY = KeyEvent.KEYCODE_H;
    static final int MENU_KB_KEY = KeyEvent.KEYCODE_I;
    static final int REVIEW_KB_KEY = KeyEvent.KEYCODE_G;
    static final int OK_KB_KEY = KeyEvent.KEYCODE_G;
    static final int BACK_KB_KEY = KeyEvent.KEYCODE_J;
    static final int SHARE_KB_KEY = KeyEvent.KEYCODE_S;

    private TextView countdownTextView;
    private CommandLine commandLine;
    private String splashMessage = "Welcome to A3DCamera by Andy Modla";

    public int getDisplayMode() {
        return displayMode;
    }

    public boolean getBurstMode() {
        return burstMode;
    }

    public int getBurstCounter() {
        return burstCounter;
    }

    public void setBurstCounter(int burstCounter) {
        this.burstCounter = burstCounter;
    }

    /*==================================================================
     * Activity Lifecycle methods
     ===================================================================*/

    /**
     * Create MainActivity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // initialize Parmeters from storage
        // shared preferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        parameters = new Parameters(prefs);
        parameters.init();

        // set parameters for my XReal Beam Pro stereo window adjustment
        parameters.writeParallaxOffset(114);
        parameters.writeVerticalOffset(-12);

        String modelName = Build.MODEL;
        String manufacturer = Build.MANUFACTURER;
        String deviceName = manufacturer + " " + modelName;
        Log.d(TAG, "Device Manufacturer and Model: " + deviceName);
        if (modelName.equals("LPD-20W")) {
            // back cameras
            leftCameraId = "0";
            rightCameraId = "2";
            //crossEye = true;
            stereoCameraId = "4";  // logical (left "0" and right "2") back cameras
            cameraWidth = 4656; // 16Mp Back camera width lens pixels
            cameraHeight = 3496;// 16MP Back camera height lens pixels

            // front cameras
//            leftCameraId = "1";
//            rightCameraId = "3";
//            stereoCameraId = "5";  // logical (left "1" and right "3") front cameras
//            cameraWidth = 4656; // 16Mp Back camera width lens pixels
//            cameraHeight = 3496;// 16MP Back camera height lens pixels

            //APP_REVIEW_PACKAGE = "com.leialoft.leiaplayer"; // Review with Leia Player app default todo

            Log.d(TAG, "set stereo cameras for " + deviceName);
        } else if (modelName.equals("LPD-10W")) {
            stereoCameraId = "4";  // logical (left "0" and right "2") back cameras
            cameraWidth = 2304; // 16Mp Back camera width lens pixels
            cameraHeight = 1728;// 16MP Back camera height lens pixels
            //APP_REVIEW_PACKAGE = "com.leialoft.leiaplayer"; // Review with Leia Player app default
            //BASE_FOLDER = Environment.DIRECTORY_DCIM;  // change base to DCIM folder for cameras
            Log.d(TAG, "set stereo cameras for " + deviceName);

        }
        // check system property for usb uvc webcam
        Log.d(TAG, "ro.usb.uvc.enabled=" + System.getProperty("ro.usb.uvc.enabled"));
        //System.setProperty("ro.usb.uvc.enabled", String.valueOf(true));
        //Log.d(TAG, "ro.usb.uvc.enabled="+System.getProperty("ro.usb.uvc.enabled"));

        // Establish media storage folders for saving photos
        media = new Media(this, parameters, aiVision);
        media.createMediaFolder();

        // Create camera manager
        mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        setContentView(R.layout.layout);

        View decorView = getWindow().getDecorView();
        // Set the pointer icon to null (invisible)
        decorView.setPointerIcon(PointerIcon.getSystemIcon(this, PointerIcon.TYPE_NULL));
        decorView.setOnGenericMotionListener((view, motionEvent) -> {
            // Handle the event here
            return handleMouseEvent(motionEvent);
        });

        checkPermissions();

        // set up UDP remote control
        if (isWiFiRemoteEnabled) {
            udpRemoteControl = new UdpRemoteControl(this);
            udpRemoteControl.setup();
        }


        // setup AI vision connection to local network vision small LM
        if (aiVisionEnabled) {
            aiVision = new AIvision(this);
        }

        countdownTextView = findViewById(R.id.overlay_text);

        // This is a crucial step: we need to wait for the view to be laid out
        // before we can get its dimensions.
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

        decorView.post(new Runnable() {
            @Override
            public void run() {
                setupSurfaces();
                openCamera();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");
        setVisibility();
        if (commandLine == null) {
            commandLine = new CommandLine(this, parameters, splashMessage + " Version: " + BuildConfig.VERSION_NAME);
        }
    }

    @Override
    protected void onStop() {
        if (MyDebug.LOG)
            Log.d(TAG, "onStop");
        super.onStop();
        // we stop location listening in onPause, but done here again just to be certain!
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        closeCamera();
        stopCameraThread();
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
        if (shutterSound) {
            CameraInfo.mustPlayShutterSound();
        }
        Log.d(TAG, "shutter sound " + ((shutterSound) ? "on" : "off"));

        // for debugging and test
        if (allPermissionsGranted) {
            String[] list = getCameraIdList();  // debug what cameras are available
            for (String id : list) {
                Log.d(TAG, "Available CameraId: |" + id + "|");
            }

            CameraInfoUtil.checkCameraSyncType(this, list);
            CameraInfoUtil.logFocusDistanceCalibration(this);  // for debug

            startCameraThread();

            openCamera();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (mImageReader0 != null) {
            mImageReader0.close();
        }
        if (mImageReader2 != null) {
            mImageReader2.close();
        }

        udpRemoteControl.destroy();

    }

    private void startCameraThread() {
        Log.d(TAG, "startCameraThread");
        if (mCameraThread == null) {
            mCameraThread = new HandlerThread("CameraBackgroundThread"); // Name the thread
            mCameraThread.start();
            mCameraHandler = new Handler(mCameraThread.getLooper());
            cameraExecutor = new HandlerExecutor(mCameraHandler);
        }
    }

    private void stopCameraThread() {
        if (mCameraThread != null) {
            mCameraThread.quitSafely(); // Safely shut down the looper
            try {
                mCameraThread.join(); // Wait for the thread to finish
                mCameraThread = null;
                mCameraHandler = null;
                cameraExecutor = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while stopping camera thread", e);
            }
        }
    }

//    @Override
//    public void onBackPressed() {
//        // Check if a specific condition is met, for example, if a drawer is open
//        // if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
//        //     drawerLayout.closeDrawer(GravityCompat.START);
//        // } else {
//        // Call the super method to allow the default back button behavior
//        Log.d(TAG, "onBackPressed()");
//        super.onBackPressed();
//        // }
//    }

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
                    return true;
                case MotionEvent.ACTION_MOVE:
                    // Mouse movement (use getX(), getY()) not used and consumed
                    return true;
                // You can also handle ACTION_HOVER_MOVE for hover events
            }
        }
        return false;
    }

    private void handleButtonPress(int buttonState) {
        if ((buttonState & MotionEvent.BUTTON_PRIMARY) != 0) {
            // Left mouse button pressed
            Log.d(TAG, "Left button pressed");
            capturePhoto();
        }
        if ((buttonState & MotionEvent.BUTTON_TERTIARY) != 0) {
            // Middle mouse button pressed
            Log.d(TAG, "Middle button pressed");
            media.printImageType();
        }
        if ((buttonState & MotionEvent.BUTTON_SECONDARY) != 0) {
            // Right mouse button pressed
            Log.d(TAG, "Right button pressed");
            displayMode = DisplayMode.SBS.ordinal();
            media.reviewPhotos(displayMode);
        }
        // Other buttons like BUTTON_BACK, BUTTON_FORWARD can also be checked
    }

    private void handleButtonRelease(int buttonState) {
        // Handle button release events similarly
    }

    private void capturePhoto() {
        if (isPhotobooth && (countdownDigit < 0)) {
            startCountdownSequence(countdownStart);
        } else {
            countdownTextView.setVisibility(View.GONE);
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    createCameraCaptureSession();
                }
            });
        }
    }

    private void startContinuousCapturePhoto() {
        if (burstModeFeature) {
            if (burstMode) {
                Toast.makeText(this, "Start Burst Mode ", Toast.LENGTH_SHORT).show();
                if (isPhotobooth && (countdownDigit < 0) ) {
                    startCountdownSequence(countdownStart);  // calls createCameraCaptureSession() after count down finished
                    burstCounter = BURST_COUNT;
                    createCameraCaptureSession();
                } else {
                    burstCounter = BURST_COUNT;
                    createCameraCaptureSession();
                }
//                else
//                { // cancel burst when burst is in progress because burstCounter > 0
//                    if (burstCounter > 0) {
//                        burstCounter = 0;
//                        Toast.makeText(this, "Burst Mode Canceled ", Toast.LENGTH_SHORT).show();
//                    } else {  // start burst
//                        burstCounter = BURST_COUNT;
//                        createCameraCaptureSession();
//                    }
//                }
            }
        } else {
            Toast.makeText(this, "Burst Mode Not Enabled", Toast.LENGTH_SHORT).show();
        }
    }

    public void nextContinuousCapturePhoto() {
        Log.d(TAG, "nextContinuousCapturePhoto()");
        if (burstModeFeature && burstMode) {
            if (burstCounter > 0) {
                burstCounter--;
                Log.d(TAG, "burstCounter=" + burstCounter);
                createCameraCaptureSession();
            }
        }
    }

    SurfaceHolder.Callback shCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(@NonNull SurfaceHolder holder) {
            Log.d(TAG, "Surface holder surfaceCreated");
            if (mSurfaceView0.isAttachedToWindow() && mSurfaceView2.isAttachedToWindow()) {
                createCameraPreviewSession();
            }
        }

        @Override
        public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }
    };

    private void setupSurfaces() {
        Log.d(TAG, "setupSurfaces()");

        mSurfaceView0 = findViewById(R.id.surfaceView);
        mSurfaceView2 = findViewById(R.id.surfaceView2);

        mSurfaceHolder0 = mSurfaceView0.getHolder();
        mSurfaceHolder2 = mSurfaceView2.getHolder();

        mSurfaceHolder0.addCallback(shCallback);
        mSurfaceHolder2.addCallback(shCallback);
    }

    /*==================================================================
     * Permissions
     ===================================================================*/

    private void checkPermissions() {
        Log.d(TAG, "checkPermissions");
        String[] permissions = {CAMERA};
        boolean needsPermission = false;

        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                needsPermission = true;
                break;
            }
        }

        if (needsPermission) {
            Log.d(TAG, "Needs permission");
            ActivityCompat.requestPermissions(this, permissions, MY_CAMERA_REQUEST_CODE);
        } else {
            allPermissionsGranted = true;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult requestCode=" + requestCode);
        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            allPermissionsGranted = true;
            for (int result : grantResults) {
                Log.d(TAG, "result=" + result);
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
        }
    }

    private void openCamera() {
        Log.d(TAG, "openCamera()");

        Log.d(TAG, "openCamera() cameraWidth=" + cameraWidth + " cameraHeight=" + cameraHeight);

        // Setup ImageReaders for capture
        mImageReader0 = ImageReader.newInstance(cameraWidth, cameraHeight, ImageFormat.JPEG, 2);  // 2 maxImages
        mImageReader2 = ImageReader.newInstance(cameraWidth, cameraHeight, ImageFormat.JPEG, 2);  // 2 maxImages

        if (ActivityCompat.checkSelfPermission(this, CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try {
                mCameraManager.openCamera(stereoCameraId, mStateCallback, mCameraHandler); // logical camera 3 combines 1 and 2
                Log.d(TAG, "mCameraManager.openCamera( " + stereoCameraId + " )");
            } catch (CameraAccessException e) {
                Log.e(TAG, "Camera access exception", e);
            }
        }
    }

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) { // Open camera
            mCameraDevice = camera;
            if ((mSurfaceView0 != null && mSurfaceView2.isAttachedToWindow()) && mSurfaceView2 != null && mSurfaceView2.isAttachedToWindow()) {
                createCameraPreviewSession();
            } else {
                Log.d(TAG, "Surface not attached to window");
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) { // Turn off camera
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            mCameraDevice.close();
            mCameraDevice = null;
            Log.e(TAG, "Camera " + camera.getId() + " hardware failure");
        }
    };

    private void closeCamera() {
        Log.d(TAG, "closeCamera()");
        if (mCameraCaptureSession != null) {
            try {
                if (mCameraCaptureSession.isReprocessable()) {
                    mCameraCaptureSession.stopRepeating();
                    mCameraCaptureSession.abortCaptures();
                    mCameraCaptureSession.close();
                }
            } catch (CameraAccessException e) {
                throw new RuntimeException(e);
            }
            if (mCameraCaptureSession != null) {
                mCameraCaptureSession.close();
            }
            mCameraCaptureSession = null;
            if (mCameraDevice != null) {
                mCameraDevice.close();
            }
            mCameraDevice = null;
        }
    }

    /**
     * Create camera view session for live preview
     */
    private void createCameraPreviewSession() {
        Log.d(TAG, "createCameraPreviewSession()");
        if (mSurfaceHolder0 == null || mSurfaceHolder0.getSurface() == null) {
            Log.e("CameraApp", "Surface 0 is not ready yet!");
            return;
        }
        if (mSurfaceHolder2 == null || mSurfaceHolder2.getSurface() == null) {
            Log.e("CameraApp", "Surface 2 is not ready yet!");
            return;
        }
        try {
            if (!mSurfaceHolder0.getSurface().isValid() || !mSurfaceHolder2.getSurface().isValid()) {
                Log.d(TAG, "Surface not valid");
                return;
            }
            OutputConfiguration opcL = new OutputConfiguration(new Size(176, 144), SurfaceTexture.class);
            OutputConfiguration opc0 = new OutputConfiguration(mSurfaceHolder0.getSurface());
            opc0.setPhysicalCameraId(leftCameraId);
            OutputConfiguration opc1 = new OutputConfiguration(mSurfaceHolder2.getSurface());
            opc1.setPhysicalCameraId(rightCameraId);

            OutputConfiguration opcCapture0 = new OutputConfiguration(mImageReader0.getSurface());
            opcCapture0.setPhysicalCameraId(leftCameraId);
            OutputConfiguration opcCapture1 = new OutputConfiguration(mImageReader2.getSurface());
            opcCapture1.setPhysicalCameraId(rightCameraId);

            List<OutputConfiguration> outputConfigsAll = Arrays.asList(opcL, opc0, opc1, opcCapture0, opcCapture1);

            SessionConfiguration sessionConfiguration = new SessionConfiguration(
                    SessionConfiguration.SESSION_REGULAR, outputConfigsAll, cameraExecutor,
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            if (null == mCameraDevice) return;
                            mCameraCaptureSession = cameraCaptureSession;
                            try {
                                captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                captureRequestBuilder.addTarget(mSurfaceHolder0.getSurface());
                                captureRequestBuilder.addTarget(mSurfaceHolder2.getSurface());

                                captureRequestBuilder.set(CaptureRequest.TONEMAP_MODE, CaptureRequest.TONEMAP_MODE_CONTRAST_CURVE);
                                captureRequestBuilder.set(CaptureRequest.TONEMAP_CURVE, new TonemapCurve(curve_srgb, curve_srgb, curve_srgb));
                                if (FOCUS_DISTANCE[focusDistanceIndex] == AUTO_FOCUS_DISTANCE) {
                                    captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                } else {
                                    captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
                                }
                                //
                                captureRequestBuilder.set(EXPOSURE_METERING, METERING[meteringIndex]);
                                captureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, FOCUS_DISTANCE[focusDistanceIndex]);

                                captureRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, 1); // NOISE_REDUCTION_MODE
                                captureRequestBuilder.set(CaptureRequest.EDGE_MODE, 1); // EDGE_MODE
                                captureRequestBuilder.set(CaptureRequest.CONTROL_EXTENDED_SCENE_MODE, 1);  // sync left and right cameras
                                mCameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, mCameraHandler);
                            } catch (CameraAccessException e) {
                                Log.e(TAG, "Camera access exception in session config", e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.e(TAG, "onConfigureFailed");
                        }
                    });

            mCameraDevice.createCaptureSession(sessionConfiguration);
            Log.d(TAG, "createCameraPreviewSession() done");

        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera access exception", e);
        }
    }

    public String[] getCameraIdList() {
        ArrayList<String> list = new ArrayList<String>();
        String cameraId = "0";
        Log.d(TAG, "getCameraIdList()");
        if (mCameraManager == null) {
            Log.e(TAG, "CameraManager service not available");
        } else {

            for (int cameraNum = 0; cameraNum < MAX_NUM_CAMERAS; cameraNum++) {
                cameraId = String.valueOf(cameraNum);
                //String[] cameraIds = {leftCameraId, frontCameraId, rightCameraId, stereoCameraId, "4", "5"};
                try {
                    // Iterate through all available camera IDs on the device
                    CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                    Set<String> group = characteristics.getPhysicalCameraIds();
                    String[] gList = group.toArray(new String[0]);
                    Log.d(TAG, "cameraId=" + cameraId + " Set: " + Arrays.toString(group.toArray()));

                    if (group.isEmpty()) {
                        list.add(cameraId);
                    } else {
                        list.add(cameraId);  // include logical camera id
                        for (String s : gList) {
                            if (!list.contains(s)) list.add(s);
                        }
                    }

                } catch (CameraAccessException e) {
                    Log.e(TAG, "getCameraIdList(): Failed to access camera characteristics");
                    break;
                } catch (IllegalArgumentException ill) {
                    Log.d(TAG, "getCameraIdList(): Illegal Argument Failed to access camera characteristics ");
                    break;
                }
            }
        }
        Log.d(TAG, "getCameraIdList(): No more cameras >= " + cameraId);
        String[] rList = list.toArray(new String[0]);
        return rList;
    }


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
            case BACK_KB_KEY:
            case BACK_KEY:
            case KeyEvent.KEYCODE_ESCAPE:
            case KeyEvent.KEYCODE_BUTTON_B:
                return true;
            default:
                exitApp = false;
                return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyUp " + keyCode);
        if (commandLine.processCommandLineKey(keyCode, event)) {
            return true;
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                //case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_3D_MODE: // camera key - first turn off auto launch of native camera app
            case SHUTTER_KEY:
            case SHUTTER_KB_KEY:
                if (burstMode) {
                    return true; // ignore key
                } else {
                    capturePhoto();
                }
                return true;
            case BURST_KEY:
            case BURST_KB_KEY: // start continuous capture mode
                if (burstModeFeature) {
                    if (burstMode) {
                        return true;  // ignore key
                    } else {
                        burstMode = true;
                        startContinuousCapturePhoto();
                    }
                }
                return true;
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
            case BACK_KB_KEY:
            case KeyEvent.KEYCODE_BUTTON_B:
                if (burstMode) {
                    burstMode = false;
                    Toast.makeText(this, "Burst Mode Canceled ", Toast.LENGTH_SHORT).show();
                    return true;
                }

                if (exitApp) {
                    finish();
                } else {
                    Toast.makeText(this, "Exit?", Toast.LENGTH_SHORT).show();
                    exitApp = true;
                }
                return true;
            case SHARE_KEY:
            case SHARE_KB_KEY:
                boolean ok = media.shareReviewImage();
                if (!ok) {
                    Toast.makeText(this, "Nothing to Share", Toast.LENGTH_SHORT).show();
                }
                return true;
            case FN_KEY:
            case FN_KB_KEY:
                closeCamera();
                meteringIndex++;
                if (meteringIndex >= METERING.length) meteringIndex = 0;
                Toast.makeText(this, METERING_NAMES[meteringIndex], Toast.LENGTH_SHORT).show();
                openCamera();
                return true;
            case FOCUS_KEY: // change focus distance, should be sub menu
            case FOCUS_KB_KEY: // change focus distance, should be sub menu
                closeCamera();
                int i = focusDistanceIndex + 1;
                if (i >= FOCUS_DISTANCE.length) i = 0;
                focusDistanceIndex = i;
                openCamera();
                Toast.makeText(this, FOCUS_DISTANCE_NAMES[focusDistanceIndex], Toast.LENGTH_SHORT).show();
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
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case REVIEW_KEY:
            case REVIEW_KB_KEY:
                media.reviewPhotos(displayMode);
                return true;
            case ANAGLYPH_KEY:
            case ANAGLYPH_KB_KEY:
//                closeCamera();
//                if (!isAnaglyphMode) isAnaglyphMode = true;
//                else isAnaglyphMode = false;
//                if (isAnaglyphMode) Toast.makeText(this, "Anaglyph", Toast.LENGTH_SHORT).show();
//                else Toast.makeText(this, "SBS", Toast.LENGTH_SHORT).show();
//                openCamera();
                return true;
            case MODE_KEY:
            case MODE_KB_KEY:
                Toast.makeText(this, "Auto Exposure - Manual, Shutter Priority", Toast.LENGTH_SHORT).show();
//                closeCamera();
//                openCamera();
                return true;
            case SHUTTER_SPEED_KEY:
            case SHUTTER_SPEED_KB_KEY:
                Toast.makeText(this, "Shutter Speed - not implemented", Toast.LENGTH_SHORT).show();
//                closeCamera();
//                openCamera();
                return true;
            case TIMER_KEY:
            case TIMER_KB_KEY:
                if (isPhotobooth) {
                    isPhotobooth = false;
                    BURST_COUNT = BURST_COUNT_DEFAULT;
                } else {
                    isPhotobooth = true;
                    BURST_COUNT = BURST_COUNT_PHOTO_BOOTH;
                }
                countdownDigit = -1;
                if (isPhotobooth) {
                    Toast.makeText(this, "Photo Booth Countdown=" + Integer.toString(countdownStart), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Timer Off", Toast.LENGTH_SHORT).show();
                }
//                closeCamera();
//                openCamera();
                return true;
            case ISO_KEY:
            case ISO_KB_KEY:
                Toast.makeText(this, "ISO - not implemented", Toast.LENGTH_SHORT).show();
//                closeCamera();
//                openCamera();
                return true;
            case DISP_KEY:
            case DISP_KB_KEY:
                displayMode++;
                if (displayMode >= DisplayMode.values().length)
                    displayMode = DisplayMode.SBS.ordinal();
                if (displayMode == DisplayMode.SBS.ordinal()) {
                    Toast.makeText(this, "Display Mode SBS", Toast.LENGTH_SHORT).show();
                } else if (displayMode == DisplayMode.ANAGLYPH.ordinal()) {
                    Toast.makeText(this, "Display Mode Anaglyph", Toast.LENGTH_SHORT).show();
                } else if (displayMode == DisplayMode.LR.ordinal()) {
                    Toast.makeText(this, "Display Mode LR", Toast.LENGTH_SHORT).show();
                }
//                closeCamera();
//                openCamera();
                return true;
            case MENU_KEY:
            case MENU_KB_KEY:
                Toast.makeText(this, "MENU - not implemented", Toast.LENGTH_SHORT).show();
//                closeCamera();
//                openCamera();
                return true;
            case PRINT_KEY:
            case PRINT_KB_KEY:
                media.printImageType();
                return true;
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
                return super.onKeyDown(keyCode, event);
        }
    }

    private void playShutterSound() {
        // Create a ToneGenerator instance.
        // The AudioManager.STREAM_SYSTEM is important here to ensure the sound plays through the system volume.
        // The volume parameter (100) is a percentage of the maximum volume.
        ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_SYSTEM, 100);

        // Play the tone.
        // TONE_PROP_BEEP is a short, distinct sound that works well as a shutter click.
        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP);

        // Release the ToneGenerator resources after a short delay.
        // It's crucial to release the resources to avoid memory leaks.
        // We use a Handler to delay the release so the sound has time to play.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                toneGen.stopTone();
                toneGen.release();
            }
        }, 100); // 100 milliseconds is a good duration for a short tone.
    }

    /**
     * Create a camera capture session to take a picture
     */
    public void createCameraCaptureSession() {
        Log.d(TAG, "createCameraCaptureSession()");
        if (mCameraDevice == null || mCameraCaptureSession == null) {
            Toast.makeText(this, "Camera not ready", Toast.LENGTH_SHORT).show();
            if (mCameraDevice == null) Log.e(TAG, "mCameraDevice is null");
            if (mCameraCaptureSession == null) Log.e(TAG, "mCameraCaptureSession is null");
            return;
        }
        try {
            // Create capture request for both cameras
            CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader0.getSurface());
            captureBuilder.addTarget(mImageReader2.getSurface());
            // default TONEMAP_MODE_CONTRAST_CURVE assumed for best contrast, color and detail capture
            captureBuilder.set(CaptureRequest.TONEMAP_CURVE, new TonemapCurve(curve_srgb, curve_srgb, curve_srgb));

            if (FOCUS_DISTANCE[focusDistanceIndex] == AUTO_FOCUS_DISTANCE) {
                captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            } else {
                captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
            }

            captureBuilder.set(EXPOSURE_METERING, METERING[meteringIndex]);
            captureBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, FOCUS_DISTANCE[focusDistanceIndex]);

            captureBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, 1); // NOISE_REDUCTION_MODE
            captureBuilder.set(CaptureRequest.EDGE_MODE, 1); // EDGE_MODE
            captureBuilder.set(CaptureRequest.CONTROL_EXTENDED_SCENE_MODE, 1);  // sync left and right cameras
            imageL = null;
            imageR = null;
            leftBytes = null;
            rightBytes = null;
            media.recycleBitmaps();

            mImageReader0.setOnImageAvailableListener(new OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    imageL = reader.acquireLatestImage();
                    saveImageFiles(imageL, imageR);
                }
            }, mCameraHandler);

            mImageReader2.setOnImageAvailableListener(new OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    imageR = reader.acquireLatestImage();
                    saveImageFiles(imageL, imageR);
                }
            }, mCameraHandler);

            CameraCaptureSession.CaptureCallback captureSingleRequestListener =
                    new CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureCompleted(
                                CameraCaptureSession session,
                                CaptureRequest request,
                                TotalCaptureResult result) {
                            // This method is called when the capture is complete
                            // You can process the result here if needed
                            Log.d(TAG, "Capture completed successfully");
                        }

                        @Override
                        public void onCaptureFailed(
                                CameraCaptureSession session,
                                CaptureRequest request,
                                CaptureFailure failure) {
                            Log.e(TAG, "Capture failed: " +
                                    "Reason: " + failure.getReason() +
                                    ", Sequence ID: " + failure.getSequenceId() +
                                    ", Frame number: " + failure.getFrameNumber());

                            switch (failure.getReason()) {
                                case CaptureFailure.REASON_ERROR:
                                    Log.e(TAG, "Capture failed due to an error");
                                    break;
                                case CaptureFailure.REASON_FLUSHED:
                                    Log.e(TAG, "Capture failed due to stream flush");
                                    break;
                                default:
                                    Log.e(TAG, "Capture failed for unknown reason");
                            }
                        }
                    };

            mCameraCaptureSession.captureSingleRequest(captureBuilder.build(), cameraExecutor, captureSingleRequestListener);

        } catch (CameraAccessException e) {
            Log.e(TAG, "Error capturing images", e);
            Toast.makeText(this, "Error capturing images", Toast.LENGTH_SHORT).show();
        }
        if (shutterSound) {
            playShutterSound();
        }
    }

    private void saveImageFiles(Image left, Image right) {
        if (left != null && right != null) {
            leftBytes = convertToBytes(left);
            rightBytes = convertToBytes(right);
            left.close();
            right.close();
            if (leftBytes != null && rightBytes != null) {
                media.saveImageFiles(leftBytes, rightBytes);
                leftBytes = null;
                rightBytes = null;
            }
        }

    }


    private byte[] convertToBytes(Image image) {
        // Get the JPEG image data, add other formats if needed
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        //Log.d(TAG, "convertToBytes Image Format: " + image.getFormat() + " planes " + planes.length);
        return bytes;
    }


    /*
     * Start countdown sequence for Photo booth function
     */
    void startCountdownSequence(int startCount) {
        if (countdownTimer == null) {
            countdownTimer = new Timer();
            countdownDigit = startCount + 1;
            countdownTextView.setText(Integer.toString(countdownDigit));
            countdownTextView.setVisibility(View.VISIBLE);

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
                                countdownTextView.setText("");
                                countdownTextView.setVisibility(View.GONE);
                                // hide digit display
                                createCameraCaptureSession(); // take a picture
                            }
                        });
                    } else {
                        MainActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                if (countdownDigit == 0) {
                                    countdownTextView.setText("");
                                    countdownTextView.setVisibility(View.GONE);
                                } else {
                                    countdownTextView.setText(Integer.toString(countdownDigit));
                                    countdownTextView.setVisibility(View.VISIBLE);
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

    /**
     * Show half second Toast message
     *
     * @param message Text message to display
     */
    private void showToast(String message) {
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
        //if (DEBUG) println("setVisibility width = "+width + " height="+height);
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

}

