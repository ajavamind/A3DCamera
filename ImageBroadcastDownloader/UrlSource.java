package com.andymodla.imagebroadcastdownloader;
import com.andymodla.imagebroadcastdownloader.DownloadHelper;

public class UrlSource {
  String url;
  DownloadHelper downloadHelper;

  // constructor
  public UrlSource(DownloadHelper downloadHelper) {
    this.downloadHelper = downloadHelper;
  }

  public void receivedUrl(String url) {
    this.url = url;
    System.out.println("urlSource="+url);
    downloadHelper.startDownload( url);
  }
}
