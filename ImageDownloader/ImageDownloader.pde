//package com.andymodla.imageloader;
/**
 * A Processing Java Android sketch for dowloading images from a http server
 * It uses a http server to allow an app to request a download image file link with a GET command
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

private DownloadHelper downloadHelper;
String ip ="";
int port = 9000;
String version = "1.0";

void onCreate() {  // not called from processing
  System.out.println("onCreate()");
}

void onStart() {
  System.out.println("onStart()");
  ip = getHostnameAddress();
  println("ip="+ip);
  downloadHelper = new DownloadHelper(getContext());
  //call contructor with local ip, port , public html directory path
  println("start web server");
  TinyWebServer.startServer(ip, port, "", downloadHelper);
}

void onDestroy() {
  //stop webserver on destroy of service or process
  TinyWebServer.stopServer();
}

void setup() {
  size(1080, 800);
  /* debug code
   //String url = "http://10.0.0.54:8333"+File.separator+"IMG_20260219_135041_2x1.jpg";
   //downloadHelper.enqueueDownload(url);
   //downloadHelper.startDownload( url);
   //Intent serviceIntent = new Intent(getContext(), DownloadServerService.class);
   //getContext().startForegroundService( serviceIntent);
   */
}

void draw() {
  background(0);
  textSize(48);
  fill(255);
  text("Image Downloader version "+ version, 50, height/16);
  text(ip+":"+port, 50, height/8);
  String filename = downloadHelper.getFilename();
  text(filename, 50, height/4);
  text(downloadHelper.getDownloadStatus(), 50, height/4+50);
  //noLoop();
}

void mousePressed() {
  loop();
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
