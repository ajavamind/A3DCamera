package com.andymodla.imagedownloader;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.URLUtil;
import java.io.File;

public class DownloadHelper {
  private Context context;
  DownloadManager downloadManager;
  long downloadId;
  String fileName = "";
  String path = "";
  int status;
  int reason;
  boolean start =  false;

  public DownloadHelper(Context context) {
    this.context = context;
    downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
  }

  //public void enqueueDownload(String url) {
  //  String fileName = android.webkit.URLUtil.guessFileName(url, null, null);
  //  System.out.println("url="+url);
  //  System.out.println("filename="+fileName);
  //  android.app.DownloadManager.Request request = new android.app.DownloadManager.Request(android.net.Uri.parse(url))
  //    .setTitle("Downloading Image")
  //    //.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
  //    //.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_PICTURES, "Photobooth" + File.separator +fileName);
  //    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
  //  android.app.DownloadManager manager = (android.app.DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
  //  if (manager != null) {
  //    manager.enqueue(request);
  //    System.out.println("manager.enqueue()");
  //  }
  //}

  public void startDownload(String url) {
    System.out.println("startDownload()");

    // Extract a filename from the URL or use a default
    fileName = URLUtil.guessFileName(url, null, null);
    System.out.println("startDownload url="+url);
    System.out.println("filename="+fileName);
    System.out.println(Uri.parse(url));

    // Create the destination directory if it doesn't exist
    File destDir = new File(
      Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
      "Photobooth"
      );
    if (!destDir.exists()) {
      boolean created = destDir.mkdirs();
      System.out.println("Dir created: " + created + " `=" + destDir.getAbsolutePath());
    }
    path = destDir.getAbsolutePath() + File.separator + fileName;
    System.out.println("path="+path);

    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
    request.setTitle("Downloading Image");
    request.setDescription(fileName);

    // Show download progress in the system notification bar
    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

    // Add browser-like headers so the server doesn't reject the request
    request.addRequestHeader("User-Agent",
      "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 " +
      "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
    request.addRequestHeader("Accept", "image/webp,image/apng,image/*,*/*;q=0.8");
    request.addRequestHeader("Accept-Language", "en-US,en;q=0.9");
    request.addRequestHeader("Referer", url); // some servers check this

    // Save to public Pictures folder (No storage permission needed on Android 10+)
    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "Photobooth" + File.separator + fileName);

    DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
    if (manager != null) {
      downloadId = manager.enqueue(request);
      System.out.println("Download started...");
      start = true;
    }
  }

  void checkDownload() {
  }

  public String getFilename() {
    return fileName;
  }

  public String getPath() {
    return path;
  }

  public boolean isStarted() {
    return start;
  }

  public int getStatus() {
    DownloadManager.Query query = new DownloadManager.Query();
    query.setFilterById(downloadId);
    android.database.Cursor cursor = downloadManager.query(query);
    if (cursor.moveToFirst()) {
      status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
      reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));
      //System.out.println( "Status: " + status + " Reason: " + reason);
      cursor.close();
      return  status;
    }
    return -1;
  }

  public String getDownloadStatus() {
    DownloadManager.Query query = new DownloadManager.Query();
    query.setFilterById(downloadId);
    android.database.Cursor cursor = downloadManager.query(query);
    if (cursor.moveToFirst()) {
      status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
      reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));
      //System.out.println( "Status: " + status + " Reason: " + reason);
      cursor.close();
      if (status == 8 && reason == 0) start = false;
      return "Status=" + status + " Reason=" + reason;
    }
    return "No download found";
  }
}
