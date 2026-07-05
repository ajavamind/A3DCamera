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
import android.graphics.Color;
import java.nio.IntBuffer;

public class StereoImage {
    private static final String TAG = "A3DCamera";

    /**
     * Align LR image pairs by shifting both eyes symmetrically (half offset each, opposite directions).
     * Matches the parallax approach used by PhotoBooth.drawSBS() so the saved SBS image
     * has the same stereo content as what the live display shows.
     * Uses a reusable output bitmap to eliminate per-call allocations.
     *
     * @param imgL       Left camera/eye image
     * @param imgR       Right camera/eye image
     * @param horzOffset Horizontal alignment offset (split: -horzOffset/2 on left, +horzOffset/2 on right)
     * @param vertOffset Vertical alignment offset (split: -vertOffset/2 on left, +vertOffset/2 on right)
     * @param crop       >0 crops SBS image horizontally to target aspect ratio
     * @param sbsBitmap  Reusable output bitmap. Pass null on first call, then reuse the returned instance.
     * @return The aligned & cropped SBS bitmap (the same instance passed in)
     */
    public static Bitmap alignLR(Bitmap imgL, Bitmap imgR, int horzOffset, int vertOffset, float crop, Bitmap sbsBitmap) {
        Log.d(TAG, "alignLR horzOffset=" + horzOffset + " vertOffset=" + vertOffset);

        int w = imgL.getWidth();
        int h = imgL.getHeight();
        Log.d(TAG, "alignLR left w=" + w + " h=" + h);

        // Ensure input bitmaps are ARGB_8888 so getPixels() returns correct
        // 0xAARRGGBB int values (fixes colour swap with RGBA_8888 sources)
        if (imgL.getConfig() != Bitmap.Config.ARGB_8888) {
            imgL = imgL.copy(Bitmap.Config.ARGB_8888, false);
        }
        if (imgR.getConfig() != Bitmap.Config.ARGB_8888) {
            imgR = imgR.copy(Bitmap.Config.ARGB_8888, false);
        }

        int dw = w - Math.abs(horzOffset);
        int dh = h - Math.abs(vertOffset);
        if (dw <= 0 || dh <= 0) {
            throw new IllegalArgumentException("Offsets too large, resulting dimensions are non-positive.");
        }

        // 1. Pre-calculate final dimensions & source offsets.
        //    Symmetric split: each eye shifts by half the total offset in opposite directions.
        //    Matches PhotoBooth.drawSBS() which uses translate(-parallax/2) / translate(+parallax/2).
        int finalH = dh;
        int finalW = 2 * dw;
        int halfHorz = horzOffset / 2;
        int halfVert = vertOffset / 2;
        int leftSrcCol  = (halfHorz >= 0) ? halfHorz : 0;
        int rightSrcCol = (halfHorz >= 0) ? 0 : Math.abs(halfHorz);
        int copyWidth   = dw;
        // Vertical source rows: each eye shifted by half vertOffset in opposite directions
        int srcRowLBase = (halfVert >= 0) ? 0 : Math.abs(halfVert);
        int srcRowRBase = (halfVert >= 0) ? halfVert : 0;

        // Apply crop parameters upfront if requested.
        // Crop from all 4 edges (both inner and outer edges of each eye)
        if (crop > 0) {
            int cropPerEdge = (int) Math.max(0, (finalW - crop * finalH) / 4);
            if (cropPerEdge > 0) {
                int newEyeWidth = dw - 2 * cropPerEdge;
                if (newEyeWidth > 0) {
                    copyWidth   = newEyeWidth;
                    finalW      = 2 * newEyeWidth;
                    leftSrcCol  += cropPerEdge;
                    rightSrcCol += cropPerEdge;
                }
            }
        }

        // 2. Initialize or validate reusable bitmap at the ACTUAL final size.
        //    (Previously used max possible size, which meant the bitmap never
        //     shrank when offsets were applied, so getWidth/Height stayed too large.)
        if (sbsBitmap == null || sbsBitmap.getWidth() != finalW || sbsBitmap.getHeight() != finalH) {
            if (sbsBitmap != null) {
                Log.d(TAG, "!!!!!!!!!!!!!!!!!!!!!!! sbsBitmap.recycle()");
                sbsBitmap.recycle();
                sbsBitmap = null;
            }
            sbsBitmap = Bitmap.createBitmap(finalW, finalH, Bitmap.Config.ARGB_8888);
        }

        // 3. Extract pixels as int arrays.
        // getPixels() on an ARGB_8888 bitmap always returns 0xAARRGGBB ints,
        // avoiding the byte-order ambiguity of copyPixelsToBuffer(IntBuffer)
        // which can swap channels on little-endian ARM devices.
        int[] lPixels = new int[w * h];
        int[] rPixels = new int[w * h];

        imgL.getPixels(lPixels, 0, w, 0, 0, w, h);
        imgR.getPixels(rPixels, 0, w, 0, 0, w, h);

        // Output buffer sized exactly to the final region
        int[] outPixels = new int[finalW * finalH];

        // 4. Direct row-by-row assembly into final array (symmetric offsets)
        for (int j = 0; j < finalH; j++) {
            int rowL = srcRowLBase + j;
            int rowR = srcRowRBase + j;

            System.arraycopy(lPixels, rowL * w + leftSrcCol, outPixels, j * finalW, copyWidth);
            System.arraycopy(rPixels, rowR * w + rightSrcCol, outPixels, j * finalW + copyWidth, copyWidth);
        }

        // 5. Clear bitmap to prevent ghosting from previous calls, then write
        sbsBitmap.eraseColor(Color.TRANSPARENT);
        sbsBitmap.setPixels(outPixels, 0, finalW, 0, 0, finalW, finalH);

        return sbsBitmap;
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
        Log.d(TAG, "colorAnaglyph left image width=" +w+ " height=" +h);
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
        Bitmap bufA = Bitmap.createBitmap(w-(abs(horzOffset)), h, Bitmap.Config.ARGB_8888);
        bufA.setPixels(pixelsA, 0, w, 0, 0, w-(abs(horzOffset)), h);

        return bufA;
    }
}