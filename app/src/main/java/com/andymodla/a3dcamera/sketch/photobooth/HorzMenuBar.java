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

        // top menu bar
        reviewKey = new MenuKey(pApplet, MainActivity.MODE_KEY, REVIEW_LABEL, yellow, backTransparent); // menuTextSize,
        settingsKey = new MenuKey(pApplet, MainActivity.SETTINGS_KEY, SETTINGS_LABEL, yellow, backTransparent); // LARGE_FONT_SIZE,
        optionsKey = new MenuKey(pApplet, MainActivity.BUTTON_Y_KEY, ZOOM_LABEL+"\n"+Y_KEY,  yellow, backTransparent);
        functionKey = new MenuKey(pApplet, MainActivity.BUTTON_X_KEY, PARALLAX_LABEL+"\n"+X_KEY, yellow, backTransparent);
        backKey = new MenuKey(pApplet, MainActivity.BUTTON_A_KEY, BACK_LABEL+"\n"+A_KEY, yellow, backTransparent);
        imageModeKey = new MenuKey(pApplet, MainActivity.ANAGLYPH_KEY, ANAGLYPH_LABEL, yellow, backTransparent);
        shutterKey = new MenuKey(pApplet, MainActivity.SHUTTER_KEY, SHUTTER_LABEL, yellow, backTransparent); // GIANT_FONT_SIZE,

        // bottom menu bar
        downArrowKey = new MenuKey(pApplet, KeyEvent.KEYCODE_DPAD_DOWN, "", yellow, backTransparent);
        leftArrowKey = new MenuKey(pApplet, KeyEvent.KEYCODE_DPAD_LEFT, "", yellow, backTransparent);
        minusKey = new MenuKey(pApplet, KeyEvent.KEYCODE_MINUS, EV_LABEL+MINUS_KEY, yellow, backTransparent);
        okKey = new MenuKey(pApplet, MainActivity.BUTTON_B_KEY, UNLOCK_LABEL+"\n" + EV_LABEL +B_KEY, yellow, backTransparent);
        plusKey = new MenuKey(pApplet, KeyEvent.KEYCODE_PLUS, IGui.EV_LABEL+PLUS_KEY, yellow, backTransparent);
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
        float kw = (this.menuWidth)/((float) numKeys /2);
        menuTextSize = FONT_SIZE;
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
        float sw = (this.menuWidth/(numKeys+2));  // spacing for center of display
        kw = (this.menuWidth/(numKeys+7))+8;
        float sInset = 6;
        menuTextSize = SMALL_FONT_SIZE;
        // top menu bar
        for (int i = 0; i < 7; i++) {
            menuKey[i].setPosition(menuX + sw + i * (sInset + kw), sInset + menuY+kh, kw - 2 * sInset, kh - sInset - sInset / 2, sInset, menuTextSize, menuWidth/2.0f, true);
            menuKey[i].setActive(true);
            menuKey[i].setVisible(true);
        }
        // bottom menu bar
        for (int i = 7; i < numKeys; i++) {
            int j = i - 7;
            menuKey[i].setPosition( menuX + sw + j * (sInset +  kw), sInset + menuY2, kw - 2 * sInset, kh - sInset - sInset / 2, sInset, menuTextSize, menuWidth/2.0f, true);
            menuKey[i].setActive(true);
            menuKey[i].setVisible(true);
        }

        //-------------------------------------------------------------
        setMenuKeyLabels(MainActivity.FUNCTION_MODE_LIVEVIEW, MainActivity.LIVE_VIEW_STATE);
    }

    public void updateEvKey(boolean showEv) {
        if (showEv) {
            menuKey[10].setText("LOCK\nEV B");
        } else {
            menuKey[10].setText("UNLOCK\nEV B");
        }
    }

    // update key labels for camera functions
    public void setMenuKeyLabels(int functionMode, int state) {
        boolean stereoscopic = ((PhotoBooth) pApplet).parameters.isStereoscopeCameraMode();
        //pApplet.println("setMenuKeyLabels " + mode);
        switch (functionMode) {
            case MainActivity.FUNCTION_MODE_LIVEVIEW:
                menuKey[0].setText(REVIEW_LABEL);
                menuKey[2].setBackgroundColor(backTransparent);
                menuKey[2].setHighlight(false);
                menuKey[2].setText(ZOOM_LABEL+"\n"+Y_KEY);
                menuKey[3].setBackgroundColor(backTransparent);
                menuKey[3].setVisible(true);
                menuKey[3].setActive(true);
                menuKey[3].setText(PARALLAX_LABEL+"\n"+X_KEY);
                menuKey[4].setText(BACK_LABEL+"\n"+A_KEY);
                menuKey[6].setText(SHUTTER_LABEL);
                //if (!stereoscopic) {menuKey[6].setFontSize(GIANT_FONT_SIZE, stereoscopic);}
                menuKey[6].setKeyCode(KeyEvent.KEYCODE_BUTTON_R1);

                menuKey[7].setText("");
                menuKey[7].setActive(false);
                menuKey[7].setVisible(false);
                menuKey[8].setText("");
                menuKey[8].setActive(false);
                menuKey[8].setVisible(false);
                menuKey[9].setKeyColor(yellow);
                menuKey[9].setText(EV_LABEL+MINUS_KEY);
                menuKey[10].setText(UNLOCK_LABEL+ "\n"+ EV_LABEL + " " + B_KEY);
                menuKey[11].setKeyColor(yellow);
                menuKey[11].setText(EV_LABEL+PLUS_KEY);
                menuKey[12].setText("");
                menuKey[12].setActive(false);
                menuKey[12].setVisible(false);
                menuKey[13].setText("");
                menuKey[13].setActive(false);
                menuKey[13].setVisible(false);
                break;

            case MainActivity.FUNCTION_MODE_REVIEW:
                menuKey[0].setText(LIVEVIEW_LABEL);
                menuKey[2].setBackgroundColor(backTransparent);
                menuKey[2].setActive(true);
                menuKey[2].setVisible(true);
                menuKey[2].setKeyColor(yellow);
                menuKey[2].setText(ZOOM_LABEL+"\n"+Y_KEY);
                menuKey[3].setText(PARALLAX_LABEL+"\n"+X_KEY);
                menuKey[3].setBackgroundColor(backTransparent);
                menuKey[4].setText(BACK_LABEL+"\n"+A_KEY);
                menuKey[6].setText(PRINT_LABEL);
                menuKey[6].setFontSize(SMALL_FONT_SIZE, stereoscopic);
                menuKey[6].setKeyCode(MainActivity.SHUTTER_KEY);  // decode print in shutter logic

                menuKey[7].setText(FIRST_LABEL + "\n" + PHOTO_LABEL + DOWN_ARROW_KEY);
                menuKey[7].setActive(true);
                menuKey[7].setVisible(true);
                menuKey[8].setText(PREVIOUS_LABEL + "\n" + PHOTO_LABEL + LEFT_ARROW_KEY);
                menuKey[8].setActive(true);
                menuKey[8].setVisible(true);
                menuKey[9].setKeyColor(graytransparent);
                menuKey[9].setText(AIEDIT_LABEL + MINUS_KEY);
                menuKey[10].setText(REVIEW_LABEL + "\n" + B_KEY);
                menuKey[11].setKeyColor(yellow);
                menuKey[11].setText(SEND_LABEL + "\n" + PHOTO_LABEL + PLUS_KEY);
                menuKey[12].setText(NEXT_LABEL + "\n" + PHOTO_LABEL + RIGHT_ARROW_KEY);
                menuKey[12].setActive(true);
                menuKey[12].setVisible(true);
                menuKey[13].setText(LAST_LABEL + "\n" + PHOTO_LABEL + UP_ARROW_KEY);
                menuKey[13].setActive(true);
                menuKey[13].setVisible(true);
                break;

            case MainActivity.FUNCTION_MODE_PARALLAX:
                menuKey[2].setBackgroundColor(backTransparent);
                menuKey[2].setActive(true);
                menuKey[3].setBackgroundColor(lighttransparent);
                menuKey[4].setText("BACK\nA");

                menuKey[7].setText("");
                menuKey[8].setActive(true);
                menuKey[8].setVisible(true);
                menuKey[8].setText("-4" + LEFT_ARROW_KEY);
                menuKey[9].setKeyColor(yellow);
                menuKey[9].setText("-1");
                menuKey[10].setText(PARALLAX_LABEL + "\n" + B_KEY);
                menuKey[11].setKeyColor(yellow);
                menuKey[11].setText("+1");
                menuKey[12].setActive(true);
                menuKey[12].setVisible(true);
                menuKey[12].setText("+4" + RIGHT_ARROW_KEY);
                menuKey[13].setText("");
                break;

            case MainActivity.FUNCTION_MODE_MAGNIFY:
                menuKey[2].setBackgroundColor(lighttransparent);
                menuKey[3].setBackgroundColor(backTransparent);
                menuKey[3].setText(RESET_LABEL+"\n"+ZOOM_LABEL+" " +X_KEY);
                menuKey[4].setText(BACK_LABEL+"\n"+A_KEY);
                menuKey[9].setKeyColor(yellow);
                menuKey[9].setText(ZOOM_LABEL+"\n"+MINUS_KEY);
                menuKey[10].setText(ZOOM_LABEL+"\n"+B_KEY);
                menuKey[11].setKeyColor(yellow);
                menuKey[11].setText(ZOOM_LABEL+"\n"+PLUS_KEY);
                if (state == MainActivity.REVIEW_PHOTO_STATE) {
                    menuKey[7].setText(SHIFT_LABEL + "\n" + DOWN_LABEL + DOWN_ARROW_KEY);
                    menuKey[8].setText(SHIFT_LABEL + "\n" + LEFT_LABEL + LEFT_ARROW_KEY);
                    menuKey[12].setText(SHIFT_LABEL + "\n" + RIGHT_LABEL + RIGHT_ARROW_KEY);
                    menuKey[13].setText(SHIFT_LABEL + "\n" + UP_LABEL + UP_ARROW_KEY);
                } else {
                    menuKey[7].setText("");
                    menuKey[8].setText("");
                    menuKey[12].setText("");
                    menuKey[13].setText("");

                }
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
