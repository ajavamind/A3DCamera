package com.andymodla.a3dcamera.sketch.photobooth;


import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;

public class MenuKey implements IGui {
    GuiDimension stereoDimension;
    GuiDimension dimension;

    String text;
    PImage img;
    int keyColor;
    int keyBackgroundColor;
    int keyCode;

    boolean visible = false;
    boolean highlight = false;
    boolean active = true;
    boolean corner = true;
    boolean textOnly = false;
    int value;

    PApplet pApplet;

    /**
     * Constructor
     */
    MenuKey() {
    }

    MenuKey(PApplet pApplet, int keyCode, String text, int keyColor, int keyBackgroundColor) {
        this.dimension = new GuiDimension();
        this.stereoDimension = new GuiDimension();
        this.pApplet = pApplet;
        this.keyCode = keyCode;
        this.text = text;
        this.keyColor = keyColor;
        this.keyBackgroundColor = keyBackgroundColor;
        this.img = null;
    }

    MenuKey(PApplet pApplet, int keyCode, PImage img, int keyColor) {
        this.dimension = new GuiDimension();
        this.stereoDimension = new GuiDimension();
        this.pApplet = pApplet;
        this.keyCode = keyCode;
        this.img = img;
        this.keyColor = keyColor;
    }

    void setParent(PApplet pApplet) {
        this.pApplet = pApplet;
    }

    void setPosition(float x, float y, float w, float h, float inset, float fontSize,  float xOffset, boolean stereoscopic) {
        if (stereoscopic) {
            this.stereoDimension.xOffset = xOffset;
            this.stereoDimension.x = x;
            this.stereoDimension.y = y;
            this.stereoDimension.w = w;
            this.stereoDimension.h = h;
            this.stereoDimension.inset = inset;
            this.stereoDimension.fontSize = fontSize/2;
        } else {
            this.dimension.xOffset = xOffset;
            this.dimension.x = x;
            this.dimension.y = y;
            this.dimension.w = w;
            this.dimension.h = h;
            this.dimension.inset = inset;
            this.dimension.fontSize = fontSize;
        }
    }

    void setKeyColor(int keyColor) {
        this.keyColor = keyColor;
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

    void setBackgroundColor( int bgc) {
        this.keyBackgroundColor = bgc;
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

    void setFontSize(float fontSize, boolean stereoscopic) {
        if (stereoscopic) {
            this.stereoDimension.fontSize = fontSize/2;
        } else {
            this.dimension.fontSize = fontSize;
        }
    }

    void draw(boolean stereoscopic) {
        if (visible) {
            if (stereoscopic) {
                drawElement(stereoDimension, 0); // left eye view
                drawElement(stereoDimension, -stereoDimension.xOffset-PhotoBooth.STEREO_OFFSET); // right eye view
            } else {
                drawElement(dimension, 0);
            }
        }
    }

    void drawElement(GuiDimension dimension, float offset) {
        pApplet.stroke(gray);
        pApplet.strokeWeight(4);
        pApplet.rectMode(PConstants.CORNER);

        // image key
        if (img != null) {
            if (active) {
                pApplet.fill(white);
            } else {
                pApplet.fill(gray);
            }
            pApplet.rect(dimension.x+dimension.xOffset+offset, dimension.y, dimension.w, dimension.h, dimension.inset);
            float ar = (float) img.width/ (float) img.height;
            float ah = dimension.h*0.8f;
            pApplet.image(img, dimension.x+dimension.xOffset+offset- (ah*ar)/2+dimension.w/2, dimension.y+ah/8, ah*ar, ah);
            //pApplet.noFill();
            pApplet.stroke(keyColor);
            if (highlight) {
                pApplet.stroke(255, 255, 0);
                pApplet.strokeWeight(12);
                pApplet.noFill();
                //pApplet.fill(255, 128, 0);
                pApplet.rect(dimension.x+dimension.xOffset+offset, dimension.y, dimension.w, dimension.h);
            }
        } else if (text != null) {
            if (active) {
                if (highlight) {
                    pApplet.fill(0, 255, 255);
                } else {
                    pApplet.fill(keyBackgroundColor);
                }
            } else {
                pApplet.fill(gray);  // inactive
            }
            if (corner) {
                pApplet.rect(dimension.x+dimension.xOffset+offset, dimension.y, dimension.w, dimension.h, dimension.inset);
            }
            pApplet.textSize(dimension.fontSize);
            pApplet.noStroke();
            pApplet.noFill();
            if (corner) {
                pApplet.fill(black);
            } else {
                pApplet.fill(graytransparent);
            }
            pApplet.textAlign(PConstants.CENTER, PConstants.CENTER);
            pApplet.fill(keyColor);
            pApplet.text(text, dimension.x+dimension.xOffset+offset, dimension.y, dimension.w, dimension.h);
        }
    }

    /**
     * @param mx mouse x coordinate
     * @param my mouse y coordinate
     * @return boolean true if mouse in key area
     */
    boolean isPressed(int mx, int my) {
        boolean hit = false;
        if (my >= dimension.y && my <= (dimension.y + dimension.h)
                && mx >= dimension.x && mx <= (dimension.x + dimension.w)) {
            hit = true;
        }
        return hit;
    }

    int getPressed(int mx, int my, int n) {
        int area = 0;
        if (my >= dimension.y && my <= (dimension.y + dimension.h)) {
            for (int i = 1; i <= n; i++) {
                if (mx >= dimension.x && mx <= (dimension.x + i * dimension.w / n)) {
                    area = i;
                    break;
                }
            }
        }
        return area;
    }

}