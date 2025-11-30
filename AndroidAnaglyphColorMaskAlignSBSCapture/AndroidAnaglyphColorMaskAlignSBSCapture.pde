// Anaglyph Camera for Xreal Beam Pro device
// This is experimental software to test approaches for creating a stereo camera with SBS and Anaglyph
// Andy Modla, November 29, 2025 code first published Github
// Written in Java for Processing Android Mode
// uses Ketai camera processing Android library
// KetaiCamera2.java is modified from KetaiCamera.java in library 
// https://github.com/ketai/ketai version v14

import processing.opengl.*;
import ketai.camera.*;

KetaiCamera2 camLeft;
KetaiCamera2 camRight;

// image sensor size and disply fixed for testing
// assumes same dimensions can be used for both front and back cameras for testing
// 
int CAMERA_WIDTH = 960;  // Samsung Galaxy S25 for test
int CAMERA_HEIGHT = 720;
int XBP_CAMERA_WIDTH = 1280;
int XBP_CAMERA_HEIGHT = 960;
int cameraWidth = XBP_CAMERA_WIDTH;  // default
int cameraHeight = XBP_CAMERA_HEIGHT;

// Parallax and vertical alignment adjustments in pixels for XBP
volatile int parallax = 108;
volatile int verticalAlignment = 8;

boolean anaglyph = false;
float AR = 1.3333333f;  // aspect ratio for Xreal Beam Pro camera image sensor

void setting() {
  orientation(LANDSCAPE);
}

void setup() {
  // set size for max Xreal Beam Pro display
  size(2048, 1080, P2D);  // draw canvas size and render using OpenGL
  background(0);
  smooth();

  frameRate(60);  // need fast draw loop to stay ahead of camera 30 FPS

  String manufacturer = android.os.Build.MANUFACTURER;
  String modelName = android.os.Build.MODEL;
  String deviceName = manufacturer + " " + modelName;
  println("Device Manufacturer and Model: " + deviceName);

  if (manufacturer.equals("XREAL") && modelName.equals("X4000")) {
    cameraWidth = XBP_CAMERA_WIDTH;
    cameraHeight = XBP_CAMERA_HEIGHT;
  } else {
    cameraWidth = CAMERA_WIDTH;
    cameraHeight = CAMERA_HEIGHT;
    parallax = 0;
    verticalAlignment = 0;
  }

  camLeft = new KetaiCamera2(this, cameraWidth, cameraHeight, 30);  // camera  sensor width/height aspect ratio is 4:3
  camRight = new KetaiCamera2(this, cameraWidth, cameraHeight, 30);

  // Set camera ids for left and right cameras
  camLeft.setCameraID(0);
  camRight.setCameraID(1);  // front camera 1 for testing only. no mirror image adjustments used here
  // camRight.setCameraID(2);  // set right camera id number (does not work with Xreal Beam Pro)
  // because KetaiCamera2 uses deprecated Camera API not Camera2

  // Start capturing the images from the camera
  camLeft.start();
  camRight.start();
}

void onCameraPreviewEvent()
{
  camLeft.read();
  camRight.read();
}

void draw() {
  if (camLeft != null && camLeft.isStarted() == true && camRight != null && camRight.isStarted()) {
    if (anaglyph) {
      drawAnaglyph(camLeft, camRight);
    } else {
      drawSBS(camLeft, camRight);
    }
  }
}

void drawSBS(PImage imgLeft, PImage imgRight) {
  if (imgLeft == null || imgRight == null) return;
  background(0);
  PGL pgl;
  pgl = beginPGL();
  pgl.viewport(0, 0, width, height);
  pgl.colorMask(true, true, true, true);
  endPGL();

  //image(imgLeft, 0, 180, imgLeft.width/2, ((float)imgLeft.height)/2);
  //image(imgRight, width/2, 180, imgRight.width/2, ((float)imgRight.height)/2);
  image(imgLeft, 0, 180, width/2, ((float)width/2)/AR);
  image(imgRight, width/2, 180, width/2, ((float)width/2)/AR);
}

void drawAnaglyph(PImage imgLeft, PImage imgRight) {
  if (imgLeft == null || imgRight == null) return;
  background(0);

  PGL pgl;
  pgl = beginPGL();
  pgl.viewport(0, 0, width, height);
  pgl.colorMask(true, false, false, true);
  pushMatrix();
  translate(-parallax / 2, -verticalAlignment / 2);
  //image(imgLeft, 0, 0, imgLeft.width, imgLeft.height, 0, 0, imgLeft.width / 2, imgLeft.height);
  image(imgLeft, ((float)width-(float)height*AR)/2, 0, (float)height*AR, height);
  popMatrix();
  endPGL();

  pgl = beginPGL();
  pgl.colorMask(false, true, true, true);
  pgl.viewport(0, 0, width, height);
  pushMatrix();
  translate(parallax / 2, verticalAlignment / 2);
  image(imgRight, ((float)width-(float)height*AR)/2, 0, (float)height*AR, height);
  popMatrix();
  endPGL();

  // used to draw over anaglyph image for hiding overlay areas outside anaglyph image
  // need to change colorMask back before filling with rectangles on edges
  pgl = beginPGL();
  pgl.colorMask(true, true, true, true);
  pgl.viewport(0, 0, width, height);
  endPGL();

  fill(0);
  rect(0, 0, width, verticalAlignment);
  rect(0, height-verticalAlignment, width, verticalAlignment);
  rect(0, 0, parallax/2, height);
  rect(width-parallax/2, 0, parallax/2, height);
}

// toggle display between SBS and anaglyph images
void mousePressed() {
  anaglyph = !anaglyph;
}

// for Bluetooth wireless keyboard input
// used to manually adjust vertical image alignment
// and stereo image parallax (horizontal adjustment to set the stereo window)
void keyPressed() {
  if (key == 'u') {
    verticalAlignment++;
    println("vert="+verticalAlignment);
    return;
  }
  if (key == 'd') {
    verticalAlignment--;
    println("vert="+verticalAlignment);
    return;
  }
  if (key == 'l') {
    parallax += 2;
    println("parallax="+parallax);
    return;
  }
  if (key == 'r') {
    parallax -= 2;
    println("parallax="+parallax);
    return;
  }
}
