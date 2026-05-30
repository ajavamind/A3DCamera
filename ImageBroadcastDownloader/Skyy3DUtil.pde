import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.PorterDuff;
import processing.core.PImage;

PImage[] splitImageLR(PImage original) {
  println("splitImageLR w="+original.width +" h="+original.height+ " width="+width + " height="+height);
  // Create an array to hold the two resulting images
  PImage[] result = new PImage[2];

  // Calculate the width of each half, using integer division
  int halfWidth = original.width / 2;

  // Create the left half image
  result[0] = createImage(halfWidth, original.height, ARGB);
  result[0].setNative(Bitmap.createBitmap(halfWidth, original.height, Bitmap.Config.ARGB_8888));
  result[0].copy(original, 0, 0, halfWidth, original.height, 0, 0, halfWidth, original.height);
  result[0].loadPixels();
  result[0].updatePixels();
  
  // Create the right half image
  result[1] = createImage(halfWidth, original.height, ARGB);
  result[1].setNative(Bitmap.createBitmap(halfWidth, original.height, Bitmap.Config.ARGB_8888));
  result[1].copy(original, halfWidth, 0, halfWidth, original.height, 0, 0, halfWidth, original.height);
  result[1].loadPixels();
  result[1].updatePixels();
  return result;
}

/**
 * Creates a column-interlaced 3D bitmap optimized for SKYY 3D autostereoscopic displays.
 *
 * @param leftImg      Source left-eye image
 * @param rightImg     Source right-eye image
 * @param targetWidth  NATIVE hardware width (do not use scaled/display pixels)
 * @param targetHeight NATIVE hardware height
 * @return New interlaced PImage. Caller MUST recycle leftImg, rightImg, and this result when done.
 */
PImage createColumnInterlaced3D(Bitmap leftImg, Bitmap rightImg, int targetWidth, int targetHeight) {
  println("createColumnInterlaced3D");
  // 1. Scale both images to native resolution while preserving aspect ratio
  // Uses nearest-neighbor interpolation to prevent left/right eye color bleeding
  Bitmap scaledLeft = scaleWithLetterbox(leftImg, targetWidth, targetHeight);
  Bitmap scaledRight = scaleWithLetterbox(rightImg, targetWidth, targetHeight);

  // 2. Extract pixel arrays
  int[] leftPixels = new int[targetWidth * targetHeight];
  int[] rightPixels = new int[targetWidth * targetHeight];
  scaledLeft.getPixels(leftPixels, 0, targetWidth, 0, 0, targetWidth, targetHeight);
  scaledRight.getPixels(rightPixels, 0, targetWidth, 0, 0, targetWidth, targetHeight);

  // 3. Column interlace (Even columns = Left Eye, Odd columns = Right Eye)
  int[] interlacedPixels = new int[targetWidth * targetHeight];
  for (int y = 0; y < targetHeight; y++) {
    int rowOffset = y * targetWidth;
    // Step by 2 to avoid modulo operator and improve cache locality
    for (int x = 0; x < targetWidth; x += 2) {
      interlacedPixels[rowOffset + x] = leftPixels[rowOffset + x];
      if (x + 1 < targetWidth) {
        interlacedPixels[rowOffset + x + 1] = rightPixels[rowOffset + x + 1];
      }
    }
  }

  // 4. Build final bitmap
  Bitmap result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
  result.setPixels(interlacedPixels, 0, targetWidth, 0, 0, targetWidth, targetHeight);

  // Clean up intermediate bitmaps immediately
  scaledLeft.recycle();
  scaledRight.recycle();

  PImage clImage = createImage(targetWidth, targetHeight, ARGB);
  clImage.setNative(result);
  return clImage;
}

/**
 * Scales a bitmap to target dimensions with letterboxing/pillarboxing.
 * Uses NEAREST-NEIGHBOR interpolation to preserve interlace integrity.
 */
Bitmap scaleWithLetterbox(Bitmap src, int targetW, int targetH) {
  // Calculate uniform scale to fit within target without stretching
  if (src == null) println("scaleWithLetterbox src null");
  float scale = Math.min((float) targetW / src.getWidth(), (float) targetH / src.getHeight());
  int newW = Math.round(src.getWidth() * scale);
  int newH = Math.round(src.getHeight() * scale);

  // Center the scaled image (letterboxing)
  int offsetX = (targetW - newW) / 2;
  int offsetY = (targetH - newH) / 2;

  Bitmap dst = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
  Canvas canvas = new Canvas(dst);

  // Fill letterbox areas with opaque black (transparent causes depth artifacts on 3D panels)
  canvas.drawColor(0xFF000000, PorterDuff.Mode.SRC);

  // Nearest-neighbor scaling configuration
  Paint paint = new Paint();
  paint.setFilterBitmap(false); // Critical: disables bilinear interpolation
  paint.setAntiAlias(false);

  Rect srcRect = new Rect(0, 0, src.getWidth(), src.getHeight());
  RectF dstRect = new RectF(offsetX, offsetY, offsetX + newW, offsetY + newH);
  canvas.drawBitmap(src, srcRect, dstRect, paint);

  return dst;
}
