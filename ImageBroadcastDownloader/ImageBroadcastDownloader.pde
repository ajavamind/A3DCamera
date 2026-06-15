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
import android.view.KeyEvent;
import com.andymodla.imagebroadcastdownloader.DownloadHelper;
import com.andymodla.imagebroadcastdownloader.UrlSource;
import android.media.MediaScannerConnection;
import android.graphics.Bitmap;
import android.os.Build;
import processing.core.*;
import processing.event.*;
import processing.opengl.*;

import java.util.HashMap;
import java.util.ArrayList;
import java.io.File;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.opengl.GLES20;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import android.app.ActivityManager;
import android.view.KeyEvent;
import android.content.Context; // Required if calling from a Fragment or Service
import static android.content.Context.DISPLAY_SERVICE;
import static android.content.Context.WINDOW_SERVICE;
import static android.content.Context.ACTIVITY_SERVICE;

//import MyDebug;

public static final boolean DEBUG = false;
String modelName;
String manufacturer;
String deviceName;
ActivityManager activityManager;
private DownloadHelper downloadHelper;
private UdpRemoteControl udpRemoteControl;
private UrlSource urlSource;
String hostIp;
int port = 8000;
String version = "1.0";
volatile public String path;
volatile PImage photo;
volatile PImage interlacedImage;
volatile PImage anaImage;
volatile PImage originalImage;
volatile PImage leftImage;
volatile PImage rightImage;
PImage[] imagePair;

String testImage = "IMG_20260330_145622_2x1.jpg";
volatile boolean ready = false;
volatile boolean show = false;
volatile boolean stereo = false;
volatile public boolean newPhoto = false;
volatile boolean statusDisplay = false;
volatile boolean debugLoadImage = false;
volatile public boolean useDownloader = true;

private static final int NO_CONVERSION = 0;
private static final int COLUMN_INTERLACE = 1;
private static final int ROW_INTERLACE = 2;
private static final int ANAGLYPH = 3;
private static final int SBS = 4;
int conversion = NO_CONVERSION;

String[] conversionName = {"NONE", "COLUMN", "ROW", "ANAGLYPH", "SBS"};
float xOffset;
int sOffset;
float ar;
float sar;
Display dispArray[];
int LeTvWidth = 3840;
int LeTvHeight = 2160;
int ScreenWidth;
int ScreenHeight;
boolean LETV = false;
String message1="No Errors";
String message2="";

// Android Activity Life Cycle ======================================================

void onCreate() {  // not called from processing, so this method is never executed!
  // that is why we set up UDP server in start()
  System.out.println("onCreate()");
}

void onStart() {
  System.out.println("onStart()");
  setVisibility();
  if (downloadHelper == null  && useDownloader) {
    println("setup DownloadHelper");
    downloadHelper = new DownloadHelper(getContext());
  }
  urlSource = new UrlSource(downloadHelper, this);

  if (udpRemoteControl == null) {
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
  udpRemoteControl = null;
  imageDestroy();
}


// Processing ==============================================================
int lastKeyCode= -1;

void keyPressed() {
  lastKeyCode = keyCode;
  println("Keycode("+keyCode+")");
}

void mousePressed() {
  statusDisplay = !statusDisplay;
}

void settings() {
  modelName = Build.MODEL;
  manufacturer = Build.MANUFACTURER;
  deviceName = manufacturer + " " + modelName;
  println("DeviceInfo", "Device manufacturer: " + manufacturer + " model: "+ modelName);
  if (modelName.equals("SM-S931U")) {
    // Samsung S25
    //conversion = SBS;
    conversion = ANAGLYPH; // test
    //LETV = true;  // for test
  } else if (manufacturer.equals("IQH3D") && modelName.equals("SKYY")) {
    conversion = COLUMN_INTERLACE;
  } else if (manufacturer.toLowerCase().equals("letv")) {
    conversion = ROW_INTERLACE;
    LETV = true;
    useDownloader = false;  // LeTv has problem with the downloader service or writing photos to storage TODO figure this out
  } else if (manufacturer.equals("LitByLeia") && (modelName.equals("LPD-20W") || modelName.equals("LPD-10W"))) {
    conversion = NO_CONVERSION;
  } else if (manufacturer.equals("Sony") && (modelName.equals("G8142") )) {
    conversion = SBS;
  } else {
    conversion = ANAGLYPH;
  }

  if (LETV) {
    ScreenWidth = LeTvWidth;
    ScreenHeight = LeTvHeight;
    size(ScreenWidth, ScreenHeight);
    fullScreen();
    smooth();
  } else {
    fullScreen(P2D);
  }
}

void setup() {
  orientation(LANDSCAPE);
  background(0); // Black background
  frameRate(5);
  activityManager = (ActivityManager) getActivity().getApplicationContext().getSystemService(ACTIVITY_SERVICE);

  // Get debug information from the DisplayManager
  DisplayManager dm = (DisplayManager) getActivity().getApplicationContext().getSystemService(DISPLAY_SERVICE);
  if (dm != null) {
    dispArray = dm.getDisplays();

    if (dispArray.length>0) {
      //Context displayContext = getActivity().getApplicationContext().createDisplayContext(dispArray[0]);
      //WindowManager wm = (WindowManager)displayContext.getSystemService(WINDOW_SERVICE);
      for (int i=0; i<dispArray.length; i++) {
        println(dispArray[i].toString());
      }
    }
  }
  // change the surface to desired 3840x2160 actual TV screen dimensions
  // because LeTV default screen for apps is 1920x1080
  if (LETV) {
    try {
      //getSurface().getSurfaceHolder().addCallback(this);
      getSurface().getSurfaceHolder().setFixedSize(LeTvWidth, LeTvHeight);
      GLES20.glViewport(0, 0, LeTvWidth, LeTvHeight);
    }
    catch (Exception e) {
    }
    // wait for surface size change, no callback kludge
    delay(200);
  }
  println("setup() ready="+ready);
}

//@Override
//public void surfaceCreated(SurfaceHolder holder) {

//}

//@Override
//public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//    ready = true;
//    println("surfaceChanged "+ format + " width="+width+ " height="+height);
//}

//@Override
//public void surfaceDestroyed(SurfaceHolder holder) {

//}

void draw() {
  background(0);
  textSize(48);
  fill(255);

  // Process key commands
  if (lastKeyCode > 0) {
    if (lastKeyCode == 4429) {  // key 123 on LeTv remote
      debugLoadImage = !debugLoadImage;
    } else if (lastKeyCode == 166 ) {  // channel up TV
      conversion++;
      if (conversion >= conversionName.length) {
        conversion = NO_CONVERSION;
      }
      newPhoto = true;
    } else if (lastKeyCode == 167) {  // channel down TV
      conversion--;
      if (conversion < 0) {
        conversion = SBS;
      }
      newPhoto = true;
    } else if (lastKeyCode == KeyEvent.KEYCODE_VOLUME_UP) {  // volume up
      conversion++;
      if (conversion >= conversionName.length) {
        conversion = NO_CONVERSION;
      }
      newPhoto = true;
    } else if (lastKeyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {  // volume down
      conversion--;
      if (conversion < 0) {
        conversion = SBS;
      }
      newPhoto = true;
    } else {
      int kc = lastKeyCode - KeyEvent.KEYCODE_0;
      conversion = (kc >= 0 && kc < conversionName.length) ? kc: 0;
      newPhoto = true;
    }

    lastKeyCode = -1; // clear keyCode
  }

  boolean start = false;
  if (useDownloader) {
    start = downloadHelper.isStarted();
  } else {
    start = true;
  }

  if (!ready || start) {
    displayStatus();
  }

  if (useDownloader) {
    int status = -1;
    if (start) status = downloadHelper.getStatus();
    //println("getStatus ="+status);
    if (status == 8) {
      newPhoto = true;
    }
  }

  if (path != null && newPhoto) {
    try {
      System.out.println("loadImage("+path+")");
      if (!debugLoadImage) {
        photo = loadImage(path);
      } else {
        photo = loadImage(testImage);
      }
      if (photo != null && photo.width>0 && photo.height>0) {
        System.out.println("Valid image path="+ path + " photo w="+photo.width + " h="+photo.height);
      } else {
        message1 = "loadImage Error path="+path + " photo null or not read";
      }
      if (path.contains("_2x1.")) {  // check if stereo suffix present
        stereo = true;
      } else {
        stereo = false;
      }
      ready = true;
      newPhoto = false;
    }
    catch (Exception ex) {
      System.out.println(ex.getClass().getSimpleName());
      //ready = false;
      System.out.println("error loadImage "+path);
      fill(255);
      message1 = "loadImage exception "+ ex.getClass().getSimpleName();
      message2 = "path="+path;
      newPhoto = false;
    }
  }
  if (ready && photo !=null && photo.width >0 && photo.height>0) {
    if (stereo) {
      // Convert image format for stereo display

      recycle(leftImage);
      recycle(rightImage);

      imagePair = splitImageLR(photo);
      leftImage = imagePair[0];
      rightImage = imagePair[1];
      imagePair[0] = null;
      imagePair[1] = null;

      // Letv has a very small RAM computer (3GB)
      // we have to conserve memory to avoid out of memory
      if (LETV) recycle(photo); // not using photo any more

      // check for conversion types
      switch(conversion) {
      case COLUMN_INTERLACE:
      case ROW_INTERLACE:
        interlacedImage = interlaced3D(conversion, (Bitmap)(leftImage.getNative()), (Bitmap)(rightImage.getNative()), width, height);
        sOffset = (width - interlacedImage.width)/2;
        sar = (float)interlacedImage.width / (float)interlacedImage.height;
        break;
      case ANAGLYPH:
        anaImage = colorAnaglyph3D((Bitmap)(leftImage.getNative()), (Bitmap)(rightImage.getNative()), 0, 0);
        break;
      case SBS:
        // assumes photo is split only
        break;
      default:
        break;
      }
    }
    ready = false;
    show = true;
    newPhoto = false;
    println("photo processed conversion="+conversion + " stereo="+stereo);
  }
  if (show ) {
    background(0);

    if ((conversion == COLUMN_INTERLACE) && stereo) {
      if (interlacedImage.width < interlacedImage.height)
        image(interlacedImage, sOffset, 0);
      else
        image(interlacedImage, sOffset, 0);
    } else if (conversion == ROW_INTERLACE && stereo) {
      if (interlacedImage.width < interlacedImage.height)
        image(interlacedImage, sOffset, 0);
      else
        image(interlacedImage, sOffset, 0);
    } else if (conversion == ANAGLYPH && stereo) {
      ar = (float)anaImage.width / (float)anaImage.height;
      xOffset = ((float)width - ((float)height*ar))/2.0;
      image(anaImage, xOffset, 0, (float)height*ar, height);
    } else if (conversion == SBS) {
      xOffset = 0; //(width- photo.width)/2;
      ar = (float)photo.width / (float)photo.height;
      // assume SBS
      float h = ((float)width)/ar;
      image(photo, 0, ((float)height-h)/2, width, h);
    } else { // no conversion
      xOffset = 0; //(width- photo.width)/2;
      ar = (float)photo.width / (float)photo.height;
      image(photo, xOffset, 0, (float)height*ar, height);
    }
    if (statusDisplay) {
      displayStatus();
    }
  }
}

void recycle(PImage leftImage, PImage rightImage) {
  recycle(leftImage);
  recycle(rightImage);
}

void recycle(PImage img) {
  if (img == null) return;

  Bitmap bitmap = ((Bitmap)(img.getNative()));
  if (bitmap != null) {
    if (!bitmap.isRecycled()) bitmap.recycle();
  }
  img.setNative(null);
}

void imageDestroy() {
  recycle(photo);
  recycle(interlacedImage);
  recycle(anaImage);
  recycle(originalImage);
  recycle(leftImage);
  recycle(rightImage);
}

void displayStatus() {
  int voffset = 6*height/10;
  int inc = 50;
  int margin = 50;
  int memoryClassMb = activityManager.getMemoryClass();
  int largeMemoryClassMb = activityManager.getLargeMemoryClass();

  text(message1, margin, 100);
  text(message2, margin, 100 + inc);

  text("Image Broadcast Downloader version "+ version + " " + manufacturer + " "+modelName, margin, voffset );
  voffset += inc;
  text("Listening On "+hostIp+":"+port, margin, voffset);
  voffset += inc;
  text("Standard Heap Limit: " + memoryClassMb + " MB"+" Large Heap Limit: " + largeMemoryClassMb + " MB", margin, voffset);
  voffset += inc;
  text("stereo="+stereo+ " conversion="+conversionName[conversion] + " screen width="+width +" height="+height, margin, voffset);
  voffset += inc;
  text("LETV="+LETV+" useDownloader="+useDownloader + " testImage="+debugLoadImage, margin, voffset);
  voffset += inc;
  if (downloadHelper != null) {
    String filename = downloadHelper.getFilename();
    text(filename, margin, voffset );
    voffset += inc;
    String displayStatus = downloadHelper.getDownloadStatus();
    text(displayStatus, margin, voffset);
    voffset += inc;
    if (useDownloader) path = downloadHelper.getPath();
  }
  text("Downloader path "+path, margin, voffset + inc);
  voffset += inc;
}

// scanImage to make it known to Android file system and apps like Gallery, etc.
// Not needed when using Downloader service.
void scanImage(String absolutePath) {
  // Trigger media scanner to make image visible in gallery
  MediaScannerConnection.scanFile(getContext(), new String[]{absolutePath},
    new String[]{"image/png"}, null);
  System.out.println( "MediaScannerConnection.scanFile Image saved: " + absolutePath);
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
