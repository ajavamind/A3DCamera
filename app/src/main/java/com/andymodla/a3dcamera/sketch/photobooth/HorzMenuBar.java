package com.andymodla.a3dcamera.sketch.photobooth;


import processing.core.PApplet;

import android.view.KeyEvent;

import com.andymodla.a3dcamera.MainActivity;

class HorzMenuBar implements IGui {
    PApplet pApplet;

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

    public HorzMenuBar() {
    }

    ;

    public HorzMenuBar(PApplet apApplet, float x, float y, float menuWidth, float menuHeight) {
        this.pApplet = apApplet;
        this.menuX = x; // top left corner of menu bar
        this.menuY = y; // top left corner of menu bar
        this.menuY2 = 1080 - menuHeight;
        this.menuWidth = menuWidth;
        this.menuHeight = menuHeight;

        menuTextSize = SMALL_FONT_SIZE;

        // top menu bar
        reviewKey = new MenuKey(pApplet, MainActivity.MODE_KEY, "REVIEW", yellow, backTransparent); // menuTextSize,
        settingsKey = new MenuKey(pApplet, MainActivity.SETTINGS_KEY, "\u2699", yellow, backTransparent); // LARGE_FONT_SIZE,
        optionsKey = new MenuKey(pApplet, MainActivity.BUTTON_Y_KEY, "",  graytransparent, backTransparent);
        functionKey = new MenuKey(pApplet, MainActivity.BUTTON_X_KEY, "PARALLAX\nX", yellow, backTransparent);
        backKey = new MenuKey(pApplet, MainActivity.BUTTON_A_KEY, "BACK\nA", yellow, backTransparent);
        imageModeKey = new MenuKey(pApplet, MainActivity.ANAGLYPH_KEY, "ANAGLYPH", yellow, backTransparent);
        shutterKey = new MenuKey(pApplet, MainActivity.SHUTTER_KEY, "\u25C9", yellow, backTransparent); // GIANT_FONT_SIZE,

        // bottom menu bar
        downArrowKey = new MenuKey(pApplet, KeyEvent.KEYCODE_DPAD_DOWN, "", yellow, backTransparent);
        leftArrowKey = new MenuKey(pApplet, KeyEvent.KEYCODE_DPAD_LEFT, "", yellow, backTransparent);
        minusKey = new MenuKey(pApplet, KeyEvent.KEYCODE_MINUS, "-EV", yellow, backTransparent);
        okKey = new MenuKey(pApplet, MainActivity.BUTTON_B_KEY, "UNLOCK\nEV", yellow, backTransparent);
        plusKey = new MenuKey(pApplet, KeyEvent.KEYCODE_PLUS, "+EV", yellow, backTransparent);
        rightArrowKey = new MenuKey(pApplet, KeyEvent.KEYCODE_DPAD_RIGHT, "", yellow, backTransparent);
        upArrowKey = new MenuKey(pApplet, KeyEvent.KEYCODE_DPAD_UP, "", yellow, backTransparent);

        menuKey = new MenuKey[numKeys];
        //gridKey = new MenuKey(pApplet, KeyEvent.KEYCODE_G, "Grid", menuTextSize, yellow, backTransparent);

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

        //-------------------------------------------------------------
        // Monoscopic initialization
        float kh = MainActivity.HIDDEN_MODE_BUTTON_Y + 16;
        //float kw = MainActivity.HIDDEN_MODE_BUTTON_X - 20;
        float kw = (this.menuWidth)/((float) numKeys /2);
        // top menu bar
        for (int i = 0; i < 7; i++) {
            menuKey[i].setPosition(menuX + i * (inset + kw), inset + menuY, kw - 2 * inset, kh - inset - inset / 2, inset, menuTextSize, 0, false);
            menuKey[i].setActive(true);
            menuKey[i].setVisible(true);
        }
        // bottom menu bar
        for (int i = 7; i < numKeys; i++) {
            int j = i - 7;
            menuKey[i].setPosition(menuX + j * (inset + kw), inset + menuY2, kw - 2 * inset, kh - inset - inset / 2, inset, menuTextSize, 0, false);
            menuKey[i].setActive(true);
            menuKey[i].setVisible(true);
        }
        //-------------------------------------------------------------
        // Stereoscopic initialization
        kh = kh/2;
        kw = (this.menuWidth/(numKeys))-8;
        float sInset = 10;

        // top menu bar
        for (int i = 0; i < 7; i++) {
            menuKey[i].setPosition(menuX + i * (sInset + kw), sInset + menuY+kh, kw - 2 * sInset, kh - sInset - sInset / 2, sInset, menuTextSize, menuWidth/2.0f, true);
            menuKey[i].setActive(true);
            menuKey[i].setVisible(true);
        }
        // bottom menu bar
        for (int i = 7; i < numKeys; i++) {
            int j = i - 7;
            menuKey[i].setPosition( menuX + j * (sInset +  kw), sInset + menuY2, kw - 2 * sInset, kh - sInset - sInset / 2, sInset, menuTextSize, menuWidth/2.0f, true);
            menuKey[i].setActive(true);
            menuKey[i].setVisible(true);
        }

        //-------------------------------------------------------------
        setMenuKeyLabels(MainActivity.FUNCTION_MODE_LIVEVIEW);
    }

    public void updateEvKey(boolean showEv) {
        if (showEv) {
            menuKey[10].setText("LOCK\nEV B");
        } else {
            menuKey[10].setText("UNLOCK\nEV B");
        }
    }

    // update key labels for camera functions
    public void setMenuKeyLabels(int mode) {
        boolean stereoscopic = ((PhotoBooth) pApplet).parameters.isStereoscopeCameraMode();
        //pApplet.println("setMenuKeyLabels " + mode);
        switch (mode) {
            case MainActivity.FUNCTION_MODE_LIVEVIEW:
                menuKey[0].setText("REVIEW");
                menuKey[2].setBackgroundColor(backTransparent);
                menuKey[2].setKeyColor(graytransparent);
                menuKey[2].setHighlight(false);
                menuKey[2].setText("");
                menuKey[2].setActive(false);
                menuKey[2].setVisible(false);
                menuKey[3].setBackgroundColor(backTransparent);
                menuKey[3].setVisible(true);
                menuKey[3].setActive(true);
                menuKey[3].setText("PARALLAX\nX");
                menuKey[4].setText("BACK\nA");
                menuKey[6].setText("\u25C9");
                //if (!stereoscopic) {menuKey[6].setFontSize(GIANT_FONT_SIZE, stereoscopic);}
                menuKey[6].setKeyCode(KeyEvent.KEYCODE_BUTTON_R1);

                menuKey[7].setText("");
                menuKey[7].setActive(false);
                menuKey[7].setVisible(false);
                menuKey[8].setText("");
                menuKey[8].setActive(false);
                menuKey[8].setVisible(false);
                menuKey[9].setKeyColor(yellow);
                menuKey[9].setText("EV-");
                //menuKey[10].setText("UNLOCK\nEV"+" B");
                menuKey[11].setKeyColor(yellow);
                menuKey[11].setText("EV+");
                menuKey[12].setText("");
                menuKey[12].setActive(false);
                menuKey[12].setVisible(false);
                menuKey[13].setText("");
                menuKey[13].setActive(false);
                menuKey[13].setVisible(false);
                break;

            case MainActivity.FUNCTION_MODE_REVIEW:
                menuKey[0].setText("LIVEVIEW");
                menuKey[2].setBackgroundColor(backTransparent);
                menuKey[2].setActive(true);
                menuKey[2].setVisible(true);
                menuKey[2].setKeyColor(yellow);
                menuKey[2].setText("ZOOM\nY");
                menuKey[3].setText("RESET\nZOOM X");
                //menuKey[3].setActive(false);
                //menuKey[3].setVisible(false);
                //menuKey[3].setBackgroundColor(backTransparent);
                menuKey[4].setText("BACK\nA");
                menuKey[6].setText("PRINT");
                menuKey[6].setFontSize(SMALL_FONT_SIZE, stereoscopic);
                menuKey[6].setKeyCode(MainActivity.SHUTTER_KEY);  // decode print in shutter logic

                menuKey[7].setText("FIRST\nPHOTO" + DOWN_ARROW);
                menuKey[7].setActive(true);
                menuKey[7].setVisible(true);
                menuKey[8].setText("PREV\nPHOTO" + LEFT_ARROW);
                menuKey[8].setActive(true);
                menuKey[8].setVisible(true);
                menuKey[9].setKeyColor(graytransparent);
                menuKey[9].setText("AI EDIT-");
                menuKey[10].setText("REVIEW\n" + " B");
                //menuKey[11].setKeyColor(graytransparent);
                menuKey[11].setKeyColor(yellow);
                menuKey[11].setText("SEND\nPHOTO+");
                menuKey[12].setText("NEXT\nPHOTO" + RIGHT_ARROW);
                menuKey[12].setActive(true);
                menuKey[12].setVisible(true);
                menuKey[13].setText("LAST\nPHOTO" + UP_ARROW);
                menuKey[13].setActive(true);
                menuKey[13].setVisible(true);
                break;

            case MainActivity.FUNCTION_MODE_PARALLAX:
                menuKey[2].setBackgroundColor(backTransparent);
                menuKey[2].setActive(true);
                menuKey[3].setBackgroundColor(lighttransparent);
                menuKey[4].setText("BACK\nA");

                menuKey[7].setText("");
                menuKey[8].setText("-4" + LEFT_ARROW);
                menuKey[9].setKeyColor(yellow);
                menuKey[9].setText("-1");
                menuKey[10].setText("PARALLAX\n" + " B");
                menuKey[11].setKeyColor(yellow);
                menuKey[11].setText("+1");
                menuKey[12].setText("+4" + RIGHT_ARROW);
                menuKey[13].setText("");
                break;

            case MainActivity.FUNCTION_MODE_ZOOM:
                menuKey[2].setBackgroundColor(lighttransparent);
                menuKey[3].setBackgroundColor(backTransparent);
                menuKey[4].setText("BACK\nA");
                menuKey[7].setText("SHIFT\nDOWN" + DOWN_ARROW);
                menuKey[8].setText("SHIFT\nLEFT" + LEFT_ARROW);
                menuKey[9].setKeyColor(yellow);
                menuKey[9].setText("ZOOM-");
                menuKey[10].setText("ZOOM\n" + " B");
                menuKey[11].setKeyColor(yellow);
                menuKey[11].setText("ZOOM+");
                menuKey[12].setText("SHIFT\nRIGHT" + RIGHT_ARROW);
                menuKey[13].setText("SHIFT\nUP" + UP_ARROW);
                break;
            default:
                break;
        }
    }

    public void setMenuKeyLabel(int keyIndex, String text) {
        menuKey[keyIndex].setText(text);
    }

    // set menu keys visibility
    public void setVisible(boolean visible) {
        for (int i = 0; i < numKeys; i++) {
            menuKey[i].setVisible(visible);
        }
    }

    // set menu keys activity
    void setActive(boolean active) {
        for (int i = 0; i < numKeys; i++) {
            menuKey[i].setActive(active);
        }
    }

    // display all menu bar keys with background
    void display() {
        pApplet.fill(gray); // background color of menu bar area
        pApplet.noStroke();
        boolean stereoscopic = ((PhotoBooth) pApplet).parameters.isStereoscopeCameraMode();

        for (int i = 0; i < numKeys; i++) {
            menuKey[i].draw(stereoscopic);
            menuKey[i].setHighlight(false);
        }
    }

    boolean isOutside(int x, int y) {
        return (y > menuY + menuHeight && y < menuY2);
    }

    int mousePressed(int x, int y) {
        boolean stereoscopic = ((PhotoBooth) pApplet).parameters.isStereoscopeCameraMode();
        float dimx = 0; float dimw = 0;
        if (stereoscopic) {
            x = (x-176)%((int)menuWidth/2)+176;  // todo refactor
        }
        int mkeyCode = 0;

        if (y < (menuY + menuHeight)) {
            // menu touch control area at either left or right side
            for (int i = 0; i < 7; i++) {
                if (menuKey[i].visible && menuKey[i].active) {
                    if (stereoscopic) {
                        dimx = menuKey[i].stereoDimension.x;
                        dimw = menuKey[i].stereoDimension.w;
                    } else {
                        dimx = menuKey[i].dimension.x;
                        dimw = menuKey[i].dimension.w;
                    }
                    pApplet.println("mousePressed " + i + " x=" + x + " dimx=" + dimx + " dimw=" + dimw);
                    if (x >= dimx && x <= (dimx + dimw)) {
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
                    if (stereoscopic) {
                        dimx = menuKey[i].stereoDimension.x;
                        dimw = menuKey[i].stereoDimension.w;
                    } else {
                        dimx = menuKey[i].dimension.x;
                        dimw = menuKey[i].dimension.w;
                    }
                    if (x >= dimx && x <= (dimx + dimw)) {
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
