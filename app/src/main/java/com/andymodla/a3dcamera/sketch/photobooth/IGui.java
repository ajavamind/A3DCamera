package com.andymodla.a3dcamera.sketch.photobooth;

import processing.core.PApplet;

// Graphical User Interface constants
public interface IGui {
    // Font sizes
    static final float SMALLER_FONT_SIZE = 24;
    static final float SMALL_FONT_SIZE = 44.0f;
    static final float FONT_SIZE = 64.0f;
    static final float MEDIUM_FONT_SIZE =  72;
    static final float LARGE_FONT_SIZE = 96;
    static final float GIANT_FONT_SIZE = 128;

    // Text display rows (y position on screen)
    static final int TITLE_ROW = 72;
    static final int STATUS_ROW = 72;

    // Processing Java color format ARGB bytes - Alpha, Red, Green, Blue
    static final int black = 0xFF000000;   // black
    static final int gray = 0xFF808080;
    static final int lighttransparent = 0x80c0c0c0;
    static final int graytransparent = 0x80808080;
    static final int darktransparent = 0x80202020; //color(32, 32, 32, 128);
    static final int white = 0xFFFFFFFF; // white
    static final int red = 0xFFFF0000; //color(255, 0, 0);
    static final int aqua = 0xFF800080; //color(128, 0, 128);
    static final int blue = 0xFF0000FF; //color(0, 0, 255);
    static final int backTransparent = 0x40008080;

    static final int lightblue = 0xFF404080; //color(64, 64, 128);
    static final int hintblue = 0xFFC080FF; //color(192, 128, 255);
    static final int darkblue = 0xFF202040; //color(32, 32, 64);
    static final int dimyellow = 0xFFFFCC00; //color(255, 204, 0);
    static final int yellow = 0xFFFFFF00; //color(255, 255, 0);
    static final int lightyellow = 0xFFFFFF80; //color(255, 255, 128);
    static final int green = 0xFF00FF00; // color(0, 255, 0);
    static final int lightgreen = 0xFF00FF80; // color(0, 255, 128);
    static final int cyan = 0xFF00FFFF; // color(0, 255, 255);
    static final int magenta = 0xFFFF00FF; // color(255, 0, 255);
    static final int lightmagenta = 0xFFFF0080; // color(255, 0, 128);
    static final int silver = 0xFFC1C2BA; // color(193, 194, 186);
    static final int brown = 0xFF45423D; //color(69, 66, 61);
    static final int bague = 0xFFB7B48B; //color(183, 180, 139);
    static final int transparentRed = 0x80FF0000;
    static final int offwhite = 0xFFE0E0E0; // color (224);

    // Graphical User Interface Symbols
    static final String INFO_SYMBOL = "\u24D8";
    static final String CIRCLE_PLUS = "\u2295";
    static final String CIRCLE_MINUS = "\u2296";
    static final String CIRCLE_LT = "\u29c0";
    static final String CIRCLE_GT = "\u29c1";
    //static final String LEFT_TRIANGLE = "\u22B2";  // Android
    //static final String RIGHT_TRIANGLE = "\u22B3"; // Android
    static final String LEFT_TRIANGLE = "<";
    static final String RIGHT_TRIANGLE = ">";
    static final String BIG_TRIANGLE_UP = "\u25B3";
    //  ↑ U+2191 Up Arrow

    //↓ U+2193 Down Arrow

    //→ U+2192 Right Arrow

    //← U+2190 Left Arrow
    static final String UP_ARROW = "\u2191";
    static final String DOWN_ARROW = "\u2193";
    static final String LEFT_ARROW = "\u2190";
    static final String RIGHT_ARROW = "\u2192";
    static final String PLAY = "\u25BA";
    static final String STOP = "\u25AA";
    static final String PLUS_MINUS = "||"; //"\u00B1";  //  alternate plus minus 2213
    static final String RESET = "\u21BB";  // loop
    static final String LEFT_ARROW_EXIT = "\u2190";  // Left arrow for exit
    static final String LEFT_ARROWHEAD = "\u02C2";
    static final String RIGHT_ARROWHEAD = "\u02C3";
    static final String CHECK_MARK = "\u2713";
    static final String LEFT_RIGHT_ARROW = "\u2194";
    static final String MICROPHONE = "\u1F3A4";

    // Screen layout parameters for GUI
    static int XBP_CAMERA_DISPLAY_WIDTH = 1280;
    static int XBP_CAMERA_DISPLAY_HEIGHT = 960;

    static int XBP_DISPLAY_WIDTH = 2400;
    static int XBP_DISPLAY_HEIGHT = 1080;

}
