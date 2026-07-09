package com.andymodla.android3dcamera.sketch;

import processing.core.PApplet;

// Graphical User Interface
public interface IGui {
    static final float SMALLER_FONT_SIZE = 24;
    static final float SMALL_FONT_SIZE = 44.0f;
    static final float FONT_SIZE = 64.0f;
    static final float MEDIUM_FONT_SIZE =  72;
    static final float LARGE_FONT_SIZE = 96;
    static final float GIANT_FONT_SIZE = 128;

    // color is ARGB bytes - Alpha, Red, Blue, Green
    static final int black = 0xFF000000;   // black
    static final int gray = 0xFF808080;
    static final int graytransparent = 0x80808080;
    static final int darktransparent = 0x80202020; //color(32, 32, 32, 128);
    static final int white = 0xFFFFFFFF; // white
    static final int red = 0xFFFF0000; //color(255, 0, 0);
    static final int aqua = 0xFF800080; //color(128, 0, 128);
    static final int blue = 0xFF0000FF; //color(0, 0, 255);
    static final int backTransparent = 0x40008080;

    static final int lightblue = 0xFF404080; //color(64, 64, 128);
    static final int darkblue = 0xFF202040; //color(32, 32, 64);
    static final int dimyellow = 0xFFFFCC00; //color(255, 204, 0);
    static final int yellow = 0xFFFFFF00; //color(255, 255, 0);
    static final int green = 0xFF00FF00; // color(0, 255, 0);
    static final int cyan = 0xFF00FFFF; // color(0, 255, 255);
    static final int magenta = 0xFFFF00FF; // color(255, 0, 255);
    //static final int silver = color(193, 194, 186);
    //static final int brown = color(69, 66, 61);
    //static final int bague = color(183, 180, 139);
    static final int transparentRed = 0x80FF0000;

//int black = 0;
//int white = 255;
//int gray = 128;
//int yellow = color(255, 255, 0);
//int green = color(0, 255, 0);
//int blue = color(0, 0, 255);
//int red = color(255, 0, 0);
//int cyan = color(0, 255, 255);
//int magenta = color(255, 0, 255);

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

}

//// The GUI assumes the sketch screen is at (0,0)
//class Gui {
//    PApplet base;  // base sketch reference
//    MenuBarVertical menuBar;  // right side menu
//
//    // information zone touch coordinates
//    // screen boundaries for click zone use
//    float menuWidth; // menu width
//    float menuHeight; // menu height
//    float menuX;
//    float menuY;
//
//    Gui() {
//    }
//
//    void createGui(PApplet base, int menuX, int menuY, int menuWidth, int menuHeight) {
//        if (DEBUG) println("createGui()");
//        this.base = base;
//        this.menuWidth = menuWidth;
//        this.menuHeight = menuHeight;
//        this.menuX = menuX;
//        this.menuY = menuY;
//
//        menuBar = new MenuBarVertical(base, menuX, menuY, menuWidth, menuHeight);
//        menuBar.setVisible(true);
//        menuBar.setActive(true);
//    }
//
//    void displayMenuBar() {
//        //if (DEBUG) println("displayMenuBar()");
//        menuBar.display();
//    }
//
//    // toggle shutter and print
//    void togglePrint(int printit) {
//        menuBar.togglePrint(printit);
//    }
//
//    // DropDownList class
//    class DropDownList implements IGui {
//        PApplet base;
//        String[] items;
//        int[] itemValues;
//        int itemValue;
//        float x, y, w, h;
//        boolean visible = false;
//        int highlight = -1;
//        float itemHeight;
//        int keyColor;
//        int bgColor;
//        int textColor;
//        int borderColor;
//        float fontSize;
//        boolean exclusive = true;
//
//        /**
//         * Constructor
//         */
//        DropDownList(PApplet base, String[] items, int[] itemValues, int itemValue, float x, float y, float w, float fontSize) {
//            this.base = base;
//            this.items = items;
//            this.itemValues = itemValues;
//            this.itemValue = itemValue;
//            this.x = x;
//            this.y = y;
//            this.w = w * 1.1;  // slightly wider for text
//            this.fontSize = fontSize;
//            this.itemHeight = fontSize * 1.2;
//            this.h = items.length * itemHeight;
//            this.keyColor = black;
//            this.bgColor = white;
//            this.textColor = black;
//            this.borderColor = gray;
//        }
//
//        void show() {
//            visible = true;
//        }
//
//        void hide() {
//            visible = false;
//        }
//
//        void display() {
//            if (!visible) return;
//            base.stroke(borderColor);
//            base.fill(bgColor);
//            base.rect(x, y, w, h, 8);
//            for (int i = 0; i < items.length; i++) {
//                float iy = y + i * itemHeight;
//                int value = itemValues[i];
//                if ((itemValue & value) != 0) {
//                    base.fill(lightblue);
//                    base.rect(x, iy, w, itemHeight);
//                }
//                base.fill(textColor);
//                base.textAlign(base.LEFT, base.CENTER);
//                base.textSize(fontSize);
//                base.text(items[i], x + 10, iy + itemHeight/2);
//            }
//        }
//
//        // Returns index if selected, -1 otherwise
//        // sets option values
//        int isPressed(int mx, int my) {
//            if (!visible) return -1;
//            if (mx >= x && mx <= x + w && my >= y && my <= y + h) {
//                int idx = int((my - y) / itemHeight);
//                if (idx >= 0 && idx < items.length) {
//                    if (exclusive) {
//                        itemValue = 0;
//                        itemValue = itemValue | itemValues[idx];
//                    } else {
//                        itemValue = itemValue ^ itemValues[idx];
//                    }
//                    return idx;
//                }
//            }
//            return -1;
//        }
//
//        int getItemValue() {
//            return itemValue;
//        }
//    }
//}
//
