package com.andymodla.android3dcamera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageResizer {

    private static final String TAG = "ImageResizer";
    private static final boolean DEBUG = true;

    /**
     * Loads an image, resizes it to 6x4 aspect ratio with padding,
     * saves it, and returns the file.
     *
     * @param filename The absolute path to the source image.
     * @return The File object of the newly created image.
     */
    public File resizeFile(String filename) {
        try {
            // 1. Load the image
            Bitmap imgCopy = BitmapFactory.decodeFile(filename);
            if (imgCopy == null) {
                Log.e(TAG, "Failed to load image at: " + filename);
                return null;
            }

            // 2. Resize
            Bitmap resized = resizeToPrint6x4(imgCopy);

            // 3. Prepare new filename
            // Removes extension and adds _6x4.jpg
            String nfilename = filename.substring(0, filename.lastIndexOf("_2x1")) + "_6x4.jpg";
            File imageFile = new File(nfilename);

            // 4. Save the image
            FileOutputStream out = new FileOutputStream(imageFile);
            resized.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();

            // Clean up memory
            imgCopy.recycle();
            resized.recycle();

            return imageFile;

        } catch (IOException e) {
            Log.e(TAG, "Error saving image: " + e.getMessage());
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Resize image to 6x4 aspect ratio pixels
     * Handles all input aspect ratios without distortion using letterbox/pillarbox
     *
     * @param img The source Bitmap
     * @return Resized bitmap padded to 1800x1200 with white borders
     */
    public Bitmap resizeToPrint6x4(Bitmap img) {
        Log.d(TAG, "resizeToPrint6x4() convert image to 6x4 aspect ratio img.width="
                + img.getWidth() + " img.height=" + img.getHeight());

        float printWidth = 1800f;
        float printHeight = 1200f;

        // Create a blank bitmap with the target dimensions
        Bitmap resizeImage = Bitmap.createBitmap((int) printWidth, (int) printHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(resizeImage);

        // Fill background white
        canvas.drawColor(Color.WHITE);

        // Calculate scale factor to fit img inside 1800x1200 preserving aspect ratio
        float scaleX = printWidth / (float) img.getWidth();
        float scaleY = printHeight / (float) img.getHeight();
        float scale = Math.min(scaleX, scaleY);

        // Calculate scaled dimensions
        float scaledW = img.getWidth() * scale;
        float scaledH = img.getHeight() * scale;

        // Calculate centering offsets
        float offsetX = (printWidth - scaledW) / 2f;
        float offsetY = (printHeight - scaledH) / 2f;

        if (DEBUG) {
            Log.d(TAG, String.format("resize6x4() scale=%f scaledW=%f scaledH=%f offsetX=%f offsetY=%f",
                    scale, scaledW, scaledH, offsetX, offsetY));
        }

        // Create a destination rectangle for the scaled image
        RectF destRect = new RectF(offsetX, offsetY, offsetX + scaledW, offsetY + scaledH);

        // Draw the original image onto the canvas using the destination rectangle (handles scaling)
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG); // Use filter for smoother scaling
        canvas.drawBitmap(img, null, destRect, paint);

        return resizeImage;
    }
}
