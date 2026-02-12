package com.andymodla.android3dcamera;

import static android.Manifest.permission.CAMERA;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.andymodla.android3dcamera.camera.Camera3D;
import com.andymodla.android3dcamera.sketch.PhotoBooth6x4;
import com.andymodla.android3dcamera.sketch.PhotoBooth;

import java.util.Timer;
import java.util.TimerTask;

import processing.android.CompatUtils;
import processing.android.PFragment;
import processing.core.PApplet;


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

    // aspect ratio
    int aspectRatioIndex = 0;  // default
    final String[] ASPECT_RATIO_NAMES = {"DEFAULT", "4:3", "16:9", "1:1"};

    // Maximum camera sensor image dimensions
    //private int cameraWidth = 1024;
    //private int cameraHeight = 768;
    //private int cameraWidth = 1920;
    //private int cameraHeight = 1080;
    //private int cameraWidth = 1440;
    //private int cameraHeight = 1080;
    //private int cameraWidth = 4080;  // results in 1920x1440 images
    //private int cameraHeight = 3060; // results in 1920x1440 images


    private AIvision aiVision;  // local network small multimodal vision AI model server (Google Gemma 3 8B 4_K_M GGUF)
    private Media media;
    private Camera3D camera;
    private Parameters parameters;

    private boolean aiVisionEnabled = false;

    private boolean isWiFiRemoteEnabled = false; //true;
    private UdpRemoteControl udpRemoteControl;


    // states definitions
    private static final int LIVEVIEW_STATE = 0;
    private static final int REVIEW_STATE = 1;
    private int state = LIVEVIEW_STATE;

    public volatile DisplayMode displayMode = DisplayMode.SBS;

    private boolean exitApp = false; // exit app flag with back or esc button

    // Photo booth variables
    private boolean isPhotobooth = true;  // work in progress
    private boolean isPhotoboothReview = false;

    private PhotoBooth photoBooth;  // photo booth sketch
    PFragment photoBoothFragment;  // processing library photo booth fragment
    View decorView; // screen window view for camera app

    Timer countdownTimer;
    int countdownStart = 0;
    int countdownDigit = -1;

    volatile boolean continuousModeFeature = true;
    volatile boolean continuousMode = false;  // continuous capture is active
    public volatile int continuousCounter = 0;
    public static final int CONTINUOUS_COUNT_DEFAULT = 59; //(one less 60)
    public static final int CONTINUOUS_COUNT_PHOTO_BOOTH = 3; //(one less 4)
    public int CONTINUOUS_COUNT = 0;

    // Key codes for 8BitDo Micro Bluetooth Keyboard controller (Android mode)
    static final int SHUTTER_KEY = KeyEvent.KEYCODE_BUTTON_R1;
    static final int FOCUS_KEY = KeyEvent.KEYCODE_BUTTON_R2;
    static final int MODE_KEY = KeyEvent.KEYCODE_BUTTON_L2;
    static final int CONTINUOUS_KEY = KeyEvent.KEYCODE_BUTTON_L1;
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
    static final int CONTINUOUS_KB_KEY = KeyEvent.KEYCODE_K;
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


    /*==================================================================
     * Activity Lifecycle methods
     ===================================================================*/

    /**
     * Create MainActivity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initLibYuv(); // Initialize native yuvlib library

        // initialize Parmeters from storage
        // shared preferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        parameters = new Parameters(prefs);
        parameters.init();

        // set parameters for my XReal Beam Pro stereo window adjustment
        // Stereo Image Alignment parameters (same values as StereoPhotoMaker)
        // 212  left/right parallax horizontal offset for stereo window placement
        // -12  left/right camera alignment vertical offset for camera correction

        parameters.writeParallaxOffset(212);
        parameters.writeVerticalOffset(-12);

        // Establish media storage folders for saving photos
        media = new Media(this, parameters, aiVision);
        media.createMediaFolder();

        // set up UDP remote control for broadcast message reception
        if (isWiFiRemoteEnabled) {
            udpRemoteControl = new UdpRemoteControl(this);
            udpRemoteControl.setup();
        }

        // setup AI vision connection to local network vision small multimodal LLM
        if (aiVisionEnabled) {
            aiVision = new AIvision(this);
        }

        if (isPhotobooth) {
            FrameLayout frame = new FrameLayout(this);
            frame.setId(CompatUtils.getUniqueViewId());
            setContentView(frame, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            photoBooth = new PhotoBooth();
            photoBoothFragment = new PFragment(photoBooth);
            photoBoothFragment.setView(frame, this);

        } else {
            setContentView(R.layout.layout);
        }

        checkPermissions();
        camera = new Camera3D(this, media, parameters, photoBooth);

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

        decorView.post(new Runnable() {
            @Override
            public void run() {
                camera.init(isPhotobooth);
                camera.openCamera();
                if (photoBooth != null) {
                    photoBooth.setCamera(camera);
                }
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");
        setVisibility();
        if (!isPhotobooth) {
            if (commandLine == null) {
                commandLine = new CommandLine(this, parameters, splashMessage + " Version: " + BuildConfig.VERSION_NAME);
            }
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        camera.closeCamera();
        camera.stopCameraThread();
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
        camera.shutterSound();

        // for debugging and test
        if (allPermissionsGranted) {
            String[] list = camera.getCameraIdList();  // debug what cameras are available
            for (String id : list) {
                Log.d(TAG, "Available CameraId: |" + id + "|");
            }

            // Debug information
            //CameraInfoUtil.checkCameraSyncType(this, list);
            //CameraInfoUtil.logFocusDistanceCalibration(this);  // for debug

            camera.startCameraThread();
            camera.openCamera();
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
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        camera.destroy();
        photoBoothFragment.onDestroy();
        if (udpRemoteControl != null) {
            udpRemoteControl.destroy();
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
        if ((buttonState & MotionEvent.BUTTON_PRIMARY) != 0) {
            // Left mouse button pressed
            Log.d(TAG, "Left button pressed");
            if (isPhotobooth) {
                if (isPhotoboothReview) {
                    media.printImageType();
                } else {
                    capturePhoto();
                }
            } else {
                capturePhoto();
            }
        }
        else if ((buttonState & MotionEvent.BUTTON_TERTIARY) != 0) {
            // Middle mouse button pressed
            Log.d(TAG, "Middle button pressed");
            displayMode = DisplayMode.SBS;

            if (isPhotobooth && !isPhotoboothReview) {
                state = REVIEW_STATE;
                camera.closeCamera();
                photoBooth.review(displayMode);
            } else if (!isPhotoboothReview) {
                media.reviewPhotos(displayMode);
            }
        }
        else if ((buttonState & MotionEvent.BUTTON_SECONDARY) != 0) {
            // Right mouse button pressed
            Log.d(TAG, "Right button pressed");
            if (isPhotobooth) {
                displayMode = displayMode.next();
                photoBooth.setDisplayMode(displayMode);
            } else {
                media.printImageType();
            }
        }
        // Other buttons like BUTTON_BACK, BUTTON_FORWARD can also be checked here
    }

    private void handleButtonRelease(int buttonState) {
        // Handle button release events similarly
    }

    private void handleMouseWheel(float delta) {
        // Handle mouse wheel events
        if (delta > 0) {
            // Scrolled Up (away from user)
            //if (state == REVIEW_STATE) {
            //    photoBooth.processKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT, 0);
            //} else {
            if (isPhotobooth)
                photoBooth.processKeyCode(KeyEvent.KEYCODE_RIGHT_BRACKET, 0);
            //}
        } else if (delta < 0) {
            // Scrolled Down (toward user)
            //if (state == REVIEW_STATE) {
            //    photoBooth.processKeyCode(KeyEvent.KEYCODE_DPAD_LEFT, 0);
            //} else {
            if (isPhotobooth)
                photoBooth.processKeyCode(KeyEvent.KEYCODE_LEFT_BRACKET, 0);
            //}
        }
    }


    private void capturePhoto() {
        if ((countdownDigit < 0)) {
            startCountdownSequence(countdownStart);
        } else {
            countdownTextView.setVisibility(View.GONE);
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    camera.createCameraCaptureSession();
                }
            });
        }
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
        char ch = (char) event.getUnicodeChar();
        Log.d(TAG, "onKeyUp " + keyCode + " "+ ch);
        if (commandLine != null && commandLine.processCommandLineKey(keyCode,ch)) {
            return true;
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                //case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_3D_MODE: // camera key - first turn off auto launch of native camera app
            case SHUTTER_KEY:
            case SHUTTER_KB_KEY:
                if (state == REVIEW_STATE) { // ignore shutter in review state
                    return true;
                }
                if (continuousMode) {
                    return true; // ignore shutter key in continuous shutter
                } else {
                    capturePhoto();
                }
                return true;

            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case REVIEW_KEY:
            case REVIEW_KB_KEY:
                if (isPhotobooth) {
                    // ignore review key in photo booth
                    return true;
                } else {
                    media.reviewPhotos(displayMode);
                }
                return true;

            case CONTINUOUS_KEY:
            case CONTINUOUS_KB_KEY: // start continuous capture mode
                if (continuousModeFeature) {
                    if (continuousMode) {
                        return true;  // ignore key
                    } else {
                        continuousMode = true;
                        startContinuousCapturePhoto();
                    }
                }
                return true;

            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
            case BACK_KB_KEY:
            case KeyEvent.KEYCODE_BUTTON_B:
                if (continuousMode) {
                    setContinuousMode(false);
                    Toast.makeText(this, "Continuous Mode Canceled ", Toast.LENGTH_SHORT).show();
                    return true;
                }
                if (state == REVIEW_STATE) {
                    // turn on camera for entering liveview state
                    state = LIVEVIEW_STATE;
                    camera.openCamera();
                    return true;
                }
                if (exitApp) {
                    finish();
                    System.exit(0);
                } else {
                    Toast.makeText(this, "Exit?", Toast.LENGTH_SHORT).show();
                    exitApp = true;
                }
                return true;

            case SHARE_KEY:
            case SHARE_KB_KEY:
                if (isPhotobooth) {
                    // ignore share key in photo booth
                    return true;
                } else {
                    boolean ok = media.shareReviewImage();
                    if (!ok) {
                        Toast.makeText(this, "Nothing to Share", Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            case FN_KEY:
            case FN_KB_KEY:
                camera.closeCamera();
                camera.setMeteringIndex();
                camera.openCamera();
                return true;
            case FOCUS_KEY: // change focus distance, should be sub menu
            case FOCUS_KB_KEY: // change focus distance, should be sub menu
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
            case ANAGLYPH_KEY:
            case ANAGLYPH_KB_KEY:
                if (isPhotobooth) {
                    displayMode = DisplayMode.ANAGLYPH;
                    photoBooth.setDisplayMode(displayMode);
                }
                return true;
            case MODE_KEY:
            case MODE_KB_KEY:
                Toast.makeText(this, "Auto Exposure - Manual, Shutter Priority", Toast.LENGTH_SHORT).show();
//                closeCamera();
//                openCamera();
                return true;
            case SHUTTER_SPEED_KEY:
            case SHUTTER_SPEED_KB_KEY:
                if (isPhotobooth) {
                    photoBooth.keyPressedReview(event.getKeyCode(), ch);
                    return true;
                }
                Toast.makeText(this, "Shutter Speed - not implemented", Toast.LENGTH_SHORT).show();
//                closeCamera();
//                openCamera();
                return true;
            case TIMER_KEY:
            case TIMER_KB_KEY:
                if (state == REVIEW_STATE) {
                    if (isPhotobooth) {
                        photoBooth.keyPressedReview(keyCode, ch);
                    }
                    return true;
                } else {
                    if (CONTINUOUS_COUNT > 0) {
                        CONTINUOUS_COUNT = 0;
                    } else {
                        if (isPhotobooth) {
                            CONTINUOUS_COUNT = CONTINUOUS_COUNT_PHOTO_BOOTH;
                        } else {
                            CONTINUOUS_COUNT = CONTINUOUS_COUNT_DEFAULT;
                        }
                    }
                    Toast.makeText(this, "Set Timer Countdown=" + Integer.toString(CONTINUOUS_COUNT), Toast.LENGTH_SHORT).show();
                    countdownDigit = -1;
                    countdownStart = CONTINUOUS_COUNT;
                }
                return true;

            case ISO_KEY:
            case ISO_KB_KEY:
                Toast.makeText(this, "ISO - not implemented", Toast.LENGTH_SHORT).show();
//                closeCamera();
//                openCamera();
                return true;
            case DISP_KEY:
            case DISP_KB_KEY:
            case KeyEvent.KEYCODE_A:
                displayMode = displayMode.next();
                if (isPhotobooth) {
                    photoBooth.setDisplayMode(displayMode);
                }
                if (displayMode == DisplayMode.SBS) {
                    Toast.makeText(this, "Display SBS", Toast.LENGTH_SHORT).show();
                } else if (displayMode == DisplayMode.ANAGLYPH) {
                    Toast.makeText(this, "Display ANAGLYPH", Toast.LENGTH_SHORT).show();
                }   else if (displayMode == DisplayMode.LEFT) {
                    Toast.makeText(this, "Display LEFT", Toast.LENGTH_SHORT).show();
                } else if (displayMode == DisplayMode.RIGHT) {
                    Toast.makeText(this, "Display RIGHT", Toast.LENGTH_SHORT).show();
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

    public void setContinuousCounter(int continuousCounter) {
        this.continuousCounter = continuousCounter;
    }

    private void startContinuousCapturePhoto() {
        if (continuousModeFeature) {
            if (continuousMode) {
                Toast.makeText(this, "Start Continuous Mode ", Toast.LENGTH_SHORT).show();
                if ((countdownDigit < 0)) {
                    startCountdownSequence(countdownStart);  // calls createCameraCaptureSession() after count down finished
                    continuousCounter = CONTINUOUS_COUNT_PHOTO_BOOTH;
                } else {
                    continuousCounter = CONTINUOUS_COUNT;
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
                Log.d(TAG, "continuousCounter=" + continuousCounter);
                camera.createCameraCaptureSession();
                if (continuousCounter == 0) {
                    continuousMode = false;
                    Toast.makeText(this, "Continuous Mode Completed ", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /*
     * Start countdown sequence logic for camera app (not photo booth)
     */
    void startCountdownSequence(int startCount) {
        Log.d(TAG, "startCountdownSequence startCount=" + startCount);
        if (isPhotobooth) {
            if (startCount == 0) {
                camera.createCameraCaptureSession(); // take a picture
                return;
            }
        }
        if (countdownTimer == null) {
            countdownTimer = new Timer();
            countdownDigit = startCount + 1;
            if (isPhotobooth) {
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
                                if (isPhotobooth) {
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
                                    if (isPhotobooth) {
                                        photoBooth.setCountdown("");
                                    } else {
                                        countdownTextView.setText("");
                                        countdownTextView.setVisibility(View.GONE);
                                    }
                                } else {
                                    if (isPhotobooth) {
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

