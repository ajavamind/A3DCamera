package com.andymodla.android3dcamera.camera;

import static androidx.core.content.ContextCompat.getSystemService;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Log;
import android.util.Size;

import java.util.Arrays;
import java.util.List;


public class CameraInfoUtil {

    //private static final String TAG = "CameraSyncChecker";
    private static final String TAG = "A3DCamera";


    public static void displayCameraInfo(Context context) {

        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        String[] cameraIdList = new String[4];
        if (cameraManager != null) {
            try {
                // 2. Get the list of available camera IDs
                cameraIdList = cameraManager.getCameraIdList();
                // overridden for testing only
                cameraIdList = new String[4];
                cameraIdList[0] = "0";
                cameraIdList[1] = "1";
                cameraIdList[2] = "2";
                cameraIdList[3] = "3";

                // 3. Loop through each camera ID to get its characteristics
                for (String cameraId : cameraIdList) {
                    CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);

                    // --- Extract Full Information ---

                    // Lens Facing (Front or Back)
                    Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    String facingStr = (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_FRONT) ? "Front" : "Back";

                    // Hardware Level (e.g., LEGACY, LIMITED, FULL, LEVEL_3)
                    Integer hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);

                    // Flash Unit Presence
                    Boolean hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);

                    // Stream Configuration Map (supported preview/recording resolutions)
                    StreamConfigurationMap streamMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                    Log.d(TAG, "Camera ID: " + cameraId +
                            ", Lens: " + facingStr +
                            ", Hardware Level: " + hardwareLevel +
                            ", Has Flash: " + hasFlash);

                    // Example: Log supported output sizes for ImageFormat.JPEG
                    if (streamMap != null) {
                        Size[] sizes = streamMap.getOutputSizes(ImageFormat.JPEG);
                        if (sizes != null) {
                            Log.d(TAG, "JPEG Resolutions count: " + sizes.length);
                        }
                    }

                    try {
                        characteristics = cameraManager.getCameraCharacteristics(cameraId);

                        // Retrieve the stream configuration map
                        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                        if (map != null) {
                            // Fetch all supported sizes for the JPEG format
                            Size[] jpegSizes = map.getOutputSizes(ImageFormat.JPEG);

                            if (jpegSizes != null) {
                                Log.d("CameraResolutions", "--- Camera ID " + cameraId + " JPEG Sizes ---");
                                for (Size size : jpegSizes) {
                                    Log.d("CameraResolutions", size.getWidth() + " x " + size.getHeight());
                                }
                            } else {
                                Log.d("CameraResolutions", "Camera ID " + cameraId + " does not support JPEG output.");
                            }
                        }

                    } catch (CameraAccessException e) {
                        Log.e("CameraResolutions", "Error accessing camera characteristics", e);
                    }
                }
            } catch (CameraAccessException e) {
                Log.e(TAG, "Camera access exception: ", e);
            }

            // Check if Camera 3 is a logical multi-camera
            try {
                CameraCharacteristics chars = cameraManager.getCameraCharacteristics(cameraIdList[3]);
                int[] capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
                boolean isLogical = false;
                for (int cap : capabilities) {
                    if (cap == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA) {
                        isLogical = true;
                        break;
                    }
                }

                if (isLogical) {
                    // Standard template creation failed, try using physical IDs explicitly
                    // or fallback to opening physical camera ID 3 or ID 1 directly.
                    Log.d(TAG, "Camera "+cameraIdList[3]+" is a logical multi-camera.");
                } else {
                    Log.d(TAG, "Camera "+ cameraIdList[3]+ " is not a logical multi-camera.");
                }
            } catch (CameraAccessException e) {
                Log.e(TAG, "Error accessing camera characteristics", e);
            }

        }
    }

    //@RequiresApi(api = Build.VERSION_CODES.P)
    public static void checkCameraSyncType(Context context, String[] cameraIds) {
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        if (cameraManager == null) {
            Log.e(TAG, "CameraManager not available.");
            return;
        }
        //String[] ids = {"3"};
        try {
            for (String cameraId : cameraIds) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);

                // Retrieve the capabilities
                int[] capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
                List<Integer> capabilitiesList = null;
                if (capabilities != null) {
                    capabilitiesList = Arrays.asList(
                            Arrays.stream(capabilities).boxed().toArray(Integer[]::new)
                    );
                }

                // Check if the camera is a logical multi-camera device
                if (capabilitiesList != null && capabilitiesList.contains(
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)) {

                    Log.i(TAG, "Camera ID " + cameraId + " is a logical multi-camera.");

                    // Get the sync type for the logical multi-camera
                    Integer syncType = characteristics.get(
                            CameraCharacteristics.LOGICAL_MULTI_CAMERA_SENSOR_SYNC_TYPE
                    );
/*
Explanation of Sync Types:
•LOGICAL_MULTI_CAMERA_SENSOR_SYNC_TYPE_CALIBRATED:
This indicates that the physical camera sensors are hardware-synchronized.
Their shutters are synchronized, and the timestamps of the images accurately reflect the start-of-exposure time.

•LOGICAL_MULTI_CAMERA_SENSOR_SYNC_TYPE_APPROXIMATE: This means a software mechanism is used to synchronize
the physical cameras.
There might be a slight offset between their start-of-exposure times,
though all images in a single capture request will share the same timestamp.
*/

                    if (syncType != null) {
                        if (syncType == CameraCharacteristics.LOGICAL_MULTI_CAMERA_SENSOR_SYNC_TYPE_CALIBRATED) {
                            Log.i(TAG, "  --> Sync type is: CALIBRATED (hardware-synchronized)");
                        } else if (syncType == CameraCharacteristics.LOGICAL_MULTI_CAMERA_SENSOR_SYNC_TYPE_APPROXIMATE) {
                            Log.i(TAG, "  --> APPROXIMATE (software-synchronized)");
                        } else {
                            Log.i(TAG, "  --> Sync type is: UNKNOWN (" + syncType + ")");
                        }
                    } else {
                        Log.i(TAG, "  --> Sync type information is not available.");
                    }
                } else {
                    Log.d(TAG, "Camera ID " + cameraId + " is not a logical multi-camera.");
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error accessing camera information: ", e);
        }
    }

    public static void logFocusDistanceCalibration(Context context) {
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        if (cameraManager == null) {
            Log.e(TAG, "CameraManager service not available");
            return;
        }
        String[] cameraIds = {"0", "1", "2", "3", "4", "5"};

        // Iterate through all available camera IDs on the device
        for (String cameraId : cameraIds) {
            try {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
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
            } catch (CameraAccessException e) {
                Log.e(TAG, "Failed to access cameraId " + cameraId + " characteristics");
            } catch (IllegalArgumentException ill) {
                Log.e(TAG, "Illegal Argument for cameraId " + cameraId + " Failed to access camera characteristics ");
            }
        }
    }

}
