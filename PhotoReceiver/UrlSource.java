package com.andymodla.photoreceiver;

import com.andymodla.photoreceiver.DownloadHelper;
import com.andymodla.photoreceiver.PhotoReceiver;

public class UrlSource {
  String url;
  DownloadHelper downloadHelper;
  PhotoReceiver imgReceiver;

  // constructor
  public UrlSource(DownloadHelper downloadHelper) {
    this.downloadHelper = downloadHelper;
  }

  public void setImageReceiver(PhotoReceiver  pr) {
    imgReceiver = pr;
  }

  //public void receivedUrl(String url) {
  //  this.url = url;
  //  System.out.println("urlSource="+url);
  //  if (imgReceiver.useDownloader) {
  //    downloadHelper.startDownload( url);
  //  } else {
  //    imgReceiver.
  //  }
  //}

  public void receivedUrl(String url) {
    this.url = url;
    imgReceiver.delay(2000);
    if (imgReceiver.useDownloader) {
      System.out.println("UrlSource.receivedUrl() use Downloader service for url="+url);
      // downloader save the image in Pictures folder storage
      downloadHelper.startDownload( url);
    } else {
      // transfered images are not saved in storage, use loadImage()
      imgReceiver.path = url;
      System.out.println("UrlSource.receivedUrl() use loadImage for url="+imgReceiver.path);
      imgReceiver.newPhoto = true;
      Thread.yield();
    }
  }
}
