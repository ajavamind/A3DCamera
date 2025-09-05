package com.andymodla.android3dcamera;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.hardware.camera2.params.TonemapCurve;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.media.MediaScannerConnection;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "A3DCamera";
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    private static final int MY_STORAGE_REQUEST_CODE = 101;

    private String SAVE_FOLDER = "A3DCamera";

    // camera dimensions
    //private int cameraWidth = 1024;//1440;
    //private int cameraHeight = 768;//1080;
    private int cameraWidth = 4080; // camera width lens pixels
    private int cameraHeight = 3072;// camera height lens pixels

    private static final float[] curve_srgb = { // sRGB curve
            0.0000f, 0.0000f, 0.0667f, 0.2864f, 0.1333f, 0.4007f, 0.2000f, 0.4845f,
            0.2667f, 0.5532f, 0.3333f, 0.6125f, 0.4000f, 0.6652f, 0.4667f, 0.7130f,
            0.5333f, 0.7569f, 0.6000f, 0.7977f, 0.6667f, 0.8360f, 0.7333f, 0.8721f,
            0.8000f, 0.9063f, 0.8667f, 0.9389f, 0.9333f, 0.9701f, 1.0000f, 1.0000f};

    private CameraDevice mCameraDevice;
    private CameraManager mCameraManager;
    private CameraCaptureSession mCameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private Handler mainHandler;

    // Display surfaces
    private SurfaceView mSurfaceView0, mSurfaceView2;
    private SurfaceHolder mSurfaceHolder0, mSurfaceHolder2;

    // Image capture
    private ImageReader mImageReader0, mImageReader2;

    // Conversion modes
    private volatile boolean isAnaglyphMode = false;

    // states
    private static final int STATE_LIVEVIEW = 0;
    private static final int STATE_REVIEW = 0;
    private int state = STATE_LIVEVIEW;

    volatile Image imageL;
    volatile Image imageR;
    volatile byte[] leftBytes;
    volatile byte[] rightBytes;
    volatile Bitmap leftBitmap;
    volatile Bitmap rightBitmap;
    volatile String timestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        setContentView(R.layout.layout);
        setupSurfaces();
        checkPermissions();
        logFocusDistanceCalibration();
    }

    private void setupSurfaces() {
        mSurfaceView0 = findViewById(R.id.surfaceView);
        mSurfaceView2 = findViewById(R.id.surfaceView2);

        mSurfaceHolder0 = mSurfaceView0.getHolder();
        mSurfaceHolder2 = mSurfaceView2.getHolder();

        // Setup anaglyph surface view
        //mAnaglyphSurfaceView = findViewById(R.id.surfaceView3);  // anaglyph surface
        //mAnaglyphSurfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);
        //mAnaglyphSurfaceView.setVisibility(SurfaceView.GONE);  // initially hide
        // ... later, when you want to show it:
        // mAnaglyphSurfaceView.setVisibility(View.VISIBLE);

        SurfaceHolder.Callback shCallback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                if (mCameraDevice == null &&
                        mSurfaceHolder0.getSurface().isValid() &&
                        mSurfaceHolder2.getSurface().isValid()) {
                    initCamera();
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

        mSurfaceHolder0.addCallback(shCallback);
        mSurfaceHolder2.addCallback(shCallback);
    }

    private void checkPermissions() {
        String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        boolean needsPermission = false;

        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                needsPermission = true;
                break;
            }
        }

        if (needsPermission) {
            ActivityCompat.requestPermissions(this, permissions, MY_CAMERA_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                initCamera();
            }
            //else {
                //Toast.makeText(this, "Camera and storage permissions required", Toast.LENGTH_SHORT).show();
            //}
        }
    }

    private void initCamera() {
        Log.d(TAG, "initCamera()");
        mainHandler = new Handler(getMainLooper());

        // Setup ImageReaders for capture
        mImageReader0 = ImageReader.newInstance(cameraWidth, cameraHeight, ImageFormat.JPEG, 2);  // 2 maxImages
        mImageReader2 = ImageReader.newInstance(cameraWidth, cameraHeight, ImageFormat.JPEG, 2);  // 2 maxImages

        if (ActivityCompat.checkSelfPermission(this, CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try {
                mCameraManager.openCamera("3", stateCallback, mainHandler);
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
                createCameraCaptureSession();
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
            Log.e(TAG, "Camera hardware failure");
        }
    };

    private void createCameraCaptureSession() {
        Log.d(TAG, "createCameraCaptureSession()");
        try {
            OutputConfiguration opc0 = new OutputConfiguration(mSurfaceHolder0.getSurface());
            OutputConfiguration opc1 = new OutputConfiguration(mSurfaceHolder2.getSurface());
            opc1.setPhysicalCameraId("2");

            OutputConfiguration opcCapture0 = new OutputConfiguration(mImageReader0.getSurface());
            OutputConfiguration opcCapture1 = new OutputConfiguration(mImageReader2.getSurface());
            opcCapture1.setPhysicalCameraId("2");

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
                                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, 0);
                                captureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0.60356647f);// hyperfocal distance
                                mCameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
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
        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera access exception", e);
        }
    }

    public void logFocusDistanceCalibration() {

        if (mCameraManager == null) {
            Log.e(TAG, "CameraManager service not available");
            return;
        }
        String[] cameraIds = {"0", "1", "2", "3"};
        try {
            // Iterate through all available camera IDs on the device
            for (String cameraId : cameraIds) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                Integer calibration = characteristics.get(CameraCharacteristics.LENS_INFO_FOCUS_DISTANCE_CALIBRATION);

                String calibrationString;
                if (calibration != null) {
                    switch (calibration) {
                        case CameraCharacteristics.LENS_INFO_FOCUS_DISTANCE_CALIBRATION_UNCALIBRATED:
                            calibrationString = "UNCALIBRATED";
                            break;
                        case CameraCharacteristics.LENS_INFO_FOCUS_DISTANCE_CALIBRATION_APPROXIMATE:
                            calibrationString = "APPROXIMATE";
                            break;
                        case CameraCharacteristics.LENS_INFO_FOCUS_DISTANCE_CALIBRATION_CALIBRATED:
                            calibrationString = "CALIBRATED";
                            break;
                        default:
                            calibrationString = "UNKNOWN";
                            break;
                    }
                } else {
                    calibrationString = "VALUE NOT AVAILABLE";
                }

                Log.d(TAG, "Camera ID: " + cameraId + " - LENS_FOCUS_DISTANCE_CALIBRATION: " + calibrationString);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to access camera characteristics", e);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //Log.d(TAG, "onKeyDown "+keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_ENTER:
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
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_3D_MODE: // camera key - first turn off auto launch of native camera app
                captureImages();
                return true;
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_BACK:
                reviewImage();
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    private void reviewImage() {
        Log.d(TAG, "reviewImage()");
    }

    private void captureImages() {
        Log.d(TAG, "captureImages()");
        if (mCameraDevice == null || mCameraCaptureSession == null) {
            Toast.makeText(this, "Camera not ready", Toast.LENGTH_SHORT).show();
            return;
        }
        isAnaglyphMode = true;  // force saving anaglyph image
        try {
            // Create capture request for both cameras
            CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader0.getSurface());
            captureBuilder.addTarget(mImageReader2.getSurface());
            captureBuilder.set(CaptureRequest.TONEMAP_MODE, CaptureRequest.TONEMAP_MODE_CONTRAST_CURVE);
            captureBuilder.set(CaptureRequest.TONEMAP_CURVE, new TonemapCurve(curve_srgb, curve_srgb, curve_srgb));

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

            // Setup image capture listeners
            mImageReader0.setOnImageAvailableListener(new OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    imageL = reader.acquireLatestImage();
                    if (imageL != null) {
                        leftBytes = convertToBytes(imageL);
                        imageL.close();
                        // Capture frames for anaglyph processing when needed
                        if (leftBytes != null && rightBytes != null) {
                            leftBitmap = saveImageFile(leftBytes, "IMG" + timestamp, true ); // left
                            rightBitmap = saveImageFile(rightBytes, "IMG" + timestamp, false); // right
                            createAndSaveSBS(timestamp, leftBitmap, rightBitmap);
                            if (isAnaglyphMode) {
                                createAndSaveAnaglyph(timestamp, leftBitmap, rightBitmap);
                            }
                        }
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
                        // Capture frames for anaglyph processing when needed
                        if (leftBytes != null && rightBytes != null) {
                            leftBitmap = saveImageFile(leftBytes, "IMG" + timestamp, true ); // left
                            rightBitmap = saveImageFile(rightBytes, "IMG" + timestamp, false); // right
                            createAndSaveSBS(timestamp, leftBitmap, rightBitmap);
                            if (isAnaglyphMode) {
                                createAndSaveAnaglyph(timestamp, leftBitmap, rightBitmap);
                            }

                        }
                   }
                }
            }, mainHandler);

            mCameraCaptureSession.capture(captureBuilder.build(), null, mainHandler);

            Toast.makeText(this, "Images captured: IMG" + timestamp, Toast.LENGTH_SHORT).show();

        } catch (CameraAccessException e) {
            Log.e(TAG, "Error capturing images", e);
            Toast.makeText(this, "Error capturing images", Toast.LENGTH_SHORT).show();
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
            Log.e(TAG, "Image decoding failed! " + (left ? "left": "right" ));
            return null;
        }

        File mediaStorageDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), SAVE_FOLDER);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.e(TAG, "failed to create directory to save photo: " + mediaStorageDir.getAbsolutePath());
                Toast.makeText(this, "Error creating folder " + SAVE_FOLDER, Toast.LENGTH_SHORT).show();
                return null;
            }
        }

        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + File.separator + SAVE_FOLDER), filename);

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
        Log.d(TAG, "convertToBytes Image Format: " + image.getFormat() + " planes "+planes.length);
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
        Bitmap anaglyphBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

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
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), SAVE_FOLDER);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.e(TAG, "failed to create directory to save photo: " + mediaStorageDir.getAbsolutePath());
                Toast.makeText(this, "Error creating folder " + SAVE_FOLDER, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String filename = "IMG" + timestamp + "_ana.jpg";
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + File.separator + SAVE_FOLDER), filename);

        try (FileOutputStream out = new FileOutputStream(file)) {
            anaglyphBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);

            MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()},
                    new String[]{"image/jpeg"}, null);

            Log.d(TAG, "Anaglyph image saved: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error saving anaglyph image", e);
        }

        anaglyphBitmap.recycle();

    }

    private void createAndSaveSBS(String timestamp, Bitmap leftBitmap, Bitmap rightBitmap) {
        Log.d(TAG, "createAndSaveSBS");
        if (leftBitmap == null || rightBitmap == null) {
            Log.d(TAG, "createAndSaveSBS failed Bitmaps null " + timestamp);
            return;
        }

        // Calculate the dimensions for the combined bitmap.
        int width = leftBitmap.getWidth() + rightBitmap.getWidth();
        int height = Math.max(leftBitmap.getHeight(), rightBitmap.getHeight());

        // Create a new bitmap with the combined dimensions.
        Bitmap sbsBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // Create a canvas to draw on the new bitmap.
        Canvas canvas = new Canvas(sbsBitmap);

        // Draw the left bitmap at position (0, 0).
        canvas.drawBitmap(leftBitmap, 0f, 0f, null);

        // Draw the right bitmap immediately to the right of the left one.
        canvas.drawBitmap(rightBitmap, leftBitmap.getWidth(), 0f, null);

        // Save SBS image
        File mediaStorageDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), SAVE_FOLDER);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.e(TAG, "failed to create directory to save photo: " + mediaStorageDir.getAbsolutePath());
                Toast.makeText(this, "Error creating folder " + SAVE_FOLDER, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String filename = "IMG" + timestamp + "_2x1.jpg";
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + File.separator + SAVE_FOLDER), filename);

        try (FileOutputStream out = new FileOutputStream(file)) {
            sbsBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            //sbsBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()},
                    new String[]{"image/jpeg"}, null);

            Log.d(TAG, "SBS 2x1 image saved: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error saving SBS 2x1 image", e);
        }

        sbsBitmap.recycle();

    }

    private void toggleDisplayMode() {
        isAnaglyphMode = !isAnaglyphMode;

        if (isAnaglyphMode) {
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
                createCameraCaptureSession();
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
    }

}