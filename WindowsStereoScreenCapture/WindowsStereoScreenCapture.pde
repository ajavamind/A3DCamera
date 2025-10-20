/*

 C:\Users\andym\Tools\scrcpy-win64-v3.3.3>scrcpy --window-borderless --window-x=0 --window-y=0
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
 
 
 C:\Users\andym\Tools\scrcpy-win64-v3.3.3>scrcpy --tcpip  --window-borderless --window-x=0 --window-y=0
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
int xWindow = 0;
int yWindow = 0;
int widthWindow = 2400;
int heightWindow = 1080;
float fps = 30;
boolean anaglyph = true;
boolean fullScreen = false;

PImage stereoImage;
Robot robot;

// Parallax and vertical alignment adjustments in pixels
int parallax = 50;
int verticalAlignment = 0;
float AR = 1020.0/768.0;
;

// Note: a lot of hard coded values to get window with stereo image from scrcpy display
/*
This code assumes a sbs image at top left corner from A3DCamera app running on Xreal Beam Pro device.
 Use genymobile scrcpy utility displays on your desktop screen.
 Shows by default the anaglyph version of sbs image on desktop window.
 */

void setup() {
  // Set the size of your sketch window for anaglyph image
  size(1020, 768, P2D);

  frameRate(fps);

  try {
    robot = new Robot();
  }
  catch (AWTException e) {
    e.printStackTrace();
  }
}

void draw() {
  background(0);

  takeScreenshot();

  if (screenshotL != null && screenshotR !=null) {
    // Display the captured screenshot within your sketch
    if (anaglyph) {
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
    } else {  // show SBS stereo image, needs a size() adjustment in the setup code
      PGL pgl = beginPGL();
      pgl.colorMask(true, true, true, true);
      image(screenshotL, 0, 0, 1020, 768);
      image(screenshotR, 1020, 0, 1020, 768);
    }
  }
}

void takeScreenshot() {
  //try {
  //Robot robot = new Robot();
  // Define the area of the screen to capture (e.g., entire display)
  // You can adjust these values to capture a specific portion :
  //screenshot = new PImage(robot.createScreenCapture(new Rectangle(xWindow, yWindow, widthWindow, heightWindow)));

  if (fullScreen) {
    screenshotL = new PImage(robot.createScreenCapture(new Rectangle(0, 0, displayWidth/2, displayHeight)));
    screenshotR = new PImage(robot.createScreenCapture(new Rectangle(1920, 0, displayWidth/2, displayHeight)));
  } else {
    // scrcpy window top left of desktop display
    screenshotL = new PImage(robot.createScreenCapture(new Rectangle(140, 156, 1020, 768)));
    screenshotR = new PImage(robot.createScreenCapture(new Rectangle(1200, 156, 1020, 768)));
  }
  //}
  //catch (AWTException e) {
  //  e.printStackTrace();
  //}
}

void keyReleased() {
  if (key == ' ') {
    save("screenhotLR.jpg");
  } else if (key == '.') {
    anaglyph = !anaglyph;
  } else if (key == '+') {
    parallax += 2;
  } else if (key == '-') {
    parallax -= 2;
  }
  println("parallax = "+parallax);
}
