/**
 * Image Broadcast Downloader app for 3D Photo Booth local area network
 * Copyright 2025-2026, Andy Modla All Rights Reserved
 */

/**
 * A Processing Java Android sketch for downloading images from a http server
 * It uses a broadcast UDP message to signal an app to request to download an image file.
 */

/**
 Constant   Value  Description
 STATUS_PENDING  1  The download is waiting to start.
 STATUS_RUNNING  2  The download is currently in progress.
 STATUS_PAUSED  4  The download is paused (e.g., waiting for network or manual pause).
 STATUS_SUCCESSFUL  8  The download has completed successfully.
 STATUS_FAILED  16  The download has failed and will not be retried.
 
 Pause Reason Codes (STATUS_PAUSED)
 If the status is STATUS_PAUSED, these codes explain why the manager is waiting.
 
 PAUSED_WAITING_TO_RETRY (1): The download is paused and will retry after a short delay.
 PAUSED_WAITING_FOR_NETWORK (2): The download is waiting for network connectivity.
 PAUSED_QUEUED_FOR_WIFI (3): The download is too large for mobile data and is waiting for a Wi-Fi connection.
 PAUSED_UNKNOWN (4): The download is paused for an unspecified reason.
 */

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import android.view.View;
import com.andymodla.imagebroadcastdownloader.DownloadHelper;
import com.andymodla.imagebroadcastdownloader.UrlSource;
import android.media.MediaScannerConnection;
import android.graphics.Bitmap;
import android.os.Build;

//import MyDebug;

private DownloadHelper downloadHelper;
private UdpRemoteControl udpRemoteControl;
private UrlSource urlSource;
String hostIp;
int port = 8000;
String version = "1.0";
String path;
PImage photo;
volatile PImage colImage;
volatile PImage originalImage;
volatile PImage leftImage;
volatile PImage rightImage;
String testImage = "IMG_20260330_145622_2x1.jpg";
volatile boolean ready = false;
volatile boolean show = false;
volatile boolean stereo = false;
boolean newPhoto = false;

int NO_CONVERSION = 0;
int COLUMN_INTERLACE = 1;
int ANAGLYPH = 2;
int conversion = NO_CONVERSION;

int xOffset; // offset
int sOffset;
float ar;
float sar;

// Android Activity Life Cycle ======================================================

void onCreate() {  // not called from processing, so this method is not executed!
  // that is why we set up UDP in start()
  System.out.println("onCreate()");
}

void onStart() {
  System.out.println("onStart()");
  setVisibility();
  if (downloadHelper == null) {
    println("setup DownloadHelper");
    downloadHelper = new DownloadHelper(getContext());
  }

  if (udpRemoteControl == null) {
    urlSource = new UrlSource(downloadHelper);
    println("setup UDP receiver");
    udpRemoteControl = new UdpRemoteControl(urlSource);
    hostIp = udpRemoteControl.getHostnameAddress();
    println("hostIp="+hostIp);

    udpRemoteControl.setUdpReceiver( hostIp);
    String broadcastIp = udpRemoteControl.getBroadcastAddress();
    println("Broadcast Ip="+broadcastIp);
  }
}

void onPause() {
  System.out.println("onPause");
}

void onResume() {
  System.out.println("onResume");
}

void onDestroy() {
  System.out.println("onDestroy");
  //stop UDP receiver on destroy of activity
  udpRemoteControl.destroy();
}

private void setVisibility() {
  //if (DEBUG) println("setVisibility width = "+width + " height="+height);
  runOnUiThread(new Runnable() {
    @Override
      public void run() {
      //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      //    // Android 11 (API 30) and above - use WindowInsetsController
      //    WindowInsetsController controller = getWindow().getInsetsController();
      //    if (controller != null) {
      //        // Hide status bar and navigation bar
      //        controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());

      //        // Set behavior for when user swipes to show system bars
      //        controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

      //        // Optional: Set light status bar (uncomment if needed)
      //        // controller.setSystemBarsAppearance(
      //        //     WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
      //        //     WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
      //        // );
      //    }

      //    // Enable edge-to-edge layout
      //    getWindow().setDecorFitsSystemWindows(false);
      //} else {
      // Fallback for older Android versions (API 29 and below)
      int newVis = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        | View.SYSTEM_UI_FLAG_FULLSCREEN
        //  | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

      final View decorView = getWindow().getDecorView();
      decorView.setSystemUiVisibility(newVis);
      //                              }
    }
  }
  );
}


// Processing ==============================================================

void settings() {
  fullScreen(P2D);
}

void setup() {
  orientation(LANDSCAPE);

  background(0); // Black background
  frameRate(5);

  String modelName = Build.MODEL;
  String manufacturer = Build.MANUFACTURER;
  String deviceName = manufacturer + " " + modelName;
  println("DeviceInfo", "Device manufacturer: " + manufacturer + " model: "+ modelName);
  if (modelName.equals("SM-S931U")) {
    // Samsung S25
  }
  if (manufacturer.equals("IQH3D") && modelName.equals("SKYY")) {
    conversion = COLUMN_INTERLACE;
  }
  //photo = loadImage(testImage);
  //ready = true;
  //stereo = true;
  //show = false;
  //path = null;
}

//PImage[] splitImageLR(PImage original) {
//  //println("splitImageLR w="+original.width +" h="+original.height+ " width="+width + " height="+height);
//  // Create an array to hold the two resulting images
//  PImage[] result = new PImage[2];

//  // Calculate the width of each half, using integer division
//  int halfWidth = original.width / 2;
//  float ar = ((float)original.width/2) / (float)original.height;
//  //println("original ar="+ar);
//  float w = (float)height * ar;
//  int iw = int(w);
//  // Create the left half image
//  result[0] = createImage(iw, height, ARGB);
//  result[0].copy(original, 0, 0, halfWidth, original.height, 0, 0, iw, height);

//  // Create the right half image
//  result[1] = createImage(iw, height, ARGB);
//  result[1].copy(original, halfWidth, 0, halfWidth, original.height, 0, 0, iw, height);


//  //println("halfWidth="+halfWidth);
//  //println("iw="+iw);
//  //println("left w="+result[0].width + " h="+result[0].height);
//  //println("right w="+result[1].width + " h="+result[1].height);

//  return result;
//}

//PImage columnInterlace(PImage bufL, PImage bufR) {
//  // 1. Create a new image with the same dimensions as left side

//  PImage result = createImage(bufL.width, bufL.height, ARGB);

//  bufL.loadPixels();
//  bufR.loadPixels();
//  result.loadPixels();

//  int len = bufL.pixels.length;

//  // 2. Interlace pixels
//  // We use a step of 2 to be much faster than using modulo (%)
//  for (int i = 0; i < len; i += 2) {
//    // Copy even pixel from Left
//    result.pixels[i] = bufL.pixels[i];

//    // Copy odd pixel from Right (check bounds to prevent crash on odd-length arrays)
//    if (i + 1 < len) {
//      result.pixels[i + 1] = bufR.pixels[i + 1];
//    }
//  }

//  result.updatePixels();

//  // Note: Do NOT call bufL.recycle() here because we may want to use
//  // them in the function that called this one.

//  return result;
//}

void displayStatus() {
  int voffset = 7*height/10;
  text("Image Broadcast Downloader version "+ version, 50, voffset );
  text(hostIp+":"+port, 50, height/8 + voffset+50);
  String filename = downloadHelper.getFilename();
  text(filename, 50, voffset + 100);
  String displayStatus = downloadHelper.getDownloadStatus();
  text(displayStatus, 50, voffset +150);
  path = downloadHelper.getPath();
  text("Downloader path "+path, 50, voffset + 200);
}

void draw() {
  background(0);
  textSize(48);
  fill(255);

  boolean start = downloadHelper.isStarted();
  if (!ready || start) {
    displayStatus();
  }

  int status = -1;
  if (start) status = downloadHelper.getStatus();
  //println("getStatus ="+status);
  if (status == 8) {
    newPhoto = true;
  } 

  if (path != null && path.startsWith("/storage") && newPhoto) {
    //if (!show && path != null) {
    try {
      photo = loadImage(path);
      println("photo w="+photo.width + " h="+photo.height);
      if (path.contains("_2x1.")) {
        stereo = true;
      } else {
        stereo = false;
      }
      ready = true;
    }
    catch (Exception e) {
      photo = null;
      ready = false;
      // newPhoto = false;
    }
  }
  if (ready && photo !=null && photo.width >0 && photo.height>0) {
    if (stereo) {
      // compute both stereo and original
      PImage[] imagePair = splitImageLR(photo);
      leftImage = imagePair[0];
      rightImage = imagePair[1];

      // check for anaglyph and column interlace type
      if (conversion == COLUMN_INTERLACE) {
        colImage = createColumnInterlaced3D((Bitmap)(leftImage.getNative()), (Bitmap)(rightImage.getNative()), width, height);
        println("colImage "+colImage);
        sOffset = (width - colImage.width)/2;
        sar = (float)colImage.width / (float)colImage.height;

        Bitmap lt = ((Bitmap)(leftImage.getNative()));
        if (lt != null) lt.recycle();
        leftImage.setNative(null);

        Bitmap rt = ((Bitmap)(rightImage.getNative()));
        if (rt != null) rt.recycle();
        rightImage.setNative(null);

      }
      xOffset = 0; //(width- photo.width)/4;
      ar = (float)photo.width / (float)photo.height;
    }
    ready = false;
    show = true;
    newPhoto = false;
    println("photo processed");
  }
  if (show ) {
    background(0);

    if (conversion== COLUMN_INTERLACE && stereo) {
      //image(colImage, x, 0, width, (float)width/ar);
      //image(colImage, sOffset, 0, sar*height, height);
      //image(colImage, sOffset, 0);

      if (colImage.width < colImage.height)
        image(colImage, sOffset, 0);
      else
        image(colImage, sOffset, 0);
    } else {
      image(photo, xOffset, 0, width, width/ar);
      displayStatus();
    }
  }
}

public void receivedUrl(String url) {
  System.out.println("url="+url);
  downloadHelper.startDownload( url);
  newPhoto = true;
}

// scanImage to make it known to Android file system and apps like Gallery, etc.
void scanImage(String absolutePath) {
  // Trigger media scanner to make image visible in gallery
  MediaScannerConnection.scanFile(getContext(), new String[]{absolutePath},
    new String[]{"image/png"}, null);
  System.out.println( "MediaScannerConnection.scanFile Image saved: " + absolutePath);
}
