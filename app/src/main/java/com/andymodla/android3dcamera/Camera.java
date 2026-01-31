package com.andymodla.android3dcamera;

import static android.Manifest.permission.CAMERA;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraInfo;
import androidx.core.app.ActivityCompat;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import processing.core.PApplet;
import processing.core.PImage;

public class Camera {
    public static final String TAG = "A3DCamera";
    Context context;
    Media media;

    PApplet pApplet; // Processing sketch base class
    private boolean useProcessing = false;
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCameraCaptureSession;
    private CaptureRequest.Builder previewRequestBuilder;

    private HandlerThread mCameraThread;
    private Handler mCameraHandler;
    private Executor cameraExecutor;

    // Image Reader threads
    private HandlerThread mImageReaderThread0;
    private volatile Handler mImageReaderHandler0;

    private static final int MAX_NUM_CAMERAS = 6;
    // Camera Ids for Xreal Beam Pro
    private String leftCameraId = "0";
    private String frontCameraId = "1";
    private String rightCameraId = "2";
    private String stereoCameraId = "3";

    int CAMERA_WIDTH_AR_DEFAULT = 4080;
    int CAMERA_HEIGHT_AR_DEFAULT = 3072;
    // todo
    int CAMERA_WIDTH_AR_4_3 = 4000;
    int CAMERA_HEIGHT_AR_4_3 = 3000;
    int CAMERA_WIDTH_AR_16_9 = 3840;
    int CAMERA_HEIGHT_AR_16_9 = 2160;
    int CAMERA_WIDTH_AR_1_1 = 3072;
    int CAMERA_HEIGHT_AR_1_1 = 3072;
    int CAMERA_WIDTH_AR_SMALL = 1080;
    int CAMERA_HEIGHT_AR_SMALL = 1080;
    int XBP_CAMERA_WIDTH = 1280;//4080;
    int XBP_CAMERA_HEIGHT = 960;//3072

    volatile int focusDistanceIndex = 0;  // default HYPERFOCAL
    //
    static final float MACRO_FOCUS_DISTANCE = 10.0f;  // 100mm
    static final float HYPERFOCAL_FOCUS_DISTANCE = 0.60356647f;  // 1.66 meters
    //static final float PHOTO_BOOTH_FOCUS_DISTANCE = 1.43f;  // 700mm  1 meter
    static final float PHOTO_BOOTH_FOCUS_DISTANCE = 2.0f;  // 500mm
    static final float AUTO_FOCUS_DISTANCE = 0.0f;
    static final float[] FOCUS_DISTANCE = {HYPERFOCAL_FOCUS_DISTANCE, PHOTO_BOOTH_FOCUS_DISTANCE, MACRO_FOCUS_DISTANCE, AUTO_FOCUS_DISTANCE};
    static final String[] FOCUS_DISTANCE_NAMES = {"HYPERFOCAL FOCUS DISTANCE", "PHOTO BOOTH FOCUS DISTANCE", "MACRO FOCUS DISTANCE", "AUTO FOCUS"};


    private int cameraWidth = CAMERA_WIDTH_AR_DEFAULT; // camera width lens pixels
    private int cameraHeight = CAMERA_HEIGHT_AR_DEFAULT;// camera height lens pixels

    // Display surfaces for preview
    private volatile SurfaceView mSurfaceView0;
    private volatile SurfaceView mSurfaceView2;
    private volatile SurfaceHolder mSurfaceHolder0;
    private volatile SurfaceHolder mSurfaceHolder2;

    // Image capture
    private volatile ImageReader mImageReader0;
    private volatile ImageReader mImageReader2;  // for SBS display

    // Processing preview ImageReader
    private volatile ImageReader imageReader0;
    private volatile ImageReader imageReader2;

    volatile Image imageL;
    volatile Image imageR;
    volatile byte[] leftBytes;
    volatile byte[] rightBytes;

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
    //    previewRequestBuilder.set(SATURATION, 5);
    //    captureBuilder.set(SATURATION, 5);

    // Sharpness 0 - 6, default 2
    private static final CaptureRequest.Key<Integer> SHARPNESS = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sharpness.strength", Integer.class);

    volatile boolean shutterSound = true;
    public volatile boolean available = false; // PImage available to access

    // Image processing got preview
    private final AtomicBoolean isProcessingLeft = new AtomicBoolean(false);
    private final AtomicBoolean isProcessingRight = new AtomicBoolean(false);
    private volatile Image imageLeft;
    private volatile Image imageRight;
    volatile public PImage self;
    volatile public PImage self2;

    // Constructor
    public Camera(Context context, Media media, PApplet pApplet) {
        this.context = context;
        this.media = media;
        this.pApplet = pApplet;
        // Create camera manager
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

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
// for DEBUG reference front cameras
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
        // check system property for usb uvc webcam for discovery
        Log.d(TAG, "ro.usb.uvc.enabled=" + System.getProperty("ro.usb.uvc.enabled"));
        //System.setProperty("ro.usb.uvc.enabled", String.valueOf(true));
        //Log.d(TAG, "ro.usb.uvc.enabled="+System.getProperty("ro.usb.uvc.enabled"));
    }

    public void setpApplet(PApplet pApplet) {
        this.pApplet = pApplet;
    }

    public void init(boolean isPhotobooth) {
        if (isPhotobooth) {
            useProcessing = isPhotobooth;
        } else {
            Log.d(TAG, "setupSurfaces()");
            // set up display surfaces
            mSurfaceView0 = ((MainActivity) context).findViewById(R.id.surfaceView);
            mSurfaceView2 = ((MainActivity) context).findViewById(R.id.surfaceView2);

            mSurfaceHolder0 = mSurfaceView0.getHolder();
            mSurfaceHolder2 = mSurfaceView2.getHolder();

            mSurfaceHolder0.addCallback(shCallback);
            mSurfaceHolder2.addCallback(shCallback);
        }
    }

    public void destroy() {
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
        if (imageReader0 != null) {
            imageReader0.close();
        }
        if (imageReader2 != null) {
            imageReader2.close();
        }

    }

    public void startCameraThread() {
        Log.d(TAG, "startCameraThread");
        if (mCameraThread == null) {
            mCameraThread = new HandlerThread("CameraBackgroundThread"); // Name the thread
            mCameraThread.start();
            mCameraHandler = new Handler(mCameraThread.getLooper());
            cameraExecutor = new HandlerExecutor(mCameraHandler);
        }
        if (mImageReaderThread0 == null) {
            mImageReaderThread0 = new HandlerThread("ImageReaderThread0");
            mImageReaderThread0.start();
            mImageReaderHandler0 = new Handler(mImageReaderThread0.getLooper());
        }
    }

    public void stopCameraThread() {
        Log.d(TAG, "stopCameraThread");
        if (mCameraThread != null) {
            mImageReaderHandler0.removeCallbacksAndMessages(null);
            mCameraThread.quitSafely(); // Safely shut down the looper
            mImageReaderThread0.quitSafely();
            try {
                mCameraThread.join(); // Wait for the thread to finish
                mCameraThread = null;
                mCameraHandler = null;
                cameraExecutor = null;

                mImageReaderThread0.join();
                mImageReaderThread0 = null;
                mImageReaderHandler0 = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while stopping camera thread", e);
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

    /**
     * Image available listener for left preview frames
     */
    private final ImageReader.OnImageAvailableListener imageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    try {
                        // Acquire and immediately discard all but the latest image
                        Image image = reader.acquireLatestImage();
                        if (image == null) return;
                        // if we are already busy processing a frame
                        if (isProcessingLeft.get()) {
                            // Drop the frame by closing it immediately to free the buffer
                            image.close();
                            return;
                        }
                        // Mark as busy and process in a background thread
                        isProcessingLeft.set(true);
                        imageLeft = image;

                        processPreviewFrames();
                    } catch (IllegalStateException e) {
                        Log.d(TAG, e.toString());
                        if (imageLeft != null) imageLeft.close();
                        imageLeft = null;
                        isProcessingLeft.set(false);
                        return;
                    } finally {

                    }
                }
            };

    /**
     * Image available listener for right preview frames
     */
    private final ImageReader.OnImageAvailableListener imageAvailableListener2 =
            new ImageReader.OnImageAvailableListener() {

                @Override
                public void onImageAvailable(ImageReader reader) {
                    try {
                        // Acquire and immediately discard all but the latest image
                        Image image = reader.acquireLatestImage();
                        if (image == null) return;
                        // if we are already busy processing a frame
                        if (isProcessingRight.get()) {
                            // Drop the frame by closing it immediately to free the buffer
                            image.close();
                            return;
                        }
                        // Mark as busy and process in a background thread
                        isProcessingRight.set(true);
                        imageRight = image;

                        processPreviewFrames();
                    } catch (IllegalStateException e) {
                        Log.d(TAG, e.toString());
                        if (imageRight != null) imageRight.close();
                        imageRight = null;
                        isProcessingRight.set(false);
                        return;
                    } finally {

                    }
                }
            };

    private void processPreviewFrames() {
        if (imageLeft != null && imageRight != null) {
            YuvConverter.yuvToBitmap(imageLeft, (Bitmap) self.getNative());
            YuvConverter.yuvToBitmap(imageRight, (Bitmap) self2.getNative());
            imageLeft.close();
            imageLeft = null;
            isProcessingLeft.set(false);
            imageRight.close();
            imageRight = null;
            isProcessingRight.set(false);
            self.loadPixels();
            self.updatePixels();
            self2.loadPixels();
            self2.updatePixels();
            available = true;
        }
    }

    public void openCamera() {
        Log.d(TAG, "openCamera() cameraWidth=" + cameraWidth + " cameraHeight=" + cameraHeight);

        // Setup ImageReaders for capture
        cameraWidth = CAMERA_WIDTH_AR_DEFAULT; // camera width lens pixels
        cameraHeight = CAMERA_HEIGHT_AR_DEFAULT;
        mImageReader0 = ImageReader.newInstance(cameraWidth, cameraHeight, ImageFormat.JPEG, 2);  // 2 maxImages
        mImageReader2 = ImageReader.newInstance(cameraWidth, cameraHeight, ImageFormat.JPEG, 2);  // 2 maxImages

        if (useProcessing) {
            // Create ImageReaders for YUV preview with buffer count
            cameraWidth = XBP_CAMERA_WIDTH;
            cameraHeight = XBP_CAMERA_HEIGHT;

            imageReader0 = ImageReader.newInstance(cameraWidth, cameraHeight, ImageFormat.YUV_420_888, 4);
            imageReader0.setOnImageAvailableListener(imageAvailableListener, mImageReaderHandler0);
            imageReader2 = ImageReader.newInstance(cameraWidth, cameraHeight, ImageFormat.YUV_420_888, 4);
            imageReader2.setOnImageAvailableListener(imageAvailableListener2, mImageReaderHandler0);
            self = pApplet.createImage(cameraWidth, cameraHeight, PImage.ARGB);
            self.setNative(Bitmap.createBitmap(cameraWidth, cameraHeight, Bitmap.Config.ARGB_8888));
            self2 = pApplet.createImage(cameraWidth, cameraHeight, PImage.ARGB);
            self2.setNative(Bitmap.createBitmap(cameraWidth, cameraHeight, Bitmap.Config.ARGB_8888));

        }

        if (ActivityCompat.checkSelfPermission(context, CAMERA) == PackageManager.PERMISSION_GRANTED) {
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
            if (useProcessing) {
                createProcessingPreviewSession();
            } else {
                if ((mSurfaceView0 != null && mSurfaceView2.isAttachedToWindow()) && mSurfaceView2 != null && mSurfaceView2.isAttachedToWindow()) {
                    createCameraPreviewSession();
                } else {
                    Log.d(TAG, "Surface not attached to window");
                }
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

    public void closeCamera() {
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
     * Create camera view session for camera live preview
     */
    public void createCameraPreviewSession() {
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
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            if (null == mCameraDevice) return;
                            mCameraCaptureSession = session;
                            try {
                                previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                previewRequestBuilder.addTarget(mSurfaceHolder0.getSurface());
                                previewRequestBuilder.addTarget(mSurfaceHolder2.getSurface());

                                previewRequestBuilder.set(CaptureRequest.TONEMAP_MODE, CaptureRequest.TONEMAP_MODE_CONTRAST_CURVE);
                                previewRequestBuilder.set(CaptureRequest.TONEMAP_CURVE, new TonemapCurve(curve_srgb, curve_srgb, curve_srgb));
                                if (FOCUS_DISTANCE[focusDistanceIndex] == AUTO_FOCUS_DISTANCE) {
                                    previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                } else {
                                    previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
                                }
                                //
                                previewRequestBuilder.set(EXPOSURE_METERING, METERING[meteringIndex]);
                                previewRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, FOCUS_DISTANCE[focusDistanceIndex]);

                                previewRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, 1); // NOISE_REDUCTION_MODE
                                previewRequestBuilder.set(CaptureRequest.EDGE_MODE, 1); // EDGE_MODE
                                previewRequestBuilder.set(CaptureRequest.CONTROL_EXTENDED_SCENE_MODE, 1);  // sync left and right cameras
                                mCameraCaptureSession.setRepeatingRequest(previewRequestBuilder.build(), null, mCameraHandler);
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

    /**
     * Create Processing camera view session for camera live preview
     */
    public void createProcessingPreviewSession() {
        Log.d(TAG, "createProcessingPreviewSession()");

        OutputConfiguration opcL = new OutputConfiguration(new Size(cameraWidth, cameraHeight), SurfaceTexture.class);
        OutputConfiguration opcCapture0 = new OutputConfiguration(imageReader0.getSurface());
        opcCapture0.setPhysicalCameraId(leftCameraId);
        OutputConfiguration opcCapture2 = new OutputConfiguration(imageReader2.getSurface());
        opcCapture2.setPhysicalCameraId(rightCameraId);
        Log.d(TAG, "PreviewRequestBuilder " + imageReader0.getSurface().toString()
                + " " + imageReader2.getSurface().toString());

        OutputConfiguration popcCapture0 = new OutputConfiguration(mImageReader0.getSurface());
        popcCapture0.setPhysicalCameraId(leftCameraId);
        OutputConfiguration popcCapture2 = new OutputConfiguration(mImageReader2.getSurface());
        popcCapture2.setPhysicalCameraId(rightCameraId);

        List<OutputConfiguration> outputConfigsAll = Arrays.asList(opcL, opcCapture0, opcCapture2, popcCapture0, popcCapture2);

        CameraCaptureSession.StateCallback sessionCallback = new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(CameraCaptureSession session) {
                if (mCameraDevice == null || session == null) {
                    return;
                }
                Log.d(TAG, "Camera Id: " + mCameraDevice.getId() + " Preview session configured");
                mCameraCaptureSession = session;
                try {
                    previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

                    previewRequestBuilder.addTarget(imageReader0.getSurface());
                    previewRequestBuilder.addTarget(imageReader2.getSurface());

                    // Set auto focus
                    //previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
                    previewRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, FOCUS_DISTANCE[focusDistanceIndex]);

                    // Set auto exposure
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                    previewRequestBuilder.set(CaptureRequest.TONEMAP_MODE, CaptureRequest.TONEMAP_MODE_CONTRAST_CURVE);
                    previewRequestBuilder.set(CaptureRequest.TONEMAP_CURVE, new TonemapCurve(curve_srgb, curve_srgb, curve_srgb));
                    previewRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_FAST); // NOISE_REDUCTION_MODE 1
                    previewRequestBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_FAST); // EDGE_MODE 1
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range<>(30, 30));
                    //previewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, bestRange);
                    //previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY); // Android 15+
                    previewRequestBuilder.set(EXPOSURE_METERING, METERING[meteringIndex]);
                    // Set scene mode
                    previewRequestBuilder.set(CaptureRequest.CONTROL_EXTENDED_SCENE_MODE, 1);
                    // Set flash mode
//                    if (enableFlash) {
//                        previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
//                    } else {
//                        previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
//                    }

                    session.setRepeatingRequest(previewRequestBuilder.build(), previewCallback, mCameraHandler);

                    Log.d(TAG, "Camera Id: " + mCameraDevice.getId() + " Preview session started");
                } catch (CameraAccessException e) {
                    Log.d(TAG, "Camera Id: " + mCameraDevice.getId() + " Failed to start preview: " + e.getMessage());
                }
            }

            @Override
            public void onConfigureFailed(CameraCaptureSession session) {
                Log.d(TAG, "Failed to configure camera session");
            }
        };

        SessionConfiguration sessionConfiguration = new SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                outputConfigsAll,
                cameraExecutor,
                sessionCallback
        );

        try {
            mCameraDevice.createCaptureSession(sessionConfiguration);
            Log.d(TAG, "Camera Id: " + mCameraDevice.getId() + " Preview session created");
        } catch (CameraAccessException e) {
            Log.d(TAG, "Camera Id: " + mCameraDevice.getId() + " access exception: " + e.getMessage());
        }
        Log.d(TAG, "createProcessingViewSession() done");
    }

    /**
     * Capture callback for preview frames
     */
    private CameraCaptureSession.CaptureCallback previewCallback =
            new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                               TotalCaptureResult result) {

                }
            };

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

    /**
     * Create a camera capture session to take a picture
     */
    public void createCameraCaptureSession() {
        Log.d(TAG, "createCameraCaptureSession()");
        if (mCameraDevice == null || mCameraCaptureSession == null) {
            Toast.makeText(context, "Camera not ready", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(context, "Error capturing images", Toast.LENGTH_SHORT).show();
        }
        if (shutterSound) {
            playShutterSound();
        }
    }

    public void dispose() {
        ((Bitmap) self.getNative()).recycle();
        ((Bitmap) self2.getNative()).recycle();
        self = null;
        self2 = null;
        System.gc();
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

    public void shutterSound() {
        if (shutterSound) {
            CameraInfo.mustPlayShutterSound();
        }
        Log.d(TAG, "shutter sound " + ((shutterSound) ? "on" : "off"));

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

    public void setFocusDistance() {
        int i = focusDistanceIndex + 1;
        if (i >= FOCUS_DISTANCE.length) i = 0;
        focusDistanceIndex = i;
        Toast.makeText(context, FOCUS_DISTANCE_NAMES[focusDistanceIndex], Toast.LENGTH_SHORT).show();
    }

    public void setMeteringIndex() {
        meteringIndex++;
        if (meteringIndex >= METERING.length) meteringIndex = 0;
        Toast.makeText(context, METERING_NAMES[meteringIndex], Toast.LENGTH_SHORT).show();

    }

}
