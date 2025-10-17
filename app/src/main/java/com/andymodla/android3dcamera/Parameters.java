package com.andymodla.android3dcamera;

import android.content.SharedPreferences;
import android.content.Context;
import android.util.Log;
import java.lang.reflect.Method;



/**
 * Application parameters
 * Copyright 2025 Andy Modla  All Rights Reserved
 */
public class Parameters {
    private final String TAG = "Parameters";
    private SharedPreferences prefs;


    // Stereo Image Alignment parameters
    // same values as StereoPhotoMaker displays after automatic alignment of a reference calibration stereo photo.
    public int parallaxOffset = 0; // 212; // left/right horizontal offset parallax for stereo window placement
    public int verticalOffset = 0; // -12; // left/right camera vertical offset alignment for camera correction

    private class ParamStore {
        String abbr; // abbreviation
        String name; // storage name
        String desc; // description
        String getterName; // Name of the getter method
        String setterName; // Name of the setter method
        Class<?> setterParamType; // Parameter type for the setter

        public ParamStore(String abbr, String name, String desc, String getterName, String setterName, Class<?> setterParamType) {
            this.abbr = abbr;
            this.name = name;
            this.desc = desc;
            this.getterName = getterName;
            this.setterName = setterName;
            this.setterParamType = setterParamType;
        }
    }

    //ParamStore parallaxOffsetStore = new ParamStore("P", "parallaxOffset", "Parallax Offset");
    //ParamStore verticalOffsetStore = new ParamStore("V", "verticalOffset", "Vertical Offset" );
    ParamStore parallaxOffsetStore = new ParamStore(
            "P", "parallaxOffset", "Parallax Offset",
            "getParallaxOffset", "setParallaxOffset", int.class);

    ParamStore verticalOffsetStore = new ParamStore(
            "V", "verticalOffset", "Vertical Offset",
            "getVerticalOffset", "setVerticalOffset", int.class);


    // default constructor
    public Parameters(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    // Generic method to call a getter using its name from a ParamStore object
    private Object callGetter(ParamStore store) {
        try {
            Method method = this.getClass().getMethod(store.getterName);
            return method.invoke(this);
        } catch (Exception e) {
            // Handle potential exceptions like NoSuchMethodException, etc.
            e.printStackTrace();
            return null;
        }
    }

    // Generic method to call a setter using its name from a ParamStore object
    private void callSetter(ParamStore store, Object value) {
        try {
            Method method = this.getClass().getMethod(store.setterName, store.setterParamType);
            method.invoke(this, value);
        } catch (Exception e) {
            // Handle potential exceptions
            e.printStackTrace();
        }
    }

    /**
     * // Example usage from an activity or other class
     *
     * // Assume 'params' is an instance of your Parameters class
     * Parameters params = new Parameters(sharedPreferences);
     *
     * // Get a value dynamically
     * int pOffset = (int) params.callGetter(params.parallaxOffsetStore);
     * System.out.println("Retrieved parallax offset via reflection: " + pOffset);
     *
     * // Set a value dynamically
     * params.callSetter(params.parallaxOffsetStore, 300);
     *
     * // Verify the new value was set correctly
     * int newPOffset = params.getParallaxOffset(); // Or use reflection again
     * System.out.println("Updated parallax offset is: " + newPOffset);
     */

    /**
     * Initialize all parameters
     */
    public void init() {
        initParallaxOffset();
        initVerticalOffset();
    }

    //------------------------------------------------------------------------------

    public void initParallaxOffset() {
        parallaxOffset = prefs.getInt(parallaxOffsetStore.name, 0);
    }

    public int getParallaxOffset() {
        return parallaxOffset;
    }

    public void setParallaxOffset(int parallaxOffset) {
        this.parallaxOffset = parallaxOffset;
        // Save to SharedPreferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(parallaxOffsetStore.name, parallaxOffset);
        editor.apply();
    }

    //------------------------------------------------------------------------------

    public void initVerticalOffset() {
        verticalOffset = prefs.getInt(verticalOffsetStore.name, 0);
    }

    public int getVerticalOffset() {
        return verticalOffset;
    }

    public void setVerticalOffset(int verticalOffset) {
        this.verticalOffset = verticalOffset;
        // Save to SharedPreferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(verticalOffsetStore.name, verticalOffset);
        editor.apply();
    }

}
