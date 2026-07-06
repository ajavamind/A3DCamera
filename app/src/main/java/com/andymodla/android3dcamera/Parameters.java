package com.andymodla.android3dcamera;

/**
 * Application parameters
 * Copyright 2025-2026 Andy Modla  All Rights Reserved
 * Command line based parameter read and set
 *
   To add parameters to this file use this prompt with gemma4 4B
   Using Parameters.java as a base pattern, I want to add more parameters as follows:
   int focusDistanceIndex
   For String default use "".
   For boolean default use false.
   For integer default use 0. Please update this file to  add these new parameters.

 ok I updated files.
 Update app/src/main/java/com/andymodla/android3dcamera/Parameters.java, add more parameters following the pattern
 established
 with similar data types.
 For String default use "".
 For boolean default use false.
 For integer default use 0.
 Please update this file to  add these new parameters:
 boolean saveLR
 boolean saveAnaglyph
 Look in Media.java for saveLr and saveAnaglyph description and parameter values.
 Note that these two parameters override the default usage of LR and anaglyph file saving set by the cameraMode.


 */

import static java.util.Arrays.*;

import android.content.SharedPreferences;
import android.content.Context;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import processing.core.PApplet;

//------------------------------------------------------------------------------

class ParamStore {
    String abbr; // abbreviation
    String name; // storage name
    String desc; // description
    String getterName; // Name of the getter method
    String setterName; // Name of the setter method
    Class<?> setterParamType; // Parameter type for the value and defaultValue
    String defaultValue; // default value
    String fullDescription; // full description of the command

    public ParamStore(String abbr, String name, String desc, String getterName, String setterName, Class<?> setterParamType, String defaultValue, String fullDescription) {
        this.abbr = abbr;
        this.name = name;
        this.desc = desc;
        this.getterName = getterName;
        this.setterName = setterName;
        this.setterParamType = setterParamType;
        this.defaultValue = String.valueOf(defaultValue);
        this.fullDescription = String.valueOf(fullDescription);
    }
}

        public class Parameters {
            private final String TAG = "Parameters";
            private final SharedPreferences prefs;
            private final Context context;

            // Camera application modes
            public static final int BASIC_MODE = 0;
            public static final int ANAGLYPH_MODE = 1;
            public static final int PHOTO_BOOTH_MODE = 2;
            volatile int cameraMode = BASIC_MODE;

            // Stereo Image Alignment parameters
            // same values as StereoPhotoMaker displays after automatic alignment of a
            // reference calibration stereo photo.
            // parallax and vertical offsets are in pixels for camera image captured correction
            public int parallaxOffset = 0;  // left/right horizontal offset parallax for stereo window placement
            public int verticalOffset = 0;  // left/right camera vertical offset alignment for camera correction
            public boolean isSoundOn = true;
            public boolean isAiEdit = true;

            // photo booth parameters
            //public boolean isPhotoBooth = false;
            public boolean isMirror = true; // for photo booth only

            private int receiverPort = 8000;  // device port to receive broadcast messages

            private boolean isBlankScreen = false;  // for display covered

            private String title1 = "";
            private String title2 = "";
            private String instruction1 = "";
            private String instruction2 = "";

            private int countdownTimer = 0;
            private boolean countDownEnabled = false;

            private boolean udpControlEnabled = false;
            private boolean udpTransmit = false;

            // New parameters
            private boolean autoReview = false;
            private boolean sbsCropPrint = false;
            public static final float sbsCrop = 0f;  // aspect ratio for mo SBS crop
            //public static final float sbsCrop = 1.5f; // aspect ratio for SBS crop to show SBS in 6x4 print
            //public static final float sbsCrop = 1.777777778f;  // aspect ratio for SBS crop to show SBS in 1080p or 2160p (4K) screen
            //public static final float sbsCrop = 2.0f; // aspect ratio for SBS crop to show SBS in square print
            private int focusDistanceIndex = 0;
            private int exposureMeteringIndex = 0;
            private boolean saveLR = false;
            private boolean saveAnaglyph = false;
            private String saveFileType = "jpg";
            private int saveFileQuality = 100;
            private int aspectRatioIndex = 0;  // default
            private int exposureCompensationIndex = 0;

            // default constructor
            public Parameters(SharedPreferences prefs, Context context) {
                this.prefs = prefs;
                this.context = context;
            }


            // Generic method to call a getter using its name from a ParamStore object
            private Object callGetter(ParamStore store) {
                try {
                    Method method = this.getClass().getMethod(store.getterName);
                    return method.invoke(this);
                } catch (Exception e) {
                    // Handle potential exceptions like NoSuchMethodException, etc.
                    Log.e(TAG, "Error Parameters calling getter: " + e.getMessage());
                    return null;
                }
            }

            // Generic method to call a setter using its name from a ParamStore object
            private void callSetter(ParamStore store, Object value) {
                try {
                    Method method = this.getClass().getMethod(store.setterName, store.setterParamType);
                    if (store.setterParamType == int.class)
                        method.invoke(this, Integer.parseInt(value.toString()));
                    else if (store.setterParamType == String.class)
                        method.invoke(this, value.toString());
                    else if (store.setterParamType == boolean.class) {
                        if (value.toString().equals("on")) method.invoke(this, true);
                        else if (value.toString().equals("off")) method.invoke(this, false);
                        else if (value.toString().equals("true")) method.invoke(this, true);
                        else if (value.toString().equals("false"))
                        method.invoke(this, Boolean.parseBoolean(value.toString()));
                    } else method.invoke(this, value);
                } catch (Exception e) {
                    // Handle potential exceptions
                    Log.e(TAG, "Error Parameters calling setter: " + e.getMessage());
                }
            }

            /**
             * Initialize all parameters from preference storage
             */
            public void init() {
                readParallaxOffset();
                readVerticalOffset();
                readCameraMode();
                readIsBlankScreen();
                readIsSoundOn();
                readIsAiEdit();
                readIsMirror();
                readTitle1();
                readTitle2();
                readInst1();
                readInst2();
                readCountdownTimer();
                readCountDownEnabled();
                readUdpControlEnabled();
                readUdpTransmit();
                readAutoReview();
                readSbsCropPrint();
                readFocusDistanceIndex();
                readExposureMeteringIndex();
                readSaveLr();
                readSaveAnaglyph();
                readSaveFileType();
                readSaveFileQuality();
                readAspectRatioIndex();
                readExposureCompensationIndex();
            }

            //------------------------------------------------------------------------------
            public void readParallaxOffset() {
                parallaxOffset = prefs.getInt(parallaxOffsetStore.name, Integer.parseInt(parallaxOffsetStore.defaultValue));
            }

            public int getParallaxOffset() {
                return parallaxOffset;
            }

            public void setParallaxOffset(int parallaxOffset) {
                this.parallaxOffset = parallaxOffset;
                // Save to SharedPreferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(parallaxOffsetStore.name, parallaxOffset);
                editor.commit(); // synchronous save: do it now
                ((MainActivity) this.context).updateParameters();
            }

            //------------------------------------------------------------------------------
            public void readVerticalOffset() {
                verticalOffset = prefs.getInt(verticalOffsetStore.name, Integer.parseInt(verticalOffsetStore.defaultValue));
            }

            public int getVerticalOffset() {
                return verticalOffset;
            }

            public void setVerticalOffset(int verticalOffset) {
                this.verticalOffset = verticalOffset;
                // Save to SharedPreferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(verticalOffsetStore.name, verticalOffset);
                editor.commit(); // asynchronous save
                ((MainActivity) this.context).updateParameters();
            }

            //------------------------------------------------------------------------------

            public boolean isBasicCameraMode() {
                return (getCameraMode() == BASIC_MODE);
            }

            public boolean isAnaglyphCameraMode() {
                return (getCameraMode() == ANAGLYPH_MODE);
            }

            public boolean isPhotoBoothCameraMode() {
                return (getCameraMode() == PHOTO_BOOTH_MODE);
            }

            //------------------------------------------------------------------------------
            public void readCameraMode() {
                cameraMode = prefs.getInt(cameraModeStore.name, Integer.parseInt(cameraModeStore.defaultValue));
                //Log.d(TAG, "readCameraMode: " + cameraMode);
            }

            public int getCameraMode() {
                return cameraMode;
            }

            public void setCameraMode(int cameraMode) {
                boolean changed = (this.cameraMode != cameraMode);
                this.cameraMode = cameraMode;
                // Save to SharedPreferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(cameraModeStore.name, this.cameraMode);
                editor.commit(); // synchronous save: do it now and return
                //editor.apply();   // asynchronous save
                // Needs restart to reinitialize the application (only if value actually changed)
                if (changed) ((MainActivity) context).restartApp();
            }

            //------------------------------------------------------------------------------
            public void readIsBlankScreen() {
                isBlankScreen = prefs.getBoolean(isBlankScreenStore.name, Boolean.parseBoolean(isBlankScreenStore.defaultValue));
            }

            public boolean getIsBlankScreen() {
                return isBlankScreen;
            }

            public void setIsBlankScreen(boolean isBlankScreen) {
                this.isBlankScreen = isBlankScreen;
                // Save to SharedPreferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(isBlankScreenStore.name, isBlankScreen);
                editor.commit(); // asynchronous save
                ((MainActivity) this.context).updateParameters();
            }

            //------------------------------------------------------------------------------
            public void readIsSoundOn() {
                isSoundOn = prefs.getBoolean(isSoundOnStore.name, Boolean.parseBoolean(isSoundOnStore.defaultValue));
            }

            public boolean getIsSoundOn() {
                return isSoundOn;
            }

            public void setIsSoundOn(boolean isSoundOn) {
                this.isSoundOn = isSoundOn;
                // Save to SharedPreferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(isSoundOnStore.name, isSoundOn);
                editor.commit(); // asynchronous save
                ((MainActivity) this.context).updateParameters();
            }


            //------------------------------------------------------------------------------
            public void readIsMirror() {
                isMirror = prefs.getBoolean(isMirrorStore.name, Boolean.parseBoolean(isMirrorStore.defaultValue));
            }

            public boolean getIsMirror() {
                return isMirror;
            }

            public void setIsMirror(boolean isMirror) {
                this.isMirror = isMirror;
                // Save to SharedPreferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(isMirrorStore.name, isMirror);
                editor.commit(); // asynchronous save
                ((MainActivity) this.context).updateParameters();

            }

            public void readIsAiEdit() {
                isAiEdit = prefs.getBoolean(isAiEditStore.name, Boolean.parseBoolean(isAiEditStore.defaultValue));
            }

            public boolean getIsAiEdit() {
                return isAiEdit;
            }

            public void setIsAiEdit(boolean isAiEdit) {
                this.isAiEdit = isAiEdit;
                // Save to SharedPreferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(isAiEditStore.name, isAiEdit);
                editor.commit(); // asynchronous save
                ((MainActivity) this.context).updateParameters();
            }

            public void readTitle1() {
                title1 = prefs.getString(title1Store.name, title1Store.defaultValue);
            }

            public String getTitle1() {
                return title1;
            }

            public void setTitle1(String title1) {
                this.title1 = title1;
                // Save to SharedPreferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(title1Store.name, title1);
                editor.commit(); // synchronous save
                ((MainActivity) this.context).updateParameters();
            }
            
            public void readTitle2() {
                title2 = prefs.getString(title2Store.name, title2Store.defaultValue);
            }

            public String getTitle2() {
                return title2;
            }

            public void setTitle2(String title2) {
                this.title2 = title2;
                // Save to SharedPreferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(title2Store.name, title2);
                editor.commit(); // synchronous save
                ((MainActivity) this.context).updateParameters();
            }

            public void readInst1() {
                instruction1 = prefs.getString(inst1Store.name, inst1Store.defaultValue);
            }

            public String getInst1() {
                return instruction1;
            }

            public void setInst1(String inst1) {
                this.instruction1 = inst1;
                // Save to SharedPreferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(inst1Store.name, instruction1);
                editor.commit(); // synchronous save
                ((MainActivity) this.context).updateParameters();
            }

            public void readInst2() {
                instruction2 = prefs.getString(inst2Store.name, inst2Store.defaultValue);
            }

            public String getInst2() {
                return instruction2;
            }

            public void setInst2(String inst2) {
                this.instruction2 = inst2;
                // Save to SharedPreferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(inst2Store.name, instruction2);
                editor.commit(); // synchronous save
                ((MainActivity) this.context).updateParameters();
            }

            public void readCountdownTimer() {
                countdownTimer = prefs.getInt(countdownTimerStore.name, Integer.parseInt(countdownTimerStore.defaultValue));
            }

            public int getCountdownTimer() {
                return countdownTimer;
            }

            public void setCountdownTimer(int countdownTimer) {
                this.countdownTimer = countdownTimer;
                // Save to SharedPreferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(countdownTimerStore.name, countdownTimer);
                editor.commit(); // synchronous save
                ((MainActivity) this.context).updateParameters();
            }

            public void readCountDownEnabled() {
                countDownEnabled = prefs.getBoolean(countDownEnabledStore.name, Boolean.parseBoolean(countDownEnabledStore.defaultValue));
            }

            public boolean getCountDownEnabled() {
                return countDownEnabled;
            }

            public void setCountDownEnabled(boolean countDownEnabled) {
                this.countDownEnabled = countDownEnabled;
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(countDownEnabledStore.name, countDownEnabled);
                editor.commit();
                ((MainActivity) this.context).updateParameters();
            }

            public void readUdpControlEnabled() {
                udpControlEnabled = prefs.getBoolean(udpControlEnabledStore.name, Boolean.parseBoolean(udpControlEnabledStore.defaultValue));
            }

            public boolean getUdpControlEnabled() {
                return udpControlEnabled;
            }

            public void setUdpControlEnabled(boolean udpControlEnabled) {
                boolean changed = (this.udpControlEnabled != udpControlEnabled);
                this.udpControlEnabled = udpControlEnabled;
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(udpControlEnabledStore.name, udpControlEnabled);
                editor.commit();
                // Needs restart to reinitialize the application (only if value actually changed)
                if (changed) ((MainActivity) context).restartApp();

            }

            public void readUdpTransmit() {
                udpTransmit = prefs.getBoolean(udpTransmitStore.name, Boolean.parseBoolean(udpTransmitStore.defaultValue));
            }

            public boolean getUdpTransmit() {
                return udpTransmit;
            }

            public void setUdpTransmit(boolean udpTransmit) {
                boolean changed = (this.udpTransmit != udpTransmit);
                this.udpTransmit = udpTransmit;
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(udpTransmitStore.name, udpTransmit);
                editor.commit();
                // Needs restart to reinitialize the application (only if value actually changed)
                if (changed) ((MainActivity) context).restartApp();

            }

            //------------------------------------------------------------------------------
            // Auto Review Parameter
            public void readAutoReview() {
                autoReview = prefs.getBoolean(autoReviewStore.name, Boolean.parseBoolean(autoReviewStore.defaultValue));
            }

            public boolean getAutoReview() {
                return autoReview;
            }

            public void setAutoReview(boolean autoReview) {
                this.autoReview = autoReview;
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(autoReviewStore.name, autoReview);
                editor.commit();
                ((MainActivity) this.context).updateParameters();
            }

            // SBS Crop Print Parameter
            public void readSbsCropPrint() {
                sbsCropPrint = prefs.getBoolean(sbsCropPrintStore.name, Boolean.parseBoolean(sbsCropPrintStore.defaultValue));
            }

            public boolean getSbsCropPrint() {
                return sbsCropPrint;
            }

            public void setSbsCropPrint(boolean sbsCropPrint) {
                this.sbsCropPrint = sbsCropPrint;
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(sbsCropPrintStore.name, sbsCropPrint);
                editor.commit();
                ((MainActivity) this.context).updateParameters();
            }

            //------------------------------------------------------------------------------
            // Focus Distance Index Parameter
            public void readFocusDistanceIndex() {
                focusDistanceIndex = prefs.getInt(focusDistanceIndexStore.name, Integer.parseInt(focusDistanceIndexStore.defaultValue));
            }

            public int getFocusDistanceIndex() {
                return focusDistanceIndex;
            }

            public void setFocusDistanceIndex(int focusDistanceIndex) {
                this.focusDistanceIndex = focusDistanceIndex;
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(focusDistanceIndexStore.name, focusDistanceIndex);
                editor.commit(); // synchronous save
                ((MainActivity) this.context).updateParameters();
            }

            //------------------------------------------------------------------------------
            // Exposure Metering Index Parameter
            public void readExposureMeteringIndex() {
                exposureMeteringIndex = prefs.getInt(exposureMeteringIndexStore.name, Integer.parseInt(exposureMeteringIndexStore.defaultValue));
            }

            public int getExposureMeteringIndex() {
                return exposureMeteringIndex;
            }

            public void setExposureMeteringIndex(int exposureMeteringIndex) {
                this.exposureMeteringIndex = exposureMeteringIndex;
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(exposureMeteringIndexStore.name, exposureMeteringIndex);
                editor.commit(); // synchronous save
                ((MainActivity) this.context).updateParameters();
            }

            //------------------------------------------------------------------------------
            // Save LR Parameter
            public void readSaveLr() {
                saveLR = prefs.getBoolean(saveLrStore.name, Boolean.parseBoolean(saveLrStore.defaultValue));
            }

            public boolean getSaveLr() {
                return saveLR;
            }

            public void setSaveLr(boolean saveLR) {
                this.saveLR = saveLR;
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(saveLrStore.name, saveLR);
                editor.commit(); // synchronous save
                ((MainActivity) this.context).updateParameters();
            }

            //------------------------------------------------------------------------------
            // Save Anaglyph Parameter
            public void readSaveAnaglyph() {
                saveAnaglyph = prefs.getBoolean(saveAnaglyphStore.name, Boolean.parseBoolean(saveAnaglyphStore.defaultValue));
            }

            public boolean getSaveAnaglyph() {
                return saveAnaglyph;
            }

            public void setSaveAnaglyph(boolean saveAnaglyph) {
                this.saveAnaglyph = saveAnaglyph;
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(saveAnaglyphStore.name, saveAnaglyph);
                editor.commit(); // synchronous save
                ((MainActivity) this.context).updateParameters();
            }

            // Save File Type Parameter
            public void readSaveFileType() {
                saveFileType = prefs.getString(saveFileTypeStore.name, saveFileTypeStore.defaultValue);
            }

            public String getSaveFileType() {
                return saveFileType;
            }

            public void setSaveFileType(String saveFileType) {
                this.saveFileType = saveFileType;
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(saveFileTypeStore.name, saveFileType);
                editor.commit(); // synchronous save
                ((MainActivity) this.context).updateParameters();
            }

            //------------------------------------------------------------------------------
            // Save File Quality Parameter
            public void readSaveFileQuality() {
                saveFileQuality = prefs.getInt(saveFileQualityStore.name, Integer.parseInt(saveFileQualityStore.defaultValue));
            }

            public int getSaveFileQuality() {
                return saveFileQuality;
            }

            public void setSaveFileQuality(int saveFileQuality) {
                this.saveFileQuality = saveFileQuality;
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(saveFileQualityStore.name, saveFileQuality);
                editor.commit(); // synchronous save
                ((MainActivity) this.context).updateParameters();
            }

            //------------------------------------------------------------------------------
            // Aspect Ratio Index Parameter
            public void readAspectRatioIndex() {
                aspectRatioIndex = prefs.getInt(aspectRatioIndexStore.name, Integer.parseInt(aspectRatioIndexStore.defaultValue));
            }

            public int getAspectRatioIndex() {
                return aspectRatioIndex;
            }

            public void setAspectRatioIndex(int aspectRatioIndex) {
                this.aspectRatioIndex = aspectRatioIndex;
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(aspectRatioIndexStore.name, aspectRatioIndex);
                editor.commit(); // synchronous save
                ((MainActivity) this.context).updateParameters();
            }

            //------------------------------------------------------------------------------
            // Exposure Compensation Index Parameter
            public void readExposureCompensationIndex() {
                exposureCompensationIndex = prefs.getInt(exposureCompensationIndexStore.name, Integer.parseInt(exposureCompensationIndexStore.defaultValue));
            }

            public int getExposureCompensationIndex() {
                return exposureCompensationIndex;
            }

            public void setExposureCompensationIndex(int exposureCompensationIndex) {
                this.exposureCompensationIndex = exposureCompensationIndex;
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(exposureCompensationIndexStore.name, exposureCompensationIndex);
                editor.commit(); // synchronous save
                ((MainActivity) this.context).updateParameters();
            }

            //------------------------------------------------------------------------------
            public int getReceiverPort() {
                return receiverPort;
            }

            // --- PARAM STORE DEFINITIONS ---

            ParamStore parallaxOffsetStore = new ParamStore(
                    "px", "parallaxOffset", "Parallax Offset",
                    "getParallaxOffset", "setParallaxOffset", int.class, "0",
                    "Camera left and right image parallax offset for stereo window placement."
            );

            ParamStore verticalOffsetStore = new ParamStore(
                    "vt", "verticalOffset", "Vertical Offset",
                    "getVerticalOffset", "setVerticalOffset", int.class, "0",
                    "Camera left and right image vertical offset alignment for 3D camera correction"
            );

            ParamStore isPhotoBoothStore = new ParamStore(
                    "pb", "isPhotoBooth", "Photo Booth",
                    "getIsPhotoBooth", "setIsPhotoBooth", boolean.class, "true",
                    "Configures a photo booth camera operation and display."
            );

            ParamStore cameraModeStore = new ParamStore(
                    "mode", "cameraMode", "Application Camera Mode",
                    "getCameraMode", "setCameraMode", int.class, "0",
                    "Configures application camera mode: Basic, Anaglyph, Photo Booth."
            );

            ParamStore isBlankScreenStore = new ParamStore(
                    "bl", "isBlankScreen", "Blank Screen",
                    "getIsBlankScreen", "setIsBlankScreen", boolean.class, "false",
                    "For covering the active display with black."
            );

            ParamStore isSoundOnStore = new ParamStore(
                    "sd", "isSoundOn", "Sound On",
                    "getIsSoundOn", "setIsSoundOn", boolean.class, "true",
                    "Turns on/off the shutter sound"
            );

            ParamStore isAiEditStore = new ParamStore(
                    "ai", "isAiEdit", "AI Edit",
                    "getIsAiEdit", "setIsAiEdit", boolean.class, "false",
                    "Turns on AI Edit mode to launch another application that prompts AI to edit a photo."
            );

            ParamStore title1Store = new ParamStore(
                    "t1", "title1", "Title 1",
                    "getTitle1", "setTitle1", String.class, "",   // "3D Photo Booth",
                    "Photo booth title appearing on the bottom first line"
            );

            ParamStore title2Store = new ParamStore(
                    "t2", "title2", "Title 2",
                    "getTitle2", "setTitle2", String.class, "",
                    "Photo booth title appearing on the bottom second line"
            );

            ParamStore inst1Store = new ParamStore(
                    "i1", "inst1", "Instruction 1",
                    "getInst1", "setInst1", String.class, "",    //  "Look at Camera",
                    "Photo booth instruction appearing on the top first line"
            );

            ParamStore inst2Store = new ParamStore(
                    "i2", "inst2", "Instruction 2",
                    "getInst2", "setInst2", String.class, "",
                    "Photo booth instruction appearing on the top second line"
            );
            
            ParamStore countdownTimerStore = new ParamStore(
                    "ct", "countdownTimer", "Countdown Timer",
                    "getCountdownTimer", "setCountdownTimer", int.class, "3",
                    "Set the camera countdown timer in seconds for shutter release"
            );

            ParamStore isMirrorStore = new ParamStore(
                    "mr", "isMirror", "Mirror",
                    "getIsMirror", "setIsMirror", boolean.class, "true",
                    "In photo booth mode it sets the display to a mirror image."
            );

            ParamStore countDownEnabledStore = new ParamStore(
                    "cd", "countDownEnabled", "Count Down Enabled",
                    "getCountDownEnabled", "setCountDownEnabled", boolean.class, "false",
                    "Enables or disables the countdown timer for the camera."
            );

            ParamStore udpControlEnabledStore = new ParamStore(
                    "uc", "udpControlEnabled", "UDP Control Enabled",
                    "getUdpControlEnabled", "setUdpControlEnabled", boolean.class, "false",
                    "Enables or disables Wi-Fi UDP broadcast message receive and transmit."
            );

            ParamStore udpTransmitStore = new ParamStore(
                    "ut", "udpTransmit", "UDP Transmit",
                    "getUdpTransmit", "setUdpTransmit", boolean.class, "false",
                    "With Wi-Fi broadcast message control enabled, this option mutually exclusive enables transmit or receive only."
            );

            ParamStore autoReviewStore = new ParamStore(
                    "ar", "autoReview", "Auto Review",
                    "getAutoReview", "setAutoReview", boolean.class, "false",
                    "In photo booth mode after a photo capture keep the booth in review mode until changed by the operator. When false the camera is ready to shoot after showing the last photo briefly"
            );

            ParamStore sbsCropPrintStore = new ParamStore(
                    "sbs", "sbsCropPrint", "SBS Crop Print",
                    "getSbsCropPrint", "setSbsCropPrint", boolean.class, "false",
                    "When enabled the SBS photo is center cropped to fit the printer paper size 6x4"
            );

            ParamStore focusDistanceIndexStore = new ParamStore(
                    "fdi", "focusDistanceIndex", "Focus Distance Index",
                    "getFocusDistanceIndex", "setFocusDistanceIndex", int.class, "0",
                    "Set the focus distance index: 0 HyperFocal Focus Distance 1.7 m; 1 Photo Booth Focus Distance 550mm; 2 Macro Focus Distance 100mm; 3 Auto Focus Distance"
            );

            ParamStore exposureMeteringIndexStore = new ParamStore(
                    "emi", "exposureMeteringIndex", "Exposure Metering Index",
                    "getExposureMeteringIndex", "setExposureMeteringIndex", int.class, "0",
                    "Set the exposure metering mode: 0 Frame Average (normal behavior); 1 Center Weighted; 2 Spot Metering"
            );

            ParamStore saveLrStore = new ParamStore(
                    "slr", "saveLR", "Save LR",
                    "getSaveLr", "setSaveLr", boolean.class, "false",
                    "Override cameraMode to enable/disable saving Left and Right image files. When true saves LR images regardless of the camera mode; when false use camera mode to determine LR save."
            );

            ParamStore saveAnaglyphStore = new ParamStore(
                    "san", "saveAnaglyph", "Save Anaglyph",
                    "getSaveAnaglyph", "setSaveAnaglyph", boolean.class, "false",
                    "Override cameraMode to enable/disable saving Anaglyph image files. When true saves anaglyph images regardless of mode; when false use camera mode to determine anaglyph save."
            );

            ParamStore saveFileTypeStore = new ParamStore(
                    "sft", "saveFileType", "Save File Type",
                    "getSaveFileType", "setSaveFileType", String.class, "jpg",
                    "File type extension selected for saving all images: jpg or png"
            );

            ParamStore saveFileQualityStore = new ParamStore(
                    "sfq", "saveFileQuality", "Save File Quality",
                    "getSaveFileQuality", "setSaveFileQuality", int.class, "100",
                    "JPG image quality percent (0-100)"
            );

            ParamStore aspectRatioIndexStore = new ParamStore(
                    "ari", "aspectRatioIndex", "Aspect Ratio Index",
                    "getAspectRatioIndex", "setAspectRatioIndex", int.class, "0",
                    "Selected single lens camera sensor size for given aspect ratios. Indexes into array: 0 DEFAULT (4080x3072), 1 4:3 (4000x3000), 2 4K_16:9 (3840x2160), 3 HD_16:9 (1920x1080), 4 1:1 (3072x3072), 5 8:9 (2560x2880), 6 3:4 (1800x2400). Certain aspect ratios center crop the max default camera sensor size."
            );

            ParamStore exposureCompensationIndexStore = new ParamStore(
                    "eci", "exposureCompensationIndex", "Exposure Compensation Index",
                    "getExposureCompensationIndex", "setExposureCompensationIndex", int.class, "0",
                    "Set the camera exposure compensation index value. Adjusts brightness relative to auto-exposure: negative values darken, positive values brighten the image."
            );

            ParamStore[] paramStores = {parallaxOffsetStore, verticalOffsetStore,
                    isPhotoBoothStore, isBlankScreenStore, isSoundOnStore, isAiEditStore,
                    title1Store, title2Store, inst1Store, inst2Store, countdownTimerStore, isMirrorStore,
                    countDownEnabledStore, udpControlEnabledStore, udpTransmitStore, autoReviewStore, sbsCropPrintStore,
                    focusDistanceIndexStore, exposureMeteringIndexStore, saveLrStore, saveAnaglyphStore,
                    saveFileTypeStore, saveFileQualityStore, aspectRatioIndexStore,
                    exposureCompensationIndexStore};

            // look up parameter by abbreviation and return its current value
            public String findParam(String abbr, String value, boolean set) {
                ParamStore store = null;
                String result;
                result = null;
                for (ParamStore paramStore : paramStores) {
                    if (paramStore.abbr.toLowerCase().equals(abbr)) {
                        store = paramStore;
                        break;
                    }
                }
                if (store != null) {
                    System.out.println("\""+value+"\"");
                    if (set && value.equals("\\")) {
                        result = store.name + " = " + store.defaultValue;
                        callSetter(store, store.defaultValue);
                    } else if (set) {
                        result = store.name + " = " + value;
                        callSetter(store, value);
                    } else {
                        result = store.name + " = " + callGetter(store);
                    }
                    return result;
                }
                return "ERROR: Parameter \""+ abbr + "\" not found";

            }

            /**
             * Retrieves an array of strings, where each string details the parameter's
             * command token, description, current value, and default value.
             * The resulting array is sorted alphabetically by the command token.
             *
             * @return String[] An array of formatted strings, sorted by command token.
             */
            public String[] getParameterDetails() {

                // Use Java 8 Streams to process the array of ParamStore objects
                List<String> detailsList = stream(paramStores)

                        // 1. SORTING STEP: Sort the ParamStore objects based on their abbreviation (command token)
                        .sorted((store1, store2) -> store1.abbr.compareTo(store2.abbr))

                        // 2. MAPPING STEP: Transform the sorted ParamStore objects into formatted strings
                        .map(store -> {
                            // Get the current value using the generic callGetter method
                            Object currentValueObject = callGetter(store);

                            // Convert the current value object to a String for display
                            String currentValue = (currentValueObject != null) ? currentValueObject.toString() : "N/A";

                            // Extract metadata
                            String commandToken = store.abbr; // The command token
                            String description = store.desc;
                            String defaultValue = store.defaultValue;

                            // Format the output string: Token | Description | Current Value | Default Value
                            return String.format(
                                    "/%s=%s | %s | Default: %s",
                                    commandToken,
                                    currentValue,
                                    description,
                                    defaultValue
                            );
                        })
                        // Collect the stream elements into a List
                        .collect(Collectors.toList());

                // Convert the List<String> to a String[] array and return it
                return detailsList.toArray(new String[0]);
            }
        }
