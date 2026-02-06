package com.andymodla.android3dcamera.camera;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

public class CameraInfoUtil {

    //private static final String TAG = "CameraSyncChecker";
    private static final String TAG = "A3DCamera";

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
