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
        reviewKey = new MenuKey(base, MainActivity.MODE_KEY, "REVIEW", menuTextSize, yellow, backTransparent);
        settingsKey = new MenuKey(base, MainActivity.SETTINGS_KEY, "\u2699", LARGE_FONT_SIZE, yellow, backTransparent);
        optionsKey = new MenuKey(base, MainActivity.BUTTON_Y_KEY, "", menuTextSize, graytransparent, backTransparent);
        functionKey = new MenuKey(base, MainActivity.BUTTON_X_KEY, "PARALLAX\nX", menuTextSize, yellow, backTransparent);
        backKey = new MenuKey(base, MainActivity.BUTTON_A_KEY, "BACK\nA", menuTextSize, yellow, backTransparent);
        imageModeKey = new MenuKey(base, MainActivity.ANAGLYPH_KEY, "ANAGLYPH", menuTextSize, yellow, backTransparent);
        shutterKey = new MenuKey(base, MainActivity.SHUTTER_KEY, "\u25C9", GIANT_FONT_SIZE, yellow, backTransparent);

        // bottom menu bar
        downArrowKey = new MenuKey(base, KeyEvent.KEYCODE_DPAD_DOWN, "", menuTextSize, yellow, backTransparent);
        leftArrowKey = new MenuKey(base, KeyEvent.KEYCODE_DPAD_LEFT, "", menuTextSize, yellow, backTransparent);
        minusKey = new MenuKey(base, KeyEvent.KEYCODE_MINUS, "-EV", menuTextSize, yellow, backTransparent);
        okKey = new MenuKey(base, MainActivity.BUTTON_B_KEY, "LIVEVIEW\nEV", menuTextSize, yellow, backTransparent);
        plusKey = new MenuKey(base, KeyEvent.KEYCODE_PLUS,  "+EV", menuTextSize, yellow, backTransparent);
        rightArrowKey = new MenuKey(base, KeyEvent.KEYCODE_DPAD_RIGHT, "" , menuTextSize, yellow, backTransparent);
        upArrowKey = new MenuKey(base, KeyEvent.KEYCODE_DPAD_UP, "", menuTextSize, yellow, backTransparent);

        menuKey = new MenuKey[numKeys];
        //gridKey = new MenuKey(base, KeyEvent.KEYCODE_G, "Grid", menuTextSize, yellow, backTransparent);

        menuKey[0] = reviewKey;
        menuKey[1] = settingsKey;
        menuKey[2] = optionsKey;
        menuKey[3] = functionKey;
        menuKey[4] = backKey;
        menuKey[5] = imageModeKey;
        menuKey[6] = shutterKey;

        menuKey[7] = downArrowKey;
        menuKey[8] = leftArrowKey;
        menuKey[9] = minusKey;
        menuKey[10] = okKey;
        menuKey[11] = plusKey;
        menuKey[12] = rightArrowKey;
        menuKey[13] = upArrowKey;

        //h = (float) menuHeight; // height of each key area rectangle
        //w = menuWidth / (float) ((numKeys)); // width of key
        h = MainActivity.HIDDEN_MODE_BUTTON_Y + 24;
        w = MainActivity.HIDDEN_MODE_BUTTON_X - 20;
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
        setMenuKeyLabels(MainActivity.FUNCTION_MODE_LIVEVIEW);
    }

    // update key labels for camera functions
    public void setMenuKeyLabels(int mode) {
        base.println("setMenuKeyLabels "+mode);
        switch (mode) {
            case MainActivity.FUNCTION_MODE_LIVEVIEW:
                menuKey[2].setBackgroundColor(backTransparent);
                menuKey[2].setKeyColor(graytransparent);
                menuKey[2].setHighlight(false);
                menuKey[2].setText("");
                menuKey[2].setActive(false);
                menuKey[2].setVisible(false);
                menuKey[3].setBackgroundColor(backTransparent);
                menuKey[3].setVisible(true);
                menuKey[3].setActive(true);
                menuKey[4].setText("BACK\nA");
                menuKey[0].setText("REVIEW");
                menuKey[6].setText("\u25C9");
                menuKey[6].setFontSize(GIANT_FONT_SIZE);
                menuKey[6].setKeyCode(KeyEvent.KEYCODE_BUTTON_R1);

                menuKey[7].setText("");
                menuKey[7].setActive(false);
                menuKey[7].setVisible(false);
                menuKey[8].setText("");
                menuKey[8].setActive(false);
                menuKey[8].setVisible(false);
                menuKey[9].setKeyColor(yellow);
                menuKey[9].setText("-EV");
                menuKey[10].setText("LIVEVIEW\nEV"+" B");
                menuKey[11].setKeyColor(yellow);
                menuKey[11].setText("+EV");
                menuKey[12].setText("");
                menuKey[12].setActive(false);
                menuKey[12].setVisible(false);
                menuKey[13].setText("");
                menuKey[13].setActive(false);
                menuKey[13].setVisible(false);
                break;

            case MainActivity.FUNCTION_MODE_REVIEW:
                menuKey[2].setBackgroundColor(backTransparent);
                menuKey[2].setActive(true);
                menuKey[2].setVisible(true);
                menuKey[2].setKeyColor(yellow);
                menuKey[2].setText("ZOOM\nY");
                menuKey[3].setActive(false);
                menuKey[3].setVisible(false);
                menuKey[3].setBackgroundColor(backTransparent);
                menuKey[4].setText("BACK\nA");
                menuKey[0].setText("LIVEVIEW");
                menuKey[6].setText("PRINT");
                menuKey[6].setFontSize(SMALL_FONT_SIZE);
                menuKey[6].setKeyCode(MainActivity.SHUTTER_KEY);  // decode print in shutter logic

                menuKey[7].setText("FIRST\nPHOTO"+DOWN_ARROW);
                menuKey[7].setActive(true);
                menuKey[7].setVisible(true);
                menuKey[8].setText("PREV\nPHOTO"+LEFT_ARROW);
                menuKey[8].setActive(true);
                menuKey[8].setVisible(true);
                menuKey[9].setKeyColor(graytransparent);
                menuKey[9].setText("AI EDIT");
                menuKey[10].setText("REVIEW\n"+" B");
                menuKey[11].setKeyColor(graytransparent);
                menuKey[11].setText("SHARE");
                menuKey[12].setText("NEXT\nPHOTO"+RIGHT_ARROW);
                menuKey[12].setActive(true);
                menuKey[12].setVisible(true);
                menuKey[13].setText("LAST\nPHOTO"+UP_ARROW);
                menuKey[13].setActive(true);
                menuKey[13].setVisible(true);
                break;

            case MainActivity.FUNCTION_MODE_PARALLAX:
                menuKey[2].setBackgroundColor(backTransparent);
                menuKey[2].setActive(true);
                menuKey[3].setBackgroundColor(lighttransparent);
                menuKey[4].setText("BACK\nA");

                menuKey[7].setText("");
                menuKey[8].setText("-4"+LEFT_ARROW);
                menuKey[9].setKeyColor(yellow);
                menuKey[9].setText("-1");
                menuKey[10].setText("PARALLAX\n"+" B");
                menuKey[11].setKeyColor(yellow);
                menuKey[11].setText("+1");
                menuKey[12].setText("+4"+RIGHT_ARROW);
                menuKey[13].setText("");
                break;

            case MainActivity.FUNCTION_MODE_ZOOM:
                menuKey[2].setBackgroundColor(lighttransparent);
                menuKey[3].setBackgroundColor(backTransparent);
                menuKey[4].setText("BACK\nA");

                menuKey[7].setText("SHIFT\nDOWN"+DOWN_ARROW);
                menuKey[8].setText("SHIFT\nLEFT"+LEFT_ARROW);
                menuKey[9].setKeyColor(yellow);
                menuKey[9].setText("ZOOM-");
                menuKey[10].setText("ZOOM\n"+" B");
                menuKey[11].setKeyColor(yellow);
                menuKey[11].setText("ZOOM+");
                menuKey[12].setText("SHIFT\nRIGHT"+RIGHT_ARROW);
                menuKey[13].setText("SHIFT\nUP"+UP_ARROW);
                break;
            default:
                break;
        }
    }

    public void setMenuKeyLabel(int keyIndex, String text) {
        menuKey[keyIndex].setText(text);
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
