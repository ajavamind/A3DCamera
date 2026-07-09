package com.andymodla.android3dcamera.sketch;


import processing.core.PApplet;
import android.view.KeyEvent;

import com.andymodla.android3dcamera.MainActivity;

class MainHorzMenuBar implements IGui {
  PApplet base;

  MenuKey settingsKey;
  MenuKey leftArrowKey;
  MenuKey downArrowKey;
  MenuKey gridKey;
  MenuKey upArrowKey;
  MenuKey rightArrowKey;
  MenuKey shutterKey;

  MenuKey[] menuKey;

  int numKeys = 7;
  float menuX;
  float menuY;
  float menuWidth;
  float menuHeight;
  float inset = 24;
  float w, h;  // width and height of key area
  float menuTextSize;

  public MainHorzMenuBar(PApplet base, float menuX, float menuY, float menuWidth, float menuHeight) {
    this.base = base;
    this.menuX = menuX; // top left corner of menu bar
    this.menuY = menuY; // top left corner of menu bar
    this.menuWidth = menuWidth;
    this.menuHeight = menuHeight;

    menuTextSize = FONT_SIZE;

    settingsKey = new MenuKey(base, KeyEvent.KEYCODE_J, "\u2699", LARGE_FONT_SIZE, yellow, backTransparent);
    leftArrowKey = new MenuKey(base, KeyEvent.KEYCODE_DPAD_LEFT, LEFT_ARROW, LARGE_FONT_SIZE, yellow, backTransparent);
    downArrowKey = new MenuKey(base, KeyEvent.KEYCODE_DPAD_DOWN, DOWN_ARROW, LARGE_FONT_SIZE, yellow, backTransparent);
    gridKey = new MenuKey(base, KeyEvent.KEYCODE_G, "Grid", menuTextSize, yellow, backTransparent);
    upArrowKey = new MenuKey(base, KeyEvent.KEYCODE_DPAD_UP, UP_ARROW, LARGE_FONT_SIZE, yellow, backTransparent);
    rightArrowKey = new MenuKey(base, KeyEvent.KEYCODE_DPAD_RIGHT, RIGHT_ARROW, LARGE_FONT_SIZE, yellow, backTransparent);
    shutterKey = new MenuKey(base, KeyEvent.KEYCODE_BUTTON_R1, "\u25C9", GIANT_FONT_SIZE, yellow, backTransparent);

    menuKey = new MenuKey[numKeys];

    menuKey[0] = settingsKey;
    menuKey[1] = leftArrowKey;
    menuKey[2] = downArrowKey;
    menuKey[3] = gridKey;
    menuKey[4] = upArrowKey;
    menuKey[5] = rightArrowKey;
    menuKey[6] = shutterKey;

    //h = (float) menuHeight; // height of each key area rectangle
    //w = menuWidth / (float) ((numKeys)); // width of key
    h = MainActivity.HIDDEN_SETTINGS_BUTTON_Y+24;
    w = MainActivity.HIDDEN_SETTINGS_BUTTON_X-20;
    // left side
    for (int i = 0; i < numKeys; i++) {
      menuKey[i].setPosition(menuX + inset + i * w, inset + menuY, w - 2 * inset, h - inset - inset/2, inset);
      menuKey[i].setActive(true);
      menuKey[i].setVisible(true);
    }

  }


  // set all visible
  public void setVisible(boolean visible) {
    for (int i = 0; i < menuKey.length; i++) {
      menuKey[i].setVisible(visible);
    }
  }

  // set all active
  void setActive(boolean active) {
    for (int i = 0; i < menuKey.length; i++) {
      menuKey[i].setActive(active);
    }
  }

  // display all menu bar keys with background
  void display() {
    base.fill(gray); // background color of menu bar area
    base.noStroke();
    //rect(0, 0, menuWidth, menuHeight);
    //base.rect(menuX, menuY, menuWidth, menuHeight);

    for (int i = 0; i < menuKey.length; i++) {
      menuKey[i].draw();
      menuKey[i].setHighlight(false);
    }
  }

  int mousePressed(int x, int y) {
    int mkeyCode = 0;
    int mkey = 0;

    if (x >= menuX && x <= (menuX + menuWidth)) {
      // menu touch control area at either left or right side
      for (int i = 0; i < numKeys; i++) {
        if (menuKey[i].visible && menuKey[i].active) {
          if (x >= menuKey[i].x && x <= (menuKey[i].x + menuKey[i].w) &&
            y >= menuKey[i].y && y <= (menuKey[i].y + menuKey[i].h)) {
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
