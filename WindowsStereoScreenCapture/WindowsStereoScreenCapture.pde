/*

 C:\Users\andym\Tools\scrcpy-win64-v3.3.3>scrcpy --window-borderless --window-x=0 --window-y=0 -m 2400
 scrcpy 3.3.3 <https://github.com/Genymobile/scrcpy>
 INFO: ADB device found:
 INFO:     -->   (usb)  R4LM47B1186245                  device  X4000
 C:\Users\andym\Tools\scrcpy-win64-v3.3.3\scrcpy-server: 1 file pushed, 0 skipped. 64.5 MB/s (90164 bytes in 0.001s)
 [server] INFO: Device: [XREAL] XREAL X4000 (Android 14)
 INFO: Renderer: direct3d
 INFO: Texture: 2400x1080
 
 
 You must connect an USB cable only once after each phone reboot (you can't keep the TCP/IP mode across reboots without a rooted device, this is an Android limitation).
 
 The first time, plug your device and run scrcpy --tcpip.
 The following times, just run scrcpy --tcpip=192.168.1.103.
 
 For completeness, since Android 11, a wireless debugging option allows to bypass having to physically connect your device directly to your computer.
 
 More details: https://github.com/Genymobile/scrcpy/blob/master/doc/connection.md#tcpip-wireless
 
 
 C:\Users\andym\Tools\scrcpy-win64-v3.3.3>scrcpy --tcpip  --window-borderless --window-x=0 --window-y=0 -m 2400
 scrcpy 3.3.3 <https://github.com/Genymobile/scrcpy>
 INFO: ADB device found:
 INFO:     --> (tcpip)  192.168.1.101:5555              device  X4000
 INFO: Device already connected via TCP/IP: 192.168.1.101:5555
 C:\Users\andym\Tools\scrcpy-win64-v3.3.3\scrcpy-server: 1 file pushed, 0 skipped. 82.1 MB/s (90164 bytes in 0.001s)
 [server] INFO: Device: [XREAL] XREAL X4000 (Android 14)
 INFO: Renderer: direct3d
 INFO: Texture: 2400x1080
 
 */

import java.awt.Robot;
import java.awt.Rectangle;
import java.awt.AWTException;
import processing.opengl.*;

PImage screenshotL, screenshotR;
PImage screenshot;

// assumes 4K computer monitor being used
int xWindow = 0; // top left corner of computer screen
int yWindow = 0;
int widthWindow = 2400;  // size of XBP screen - scrcpy shows this size using parameter -m 2400
int heightWindow = 1080; // size of XBP screen - scrcpy shows this size using parameter -m 2400

int leftx = 140;
int lefty = 156;
int leftw = 1026;
int lefth = 770;

int rightx = 1200;
int righty = 156;
int rightw = 1026;
int righth = 770;

float fps = 30;
static final int SCREEN = 0;
static final int SBS = 1;
static final int ANAGLYPH = 2;
int mode = SCREEN; //ANAGLYPH;
float AR;
PImage stereoImage;
Robot robot;

// Parallax and vertical alignment adjustments in pixels
int parallax = 50;
int verticalAlignment = 0;

;

// Note: a lot of hard coded values to get window with stereo image from scrcpy display
/*
This code assumes a sbs image at top left corner from A3DCamera app running on Xreal Beam Pro device.
 Use genymobile scrcpy utility displays on your desktop screen.
 Shows by default the anaglyph version of sbs image on desktop window.
 */

void setup() {
  // Set the size of your sketch window for anaglyph image
  //size(1020, 768, P2D);
  size(2052, 1080, P2D);
  AR = (float)width/(float)height; // your sketch screen aspect ratio
  frameRate(fps);

  try {
    robot = new Robot();  // screenshot capture robot
  }
  catch (AWTException e) {
    e.printStackTrace();
  }
}

void draw() {
  background(0);

  takeScreenshot();

  if (screenshot != null || screenshotL != null && screenshotR !=null) {
    // Display the captured screenshot within your sketch
    if (mode == ANAGLYPH) {
      int w = screenshotL.width;
      int h = screenshotL.height;
      AR = (float) w / (float) h;
      PGL pgl = beginPGL();
      pgl.colorMask(true, false, false, true);
      pgl.viewport(0, 0, w, h);
      pushMatrix();
      translate(-parallax / 2, -verticalAlignment / 2);
      image(screenshotL, 0, 0, w, (float)w/AR);
      popMatrix();
      endPGL();

      pgl = beginPGL();
      pgl.colorMask(false, true, true, true);
      pgl.viewport(0, 0, w, h);
      pushMatrix();
      translate(parallax / 2, verticalAlignment / 2);
      image(screenshotR, 0, 0, w, (float)w/AR);
      popMatrix();
      endPGL();

      pgl = beginPGL();
      pgl.colorMask(true, true, true, true);
      pgl.viewport(0, 0, w, h);
      pushMatrix();
      fill(0);
      rect(0, 0, parallax/2, height);
      rect(width-parallax/2, 0, parallax/2, height);
      popMatrix();
      endPGL();
    } else if (mode == SBS) {  // show SBS stereo image, needs a size() adjustment in the setup code
      PGL pgl = beginPGL();
      pgl.colorMask(true, true, true, true);
      image(screenshotL, 0, 0, leftw, lefth);
      image(screenshotR, leftw, 0, rightw, righth);
    } else {
      PGL pgl = beginPGL();
      pgl.colorMask(true, true, true, true);
      float sar = (float)screenshot.width/(float)screenshot.height;
      image(screenshot, 0, 0, width, (float)width/sar);
    }
  }
}

void takeScreenshot() {

  if (mode == SCREEN) {
    screenshot = new PImage(robot.createScreenCapture(new Rectangle(xWindow, yWindow, widthWindow, heightWindow)));
  } else {
    screenshotL = new PImage(robot.createScreenCapture(new Rectangle(leftx, lefty, leftw, lefth)));
    screenshotR = new PImage(robot.createScreenCapture(new Rectangle(rightx, righty, rightw, righth)));
  }
}

void keyReleased() {
  if (key == ' ') {
    save("screenhotLR.jpg");
  } else if (key == '.') {
    mode++;
    if (mode >ANAGLYPH) mode = SCREEN;
  } else if (key == '+') {
    parallax += 2;
  } else if (key == '-') {
    parallax -= 2;
  }
  println("parallax = "+parallax);
}
