# Photo Booth

# Photo Booth Block Diagram

![Photo Booth Block Diagram](../images/PhotoBoothBlockDiagram.png)

# Photo Booth Equipment

## XBP - XReal Beam Pro (256 GB version) 3D Camera Tablet
The XBP is an Android tablet with a 3D camera. For this application it functions as a 3D camera, photo booth controller, and photo server.

The A3DCamera app has not been tested with a XBP with only 128 GB storage and 6 GB RAM.
It is not known how well it will perform with less RAM.

## Buzzer Box
This is a repurposed Bluetooth mouse with rewired buttons to push-button switches in a box.
It used to capture photo booth images and review the results.
This device has to be Bluetooth paired with the XBP. The buzzer box would normally be used by a photo booth posing assistant.

Any Bluetooth mouse may be used to function like a buzzer box without rewiring.

## Bluetooth Keyboard
A XBP paired Bluetooth keyboard is needed to configure the A3DCamera for photo booth mode.
It can also serve as a Buzzer box substitute when the keyboard includes mouse features.

I use the Rii i4 Mini Bluetooth Keyboard with Touchpad, Blacklit Portable Wireless Keyboard with 2.4G USB Dongle for Smartphones, PC, Tablet, Laptop TV Box iOS Android Windows Mac.
It is small and has a built-in mouse with buttons.

## Mobile Monitor 
This touch screen 1920x1080 monitor displays the photo booth live view and review photos.
My monitor is TopMonitor PDM-15T with 15.6 inch touch screen and two USB-C inputs (used for video-in and power) and integrated speaker.

## IQH3D SKYY Android Glasses Free 3D Tablet
This tablet shows photo captures from the photo booth in real time.
It is connected to the local Wi-Fi network and configured as a fixed static network address 192.168.8.99.

## Canon CP1300 4x6 Photo Printer
The networked printer used to print photo booth images.

## Network Router
This Wi-Fi network router connects all the devices used with the photo booth. It also connects to the Internet providers for AI Image editing services.

## Additional Cameras
Not shown in the block diagram are other cameras that can be triggered over Wi-Fi from the A3DCamera app when it starts to take a photo. 
This is accomplished using network broadcast messages to waiting networked based cameras.
When these cameras are Android phones with the [MultRemoteCamera](https://sourceforge.net/p/multi-remote-camera/wiki/Home/) Android app, the photo files will have the same date/time as the corresponding A3DCamera app photo.

In my photo booth I also use two Samsung S8 phones (introduced in 2017) each using the MultiRemoteCamera app for a custom 3D rig. 
This phone's images are better quality than XBP (introduced in 2024) images and are not as wide angle.
The images need post processing with Stereo Photo Maker to align and convert to 3D and set the stereo window (parallax).
This is not done while the booth is in operation, although it is possible if the cameras also have the Simple HTTP Server PLUS Android App installed and running

Other XReal Beam Pro cameras using A3DCamera can be configured to receive these broadcast messages and trigger photo captures at the same time.

## Miscellaneous
Camera tripod, brackets, cables, power adapters, and optional photo lamp.

# Photo Booth Software

## A3DCamera Android App
The A3DCamera app runs on the XBP 3D camera tablet. The A3DCamera app is the photo booth camera and controller.
In photo booth mode the XBP is normally connected to a local Wi-Fi network.
The app saves 3D SBS (side-by-side), anaglyph, and both left and right photos. 
It adjusts SBS and anaglyph photos for parallax and vertical alignment before saving.
The SBS images have reduced resolution because the camera has limited processor speed and RAM memory for processing images.
The other saved photos have maximum resolution taken.

The app must first be configured as a photo booth. Configuration is done by commands using a keyboard.
The command is //pb=true<enter>

This app uses the touch screen for shutter release only in camera photo booth mode when the tablet is hand held. 
The shutter button is not visible and is at the display's upper right top corner.

## Simple HTTP Server PLUS Android App
This app runs on the XReal Beam Pro. It is a HTTP server that supplies the saved photos to a networked 3D tablet or notebook computer for review.
This is the purchased app version 3.05. The app is configured to use the photo booth's local Wi-Fi network.

[Simple HTTP Server Plus](https://play.google.com/store/apps/details?id=com.phlox.simpleserver.plus)

## itCamera Android App
The itCamera app controls AI editing of photo booth captures. It sends edit prompt requests to an AI Image editing cloud service. 
Images sent to the service are reduced in size and set to 6x4 aspect ratio (for the printer).
The multimodal LLM model used for photo editing is Google "gemini-3.1-flash-image-preview" a paid service.
This app is a work in progress and is not available as open source.

AI editing is also configured by a command in the A3DCamera app.
The app requires a touch screen to operate.

## ImageDownloader Android App
This Android app runs on the IQH3D SKYY glasses free 3D tablet. It is connected by Wi-Fi to the photo booth local network.
The app has a very simple HTTP server that waits for a message to download a photo from the HTTP server running on the XReal Beam Pro tablet.
Once a new photo is downloaded the app converts it to appear on the tablet screen in 3D with column interlace for presentation.
The app continues to display and  waits for the next photo.

The app is written in Processing Android Java and is open source.
It can also run on the Android Leia tablets both 1 and 2 versions, but has to use the LeiaPlayer app to view in 3D.

The app source code can be modified easily to run on Windows, Linux, or iOS using the Processing.org IDE.
In this way other devices with 3D displays may be able to show 3D images glasses free.

It could also be used on another XReal Beam Pro with its glasses to download and view the photo booth's photos. (But I have not tested this feature)

The APK is not ready for distribution release.

## Notes
In Photo Booth mode the XBP must be placed in developer mode with the "Stay awake" option enabled (Screen will never sleep while charging).
This is needed to keep the app running for 7 hours in the photo booth with power connected to charge the XBP.

Since the booth may run for hours, all Android devices used by the booth will require developer mode "Stay awake" enabled to prevent a screen timeout. 
These devices must be charging to prevent sleep.

# Photo Booth Configuration

See [Photo Booth Configuration](PhotoBoothConfiguration.md) documentation.
