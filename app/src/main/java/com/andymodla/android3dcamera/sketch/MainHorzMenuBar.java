package com.andymodla.android3dcamera.sketch;


import processing.core.PApplet;

import android.view.KeyEvent;

import com.andymodla.android3dcamera.MainActivity;

class MainHorzMenuBar implements IGui {
    PApplet base;

    MenuKey settingsKey;
    MenuKey imageModeKey;
    MenuKey functionKey;
    MenuKey backKey;
    MenuKey optionsKey;
    MenuKey reviewKey;
    MenuKey shutterKey;

    MenuKey minusKey;
    MenuKey downArrowKey;
    MenuKey leftArrowKey;
    MenuKey okKey;
    MenuKey rightArrowKey;
    MenuKey upArrowKey;
    MenuKey plusKey;

    MenuKey gridKey;
    MenuKey[] menuKey;

    int numKeys = 14;
    float menuX;
    float menuY;
    float menuY2;
    float menuWidth;
    float menuHeight;
    float inset = 24;
    float w, h;  // width and height of key area
    float menuTextSize;

    public MainHorzMenuBar(PApplet abase, float x, float y, float menuWidth, float menuHeight) {
        this.base = abase;
        this.menuX = x; // top left corner of menu bar
        this.menuY = y; // top left corner of menu bar
        this.menuY2 = 1080-menuHeight;
        this.menuWidth = menuWidth;
        this.menuHeight = menuHeight;

        menuTextSize = SMALL_FONT_SIZE;

        // top menu bar
        settingsKey = new MenuKey(base, KeyEvent.KEYCODE_BUTTON_L1, "\u2699", LARGE_FONT_SIZE, yellow, backTransparent);
        imageModeKey = new MenuKey(base, KeyEvent.KEYCODE_BUTTON_L2, "SBS/ANA\nLEFT/RIGHT", menuTextSize, yellow, backTransparent);
        functionKey = new MenuKey(base, MainActivity.BUTTON_X_KEY, "FN\nX", menuTextSize, yellow, backTransparent);
        backKey = new MenuKey(base, MainActivity.BUTTON_A_KEY, "Back\nA", menuTextSize, yellow, backTransparent);
        optionsKey = new MenuKey(base, MainActivity.BUTTON_Y_KEY, "OPTIONS\nY", menuTextSize, yellow, backTransparent);
        reviewKey = new MenuKey(base, MainActivity.MODE_KEY, "LIVE VIEW/\nREVIEW", menuTextSize, yellow, backTransparent);
        shutterKey = new MenuKey(base, KeyEvent.KEYCODE_BUTTON_R1, "\u25C9", GIANT_FONT_SIZE, yellow, backTransparent);

        // bottom menu bar
        minusKey = new MenuKey(base, KeyEvent.KEYCODE_MINUS, "EV-", menuTextSize, yellow, backTransparent);
        downArrowKey = new MenuKey(base, KeyEvent.KEYCODE_DPAD_DOWN, DOWN_ARROW, menuTextSize, yellow, backTransparent);
        leftArrowKey = new MenuKey(base, KeyEvent.KEYCODE_DPAD_LEFT, LEFT_ARROW, menuTextSize, yellow, backTransparent);
        okKey = new MenuKey(base, MainActivity.BUTTON_B_KEY, "LIVEVIEW\nEV", menuTextSize, yellow, backTransparent);
        rightArrowKey = new MenuKey(base, KeyEvent.KEYCODE_DPAD_RIGHT, RIGHT_ARROW , menuTextSize, yellow, backTransparent);
        upArrowKey = new MenuKey(base, KeyEvent.KEYCODE_DPAD_UP, UP_ARROW, menuTextSize, yellow, backTransparent);
        plusKey = new MenuKey(base, KeyEvent.KEYCODE_PLUS,  "EV+", menuTextSize, yellow, backTransparent);

        menuKey = new MenuKey[numKeys];
        //gridKey = new MenuKey(base, KeyEvent.KEYCODE_G, "Grid", menuTextSize, yellow, backTransparent);

        menuKey[0] = settingsKey;
        menuKey[1] = imageModeKey;
        menuKey[2] = optionsKey;
        menuKey[3] = functionKey;
        menuKey[4] = backKey;
        menuKey[5] = reviewKey;
        menuKey[6] = shutterKey;

        menuKey[7] = minusKey;
        menuKey[8] = downArrowKey;
        menuKey[9] = leftArrowKey;
        menuKey[10] = okKey;
        menuKey[11] = rightArrowKey;
        menuKey[12] = upArrowKey;
        menuKey[13] = plusKey;

        //h = (float) menuHeight; // height of each key area rectangle
        //w = menuWidth / (float) ((numKeys)); // width of key
        h = MainActivity.HIDDEN_SETTINGS_BUTTON_Y + 24;
        w = MainActivity.HIDDEN_SETTINGS_BUTTON_X - 20;
        // top menu bar
        for (int i = 0; i < 7; i++) {
            menuKey[i].setPosition(menuX + inset + i * w, inset + menuY, w - 2 * inset, h - inset - inset / 2, inset);
            menuKey[i].setActive(true);
            menuKey[i].setVisible(true);
        }
        // bottom menu bar
        for (int i = 7; i < numKeys; i++) {
            int j = i - 7;
            menuKey[i].setPosition(menuX + inset + j * w, inset + menuY2, w - 2 * inset, h - inset - inset / 2, inset);
            menuKey[i].setActive(true);
            menuKey[i].setVisible(true);
        }

    }

    // update key labels for camera functions
    public void setMenuKeyLabels(int mode) {
        switch (mode) {
            case MainActivity.FUNCTION_MODE_LIVEVIEW:
                menuKey[6].setText("\u25C9");
                menuKey[6].setFontSize(GIANT_FONT_SIZE);
                menuKey[7].setText("EV-");
                menuKey[8].setText("");
                menuKey[9].setText("");
                menuKey[10].setText("LIVEVIEW\nEV");
                menuKey[11].setText("");
                menuKey[12].setText("");
                menuKey[13].setText("EV+");
                break;
            case MainActivity.FUNCTION_MODE_REVIEW:
                menuKey[6].setText("PRINT");
                menuKey[6].setFontSize(SMALL_FONT_SIZE);
                menuKey[7].setText("");
                menuKey[8].setText("FIRST\nPHOTO");
                menuKey[9].setText("PREV\nPHOTO");
                menuKey[10].setText("REVIEW");
                menuKey[11].setText("NEXT\nPHOTO");
                menuKey[12].setText("LAST\nPHOTO");
                menuKey[13].setText("");
                break;
            case MainActivity.FUNCTION_MODE_PARALLAX:
                menuKey[7].setText("-8");
                menuKey[8].setText("-4");
                menuKey[9].setText("-1");
                menuKey[10].setText("PARALLAX");
                menuKey[11].setText("+1");
                menuKey[12].setText("+4");
                menuKey[13].setText("+8");
                break;
            case MainActivity.FUNCTION_MODE_ZOOM:
                menuKey[7].setText("ZOOM-");
                menuKey[8].setText("MOVE\nDOWN");
                menuKey[9].setText("MOVE\nLEFT");
                menuKey[10].setText("ZOOM\nRESET");
                menuKey[11].setText("MOVE\nRIGHT");
                menuKey[12].setText("MOVE\nUP");
                menuKey[13].setText("ZOOM+");
                break;
            default:
                break;
        }

    }

    // set all visible
    public void setVisible(boolean visible) {
        for (int i = 0; i < numKeys; i++) {
            menuKey[i].setVisible(visible);
        }
    }

    // set all active
    void setActive(boolean active) {
        for (int i = 0; i < numKeys; i++) {
            menuKey[i].setActive(active);
        }
    }

    // display all menu bar keys with background
    void display() {
        base.fill(gray); // background color of menu bar area
        base.noStroke();
        //rect(0, 0, menuWidth, menuHeight);
        //base.rect(menuX, menuY, menuWidth, menuHeight);

        for (int i = 0; i < numKeys; i++) {
            menuKey[i].draw();
            menuKey[i].setHighlight(false);
        }
    }

    boolean isOutside(int x, int y) {
        return ( y > menuY+menuHeight && y < menuY2);
    }

    int mousePressed(int x, int y) {
        int mkeyCode = 0;

        if (y < (menuY+menuHeight)) {
            // menu touch control area at either left or right side
            for (int i = 0; i < 7; i++) {
                if (menuKey[i].visible && menuKey[i].active) {
                    if (x >= menuKey[i].x && x <= (menuKey[i].x + menuKey[i].w)
                            //&& y >= menuKey[i].y && y <= (menuKey[i].y + menuKey[i].h)
                    ) {
                        mkeyCode = menuKey[i].keyCode;
                        menuKey[i].setHighlight(true);
                        break;
                    }
                }
            }
        } else if (y >= menuY2) {
            // menu touch control area at either left or right side
            for (int i = 7; i < numKeys; i++) {
                if (menuKey[i].visible && menuKey[i].active) {
                    if (x >= menuKey[i].x && x <= (menuKey[i].x + menuKey[i].w)
                            //&& y >= menuKey[i].y && y <= (menuKey[i].y + menuKey[i].h)
                    ) {
                        mkeyCode = menuKey[i].keyCode;
                        menuKey[i].setHighlight(true);
                        break;
                    }
                }
            }

        }
        return mkeyCode;

    }
}
