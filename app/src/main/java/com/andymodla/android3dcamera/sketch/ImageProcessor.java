package com.andymodla.android3dcamera.sketch;

import static android.graphics.Color.blue;
import static android.graphics.Color.green;
import static android.graphics.Color.red;
import static processing.core.PApplet.DEBUG;
import static processing.core.PApplet.floor;
import static processing.core.PApplet.println;
import static processing.core.PConstants.GRAY;
import static processing.core.PConstants.INVERT;
import static processing.core.PConstants.POSTERIZE;
import static processing.core.PConstants.RGB;
import static processing.core.PConstants.THRESHOLD;

import android.graphics.BitmapFactory;
import android.view.KeyEvent;
import android.graphics.Bitmap;
import com.andymodla.android3dcamera.DisplayMode;
import com.andymodla.android3dcamera.Media;
import com.andymodla.android3dcamera.camera.Camera3D;
import com.andymodla.android3dcamera.Parameters;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PGraphics;
import processing.event.MouseEvent;
import processing.opengl.PGL;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;

// Apply filters to the live view

class ImageProcessor {
    private static final int FONT_SIZE = 24;
    private static final boolean DEBUG = true;
    PApplet pApplet;

    // 6x4 paper used with Canon CP1300
    int PRINT_WIDTH = 6; //10; //14; //4  // inches
    int PRINT_HEIGHT = 4; //8; //11; //3; // inches
    int DISPLAY_SHRINK_FACTOR = 1;
    String PRINT_AR_SUFFIX = "_" + PApplet.str(PRINT_WIDTH) + "x" + PApplet.str(PRINT_HEIGHT);
    String SAVE_IMAGE_SUFFIX = "";
    int saveFileCounter = 1;
    float PRINT_AR = (float)PRINT_WIDTH/(float)PRINT_HEIGHT;
    int PRINTER_DPI = 300;  // CP1300 dots per inch
    int PRINT_PIXEL_WIDTH = PRINTER_DPI*PRINT_WIDTH;
    int PRINT_PIXEL_HEIGHT = PRINTER_DPI*PRINT_HEIGHT;
    int fontHeight = FONT_SIZE;

    // Processing filters available are:
    String[] filters = {"NONE", "THRESHOLD", "GRAY", "OPAQUE", "INVERT", "POSTERIZE", "BLUR", "ERODE", "DILATE"};
    // Graphical User Interface
    int black = pApplet.color(0);   // black
    int gray = pApplet.color(128);
    int graytransparent = pApplet.color(128, 128, 128, 128);
    int darktransparent = pApplet.color(32, 32, 32, 128);
    int white = pApplet.color(255); // white
    int red = pApplet.color(255, 0, 0);
    int aqua = pApplet.color(128, 0, 128);
    int lightblue = pApplet.color(64, 64, 128);
    int darkblue = pApplet.color(32, 32, 64);
    int blue = pApplet.color(0, 0, 255);
    int hintblue = pApplet.color(192, 128, 255);
    int yellow = pApplet.color(255, 204, 0);
    int silver = pApplet.color(193, 194, 186);
    int brown = pApplet.color(69, 66, 61);
    int bague = pApplet.color(183, 180, 139);
    int offWhite = pApplet.color(224);

    int filterNum; // selects the filter to use
    int verticalOffset; // for 3D photo Side by Side correcton
    int horizontalOffset;  // for 3D photo stereo window correction
    static final int VERTICAL_CORRECTION = 1;
    static final int HORIZONTAL_CORRECTION = 4;

    // broken mirror filter parameters
    // Size of each cell in the grid
    int cellSize = 16;
    // Number of columns and rows for mirror
    int cols, rows;

    public ImageProcessor() {
        filterNum = 0;
        verticalOffset = 0;
        horizontalOffset = 0;
        pApplet.colorMode(RGB, 255, 255, 255, 100);
    }

    public void updateVerticalOffset(int signDirection) {
        verticalOffset += (signDirection * VERTICAL_CORRECTION);
    }

    public void updateHorizontalOffset(int signDirection) {
        horizontalOffset += (signDirection * HORIZONTAL_CORRECTION);
    }

    public PImage filter(PImage img, int filterNum) {
        if (img == null) return img;
        this.filterNum = filterNum;
        PImage result = processImage(img);
        return result;
    }

    public PImage processImage(PImage temp) {
        if (temp == null || temp.width <= 0 || temp.height <= 0) {
            if (DEBUG) println("processImage filterNum=" + filterNum + " image null");
        }
        switch (filterNum) {
            case 0:
                temp.filter(0);
                SAVE_IMAGE_SUFFIX = SAVE_IMAGE_SUFFIX + PRINT_AR_SUFFIX;
                break;
            case 1:
                temp.filter(GRAY);
                SAVE_IMAGE_SUFFIX = "_bw";
                break;
            case 2:
                temp.filter(THRESHOLD, 0.5f);
                SAVE_IMAGE_SUFFIX = "_t";
                break;
            case 3:
                temp.filter(INVERT);
                temp.filter(THRESHOLD, 0.5f);
                SAVE_IMAGE_SUFFIX = "_it";
                break;
            case 4:
                temp.filter(POSTERIZE, 13);
                SAVE_IMAGE_SUFFIX = "_p13";
                break;
            case 5:
                temp.filter(POSTERIZE, 8);  // best
                SAVE_IMAGE_SUFFIX = "_p8";
                break;
            case 6:
                temp.filter(POSTERIZE, 5);
                SAVE_IMAGE_SUFFIX = "_p5";
                break;
            case 7:
                temp.filter(POSTERIZE, 4);
                SAVE_IMAGE_SUFFIX = "_p4";
                break;
            case 8:
                //temp = mirror(temp);
                //SAVE_IMAGE_SUFFIX =  "_m";
                break;
            case 9:
                temp = cropForPrint(temp, PRINT_AR);
                SAVE_IMAGE_SUFFIX = "_cr";
                break;
            case 10:
                temp = anaglyph(temp, verticalOffset, horizontalOffset);
                SAVE_IMAGE_SUFFIX = "_ana";
                break;
            case 11:
                temp = spmAnaglyph(temp, verticalOffset, horizontalOffset); // SPM
                SAVE_IMAGE_SUFFIX = "_spm";
                break;
            case 12:
                temp = leftImage(temp);
                SAVE_IMAGE_SUFFIX = "_l";
                break;
            case 13:
                temp = rightImage(temp);
                SAVE_IMAGE_SUFFIX = "_r";
                break;
            case 14:  // Crop to print
                if ((float) temp.width / 2.0 / (float) temp.height > PRINT_AR) {
                    temp = centerCrop3DImage(temp, PRINT_AR);
                }
                break;
            default:  // do nothing error
                break;
        }
        return temp;
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////

    // get left image from stereo image Side by side
    PImage leftImage(PImage img) {
        PImage temp;
        int w = img.width / 2;
        int h = img.height;

        PGraphics pg = pApplet.createGraphics(w, h);
        pg.beginDraw();
        pg.background(0);
        pg.image(img, 0, 0);
        pg.endDraw();
        temp = pg.get();
        return temp;
    }

    // get right image from stereo image Side by side
    PImage rightImage(PImage img) {
        PImage temp;
        int w = img.width / 2;
        int h = img.height;

        PGraphics pg = pApplet.createGraphics(w, h);
        pg.beginDraw();
        pg.background(0);
        pg.copy(img, w, 0, w, h, 0, 0, w, h);
        pg.endDraw();
        temp = pg.get();
        return temp;
    }

    // crop for print
    PImage cropForPrint(PImage src, float printAspectRatio) {
        if (DEBUG)
            println("cropForPrint print ar=" + printAspectRatio + " w=" + src.width + " h=" + src.height);
        // first crop creating a new PImage
        float ar = (float) src.width / (float) src.height;
        if (ar <= printAspectRatio) return src;
        float bw = (src.width - (src.height * printAspectRatio));
        int sx = (int)(bw / 2);
        int sy = 0;
        int sw = src.width -(int)(bw);
        int sh = src.height;
        int dx = 0;
        int dy = 0;
        int dw = sw;
        int dh = src.height;
        if (DEBUG)
            println("cropForPrint " + sx + " " + sy + " " + sw + " " + sh + " " + dx + " " + dy + " " + dw + " " + dh);
        PImage img = pApplet.createImage(dw, dh, RGB);
        img.copy(src, sx, sy, sw, sh, dx, dy, dw, dh);  // cropped copy
        return img;
    }

    // center crop image from stereo image Side by side
    // mask out the left and right sides of stereo image
    PImage centerCrop3DImage(PImage src, float printAspectRatio) {
        PImage temp;
        if (DEBUG) println("centerCrop3DImage print ar=" + printAspectRatio);
        float bw = (src.width / 2.0f - (src.height * printAspectRatio));
        int sx = (int)(bw / 2);
        int sy = 0;
        int sw = src.width / 2 -(int)bw;
        int sh = src.height;
        int dx = 0;
        int dy = 0;
        int dw = sw;
        int dh = src.height;
        if (DEBUG)
            println("centerCrop3DImage " + sx + " " + sy + " " + sw + " " + sh + " " + dx + " " + dy + " " + dw + " " + dh);
        temp = pApplet.createImage(2 * dw, dh, RGB);
        temp.copy(src, sx, sy, sw, sh, dx, dy, dw, dh);  // cropped copy left image
        temp.copy(src, sx + src.width / 2, sy, sw, sh, dx + dw, dy, dw, dh);  // cropped copy right image
        return temp;
    }

    // create anaglyph using side-by-side LR image input img
    PImage anaglyph(PImage img, int vertOffset, int horzOffset) {
        int w2 = img.width;
        int w = img.width / 2;
        int h = img.height;
        PImage temp = pApplet.createImage(w, h, RGB);
        img.loadPixels();
        temp.loadPixels();
        int cr = 0;
        int cl = 0;
        int idx = 0;
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                cr = img.pixels[j * w2 + i + w];
                idx = (j - vertOffset) * w2 + i + horzOffset;
                if ((j - vertOffset) >= 0 && (j - vertOffset) < h && (i + horzOffset) >= 0 && ((i + horzOffset) < w)) {
                    cl = img.pixels[idx];
                } else {
                    cl = 0;
                }
                temp.pixels[j * w + i] = pApplet.color(red(cl), green(cr), blue(cr));
            }
        }
        temp.updatePixels();
        if (DEBUG) println("temp w=" + temp.width + " h=" + temp.height);

        // adjust size
        PImage temp2 = pApplet.createImage(w - horzOffset, h - vertOffset, RGB);
        temp2.copy(temp, 0, 0, w - horzOffset, h - vertOffset, 0, 0, temp.width, temp.height);
        if (DEBUG) println("temp2 w=" + temp2.width + " h=" + temp2.height);
        return temp2;
    }

    // create anaglyph using side-by-side LR image input img
    PImage spmAnaglyph(PImage img, int vertOffset, int horzOffset) {
        int w2 = img.width;
        int w = img.width / 2;
        int h = img.height;
        PImage temp = pApplet.createImage(w, h, RGB);
        img.loadPixels();
        temp.loadPixels();
        int cl = 0;
        int cr = 0;
        float r = 0;
        float g = 0;
        float b = 0;
        float rl = 0;
        float gl = 0;
        float bl = 0;
        float rr = 0;
        float gr = 0;
        float br = 0;
        int idx = 0;
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                cr = img.pixels[j * w2 + i + w];
                idx = (j - vertOffset) * w2 + i + horzOffset;
                if ((j - vertOffset) >= 0 && (j - vertOffset) < h && (i + horzOffset) >= 0 && ((i + horzOffset) < w)) {
                    cl = img.pixels[idx];
                } else {
                    cl = 0;
                }
                rl = red(cl);
                gl = green(cl);
                bl = blue(cl);
                rr = red(cr);
                gr = green(cr);
                br = blue(cr);
                r = floor(0.606100f * rl + 0.400484f * gl + 0.126381f * bl - 0.0434706f * rr - 0.0879388f * gr - 0.00155529f * br);
                g = floor(-0.0400822f * rl - 0.0378246f * gl - 0.0157589f * bl + 0.078476f * rr + 1.03364f * gr - 0.0184503f * br);
                b = floor(-0.0152161f * rl - 0.0205971f * gl - 0.00546856f * bl - 0.0721527f * rr - 0.112961f * gr + 1.2264f * br);
                temp.pixels[j * w + i] = pApplet.color(r, g, b);
            }
        }
        temp.updatePixels();
        if (DEBUG) println("temp w=" + temp.width + " h=" + temp.height);

        // adjust size
        PImage temp2 = pApplet.createImage(w - horzOffset, h - vertOffset, RGB);
        temp2.copy(temp, 0, 0, w - horzOffset, h - vertOffset, 0, 0, temp.width, temp.height);
        if (DEBUG) println("temp2 w=" + temp2.width + " h=" + temp2.height);
        return temp2;
    }

    private PGraphics spmAnaglyph(PGraphics bufL, PGraphics bufR) {
        // anaglyph interlace merge left and right images
        // reuse left image for faster performance
        bufL.loadPixels();
        bufR.loadPixels();

        int cl = 0;
        int cr = 0;
        float r = 0;
        float g = 0;
        float b = 0;
        float rl = 0;
        float gl = 0;
        float bl = 0;
        float rr = 0;
        float gr = 0;
        float br = 0;

        int len = bufL.pixels.length;
        int i = 0;
        while (i < len) {
            cl = bufL.pixels[i];
            cr = bufR.pixels[i];
            rl = red(cl);
            gl = green(cl);
            bl = blue(cl);
            rr = red(cr);
            gr = green(cr);
            br = blue(cr);
            r = floor(0.606100f * rl + 0.400484f * gl + 0.126381f * bl - 0.0434706f * rr - 0.0879388f * gr - 0.00155529f * br);
            g = floor(-0.0400822f * rl - 0.0378246f * gl - 0.0157589f * bl + 0.078476f * rr + 1.03364f * gr - 0.0184503f * br);
            b = floor(-0.0152161f * rl - 0.0205971f * gl - 0.00546856f * bl - 0.0721527f * rr - 0.112961f * gr + 1.2264f * br);
            bufL.pixels[i] = pApplet.color(r, g, b);
            i++;
        }
        bufL.updatePixels();
        return bufL;
    }
}
