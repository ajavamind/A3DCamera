package com.andymodla.android3dcamera;

import android.content.SharedPreferences;
import android.content.Context;
import android.util.Log;

import java.lang.reflect.Method;

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

/**
 * Application parameters
 * Copyright 2025 Andy Modla  All Rights Reserved
 */
public class Parameters {
    private final String TAG = "Parameters";
    private final SharedPreferences prefs;


    // Stereo Image Alignment parameters
    // same values as StereoPhotoMaker displays after automatic alignment of a reference calibration stereo photo.
    public int parallaxOffset = 0; // 212; // left/right horizontal offset parallax for stereo window placement
    public int verticalOffset = 0; // -12; // left/right camera vertical offset alignment for camera correction

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
            else if (store.setterParamType == String.class) method.invoke(this, value.toString());
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
    }

    //------------------------------------------------------------------------------
    public void readParallaxOffset() {
        parallaxOffset = prefs.getInt(parallaxOffsetStore.name, Integer.parseInt(parallaxOffsetStore.defaultValue));
    }

    public int getParallaxOffset() {
        return parallaxOffset;
    }

    public void writeParallaxOffset(int parallaxOffset) {
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

    public void writeVerticalOffset(int verticalOffset) {
        this.verticalOffset = verticalOffset;
        // Save to SharedPreferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(verticalOffsetStore.name, verticalOffset);
        editor.apply(); // asynchronous save
    }

    ParamStore parallaxOffsetStore = new ParamStore(
            "p", "parallaxOffset", "Parallax Offset",
            "getParallaxOffset", "setParallaxOffset", int.class, "0");

    ParamStore verticalOffsetStore = new ParamStore(
            "v", "verticalOffset", "Vertical Offset",
            "getVerticalOffset", "setVerticalOffset", int.class, "0");

    ParamStore[] paramStores = {parallaxOffsetStore, verticalOffsetStore};

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
            if (set) {
                result = store.name + " = " + value;
                callSetter(store, value);
            }
            else {
                result = store.name + " = " +  callGetter(store);
            }
            return result;
        }
        return "ERROR: Parameter not found";
    }

}
