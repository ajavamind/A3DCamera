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

  // Create the right half image
  result[1] = createImage(halfWidth, original.height, ARGB);
  result[1].setNative(Bitmap.createBitmap(halfWidth, original.height, Bitmap.Config.ARGB_8888));
  result[1].copy(original, halfWidth, 0, halfWidth, original.height, 0, 0, halfWidth, original.height);
  result[1].loadPixels();
  return result;
}

/**
 * Creates a column-interlaced 3D bitmap optimized for display size,
 * SKYY 3D autostereoscopic displays and LeTv android TV
 * The imput bit maps are recycled.
 
 * @param conversion   perform Column or Row interlacing left and right image stereo
 * @param leftImg      Source left-eye image
 * @param rightImg     Source right-eye image
 * @param targetWidth  NATIVE hardware width (do not use scaled/display pixels)
 * @param targetHeight NATIVE hardware height
 * @return New interlaced PImage. Caller MUST recycle leftImg, rightImg, and this result when done.
 */
PImage interlaced3D(int conversion, Bitmap leftImg, Bitmap rightImg, int targetWidth, int targetHeight) {
  println("interlaced3D " + conversion);
  if (!(conversion == COLUMN_INTERLACE || conversion == ROW_INTERLACE)) return null;

  // 1. Scale both images to native resolution while preserving aspect ratio
  // Uses nearest-neighbor interpolation to prevent left/right eye color bleeding
  Bitmap scaledLeft = scaleWithLetterbox(leftImg, targetWidth, targetHeight);
  leftImg.recycle();
  Bitmap scaledRight = scaleWithLetterbox(rightImg, targetWidth, targetHeight);
  rightImg.recycle();

  // 2. Column interlace (Even columns = Left Eye, Odd columns = Right Eye)
  if (conversion == COLUMN_INTERLACE) {
    for (int y=0; y<scaledLeft.getHeight(); y++) {
      for (int x=0; x<scaledLeft.getWidth(); x++) {
        if (x%2 == 0) {
          //scaledLeft.setPixel(x, y, scaledLeft.getPixel(x, y));
        } else {
          scaledLeft.setPixel(x, y, scaledRight.getPixel(x, y));
        }
      }
    }
  } else {  // Row interlaced
    for (int y=0; y<scaledLeft.getHeight(); y++) {
      for (int x=0; x<scaledLeft.getWidth(); x++) {
        if (y%2 == 0) {
          //scaledLeft.setPixel(x, y, scaledLeft.getPixel(x, y));
        } else {
          scaledLeft.setPixel(x, y, scaledRight.getPixel(x, y));
        }
      }
    }
  }

  // Clean up intermediate bitmaps immediately
  scaledRight.recycle();

  PImage clImage = createImage(targetWidth, targetHeight, ARGB);
  clImage.setNative(scaledLeft);
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

/**
 * Create color anaglyph Bitmap from left and right eye view Bitmaps
 * with horizontal offset for stereo window and vertical offset for camera alignment correction
 *
 * @param bufL       Left eye view image
 * @param bufR       Right eye view image
 * @param horzOffset Horizontal offset for stereo window placement parallax
 * @param vertOffset Vertical offset for camera alignment correction
 */
PImage colorAnaglyph3D(Bitmap bufL, Bitmap bufR, int horzOffset, int vertOffset) {
  if (DEBUG) println("colorAnaglyph3D horzOffset=" + horzOffset + " vertOffset=" + vertOffset);

  if (bufL == null || bufR == null) {
    if (DEBUG) println("colorAnaglyph null images");
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

  PImage anaImage = createImage(w, h, ARGB);
  anaImage.setNative(bufA);
  anaImage.loadPixels();
  return anaImage;
}
