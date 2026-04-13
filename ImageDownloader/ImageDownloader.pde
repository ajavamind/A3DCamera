//package com.andymodla.imageloader;
/**
 * A Processing Java Android sketch for dowloading images from a http server
 * It uses a http server to allow an app to request a download image file link with a GET command
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
 Brightcove
 Brightcove
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
import com.andymodla.imagedownloader.TinyWebServer;
import com.andymodla.imagedownloader.DownloadHelper;
import android.media.MediaScannerConnection;
import android.graphics.Bitmap;

private DownloadHelper downloadHelper;
String ip ="";
int port = 9000;
String version = "1.0";
String path;
PImage photo;
PImage originalImage;
PImage leftImage;
PImage rightImage;
String testImage = "IMG_20260330_145622_2x1.jpg";
boolean ready = false;
boolean stereo = true;

void onCreate() {  // not called from processing
  System.out.println("onCreate()");
}

void onStart() {
  System.out.println("onStart()");
  ip = getHostnameAddress();
  println("ip="+ip);
  if (downloadHelper == null) {
    downloadHelper = new DownloadHelper(getContext());
  }
  //call contructor with local ip, port , public html directory path
  println("start web server");
  TinyWebServer.startServer(ip, port, "", downloadHelper);
}

void onDestroy() {
  //stop webserver on destroy of service or process
  TinyWebServer.stopServer();
}

void settings() {
  fullScreen(P2D);
}

void setup() {
  orientation(LANDSCAPE);
  /* debug code
   //String url = "http://10.0.0.54:8333"+File.separator+"IMG_20260219_135041_2x1.jpg";
   //downloadHelper.enqueueDownload(url);
   //downloadHelper.startDownload( url);
   //Intent serviceIntent = new Intent(getContext(), DownloadServerService.class);
   //getContext().startForegroundService( serviceIntent);
   */
  /* test code
   originalImage = loadImage(testImage);
   if (originalImage == null) {
   println("ERROR: Could not load image. Make sure '" + testImage + "' is in the data folder.");
   // If the image fails to load, we stop the sketch
   exit();
   }
   
   PImage[] imagePair = splitImageLR(originalImage);
   leftImage = imagePair[0];
   rightImage = imagePair[1];
   
   // Clear the screen every frame
   background(0); // Black background
   
   // Calculate the width of each resulting image
   //float halfWidth = leftImage.width;
   //float totalHeight = leftImage.height;
   //float AR = halfWidth/totalHeight;
   
   //image(leftImage, 0, 0, width/2, totalHeight/AR);
   //image(rightImage, width/2, 0, width/2,totalHeight/AR);
   PImage colImage = columnInterlace(leftImage, rightImage);
   
   image(colImage, 0, 0);
   */
  background(0); // Black background
  frameRate(1);
}

PImage[] splitImageLR(PImage original) {
  println("splitImageLR");
  // Create an array to hold the two resulting images
  PImage[] result = new PImage[2];

  // Calculate the width of each half, using integer division
  int halfWidth = original.width / 2;
  if (halfWidth % 2 == 1) halfWidth--;
  // Create the left half image
  result[0] = createImage(halfWidth, original.height, ARGB);
  result[0].copy(original, 0, 0, halfWidth, original.height, 0, 0, halfWidth, original.height);

  // Create the right half image
  result[1] = createImage(halfWidth, original.height, ARGB);
  result[1].copy(original, halfWidth, 0, halfWidth, original.height, 0, 0, halfWidth, original.height);
  //if (DEBUG) {
    println("halfWidth="+halfWidth);
    println("left w="+result[0].width + " h="+result[0].height);
    println("right w="+result[1].width + " h="+result[1].height);  
  //}
  return result;
}

PImage columnInterlace(PImage bufL, PImage bufR) {
  // column interlace merge left and right images
  // reuse left image for faster performance
  //System.gc();
  bufL.loadPixels();
  bufR.loadPixels();
  int len = bufL.pixels.length;
  int i = 0;
  while (i < len) {
    i++;
    bufL.pixels[i] = bufR.pixels[i];
    i++;
  }
  bufL.updatePixels();
  return bufL;
}


void draw() {

  textSize(48);
  fill(255);
  //textAlign(CENTER, CENTER);
  boolean start = downloadHelper.isStarted();
  if (!ready || start) {
    background(0);

    text("Image Downloader version "+ version, 50, height/16);
    text(ip+":"+port, 50, height/8);
    String filename = downloadHelper.getFilename();
    text(filename, 50, height/4);
    String displayStatus = downloadHelper.getDownloadStatus();
    text(displayStatus, 50, height/4+50);

    path = downloadHelper.getPath();
    text(path, 50, height/2);
  }

  int status = downloadHelper.getStatus();
  if (status != 8) {
    return;
  }

  if (path != null && path.startsWith("/storage")) {
    try {
      photo = loadImage(path);
      println("photo w="+photo.width + " h="+photo.height);
    }
    catch (Exception e) {
      photo = null;
    }
  }
  if (photo !=null && photo.width >0 && photo.height>0) {
    if (stereo) {
      PImage[] imagePair = splitImageLR(photo);
      leftImage = imagePair[0];
      rightImage = imagePair[1];
      PImage colImage = columnInterlace(leftImage, rightImage);
      int x = (width- colImage.width)/2;
      background(0);
      float ar = (float)colImage.width / (float)colImage.height;
      //image(colImage, x, 0, width, (float)width/ar);
      image(colImage, 0, 0, ar*height, height);
      Bitmap lt = ((Bitmap)(leftImage.getNative()));
      if (lt != null) lt.recycle();
      leftImage.setNative(null);
      Bitmap rt = ((Bitmap)(rightImage.getNative()));
      if (rt != null) rt.recycle();
      rightImage.setNative(null);

      //image(colImage, x, 0);
      //image(colImage, 0, 0);
      ready = true;
    } else {
      background(0);
      int x = 0; //(width- photo.width)/4;
      float ar = (float)photo.width / (float)photo.height;
      //image(photo, x, 0, width, (float)width/ar); 
      image(photo, x, 0, width, (float)width/ar);
      ready = true;

    }
  }
}

private String getHostnameAddress() {
  try {
    for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en
      .hasMoreElements(); ) {
      NetworkInterface intf = en.nextElement();
      Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces();
      String last = null;
      while (niEnum.hasMoreElements()) {
        NetworkInterface ni = niEnum.nextElement();
        if (!ni.isLoopback() && !ni.isPointToPoint()) {
          for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
            println( "network prefix length=" + interfaceAddress.getNetworkPrefixLength());
            if (interfaceAddress.getAddress() != null) {

              println( interfaceAddress.getAddress().getHostAddress());
              last = (interfaceAddress.getAddress().getHostAddress());
              if (last.matches("\\d*\\.\\d*\\.\\d*\\.\\d*")) {
                return last;
              }
            }
          }
        }
      }
    }
  }
  catch (SocketException ex) {
    println("Socket Exception");
  }
  return null;
}

void scanImage(String absolutePath) {
  // Trigger media scanner to make image visible in gallery
  MediaScannerConnection.scanFile(getContext(), new String[]{absolutePath},
    new String[]{"image/png"}, null);
  System.out.println( "MediaScannerConnection.scanFile Image saved: " + absolutePath);
}

void mouseReleased() {
  stereo = !stereo;
}

//DownloadManager.Query query = new DownloadManager.Query();
//query.setFilterById(downloadId);
//Cursor cursor = downloadManager.query(query);

//if (cursor.moveToFirst()) {
//    int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
//    int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));
//    // Use status and reason constants to handle the state
//}
//cursor.close();
