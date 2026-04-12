package com.andymodla.android3dcamera;

/**
 * Application parameters
 * Copyright 2025-2026 Andy Modla  All Rights Reserved
 * Command line based parameter read and set
 *
 * To add parameters use this prompt with gemma4 4B
 * Using Parameters.java as a base pattern, I want to add more parameters as follows:
 * String title1, String title2, String inst1, String inst2, int countdownTimer.
 * For String default use "".
 * For boolean default use false.
 * For integer default use 0. Please update this file to  add these new parameters.
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

//------------------------------------------------------------------------------

class ParamStore {
    String abbr; // abbreviation
    String name; // storage name
    String desc; // description
    String getterName; // Name of the getter method
    String setterName; // Name of the setter method
    Class<?> setterParamType; // Parameter type for the value and defaultValue
    String defaultValue; // default value

    public ParamStore(String abbr, String name, String desc, String getterName, String setterName, Class<?> setterParamType, String defaultValue) {
        this.abbr = abbr;
        this.name = name;
        this.desc = desc;
        this.getterName = getterName;
        this.setterName = setterName;
        this.setterParamType = setterParamType;
        this.defaultValue = String.valueOf(defaultValue);
    }
}

        public class Parameters {
            private final String TAG = "Parameters";
            private final SharedPreferences prefs;
            private final Context context;

            // Stereo Image Alignment parameters
            // same values as StereoPhotoMaker displays after automatic alignment of a reference calibration stereo photo.
            public int parallaxOffset = 0;  // left/right horizontal offset parallax for stereo window placement
            public int verticalOffset = 0;  // left/right camera vertical offset alignment for camera correction
            public boolean isSoundOn = true;
            public boolean isAiEdit = true;

            // photo booth parameters
            public boolean isPhotoBooth = false;
            public boolean anaglyphMode = false; // for photo booth only
            public boolean isMirror = true; // for photo booth only

            String receiverIp = "";  // device IP address to receive URL link to saved photo
            int receiverPort = 9000;  // device port to receive URL link to saved photo

            public boolean isBlankScreen = false;  // for camera

            // NEW PARAMETERS
            public String title1 = "";
            public String title2 = "";
            public String inst1 = "";
            public String inst2 = "";
            public int countdownTimer = 0;


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
                    else if (store.setterParamType == boolean.class)
                        method.invoke(this, Boolean.parseBoolean(value.toString()));
                    else method.invoke(this, value);
                } catch (Exception e) {
                    // Handle potential exceptions
                    Log.e(TAG, "Error Parameters calling setter: " + e.getMessage());
                }
            }

            /**
             * Initialize all parameters
             */
            public void init() {
                // TODO use use ParamStore array to initialize
                readParallaxOffset();
                readVerticalOffset();
                readReceiverIp();
                readIsPhotoBooth();
                readIsBlankScreen();
                readIsSoundOn();
                readIsAiEdit();
                readIsMirror();
                // Initialize new parameters
                readTitle1();
                readTitle2();
                readInst1();
                readInst2();
                readCountdownTimer();
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
                editor.apply(); // asynchronous save
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
                editor.apply(); // asynchronous save
            }

            //------------------------------------------------------------------------------
            public void readReceiverIp() {
                receiverIp = prefs.getString(receiverIpStore.name, receiverIpStore.defaultValue);
            }

            public String getReceiverIp() {
                return receiverIp;
            }

            public void setReceiverIp(String receiverIp) {
                this.receiverIp = receiverIp;
                // Save to SharedPreferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(receiverIpStore.name, receiverIp);
                editor.apply(); // asynchronous save
            }

            //------------------------------------------------------------------------------
            public void readIsPhotoBooth() {
                isPhotoBooth = prefs.getBoolean(isPhotoBoothStore.name, Boolean.parseBoolean(isPhotoBoothStore.defaultValue));
            }

            public boolean getIsPhotoBooth() {
                return isPhotoBooth;
            }

            public void setIsPhotoBooth(boolean isPhotoBooth) {
                boolean needsRestart = false;
                if (this.isPhotoBooth != isPhotoBooth) {
                    needsRestart = true;
                }
                this.isPhotoBooth = isPhotoBooth;
                // Save to SharedPreferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(isPhotoBoothStore.name, isPhotoBooth);
                editor.commit(); // synchronous save: do it now and return
                //editor.apply();   // asynchronous save
                if (needsRestart) {
                    ((MainActivity) context).restartApp();
                }
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
                editor.apply(); // asynchronous save

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
                editor.apply(); // asynchronous save

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
                editor.apply(); // asynchronous save

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
                editor.apply(); // asynchronous save

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
                editor.apply(); // asynchronous save
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
                editor.apply(); // asynchronous save
            }

            public void readInst1() {
                inst1 = prefs.getString(inst1Store.name, inst1Store.defaultValue);
            }

            public String getInst1() {
                return inst1;
            }

            public void setInst1(String inst1) {
                this.inst1 = inst1;
                // Save to SharedPreferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(inst1Store.name, inst1);
                editor.apply(); // asynchronous save
            }

            public void readInst2() {
                inst2 = prefs.getString(inst2Store.name, inst2Store.defaultValue);
            }

            public String getInst2() {
                return inst2;
            }

            public void setInst2(String inst2) {
                this.inst2 = inst2;
                // Save to SharedPreferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(inst2Store.name, inst2);
                editor.apply(); // asynchronous save
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
                editor.apply(); // asynchronous save
            }


            //------------------------------------------------------------------------------
            public int getReceiverPort() {
                return receiverPort;
            }

            // --- PARAM STORE DEFINITIONS ---

            ParamStore parallaxOffsetStore = new ParamStore(
                    "px", "parallaxOffset", "Parallax Offset",
                    "getParallaxOffset", "setParallaxOffset", int.class, "0");

            ParamStore verticalOffsetStore = new ParamStore(
                    "vt", "verticalOffset", "Vertical Offset",
                    "getVerticalOffset", "setVerticalOffset", int.class, "0");

            ParamStore receiverIpStore = new ParamStore(
                    "rip", "receiverIp", "Receiver IP",
                    "getReceiverIp", "setReceiverIp", String.class, "192.168.8.131");

            ParamStore isPhotoBoothStore = new ParamStore(
                    "pb", "isPhotoBooth", "Photo Booth",
                    "getIsPhotoBooth", "setIsPhotoBooth", boolean.class, "false");

            ParamStore isBlankScreenStore = new ParamStore(
                    "bl", "isBlankScreen", "Blank Screen",
                    "getIsBlankScreen", "setIsBlankScreen", boolean.class, "false");

            ParamStore isSoundOnStore = new ParamStore(
                    "sd", "isSoundOn", "Sound On",
                    "getIsSoundOn", "setIsSoundOn", boolean.class, "true");

            ParamStore isAiEditStore = new ParamStore(
                    "aiedit", "isAiEdit", "AI Edit",
                    "getIsAiEdit", "setIsAiEdit", boolean.class, "false");

            ParamStore title1Store = new ParamStore(
                    "t1", "title1", "Title 1",
                    "getTitle1", "setTitle1", String.class, "");

            ParamStore title2Store = new ParamStore(
                    "t2", "title2", "Title 2",
                    "getTitle2", "setTitle2", String.class, "");

            ParamStore inst1Store = new ParamStore(
                    "i1", "inst1", "Instruction 1",
                    "getInst1", "setInst1", String.class, "");

            ParamStore inst2Store = new ParamStore(
                    "i2", "inst2", "Instruction 2",
                    "getInst2", "setInst2", String.class, "");
            
            ParamStore countdownTimerStore = new ParamStore(
                    "ct", "countdownTimer", "Countdown Timer",
                    "getCountdownTimer", "setCountdownTimer", int.class, "0");

            ParamStore isMirrorStore = new ParamStore(
                    "mr", "isMirror", "Mirror",
                    "getIsMirror", "setIsMirror", boolean.class, "true");

            ParamStore[] paramStores = {parallaxOffsetStore, verticalOffsetStore, receiverIpStore,
                    isPhotoBoothStore, isBlankScreenStore, isSoundOnStore, isAiEditStore,
                    title1Store, title2Store, inst1Store, inst2Store, countdownTimerStore, isMirrorStore};

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
