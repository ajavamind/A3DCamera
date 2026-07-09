package com.andymodla.android3dcamera.sketch;


import processing.core.PApplet;
import processing.core.PImage;

public class MenuKey implements IGui {
    float x, y, w, h; // location
    float inset;
    String text;
    PImage img;
    int keyColor;
    int keyBackgroundColor;
    int keyCode;
    float fontSize;
    boolean visible = false;
    boolean highlight = false;
    boolean active = true;
    boolean corner = true;
    boolean textOnly = false;
    int value;
    PApplet base;

    /**
     * Constructor
     */
    MenuKey() {
    }

    MenuKey(PApplet base, int keyCode, String text, float fontSize, int keyColor, int keyBackgroundColor) {
        this.base = base;
        this.keyCode = keyCode;
        this.text = text;
        this.keyColor = keyColor;
        this.keyBackgroundColor = keyBackgroundColor;
        this.fontSize = fontSize;
        this.img = null;
    }

    MenuKey(PApplet base, int keyCode, PImage img, int keyColor) {
        this.base = base;
        this.keyCode = keyCode;
        this.img = img;
        this.keyColor = keyColor;
    }

    void setBase(PApplet base) {
        this.base = base;
    }

    void setPosition(float x, float y, float w, float h, float inset) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.inset = inset;
    }

    void setValue(int value) {
        this.value = value;
    }

    int getValue() {
        return value;
    }

    void setHighlight(boolean highlight) {
        this.highlight = highlight;
    }

    void setVisible(boolean visible) {
        this.visible = visible;
    }

    void setActive(boolean active) {
        this.active = active;
    }

    void setCorner(boolean corner) {
        this.corner = corner;
    }

    void setTextOnly(boolean value) {
        this.textOnly = value;
    }

    void draw() {
        if (visible) {
            base.stroke(gray);
            base.strokeWeight(4);
            base.rectMode(base.CORNER);

            // image key
            if (img != null) {
                if (active) {
                    base.fill(white);
                } else {
                    base.fill(gray);
                }
                base.rect(x, y, w, h, inset);
                float ar = (float) img.width/ (float) img.height;
                float ah = h*0.8f;
                base.image(img, x- (ah*ar)/2+w/2, y+ah/8, ah*ar, ah);
                //base.noFill();
                base.stroke(keyColor);
                if (highlight) {
                    base.stroke(255, 255, 0);
                    base.strokeWeight(12);
                    base.noFill();
                    //base.fill(255, 128, 0);
                    base.rect(x, y, w, h);
                }
            } else if (text != null) {
                if (active) {
                    if (highlight) {
                        base.fill(0, 255, 255);
                    } else {
                        base.fill(keyBackgroundColor);
                    }
                } else {
                    base.fill(gray);  // inactive
                }
                if (corner) {
                    base.rect(x, y, w, h, inset);
                }
                base.textSize(fontSize);
                base.noStroke();
                base.noFill();
                if (corner) {
                    base.fill(black);
                } else {
                    base.fill(graytransparent);
                }
                base.textAlign(base.CENTER, base.CENTER);
                base.fill(keyColor);
                base.text(text, x, y, w, h);
            }
        }
    }

    /**
     * @param mx mouse x coordinate
     * @param my mouse y coordinate
     * @return boolean true if mouse in key area
     */
    boolean isPressed(int mx, int my) {
        boolean hit = false;
        if (my >= y && my <= (y + h)
                && mx >= x && mx <= (x + w)) {
            hit = true;
        }
        return hit;
    }

    int getPressed(int mx, int my, int n) {
        int area = 0;
        if (my >= y && my <= (y + h)) {
            for (int i = 1; i <= n; i++) {
                if (mx >= x && mx <= (x + i * w / n)) {
                    area = i;
                    break;
                }
            }
        }
        return area;
    }

    void setText(String text) {
        this.text = text;
    }

    void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }

    int getKeyCode() {
        return keyCode;
    }

    public void setImage(PImage img) {
        this.img = img;
    }
}