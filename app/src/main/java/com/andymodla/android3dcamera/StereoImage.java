package com.andymodla.android3dcamera;

import static android.graphics.Color.blue;
import static android.graphics.Color.green;
import static android.graphics.Color.red;
import static android.graphics.ColorSpace.Model.RGB;
import static java.lang.Math.abs;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorSpace;
import android.graphics.ImageFormat;
import android.util.Log;

public class StereoImage {
    private static final String TAG = "A3DCamera";

    // Merge left and right images into SBS image
    public static Bitmap mergeLR(Bitmap leftBitmap, Bitmap rightBitmap) {
        // Calculate the dimensions for the combined bitmap.
        int width = leftBitmap.getWidth() + rightBitmap.getWidth();
        int height = Math.max(leftBitmap.getHeight(), rightBitmap.getHeight());

        // Create a new bitmap with the combined dimensions.
        Bitmap sbsBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // Create a canvas to draw on the new bitmap.
        Canvas canvas = new Canvas(sbsBitmap);

        // Draw the left bitmap at position (0, 0).
        canvas.drawBitmap(leftBitmap, 0f, 0f, null);

        // Draw the right bitmap immediately to the right of the left one.
        canvas.drawBitmap(rightBitmap, leftBitmap.getWidth(), 0f, null);

        return sbsBitmap;
    }

    /**
     * Align LR image pairs by moving left image using offsets.
     * Fill unused area with black (0)
     * Bitmap imgL left camera/eye image
     * Bitmap imgR right camera/eye image
     * output img size is reduced by horzOffset and vertOffset values
     * horzOffset camera alignment horizontally to adjust stereo window placement parallax
     * vertOffset adjust camera misalignment vertically for stereoscopic viewing
     * Assumes LR images are exact same dimensions
     */
    public static Bitmap alignLR(Bitmap imgL, Bitmap imgR, int horzOffset, int vertOffset) {
        Log.d(TAG, "alignLR horzOffset=" + horzOffset + " vertOffset=" + vertOffset);

        int w = imgL.getWidth();
        int h = imgL.getHeight();
        int dw = w - Math.abs(horzOffset);
        int dh = h - Math.abs(vertOffset);

        // Extract pixel arrays from source images
        int[] pixelsL = new int[w * h];
        int[] pixelsR = new int[w * h];
        imgL.getPixels(pixelsL, 0, w, 0, 0, w, h);
        imgR.getPixels(pixelsR, 0, w, 0, 0, w, h);

        // Create output pixel array
        int[] outputPixels = new int[2 * dw * dh];

        // Calculate starting positions based on offset signs
        int srcColL = (horzOffset >= 0) ? horzOffset : 0;
        int srcRowOffsetL = (vertOffset >= 0) ? -vertOffset : Math.abs(vertOffset);

        // Copy rows from left and right images
        for (int j = 0; j < dh; j++) {
            // Calculate source row indices
            int srcRowL = j + srcRowOffsetL;
            int srcRowR = j;

            // Copy left image row
            System.arraycopy(pixelsL, srcRowL * w + srcColL,
                    outputPixels, j * 2 * dw, dw);

            // Copy right image row
            System.arraycopy(pixelsR, srcRowR * w,
                    outputPixels, j * 2 * dw + dw, dw);
        }

        // Create output bitmap
        Bitmap temp = Bitmap.createBitmap(2 * dw, dh, Bitmap.Config.ARGB_8888);
        temp.setPixels(outputPixels, 0, 2 * dw, 0, 0, 2 * dw, dh);

        return temp;
    }

    /**
     * Align LR image pairs by moving left image using offsets.
     * Fill unused area with black (0)
     * Bitmap imgL left camera/eye image
     * Bitmap imgR right camera/eye image
     * output img size is reduced by horzOffset and vertOffset values
     * horzOffset camera alignment horizontally to adjust stereo window placement parallax
     * vertOffset adjust camera misalignment vertically for stereoscopic viewing
     * Assumes LR images are exact same dimensions
     */
    public static Bitmap alignLRWorks(Bitmap imgL, Bitmap imgR, int horzOffset, int vertOffset) {
        Log.d(TAG, "alignLR horzOffset=" + horzOffset + " vertOffset=" + vertOffset);

        int w = imgL.getWidth();
        int h = imgL.getHeight();
        int dw = w - Math.abs(horzOffset);
        int dh = h - Math.abs(vertOffset);

        // Extract pixel arrays from source images
        int[] pixelsL = new int[w * h];
        int[] pixelsR = new int[w * h];
        imgL.getPixels(pixelsL, 0, w, 0, 0, w, h);
        imgR.getPixels(pixelsR, 0, w, 0, 0, w, h);

        // Create output pixel array
        int[] outputPixels = new int[2 * dw * dh];

        // Copy rows from left and right images
        for (int j = 0; j < dh; j++) {
            // Calculate source row indices
            int srcRowL = j - vertOffset;
            int srcRowR = j;

            // Copy left image row
            System.arraycopy(pixelsL, srcRowL * w + horzOffset,
                    outputPixels, j * 2 * dw, dw);

            // Copy right image row
            System.arraycopy(pixelsR, srcRowR * w,
                    outputPixels, j * 2 * dw + dw, dw);
        }

        // Create output bitmap
        Bitmap temp = Bitmap.createBitmap(2 * dw, dh, Bitmap.Config.ARGB_8888);
        temp.setPixels(outputPixels, 0, 2 * dw, 0, 0, 2 * dw, dh);

        return temp;
    }


    /**
     * Align LR image pairs by moving left image using offsets.
     * Fill unused area with black (0)
     * Bitmap img left camera/eye image
     * Bitmap img right camera/eye image
     * output img size is reduced by horzOffset and vertOffset values
     * horzOffset camera alignment horizontally to adjust stereo window placement parallax
     * vertOffset adjust camera misalignment vertically for stereoscopic viewing
     * Assumes LR images are exact same dimensions
     */
    public static Bitmap alignLRslow(Bitmap imgL, Bitmap imgR, int horzOffset, int vertOffset) {
        Log.d(TAG, "alignLR horzOffset=" + horzOffset + " vertOffset=" + vertOffset);

        int w = imgL.getWidth();
        int h = imgL.getHeight();
        int dw = w - abs(horzOffset);
        int dh = h - abs(vertOffset);

        Bitmap temp = Bitmap.createBitmap(2 * dw, dh, Bitmap.Config.ARGB_8888);

        for (int j = 0; j < dh; j++) {  // j vertical scan rows
            for (int i = 0; i < dw; i++) { // i horizontal scan columns
                temp.setPixel(i, j, imgL.getPixel(i + horzOffset, j - vertOffset));
                temp.setPixel(i + dw, j, imgR.getPixel(i, j));
            }
        }
        return temp;
    }

    /**
     * Create color anaglyph Bitmap from left and right eye view Bitmaps
     * with horizontal offset for stereo window and vertical offset for camera alignment correction
     *
     * @param bufL       Left eye view image
     * @param bufR       Right eye view image
     * @param horzOffset Horizontal offset for stereo window placement parallax
     * @param vertOffset Vertical offset for camera alignment correction
     */
    public static Bitmap colorAnaglyph(Bitmap bufL, Bitmap bufR, int horzOffset, int vertOffset) {
        Log.d(TAG, "colorAnaglyph horzOffset=" + horzOffset + " vertOffset=" + vertOffset);

        if (bufL == null || bufR == null) {
            Log.d(TAG, "colorAnaglyph null images");
            return null;
        }

        int w = bufL.getWidth();
        int h = bufL.getHeight();

        // Pre-allocate pixel arrays for batch processing
        int[] pixelsL = new int[w * h];
        int[] pixelsR = new int[w * h];
        int[] pixelsA = new int[w * h];

        // Get all pixels at once (much faster than individual getPixel calls)
        bufL.getPixels(pixelsL, 0, w, 0, 0, w, h);
        bufR.getPixels(pixelsR, 0, w, 0, 0, w, h);

        // Process pixels with optimized indexing
        int idx = 0;
        int srcIdx;
        int srcRow, srcCol;

        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                // Calculate source position with offsets
                srcRow = j + vertOffset;
                srcCol = i + horzOffset;

                // Check bounds
                if (srcRow >= 0 && srcRow < h && srcCol >= 0 && srcCol < w) {
                    srcIdx = srcRow * w + srcCol;
                    // Merge: red channel from left, cyan (GB) from right
                    pixelsA[idx] = (pixelsL[srcIdx] & 0xFFFF0000) | (pixelsR[idx] & 0x0000FFFF);
                } else {
                    // Make unused pixels transparent
                    pixelsA[idx] = 0;
                }
                idx++;
            }
        }

        // Create output bitmap and set all pixels at once
        Bitmap bufA = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bufA.setPixels(pixelsA, 0, w, 0, 0, w, h);

        return bufA;
    }

    /**
     * Create color anaglyph Bitmap from left and right eye view Bitmaps
     * with horizontal offset for stereo window and vertical offset for camera alignment correction
     *
     * @param bufL       Left eye view image
     * @param bufR       Right eye view image
     * @param horzOffset Horizontal offset for stereo window placement parallax
     * @param vertOffset Vertical offset for camera alignment correction
     */
    public static Bitmap colorAnaglyphSlow(Bitmap bufL, Bitmap bufR, int horzOffset, int vertOffset) {
        // color anaglyph merge left and right images
        int horz = horzOffset;
        int vert = vertOffset;
        Log.d(TAG, "colorAnaglyph horzOffset=" + horzOffset + " vertOffset=" + vertOffset);
        if (bufL == null || bufR == null) {
            Log.d(TAG, "colorAnaglyph null images");
            return null;
        }
        Bitmap bufA = Bitmap.createBitmap(bufL.getWidth(), bufL.getHeight(), Bitmap.Config.ARGB_8888);

        int w = bufL.getWidth();
        int h = bufL.getHeight();
        int cr = 0;  // pixel color right eye cyan (green+blue) channels
        int cl = 0;  // pixel color left eye red channel

        for (int j = 0; j < h; j++) {  // j vertical scan
            for (int i = 0; i < w; i++) { // i horizontal scan
                cr = bufR.getPixel(i, j); // get pixel color at right eye row,col
                if ((j + vert) >= 0 && (j + vert) < h && (i + horz) >= 0 && ((i + horz) < w)) {
                    cl = bufL.getPixel(i + horz, j + vert);
                } else {
                    cl = 0; // make unused pixel transparent when adjusted
                    cr = 0;
                }
                bufA.setPixel(i, j, cl & 0xFFFF0000 | cr & 0x0000FFFF); // store merged anaglyph pixel parallel
            }
        }
        return bufA;
    }

    /**
     * Create color anaglyph Bitmap from left and right eye view Bitmaps
     * with horizontal offset for stereo window and vertical offset for camera alignment correction
     *
     * @param leftBitmap  Left eye view image
     * @param rightBitmap Right eye view image
     */
    public static Bitmap colorAnaglyph(Bitmap leftBitmap, Bitmap rightBitmap) {
        int width = Math.min(leftBitmap.getWidth(), rightBitmap.getWidth());
        int height = Math.min(leftBitmap.getHeight(), rightBitmap.getHeight());

        // Create anaglyph bitmap
        Bitmap anaglyphBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        int[] leftPixels = new int[width * height];
        int[] rightPixels = new int[width * height];

        leftBitmap.getPixels(leftPixels, 0, width, 0, 0, width, height);
        rightBitmap.getPixels(rightPixels, 0, width, 0, 0, width, height);

        int[] anaglyphPixels = new int[width * height];

        for (int i = 0; i < leftPixels.length; i++) {
            int leftPixel = leftPixels[i];
            int rightPixel = rightPixels[i];

            // Extract RGB components
            int leftRed = (leftPixel >> 16) & 0xFF;
            int rightGreen = (rightPixel >> 8) & 0xFF;
            int rightBlue = rightPixel & 0xFF;

            // Create anaglyph pixel: left red + right green/blue
            anaglyphPixels[i] = (0xFF << 24) | (leftRed << 16) | (rightGreen << 8) | rightBlue;
        }

        anaglyphBitmap.setPixels(anaglyphPixels, 0, width, 0, 0, width, height);
        return anaglyphBitmap;
    }
}