package com.andymodla.android3dcamera;

import static android.Manifest.permission.CAMERA;

import android.content.Context;
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
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
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

public class Camera {
    public static final String TAG = "A3DCamera";
    Context context;
    Media media;

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;

    private HandlerThread mCameraThread;
    private Handler mCameraHandler;
    private Executor cameraExecutor;

    private static final int MAX_NUM_CAMERAS = 6;
    // Camera Ids for Xreal Beam Pro
    private String leftCameraId = "0";
    private String frontCameraId = "1";
    private String rightCameraId = "2";
    private String stereoCameraId = "3";

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

    // Display surfaces
    private volatile SurfaceView mSurfaceView0;
    private volatile SurfaceView mSurfaceView2;
    private volatile SurfaceHolder mSurfaceHolder0;
    private volatile SurfaceHolder mSurfaceHolder2;

    // Image capture
    private volatile ImageReader mImageReader0;
    private volatile ImageReader mImageReader2;  // for SBS display

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
    //    captureRequestBuilder.set(SATURATION, 5);
    //    captureBuilder.set(SATURATION, 5);

    // Sharpness 0 - 6, default 2
    private static final CaptureRequest.Key<Integer> SHARPNESS = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sharpness.strength", Integer.class);
    volatile boolean shutterSound = true;

    // Constructor
    public Camera(Context context, Media media) {
        this.context = context;
        this.media = media;
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
    }

    public void init() {
        Log.d(TAG, "setupSurfaces()");
        // set up display surfaces
        mSurfaceView0 = ((MainActivity) context).findViewById(R.id.surfaceView);
        mSurfaceView2 = ((MainActivity) context).findViewById(R.id.surfaceView2);

        mSurfaceHolder0 = mSurfaceView0.getHolder();
        mSurfaceHolder2 = mSurfaceView2.getHolder();

        mSurfaceHolder0.addCallback(shCallback);
        mSurfaceHolder2.addCallback(shCallback);
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

    }

    public void startCameraThread() {
        Log.d(TAG, "startCameraThread");
        if (mCameraThread == null) {
            mCameraThread = new HandlerThread("CameraBackgroundThread"); // Name the thread
            mCameraThread.start();
            mCameraHandler = new Handler(mCameraThread.getLooper());
            cameraExecutor = new HandlerExecutor(mCameraHandler);
        }
    }

    public void stopCameraThread() {
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


    public void openCamera() {
        Log.d(TAG, "openCamera() cameraWidth=" + cameraWidth + " cameraHeight=" + cameraHeight);

        // Setup ImageReaders for capture
        mImageReader0 = ImageReader.newInstance(cameraWidth, cameraHeight, ImageFormat.JPEG, 2);  // 2 maxImages
        mImageReader2 = ImageReader.newInstance(cameraWidth, cameraHeight, ImageFormat.JPEG, 2);  // 2 maxImages

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
     * Create camera view session for live preview
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
