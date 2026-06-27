package com.andymodla.imagebroadcastdownloader;
import com.andymodla.imagebroadcastdownloader.DownloadHelper;
import com.andymodla.imagebroadcastdownloader.ImageBroadcastDownloader;

public class UrlSource {
  String url;
  DownloadHelper downloadHelper;
  ImageBroadcastDownloader imageBroadcastDownloader;

  // constructor
  public UrlSource(DownloadHelper downloadHelper, ImageBroadcastDownloader imageBroadcastDownloader) {
    this.downloadHelper = downloadHelper;
    this.imageBroadcastDownloader = imageBroadcastDownloader;
  }

  //public void receivedUrl(String url) {
  //  this.url = url;
  //  System.out.println("urlSource="+url);
  //  if (imageBroadcastDownloader.useDownloader) {
  //    downloadHelper.startDownload( url);
  //  } else {
  //    imageBroadcastDownloader.
  //  }
  //}

  public void receivedUrl(String url) {
    this.url = url;
    imageBroadcastDownloader.delay(2000);
    if (imageBroadcastDownloader.useDownloader) {
      System.out.println("UrlSource.receivedUrl() use Downloader service for url="+url);
      // downloader save the image in Pictures folder storage
      downloadHelper.startDownload( url);
    } else {
      // transfered images are not saved in storage, use loadImage()
      imageBroadcastDownloader.path = url;
      System.out.println("UrlSource.receivedUrl() use loadImage for url="+imageBroadcastDownloader.path);
      imageBroadcastDownloader.newPhoto = true;
      Thread.yield();

    }

  }
}
