package com.andymodla.android3dcamera.camera;

import android.graphics.Bitmap;
import android.media.Image;
import io.github.crow_misia.libyuv.ArgbBuffer;
import io.github.crow_misia.libyuv.Nv21Buffer;
import io.github.crow_misia.libyuv.PlaneNative;

public class YuvConverter {
    private static final String TAG = "com.andymodla.stereocamera";

    // Image image must be closed by calling program
    public static void yuvToBitmap(Image image, Bitmap bitmap) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Extract planes from the Image
        Image.Plane[] planes = image.getPlanes();

        // Extract planes from the Image
        // For NV21: Y plane + interleaved UV plane
        PlaneNative yPlane = new PlaneNative(planes[0]);
        PlaneNative uvPlane = new PlaneNative(planes[1]); // NV21 uses plane[1] for UV

        // Create NV21Buffer (works for both NV21 and NV21 semi-planar formats)
        Nv21Buffer nv21Buffer = Nv21Buffer.Factory.wrap(
                yPlane,
                uvPlane,
                width,
                height
        );

        // Prepare the destination ARGB buffer
        ArgbBuffer argbBuffer = ArgbBuffer.Factory.allocate(width, height);

        // Perform the conversion
        nv21Buffer.convertTo(argbBuffer);
        nv21Buffer.close();

        // Copy the converted pixel data into the Bitmap
        bitmap.copyPixelsFromBuffer(argbBuffer.asBuffer());
        argbBuffer.close();
    }
}