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
        int srcRowL = (vertOffset >= 0) ? 0 : Math.abs(vertOffset);

        // Copy rows from left and right images
        for (int j = 0; j < dh; j++) {
            // Calculate actual source row indices
            int rowL = srcRowL + j;
            int rowR = j;

            // Copy left image row
            System.arraycopy(pixelsL, rowL * w + srcColL,
                    outputPixels, j * 2 * dw, dw);

            // Copy right image row
            System.arraycopy(pixelsR, rowR * w,
                    outputPixels, j * 2 * dw + dw, dw);
        }

        // Create output bitmap
        Bitmap temp = Bitmap.createBitmap(2 * dw, dh, Bitmap.Config.ARGB_8888);
        temp.setPixels(outputPixels, 0, 2 * dw, 0, 0, 2 * dw, dh);

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
}