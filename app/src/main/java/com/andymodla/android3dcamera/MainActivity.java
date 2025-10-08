package com.andymodla.android3dcamera;

import static android.Manifest.permission.CAMERA;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraInfo;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.hardware.camera2.params.TonemapCurve;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Toast;

import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import android.media.AudioManager;
import android.media.ToneGenerator;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import com.andymodla.android3dcamera.AnaglyphRenderer;

import netP5.Bytes;
import netP5.UdpServer1; // network library for UDP Server
import netP5.AbstractUdpServer1; // network library for UDP Server
import netP5.NetListener; // network library for UDP Server
import netP5.NetMessage; // network library for UDP Server
import netP5.NetStatus; // network library for UDP Server

// README:   https://github.com/ajavamind/A3DCamera/blob/main/README.md

/**
 * Copyright 2025 Andy Modla  All Rights Reserved
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "A3DCamera";
    private static final int MY_CAMERA_REQUEST_CODE = 100;

    private String BASE_FOLDER = Environment.DIRECTORY_PICTURES;  // Environment.DIRECTORY_DCIM;  //
    private String SAVE_FOLDER = "A3DCamera";
    private String PHOTO_PREFIX = "IMG_";
    private String APP_REVIEW_PACKAGE = "jp.suto.stereoroidpro"; // Review with StereoRoidPro app default

    volatile boolean allPermissionsGranted = false;
    volatile boolean shutterSound = true;
    //volatile boolean autoFocus = true;

    volatile int focusDistanceIndex = 0;  // default HYPERFOCAL
    static final float MACRO_FOCUS_DISTANCE = 10.0f;
    static final float HYPERFOCAL_FOCUS_DISTANCE = 0.60356647f;
    static final float PHOTO_BOOTH_FOCUS_DISTANCE = 1.0f;
    static final float AUTO_FOCUS_DISTANCE = 0.0f;
    static final float[] FOCUS_DISTANCE = {HYPERFOCAL_FOCUS_DISTANCE, PHOTO_BOOTH_FOCUS_DISTANCE, MACRO_FOCUS_DISTANCE, AUTO_FOCUS_DISTANCE};
    static final String[] FOCUS_DISTANCE_NAMES = {"HYPERFOCAL FOCUS DISTANCE", "PHOTO BOOTH FOCUS DISTANCE", "MACRO FOCUS DISTANCE", "AUTO FOCUS"};
    volatile boolean burstModeFeature = true;
    volatile boolean burstMode = true;
    int BURST_COUNT = 60;  // approximately 1 capture per second
    volatile int burstCounter = 0;

    // Maximum camera sensor image dimensions
    //private int cameraWidth = 1024;//1440;
    //private int cameraHeight = 768;//1080;
    //private int cameraWidth = 1920;
    //private int cameraHeight = 1080;
    //private int cameraWidth = 4080;  // results in 1920x1440 images
    //private int cameraHeight = 3060; // results in 1920x1440 images
    private int cameraWidth = 4080; // camera width lens pixels
    private int cameraHeight = 3072;// camera height lens pixels

    private static final float[] curve_srgb = { // sRGB curve
            0.0000f, 0.0000f, 0.0667f, 0.2864f, 0.1333f, 0.4007f, 0.2000f, 0.4845f,
            0.2667f, 0.5532f, 0.3333f, 0.6125f, 0.4000f, 0.6652f, 0.4667f, 0.7130f,
            0.5333f, 0.7569f, 0.6000f, 0.7977f, 0.6667f, 0.8360f, 0.7333f, 0.8721f,
            0.8000f, 0.9063f, 0.8667f, 0.9389f, 0.9333f, 0.9701f, 1.0000f, 1.0000f};

    private static final CaptureRequest.Key<Integer> EXPOSURE_METERING = new CaptureRequest.Key<>("org.codeaurora.qcamera3.exposure_metering.exposure_metering_mode", Integer.TYPE);
    static final int FRAME_AVERAGE = 0; // normal behavior
    static final int CENTER_WEIGHTED = 1;
    static final int SPOT_METERING = 2;
    int meteringIndex = 0;  // default
    static final int[] METERING = {FRAME_AVERAGE, CENTER_WEIGHTED, SPOT_METERING};
    String[] METERING_NAMES = {"FRAME AVERAGE", "CENTER WEIGHTED", "SPOT METERING"};
    // Saturation 0 - 10, default 5
    private static final CaptureRequest.Key<Integer> SATURATION = new CaptureRequest.Key<>("org.codeaurora.qcamera3.saturation.use_saturation", Integer.class);

    // Sharpness 0 - 6, default 2
    private static final CaptureRequest.Key<Integer> SHARPNESS = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sharpness.strength", Integer.class);

    //    captureRequestBuilder.set(SATURATION, 5);
    //    captureBuilder.set(SATURATION, 5);

    // Camera Ids for Xreal Beam Pro
    private String leftCameraId = "0";
    private String frontCameraId = "1";
    private String rightCameraId = "2";
    private String stereoCameraId = "3";
    private CameraDevice mCameraDevice;
    private CameraManager mCameraManager;
    private CameraCaptureSession mCameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private Handler mainHandler;

    // Display surfaces
    private SurfaceView mSurfaceView0, mSurfaceView2;
    private SurfaceHolder mSurfaceHolder0, mSurfaceHolder2;
    private GLSurfaceView glSurfaceView;
    private AnaglyphRenderer anaglyphRenderer;
    private AIvision aiVision;
    private View view;
    private Surface surface;

    // Image capture
    private ImageReader mImageReader0, mImageReader2;

    // Image Save File modes
    private volatile boolean saveAnaglyph = false;
    private volatile boolean saveSBS = true;
    private volatile boolean saveLR = false;
    // at least one of above booleans must be true;
    private volatile boolean crossEye = false;  // reverse SBS output to cross eye
    private volatile boolean isAnaglyphDisplayMode = false; //true;

    // states - work in progress
    private static final int STATE_LIVEVIEW = 0;
    private static final int STATE_REVIEW = 0;
    private int state = STATE_LIVEVIEW;

    volatile Image imageL;
    volatile Image imageR;
    volatile byte[] leftBytes;
    volatile byte[] rightBytes;
    volatile Bitmap leftBitmap;
    volatile Bitmap rightBitmap;
    volatile Bitmap sbsBitmap;
    volatile Bitmap anaglyphBitmap;
    String timestamp;
    volatile File reviewSBS;

    // UDP Server variables
    private volatile UdpServer1 udpServer;    // Broadcast receiver
    private int port = 8000;  // Broadcast port
    public static String httpUrl = "";
    public static String sCount = ""; // Photo counter received from Broadcast message

    public enum Stereo {MONO, LEFT, RIGHT} // no suffix, left suffix, right suffix

    public static Stereo suffixSelection = Stereo.MONO; // no suffix, left suffix, right suffix
    public static String lastSavedFilePath = null;

    private boolean isWiFiRemoteEnabled = false; //true;
    private boolean blankScreen = false;
    private boolean isVideo = false;

    private boolean isPhotobooth = false; //true;  // work in progress
    String prompt = "Does this photo show a deer? Answer only with deer or none/";
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
    static final int BLANK_SCREEN_KEY = KeyEvent.KEYCODE_BUTTON_SELECT; // 109-82 KeyEvent.KEYCODE_MENU;
    static final int AEL_KEY = KeyEvent.KEYCODE_BUTTON_START; // 108
    static final int FN_KEY = KeyEvent.KEYCODE_BUTTON_X; //  99 KeyEvent.KEYCODE_DEL = 67
    static final int MENU_KEY = KeyEvent.KEYCODE_BUTTON_Y;  // 100  KeyEvent.KEYCODE_SPACE = 62
    static final int REVIEW_KEY = KeyEvent.KEYCODE_BUTTON_A;  // 96 KEYCODE_DPAD_CENTER = 23
    static final int OK_KEY = KeyEvent.KEYCODE_BUTTON_A;  // 96 KEYCODE_DPAD_CENTER = 23
    static final int BACK_KEY = KeyEvent.KEYCODE_BACK;  // KeyEvent.KEYCODE_BUTTON_B = 97 KEYCODE_BACK = 04
    static final int SHARE_KEY = KeyEvent.KEYCODE_BUTTON_MODE;  // 110

    // Key codes for 8BitDo Micro Bluetooth Keyboard controller (Android mode)
    static final int SHUTTER_KB_KEY = KeyEvent.KEYCODE_M;
    static final int FOCUS_KB_KEY = KeyEvent.KEYCODE_R;
    static final int MODE_KB_KEY = KeyEvent.KEYCODE_L;
    static final int BURST_KB_KEY = KeyEvent.KEYCODE_K;
    static final int DISP_KB_KEY = KeyEvent.KEYCODE_C;
    static final int ISO_KB_KEY = KeyEvent.KEYCODE_D;
    static final int TIMER_KB_KEY = KeyEvent.KEYCODE_E;
    static final int SHUTTER_SPEED_KB_KEY = KeyEvent.KEYCODE_F;
    static final int BLANK_SCREEN_KB_KEY = KeyEvent.KEYCODE_N;
    static final int AEL_KB_KEY = KeyEvent.KEYCODE_O;
    static final int FN_KB_KEY = KeyEvent.KEYCODE_H;
    static final int MENU_KB_KEY = KeyEvent.KEYCODE_I;
    static final int REVIEW_KB_KEY = KeyEvent.KEYCODE_G;
    static final int OK_KB_KEY = KeyEvent.KEYCODE_G;
    static final int BACK_KB_KEY = KeyEvent.KEYCODE_J;
    static final int SHARE_KB_KEY = KeyEvent.KEYCODE_S;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String modelName = Build.MODEL;
        String manufacturer = Build.MANUFACTURER;
        String deviceName = manufacturer + " " + modelName;
        Log.d(TAG, "Device Manufacturer and Model: " + deviceName);
        if (modelName.equals("LPD-20W")) {
            stereoCameraId = "4";  // logical (left "0" and right "2") back cameras
            cameraWidth = 4656; // 16Mp Back camera width lens pixels
            cameraHeight = 3496;// 16MP Back camera height lens pixels
            //APP_REVIEW_PACKAGE = "com.leialoft.leiaplayer"; // Review with Leia Player app default
            BASE_FOLDER = Environment.DIRECTORY_DCIM;  // change base to DCIM folder for cameras
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

        mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        if (isAnaglyphDisplayMode) {
            setContentView(R.layout.layout_anaglyph);
            setupAnaglyphSurfaces();
        } else {
            setContentView(R.layout.layout);
            setupSurfaces();
        }

        checkPermissions();
        setupUdpServer();  // listens for broadcast messages to control camera remotely
        //aiVision = new AIvision(this);
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

    /**
     * UDP server setup
     * Create the UDP server to listen for incoming broadcast messages,
     * create a listener for the server.
     * A listener will receive NetMessages which contain camera commands.
     * NetListener is an interface and requires methods netEvent
     * and netStatus.
     */
    private void setupUdpServer() {
        if (isWiFiRemoteEnabled) {
            if (udpServer == null) {
                // create listener for UDP messages
                NetListener udpListener = new NetListener() {
                    public void netEvent(NetMessage m) {
                        if (MyDebug.LOG)
                            Log.d(TAG, "netEvent from ip address=" + m.getDatagramPacket().getAddress());
                        byte[] data = m.getData();
                        byte[] b = new byte[1];
                        b[0] = data[0];
                        String command = Bytes.getAsString(b);
                        if (MyDebug.LOG) Log.d(TAG, "netEvent (UDP Server) command=" + command);
                        if (command.startsWith("F")) {
                            if (!isVideo) {
                                if (!isFocusWaiting()) {
                                    if (MyDebug.LOG) Log.d(TAG, "remote request focus");
                                    MainActivity.this.runOnUiThread(new Runnable() {
                                        public void run() {
                                            requestAutoFocus();
                                        }
                                    });
                                }
                            } else {
                                showToast("Remote Not In Photo Mode");
                            }
                        } else if (command.startsWith("S") || command.startsWith("C")) {
                            sCount = getParam(data);
                            if (MyDebug.LOG) Log.d(TAG, "remote takePicture() " + sCount);
                            if (!isVideo) {
                                MainActivity.this.runOnUiThread(new Runnable() {
                                    public void run() {
                                        captureImages(); //takePicture(false);
                                    }
                                });
                            } else {
                                showToast("Remote Not In Photo Mode");
                            }
                        } else if (command.startsWith("V")) { // record/stop video
                            sCount = getParam(data);
                            if (MyDebug.LOG) Log.d(TAG, "remote record video " + sCount);
                            if (isVideo) {
                                MainActivity.this.runOnUiThread(new Runnable() {
                                    public void run() {
                                        if (isVideoRecording())
                                            stopVideo(true);
                                        else
                                            captureImages();//takePicture(false);
                                    }
                                });
                            } else {
                                showToast("Remote Not In Video Mode");
                            }
                        } else if (command.startsWith("P")) { // pause
                            if (MyDebug.LOG) Log.d(TAG, "remote pause Video ");
                            if (isVideo) {
                                MainActivity.this.runOnUiThread(new Runnable() {
                                    public void run() {
                                        if (isVideoRecording()) {
                                            pauseVideo();
                                        }
                                    }
                                });
                            } else {
                                showToast("Remote Not In Video Mode");
                            }
                        } else if (command.startsWith("R")) { // reset / information request
                            httpUrl = getHostnameUrl();
                            if (MyDebug.LOG) Log.d(TAG, "Reset information request " + httpUrl);
                            showToast(httpUrl);
                        }
                    }

                    public void netStatus(NetStatus s) {
                        if (MyDebug.LOG) Log.d(TAG, "netStatus (UDP Server) : " + s);
                    }
                };
                // UDP server for receiving Broadcast messages
                if (udpServer == null) {
                    udpServer = new UdpServer1(udpListener, port);
                    if (udpServer == null) {
                        if (MyDebug.LOG) Log.d(TAG, "UdpServer error");
                        showToast("Remote Message Server not running");
                    } else {
                        if (udpServer.socket() == null) {
                            if (MyDebug.LOG) Log.d(TAG, "UdpServer not connected retry");
                        }
                    }
                }
            }
        }
    }

    private void requestAutoFocus() {
    }

    private void stopVideo(boolean b) {
    }

    private boolean isVideoRecording() {
        return false;
    }

    private void pauseVideo() {
    }

    /**
     * Show half second Toast
     *
     * @param message Text message to display
     */
    private void showToast(String message) {
        //    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        ToastHelper.showToast(this, message);
    }

    private boolean isFocusWaiting() {
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");
        setVisibility();
    }

    private static final int MAXPARAM_SIZE = 16;

    private String getParam(byte[] data) {
        String param = "";
        byte[] s = new byte[MAXPARAM_SIZE];
        boolean done = false;
        for (int i = 0; i < MAXPARAM_SIZE; i++) {
            byte b = data[i + 1];
            if (done || b == 0 || b == '\r' || b == '\n') {
                s[i] = ' ';
                done = true;
            } else {
                s[i] = data[i + 1];
            }
        }
        param = Bytes.getAsString(s).trim();
        return param;
    }

    public String getHostnameUrl() {
        String hostname = getHostnameAddress();
        //httpUrl = "http://" + hostname + ":" + serverPort +"/";
        httpUrl = "http://" + hostname + "/";
        return httpUrl;
    }

//    public String getHostnameAddress() {
//        WifiManager wifiMan = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//        WifiInfo wifiInf = wifiMan.getConnectionInfo();
//        int ipAddress = wifiInf.getIpAddress();
//        String ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
//        return ip;
//    }

    public String getHostnameAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en
                    .hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces();
                String last = null;
                while (niEnum.hasMoreElements()) {
                    NetworkInterface ni = niEnum.nextElement();
                    if (!ni.isLoopback() && !ni.isPointToPoint()) {
                        for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
                            if (MyDebug.LOG)
                                Log.d(TAG, "network prefix length=" + interfaceAddress.getNetworkPrefixLength());
                            if (interfaceAddress.getAddress() != null) {
                                if (MyDebug.LOG)
                                    Log.d(TAG, interfaceAddress.getAddress().getHostAddress());
                                last = (interfaceAddress.getAddress().getHostAddress());
                                if (last.matches("\\d*\\.\\d*\\.\\d*\\.\\d*")) {
                                    return last;
                                }
                            }
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            if (MyDebug.LOG) {
                Log.d(TAG, "Socket Exception");
            }
        }
        return null;
    }

    public String getBroadcastAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en
                    .hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces();
                while (niEnum.hasMoreElements()) {
                    NetworkInterface ni = niEnum.nextElement();
                    if (!ni.isLoopback()) {
                        for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
                            if (interfaceAddress.getBroadcast() != null) {
                                //println(interfaceAddress.getBroadcast().toString());
                                return (interfaceAddress.getBroadcast().toString().substring(1));
                            }
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            if (MyDebug.LOG) {
                Log.d(TAG, "Socket Exception");
            }
        }
        return null;
    }

    @Override
    protected void onStop() {
        if (MyDebug.LOG)
            Log.d(TAG, "onStop");
        super.onStop();
        // we stop location listening in onPause, but done here again just to be certain!
        //applicationInterface.getLocationSupplier().freeLocationListeners();
    }

    // stop UDP server for broadcast message reception
    void destroyUDPServer() {
        if (MyDebug.LOG) Log.d(TAG, "Destroy UDP server");
        if (udpServer != null) {
            Vector list = udpServer.getListeners();
            if (MyDebug.LOG) Log.d(TAG, "NetListener size=" + list.size());
            for (int i = 0; i < list.size(); i++) {
                udpServer.removeListener((netP5.NetListener) list.get(i));
                if (MyDebug.LOG) Log.d(TAG, "remove NetListener " + list.get(i).toString());
            }
            udpServer.stop();
            udpServer.dispose();
            udpServer = null;
        }
    }

//    Extra events for controlling the app, mouse
//    @Override
//    public boolean onGenericMotionEvent(MotionEvent event) {
//        boolean consumed = false;
//        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//        mouseCapture = sharedPreferences.getBoolean(PreferenceKeys.MouseCapturePreferenceKey, false);
//
//        if (!mouseCapture) {
//            consumed = super.onGenericMotionEvent(event);
//            return consumed;
//        }
//        if (!consumed  && (0 != (event.getSource() & InputDevice.SOURCE_MOUSE))) {
//            //if (0 != (event.getSource() & InputDevice.SOURCE_CLASS_POINTER)) {
//            if (MyDebug.LOG) Log.d(TAG, "mouse button state="+event.getButtonState());
//            if (mainUI.popupIsOpen() || settingsIsOpen())
//                return false;
//            consumed = true;
//            int buttonState = event.getButtonState();
//            int actionmasked = event.getActionMasked();
//            if (MyDebug.LOG) Log.d(TAG, "action mask=" + actionmasked);
//            if (actionmasked == MotionEvent.ACTION_HOVER_ENTER) {
//                if (MyDebug.LOG) Log.d(TAG, "HOOVER ENTER");
//                consumed = true;
//            } else if (actionmasked == MotionEvent.ACTION_HOVER_EXIT) {
//                if (MyDebug.LOG) Log.d(TAG, "HOVER EXIT");
//                //
//                consumed = true;
//            } else if (buttonState == MotionEvent.BUTTON_PRIMARY) {
//                int action = event.getAction();
//                if (action == MotionEvent.ACTION_BUTTON_PRESS) {
//                    if (MyDebug.LOG) Log.d(TAG, "Primary Mouse button press");
//                    if (!preview.isVideo()) {
//                        sCount = "";
//                        MainActivity.this.runOnUiThread(new Runnable() {
//                            public void run() {
//                                takePicture(false);
//                            }
//                        });
//                    }
//                    else {
//                        sCount = "";
//                        MainActivity.this.runOnUiThread(new Runnable() {
//                            public void run() {
//                                if (preview.isVideoRecording())
//                                    preview.stopVideo(true);
//                                else
//                                    takePicture(false);
//                            }
//                        });
//                    }
//                }
//                consumed = true;
//            } else if (buttonState == MotionEvent.BUTTON_SECONDARY) {
//                if (MyDebug.LOG) Log.d(TAG, "Secondary Mouse button press");
//                sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//                String volume_keys = sharedPreferences.getString(PreferenceKeys.VolumeKeysPreferenceKey, "volume_take_photo");
//                if (volume_keys.equals("volume_focus")) {
//                    getPreview().requestAutoFocus();
//                    consumed = true;
//                }
//                else{
//                    openGallery();
//                    consumed = true;
//                }
//            } else if (buttonState == MotionEvent.BUTTON_TERTIARY) {
//                if (MyDebug.LOG) Log.d(TAG, "Tertiary Mouse button press");
//                if (!preview.isVideo()) {
//                    if (!preview.isFocusWaiting()) {
//                        if( MyDebug.LOG ) Log.d(TAG, "remote request focus");
//                        MainActivity.this.runOnUiThread(new Runnable() {
//                            public void run() {
//                                preview.requestAutoFocus();
//                            }
//                        });
//                    }
//                }
//                consumed = true;
//            } else switch (event.getAction()) {
//                case MotionEvent.ACTION_SCROLL:
//                    float speed = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
//                    if (MyDebug.LOG) Log.d(TAG, "scroll="+speed);
//                    // zoom camera  here (ZOOM_STEP * speed);
//                    if (speed < 0)
//                        zoomOut();
//                    else
//                        zoomIn();
//                    consumed = true;
//                    break;
//                case MotionEvent.ACTION_BUTTON_PRESS:
//                    if (MyDebug.LOG) Log.d(TAG, "ACTION_BUTTON_PRESS");
//                    consumed = true;
//                    break;
//                case MotionEvent.ACTION_BUTTON_RELEASE:
//                    if (MyDebug.LOG) Log.d(TAG, "ACTION_BUTTON_RELEASE");
//                    consumed = true;
//                    break;
//            }

    /// /            consumed = true;
//            return consumed;
//        }
//        return consumed;
//    }
    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();

        // for debugging and test
        if (allPermissionsGranted) {
            String[] list = getCameraIdList();  // debug what cameras are available
            for (String id : list) {
                Log.d(TAG, "Available CameraId: |" + id + "|");
            }

            CameraInfoUtil.checkCameraSyncType(this, list);
            CameraInfoUtil.logFocusDistanceCalibration(this);  // for debug

            initCamera();
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
        stopCamera();
    }


    private void setupSurfaces() {
        Log.d(TAG, "setupSurfaces()");
        mSurfaceView0 = findViewById(R.id.surfaceView);
        mSurfaceView2 = findViewById(R.id.surfaceView2);

        mSurfaceHolder0 = mSurfaceView0.getHolder();
        mSurfaceHolder2 = mSurfaceView2.getHolder();

        // Setup anaglyph surface view TODO
        //mAnaglyphSurfaceView = findViewById(R.id.surfaceView3);  // anaglyph surface
        //mAnaglyphSurfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);
        //mAnaglyphSurfaceView.setVisibility(SurfaceView.GONE);  // initially hide
        // ... later, when you want to show it:
        // mAnaglyphSurfaceView.setVisibility(View.VISIBLE);

        SurfaceHolder.Callback shCallback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
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

        mSurfaceHolder0.addCallback(shCallback);
        mSurfaceHolder2.addCallback(shCallback);
    }

    private void setupAnaglyphSurfaces() {
        Log.d(TAG, "setupAnaglyphSurfaces()");
        // Setup GLSurfaceView
        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2);
        anaglyphRenderer = new AnaglyphRenderer();
        glSurfaceView.setRenderer(anaglyphRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        anaglyphRenderer.setupSurfaces();
//        mSurfaceView0 = findViewById(R.id.glSurfaceView);
//        mSurfaceView2 = mSurfaceView0; //findViewById(R.id.surfaceView2);
//
//        mSurfaceHolder0 = mSurfaceView0.getHolder();
//        mSurfaceHolder2 = mSurfaceHolder0; // mSurfaceView2.getHolder();

        // Setup anaglyph surface view TODO
        //mAnaglyphSurfaceView = findViewById(R.id.surfaceView3);  // anaglyph surface
        //mAnaglyphSurfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);
        //mAnaglyphSurfaceView.setVisibility(SurfaceView.GONE);  // initially hide
        // ... later, when you want to show it:
        // mAnaglyphSurfaceView.setVisibility(View.VISIBLE);

//        SurfaceHolder.Callback shCallback = new SurfaceHolder.Callback() {
//            @Override
//            public void surfaceCreated(@NonNull SurfaceHolder holder) {
//            }
//
//            @Override
//            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
//            }
//
//            @Override
//            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
//                if (null != mCameraDevice) {
//                    mCameraDevice.close();
//                    mCameraDevice = null;
//                }
//            }
//        };

        //mSurfaceHolder0.addCallback(shCallback);
        //mSurfaceHolder2.addCallback(shCallback);
    }

    private void checkPermissions() {
        Log.d(TAG, "checkPermissions");
        String[] permissions = {Manifest.permission.CAMERA}; // Manifest.permission.WRITE_EXTERNAL_STORAGE};
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

    private void initCamera() {
        Log.d(TAG, "initCamera()");
        if (shutterSound) {
            CameraInfo.mustPlayShutterSound();
        }
        Log.d(TAG, "shutter sound " + ((shutterSound) ? "on" : "off"));
        mainHandler = new Handler(getMainLooper());

        // Setup ImageReaders for capture
        mImageReader0 = ImageReader.newInstance(cameraWidth, cameraHeight, ImageFormat.JPEG, 2);  // 2 maxImages
        mImageReader2 = ImageReader.newInstance(cameraWidth, cameraHeight, ImageFormat.JPEG, 2);  // 2 maxImages

        if (ActivityCompat.checkSelfPermission(this, CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try {
                mCameraManager.openCamera(stereoCameraId, stateCallback, mainHandler); // logical camera 3 combines 1 and 2
                Log.d(TAG, "openCamera(3)");
            } catch (CameraAccessException e) {
                Log.e(TAG, "Camera access exception", e);
            }
        }
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) { // Open camera
            mCameraDevice = camera;
            if (mSurfaceView0.isAttachedToWindow() && mSurfaceView2.isAttachedToWindow()) {
                createCameraViewSession();
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
            Log.e(TAG, "Camera " + camera.getId() + " hardware failure");
        }
    };

    private void stopMainHandlerThread() {
        if (mainHandler != null) {
            Looper looper = mainHandler.getLooper();
            looper.quitSafely();
//            try {
//                Thread lThread = looper.getThread();
//                lThread.join();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
            mainHandler = null;
        }
    }

    private void stopCamera() {
        Log.d(TAG, "stopCamera()");
        if (mCameraCaptureSession != null) {
            try {
                if (mCameraCaptureSession.isReprocessable()) {
                    mCameraCaptureSession.stopRepeating();
                    mCameraCaptureSession.abortCaptures();
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
            //stopMainHandlerThread(); // problem causes main thread to quit - not allowed
        }
    }

    private void createCameraViewSession() {
        Log.d(TAG, "createCameraViewSession()");
        try {
            //mSurfaceHolder0.getSurface().
            OutputConfiguration opc0 = new OutputConfiguration(mSurfaceHolder0.getSurface());
            opc0.setPhysicalCameraId(leftCameraId);
            OutputConfiguration opc1 = new OutputConfiguration(mSurfaceHolder2.getSurface());
            opc1.setPhysicalCameraId(rightCameraId);

            OutputConfiguration opcCapture0 = new OutputConfiguration(mImageReader0.getSurface());
            opcCapture0.setPhysicalCameraId(leftCameraId);
            OutputConfiguration opcCapture1 = new OutputConfiguration(mImageReader2.getSurface());
            opcCapture1.setPhysicalCameraId(rightCameraId);

            List<OutputConfiguration> outputConfigsAll = Arrays.asList(opc0, opc1, opcCapture0, opcCapture1);

            SessionConfiguration sessionConfiguration = new SessionConfiguration(
                    SessionConfiguration.SESSION_REGULAR, outputConfigsAll, AsyncTask.SERIAL_EXECUTOR,
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
                                //mCameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                            } catch (CameraAccessException e) {
                                Log.e(TAG, "Camera access exception in session config", e);
                            }
                            // Create your CaptureCallback instance.
                            CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
                                @Override
                                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                                    super.onCaptureCompleted(session, request, result);
                                    // This is where you get the "capture completed" notification.
                                    // The TotalCaptureResult contains the final metadata.
                                    // Your logic for handling the completed capture goes here.
                                    //Log.d(TAG, "Capture completed!");
                                    //aiVision.getInformationFromSurfaceView(mSurfaceView0, prompt);
                                }
                            };
                            try {
                                mCameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), captureCallback, mainHandler);
                                //mCameraCaptureSession.capture(captureRequestBuilder.build(), captureCallback, mainHandler);
                            } catch (CameraAccessException e) {
                                Log.e(TAG, "createCameraViewSession"+e.toString());
                            }

                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.e(TAG, "onConfigureFailed");
                        }
                    });

            mCameraDevice.createCaptureSession(sessionConfiguration);

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

            for (int cameraNum = 0; cameraNum < 6; cameraNum++) {
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
        //Log.d(TAG, "onKeyDown "+keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_ENTER:
            case SHUTTER_KEY:
                return true;
            case KeyEvent.KEYCODE_3D_MODE: // ignore so that this key does not launch XReal camera app
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyUp " + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                //case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_3D_MODE: // camera key - first turn off auto launch of native camera app
            case SHUTTER_KEY:
            case SHUTTER_KB_KEY:
                if (isPhotobooth && (countdownDigit < 0)) {
                    startCountdownSequence(countdownStart);
                } else {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            captureImages();
                        }
                    });
                }
                return true;
            case KeyEvent.KEYCODE_BACK:
            case BACK_KB_KEY:
                Toast.makeText(this, "Back", Toast.LENGTH_SHORT).show();
                return true;
            case KeyEvent.KEYCODE_ESCAPE:
                return true;
            case SHARE_KEY:
            case SHARE_KB_KEY:
                if (reviewSBS != null) {
                    shareImage2(reviewSBS, null);
                } else {
                    Toast.makeText(this, "Nothing to Share", Toast.LENGTH_SHORT).show();
                }
                return true;
            case FN_KEY:
            case FN_KB_KEY:
                stopCamera();
                meteringIndex++;
                if (meteringIndex >= METERING.length) meteringIndex = 0;
                Toast.makeText(this, METERING_NAMES[meteringIndex], Toast.LENGTH_SHORT).show();
                initCamera();
                return true;
            case FOCUS_KEY: // change focus distance, should be sub menu
            case FOCUS_KB_KEY: // change focus distance, should be sub menu
                stopCamera();
                int i = focusDistanceIndex + 1;
                if (i >= FOCUS_DISTANCE.length) i = 0;
                focusDistanceIndex = i;
                initCamera();
                Toast.makeText(this, FOCUS_DISTANCE_NAMES[focusDistanceIndex], Toast.LENGTH_SHORT).show();
                return true;
//            case KeyEvent.KEYCODE_ENTER:
//            case OK_KEY:
            //           case OK_KB_KEY:
//                Toast.makeText(this, " OK/Review - not implemented", Toast.LENGTH_SHORT).show();
//                stopCamera();
//                initCamera();
//                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE: // 85 not used with 8BitDo
                Toast.makeText(this, "Not implemented", Toast.LENGTH_SHORT).show();
//                stopCamera();
//                initCamera();
                return true;
            case BURST_KEY:
            case BURST_KB_KEY: // start and cancel Burst capture mode
                if (burstModeFeature && burstMode) {
                    //Toast.makeText(this, "Burst Mode ", Toast.LENGTH_SHORT).show();
                    if (burstCounter > 0) {
                        burstCounter = 0;
                        //Toast.makeText(this, "Burst Mode Canceled ", Toast.LENGTH_SHORT).show();
                    } else {
                        burstCounter = BURST_COUNT;
                        captureImages();
                    }
                } else {
                    Toast.makeText(this, "Burst Mode Not Enabled", Toast.LENGTH_SHORT).show();
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case REVIEW_KEY:
            case REVIEW_KB_KEY:
                if (reviewSBS != null) {
                    shareImage2(reviewSBS, APP_REVIEW_PACKAGE);
                } else {
                    Toast.makeText(this, "Nothing to Review", Toast.LENGTH_SHORT).show();
                }
                return true;
            case AEL_KEY:
            case AEL_KB_KEY:
                Toast.makeText(this, "Not implemented", Toast.LENGTH_SHORT).show();
//                stopCamera();
//                initCamera();
                return true;
            case MODE_KEY:
            case MODE_KB_KEY:
                Toast.makeText(this, "Auto Exposure - Manual, Shutter Priority", Toast.LENGTH_SHORT).show();
//                stopCamera();
//                initCamera();
                return true;
            case SHUTTER_SPEED_KEY:
            case SHUTTER_SPEED_KB_KEY:
                Toast.makeText(this, "Shutter Speed - not implemented", Toast.LENGTH_SHORT).show();
//                stopCamera();
//                initCamera();
                return true;
            case TIMER_KEY:
            case TIMER_KB_KEY:
                Toast.makeText(this, "Timer - not implemented", Toast.LENGTH_SHORT).show();
//                stopCamera();
//                initCamera();
                return true;
            case ISO_KEY:
            case ISO_KB_KEY:
                Toast.makeText(this, "ISO - not implemented", Toast.LENGTH_SHORT).show();
//                stopCamera();
//                initCamera();
                return true;
            case DISP_KEY:
            case DISP_KB_KEY:
                Toast.makeText(this, "DISP - not implemented", Toast.LENGTH_SHORT).show();
//                stopCamera();
//                initCamera();
                return true;
            case MENU_KEY:
            case MENU_KB_KEY:
                Toast.makeText(this, "MENU - not implemented", Toast.LENGTH_SHORT).show();
//                stopCamera();
//                initCamera();
                return true;
//            case VIDEO_RECORD_KEY:
//            case VIDEO_RECORD_KB_KEY:
//                Toast.makeText(this, "Video Record - not implemented", Toast.LENGTH_SHORT).show();
//                stopCamera();
//                initCamera();
//                return true;
            case BLANK_SCREEN_KEY:
            case BLANK_SCREEN_KB_KEY:
                blankScreen = !blankScreen;

                String id = String.valueOf(mCameraCaptureSession.getDevice().getId());
                Toast.makeText(this, (blankScreen ? "Id: " + id + " Blank Screen" : "UnBlank Screen"), Toast.LENGTH_SHORT).show();
                if (blankScreen) {
                    //mSurfaceView0.setVisibility(View.GONE);
                    //mSurfaceView2.setVisibility(View.GONE);

                } else {
                    //mSurfaceView0.setVisibility(View.VISIBLE);
                    //mSurfaceView2.setVisibility(View.VISIBLE);
                    stopCamera();
                    initCamera();

                }

                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    private void reviewImages() {
        Log.d(TAG, "reviewImages()");
        if (reviewSBS != null) {
            shareImage(reviewSBS);
        } else {
            Toast.makeText(this, "Photo Albums Available", Toast.LENGTH_SHORT).show();
            shareImage(reviewSBS);
        }
    }

    private void reviewImage() {

        //
        // if in live view state
        //   turn off and shutdown camera
        //   set review state
        //   switch layouts and show first saved image
        // else in review mode
        //   get next image to show in review and show it
        //    if no more images to show
        //    turn camera back on and switch layouts
        //    set live view state
        //

    }

    public void captureImages() {
        Log.d(TAG, "captureImages()");
        if (mCameraDevice == null || mCameraCaptureSession == null) {
            Toast.makeText(this, "Camera not ready", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            // Create capture request for both cameras
            CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader0.getSurface());
            captureBuilder.addTarget(mImageReader2.getSurface());
            // default TONEMAP_MODE_CONTRAST_CURVE assumed for best contrast, color and detail capture
            captureBuilder.set(CaptureRequest.TONEMAP_CURVE, new TonemapCurve(curve_srgb, curve_srgb, curve_srgb));
            //
            if (FOCUS_DISTANCE[focusDistanceIndex] == AUTO_FOCUS_DISTANCE) {
                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            } else {
                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
            }

            captureBuilder.set(EXPOSURE_METERING, METERING[meteringIndex]);
            captureBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, FOCUS_DISTANCE[focusDistanceIndex]);

            captureBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, 1); // NOISE_REDUCTION_MODE
            captureBuilder.set(CaptureRequest.EDGE_MODE, 1); // EDGE_MODE

            timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            imageL = null;
            imageR = null;
            leftBytes = null;
            rightBytes = null;
            if (leftBitmap != null) {
                leftBitmap.recycle();
                leftBitmap = null;
            }
            if (rightBitmap != null) {
                rightBitmap.recycle();
                rightBitmap = null;
            }
            if (sbsBitmap != null) {
                sbsBitmap.recycle();
                sbsBitmap = null;
            }
            if (anaglyphBitmap != null) {
                anaglyphBitmap.recycle();
                anaglyphBitmap = null;
            }

            // Setup image capture listeners
            mImageReader0.setOnImageAvailableListener(new OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    imageL = reader.acquireLatestImage();
                    if (imageL != null) {
                        leftBytes = convertToBytes(imageL);
                        imageL.close();
                        // Save frames
                        saveImageFiles();
                    }
                }
            }, mainHandler);

            mImageReader2.setOnImageAvailableListener(new OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    imageR = reader.acquireLatestImage();
                    if (imageR != null) {
                        rightBytes = convertToBytes(imageR);
                        imageR.close();
                        // Save frames
                        saveImageFiles();
                    }
                }
            }, mainHandler);
            if (shutterSound) {
                playShutterSound();
            }
            mCameraCaptureSession.capture(captureBuilder.build(), null, mainHandler);

        } catch (CameraAccessException e) {
            Log.e(TAG, "Error capturing images", e);
            Toast.makeText(this, "Error capturing images", Toast.LENGTH_SHORT).show();
        }
    }

    public void playShutterSound() {
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
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                toneGen.release();
            }
        }, 100); // 100 milliseconds is a good duration for a short tone.
    }

    private void saveImageFiles() {
        if (leftBytes != null && rightBytes != null) {
            showToast("Saved IMG" + timestamp);
            leftBitmap = saveImageFile(leftBytes, PHOTO_PREFIX + timestamp, true); // left
            rightBitmap = saveImageFile(rightBytes, PHOTO_PREFIX + timestamp, false); // right
            //String response = aiVision.getInformationFromImage(leftBitmap, prompt);
            //Log.d(TAG, "AI Vision response: " + response);
            //Toast.makeText(this, "AI Vision response: " + response, Toast.LENGTH_SHORT).show();
            if (saveAnaglyph) {
                createAndSaveAnaglyph(PHOTO_PREFIX + timestamp, leftBitmap, rightBitmap);
            }
            if (saveSBS) {
                if (burstCounter > 0)
                    timestamp += "_" + String.valueOf(BURST_COUNT - burstCounter + 1);
                if (crossEye) {
                    reviewSBS = createAndSaveSBS(PHOTO_PREFIX + timestamp, rightBitmap, leftBitmap);
                } else {
                    reviewSBS = createAndSaveSBS(PHOTO_PREFIX + timestamp, leftBitmap, rightBitmap);
                }
                if (burstCounter > 0) {
                    burstCounter = burstCounter - 1;
                    if (burstCounter > 0) {
                        captureImages();
                    }
                }
            }
            //Toast.makeText(this, "Saved IMG" + timestamp, Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap saveImageFile(byte[] bytes, String filename, boolean left) {
        Bitmap bitmap = null;

        if (left) {
            filename += "_l.jpg";
        } else {
            filename += "_r.jpg";
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        // options.inSampleSize = 2; // Try reducing image size
        // options.inPreferredConfig = Bitmap.Config.ARGB_8888; // Try specifying a config

        Log.d(TAG, "SaveImageFile " + filename);
        bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        if (bitmap == null) {
            Log.e(TAG, "Image decoding failed! " + (left ? "left" : "right"));
            return null;
        } else {
            if (!saveLR) {
                return bitmap;
            }
        }

        File mediaStorageDir = new File(
                Environment.getExternalStoragePublicDirectory(BASE_FOLDER), SAVE_FOLDER);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.e(TAG, "failed to create directory to save photo: " + mediaStorageDir.getAbsolutePath());
                Toast.makeText(this, "Error creating folder " + SAVE_FOLDER, Toast.LENGTH_SHORT).show();
                return null;
            }
        }

        File file = new File(Environment.getExternalStoragePublicDirectory(BASE_FOLDER + File.separator + SAVE_FOLDER), filename);

        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(bytes);
            //xxx.compress(Bitmap.CompressFormat.JPEG, 100, output);

            // Trigger media scanner to make image visible in gallery
            MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()},
                    new String[]{"image/jpeg"}, null);

            Log.d(TAG, "Image saved: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error saving image", e);
            return null;
        }
        return bitmap;
    }

    private byte[] convertToBytes(Image image) {
        // Get the JPEG image data, add other formats if needed
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        Log.d(TAG, "convertToBytes Image Format: " + image.getFormat() + " planes " + planes.length);
        return bytes;
    }

    private void createAndSaveAnaglyph(String timestamp, Bitmap leftBitmap, Bitmap rightBitmap) {
        Log.d(TAG, "createAndSaveAnaglyph");
        if (leftBitmap == null || rightBitmap == null) {
            Log.d(TAG, "createAndSaveAnaglyph failed Bitmaps null " + timestamp);
            return;
        }

        int width = Math.min(leftBitmap.getWidth(), rightBitmap.getWidth());
        int height = Math.min(leftBitmap.getHeight(), rightBitmap.getHeight());

        // Create anaglyph bitmap
        anaglyphBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        int[] leftPixels = new int[width * height];
        int[] rightPixels = new int[width * height];

        leftBitmap.getPixels(leftPixels, 0, width, 0, 0, width, height);
        rightBitmap.getPixels(rightPixels, 0, width, 0, 0, width, height);

        int[] anaglyphPixels = new int[width * height];

        for (int i = 0; i < leftPixels.length; i++) {
            int leftPixel = leftPixels[i];
            int rightPixel = rightPixels[i];

            // Extract RGB components
            int leftRed = (leftPixel >> 16) & 0xFF;
            int rightGreen = (rightPixel >> 8) & 0xFF;
            int rightBlue = rightPixel & 0xFF;

            // Create anaglyph pixel: left red + right green/blue
            anaglyphPixels[i] = (0xFF << 24) | (leftRed << 16) | (rightGreen << 8) | rightBlue;
        }

        anaglyphBitmap.setPixels(anaglyphPixels, 0, width, 0, 0, width, height);

        // Save anaglyph image
        File mediaStorageDir = new File(
                Environment.getExternalStoragePublicDirectory(BASE_FOLDER), SAVE_FOLDER);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.e(TAG, "failed to create directory to save photo: " + mediaStorageDir.getAbsolutePath());
                Toast.makeText(this, "Error creating folder " + SAVE_FOLDER, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String filename = timestamp + "_ana.jpg";
        File file = new File(Environment.getExternalStoragePublicDirectory(BASE_FOLDER + File.separator + SAVE_FOLDER), filename);

        try (FileOutputStream out = new FileOutputStream(file)) {
            anaglyphBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);

            MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()},
                    new String[]{"image/jpeg"}, null);

            Log.d(TAG, "Anaglyph image saved: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error saving anaglyph image", e);
        }

    }

    private File createAndSaveSBS(String timestamp, Bitmap leftBitmap, Bitmap rightBitmap) {
        Log.d(TAG, "createAndSaveSBS");
        if (leftBitmap == null || rightBitmap == null) {
            Log.d(TAG, "createAndSaveSBS failed Bitmaps null " + timestamp);
            return null;
        }

        // Calculate the dimensions for the combined bitmap.
        int width = leftBitmap.getWidth() + rightBitmap.getWidth();
        int height = Math.max(leftBitmap.getHeight(), rightBitmap.getHeight());

        // Create a new bitmap with the combined dimensions.
        sbsBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // Create a canvas to draw on the new bitmap.
        Canvas canvas = new Canvas(sbsBitmap);

        // Draw the left bitmap at position (0, 0).
        canvas.drawBitmap(leftBitmap, 0f, 0f, null);

        // Draw the right bitmap immediately to the right of the left one.
        canvas.drawBitmap(rightBitmap, leftBitmap.getWidth(), 0f, null);

        // Save SBS image
        File mediaStorageDir = new File(
                Environment.getExternalStoragePublicDirectory(BASE_FOLDER), SAVE_FOLDER);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.e(TAG, "failed to create directory to save photo: " + mediaStorageDir.getAbsolutePath());
                Toast.makeText(this, "Error creating folder " + SAVE_FOLDER, Toast.LENGTH_SHORT).show();
                return null;
            }
        }

        String filename = timestamp + "_2x1.jpg";
        File file = new File(Environment.getExternalStoragePublicDirectory(BASE_FOLDER + File.separator + SAVE_FOLDER), filename);

        try (FileOutputStream out = new FileOutputStream(file)) {
            sbsBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            //sbsBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()},
                    new String[]{"image/*"}, null);

            Log.d(TAG, "SBS image saved: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error saving SBS image", e);
            return null;
        }
        return file;
    }

    public void shareImage(File imageFile) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        photoPickerLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> photoPickerLauncher =
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
                                startActivity(Intent.createChooser(shareIntent, "Share image..."));
                            }
                        }
                    });

    private Uri getContentUriForFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        }

        ContentResolver resolver = getContentResolver();

        // First try to find existing MediaStore entry
        String[] projection = {MediaStore.Images.Media._ID};
        String selection = MediaStore.Images.Media.DATA + "=?";
        String[] selectionArgs = {file.getAbsolutePath()};

        Cursor cursor = resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            long id = cursor.getLong(idColumn);
            cursor.close();
            return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
        }

        if (cursor != null) {
            cursor.close();
        }

        // If not found, add to MediaStore
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, file.getName());
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, getMimeType(file.getName()));
        contentValues.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
        contentValues.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);

        return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
    }

    private String getMimeType(String fileName) {
        if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.toLowerCase().endsWith(".png")) {
            return "image/png";
        }
        return "image/*";
    }

    public void shareImage2(File imageFile, String appPackage) {
        Uri contentUri = getContentUriForFile(imageFile.getPath());

        if (contentUri != null) {
            try {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_STREAM, contentUri);
                intent.setType("image/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if (appPackage != null) {
                    intent.setPackage(appPackage); //  actual package name
                }
                this.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                // Handle the case where the target app is not installed
                Log.d(TAG, "Failed to launch 3DSteroid Pro.");
                Toast.makeText(this, "3DSteroidPro not installed", Toast.LENGTH_SHORT).show();
                // Toast message or direct the user to the Play Store
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_STREAM, contentUri);
                intent.setType("image/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                this.startActivity(Intent.createChooser(intent, "Share image with:"));
            }
        } else {
            Log.e(TAG, "Failed to create MediaStore entry.");
        }
    }

    public void findIntent() {
        // First, find what activities can handle image sharing
        PackageManager pm = getPackageManager();
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");

        List<ResolveInfo> activities = pm.queryIntentActivities(shareIntent, 0);
        for (ResolveInfo activity : activities) {
            String appName = activity.loadLabel(pm).toString();
            String packageName = activity.activityInfo.packageName;
            Log.d(TAG, "App: " + appName + ", Package: " + packageName);

            if (appName.toLowerCase().contains("stereo") ||
                    packageName.toLowerCase().contains("stereo")) {
                Log.d(TAG, "StereoRoidPro: " + packageName + " / " + activity.activityInfo.name);
            }
        }
    }

    public void shareImage1(File imageFile) {
        findIntent();
        Uri contentUri = FileProvider.getUriForFile(this, this.getPackageName() + ".fileprovider", imageFile);
        Log.d(TAG, "shareImage1 " + imageFile.getAbsolutePath());
        if (contentUri != null) {

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Required for receiving app
            shareIntent.setPackage(APP_REVIEW_PACKAGE); // Replace with the actual package name

            try {
                this.startActivity(shareIntent);
            } catch (ActivityNotFoundException e) {
                // Handle the case where the target app is not installed
                Log.d(TAG, "Failed to launch stereoroidpro.");
                Toast.makeText(this, "StereoRoidPro not installed", Toast.LENGTH_SHORT).show();
                // Toast message or direct the user to the Play Store
            }
        }
    }

    public void shareImages(File imageFile) {
        //Uri contentUri = FileProvider.getUriForFile(this, this.getPackageName() + ".fileprovider", imageFile);
        ContentResolver resolver = this.getContentResolver();
        ContentValues contentValues = new ContentValues();
        try {
            contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, imageFile.getCanonicalPath());
        } catch (IOException e) {
            contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, imageFile.getPath());
        }
        String fileName = imageFile.getName();
        String path = imageFile.getPath();
        String mimeType = "image/*";

        contentValues.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
        Log.d(TAG, "Filename: " + fileName + " Path=" + path);
        Uri contentUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        if (contentUri != null) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
            intent.setType(mimeType); // Or the correct MIME type
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            this.startActivity(Intent.createChooser(intent, "Share using:")); // Allows the user to select the print service
            //this.startActivity(intent); // SEND only
        } else {
            // Handle the case where the insertion failed
            Log.d(TAG, "Failed to share image");
        }
    }

    // TODO anaglyph live view display
    private void toggleDisplayMode() {
        isAnaglyphDisplayMode = !isAnaglyphDisplayMode;

        if (isAnaglyphDisplayMode) {
            // Switch to anaglyph mode - create anaglyph view programmatically

            //createAnaglyphView();
            Toast.makeText(this, "Anaglyph mode enabled", Toast.LENGTH_SHORT).show();
        } else {
            // Switch to side-by-side mode
            setContentView(R.layout.layout);
            setupSurfaces();
            Toast.makeText(this, "Side-by-side mode enabled", Toast.LENGTH_SHORT).show();
        }

        // Recreate camera session
        if (mCameraDevice != null) {
            try {
                if (mCameraCaptureSession != null) {
                    mCameraCaptureSession.close();
                }
                //createCameraCaptureSession();
            } catch (Exception e) {
                Log.e(TAG, "Error recreating camera session", e);
            }
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

        //destroyHTTPServer();
        destroyUDPServer();

    }

    void startCountdownSequence(int startCount) {
        if (countdownTimer == null) {
            countdownTimer = new Timer();
            countdownDigit = startCount;
            // define a task to decrement the countdown digit every second
            TimerTask task = new TimerTask() {
                public void run() {
                    countdownDigit--;
                    if (countdownDigit < 0) {
                        // stop the timer when the countdown reaches 0
                        countdownTimer.cancel();
                        countdownTimer = null;
                        // hide digit display
                        captureImages(); // take a picture
                    } else {
                        Log.d(TAG, "countdown=" + countdownDigit); // show digit display
                    }
                }
            };

            countdownTimer.schedule(task, 0, 1000);
        }
    }

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

