# A3DCamera
A personal 3D Camera Android App Project

This is a personal camera project intended for 3D photography hobbyists and experimenters.
The app runs on the Xreal Beam Pro device and takes 3D photos exclusively. It is not intended to replace the native camera app.
It is a starting point for special purpose experimental 3D camera apps using the Xreal Beam Pro camera.

## Design Principles
### Use Cases
The intended uses for the app are situations where the camera is not in your hands and the screen cannot or should not be touched. 
Specific uses I would like to have with a 3D camera app are:

* 3D Photo Booth
* 3D live view, photo capture, or photo viewing using any stereoscope
* Live 3D demonstrations of the stereo window
* Live 3D capture for other types of 3D displays (monitors, TV)
* Remote control of the camera using Bluetooth or a local WiFi network
* Simultaneous multiple 3D cameras remote control
* Anaglyph or side by side parallel image web camera
* Sharing photos via email, for direct printing SBS or Anaglyph, or with other 3D apps like 3DSteroid

The display of the 3D parallel L/R image should be centered on the display and no larger than 130 cm wide for a stereoscope or free-viewing the image to minimize eye strain. 
There is a display mode where only the stereo image appears without controls or other information.
The display can be turned off, but the camera still functions with remote control.

### Camera Control
For the above uses cases the app requires remote key control of its functions, not the touch screen.

With the remote control requirements for the app,  a minimum Bluetooth controller is needed. I want to keep the GUI at a minimum for viewing 3D images and use for displaying information status or settings.
Therefore only key input will determine the camera operation.

I chose the [8BitDo](https://www.8bitdo.com) Micro Bluetooth key controller in its Android mode. With this controller's 15 keys many camera functions can be set or controlled with a single key.
The 8BitDo Micro is sold as key programmable in its keyboard mode, but I found it impossible to modify key codes using the manufacturer's [Google Play Store app](https://play.google.com/store/apps/details?id=com.abitdo.advance). Fortunately the out of the box Android key mode is good enough.

## Camera Functions
### Camera Mode
Captures 3D photos only. A 3D video option is not implemented.

### Focus
The camera is set to fixed focus of approximately 166 cm, which is the hyper focal distance of the lens.
Note the code uses 0.60356647 diopters to set the hyper focal distance.
The hyper focal distance in cm is one divided by this value.
It is considered sharp from 83 cm (half the hyperfocal distance) and beyond.
The camera reports its LENS_FOCUS_DISTANCE_CALIBRATION as APPROXIMATE.
The focus distance options are hyper focal 1.66 meters, photo booth 1 meter, and macro 100 cm. Macro may not be useful for 3D but shows how the lens can focus close.

### Exposure
Auto exposure sets the best subject lighting by automatically changing shutter speed and ISO. There is no manual exposure control implemented. 
The photographer can set the type of exposure metering: Frame Average, Center Weighted, and Spot Metering.

### Image Storage
The app stores image files in the "Pictures/A3DCamera" folder.

There are options for storing 3D photos in several formats. Left and Right Camera images are stored respectively as "_l" and "_r" suffix filename jpg files.
Side by Side parallel left and right images are stored as "_2x1" suffix filename jpg files.
Anaglyph 3D images are stored as "_ana" suffix filename jpg files.
The default is SBS image storage and cannot be altered until menu settings is implemented.

Left and right images contain limited EXIF capture information: for example- IMG20250904_r.jpg f2.2, 1/3 second, 2.16mm, ISO413
Each camera image captured is 4080 x 3072 pixels (4/3 ratio) the full sensor size of each left and right camera.

### Display
The app display is a centered viewfinder sized to permit use of stereoscopic "free-viewing". This is a learned eye relaxing technique you can use to help see your subject in 3D. See 
[Learning To Free View](https://stereoscopy.blog/2022/03/11/learning-to-free-view-see-stereoscopic-images-with-the-naked-eye/).

The display is sized for mounting the camera in a stereoscope.

The app does not vertically align the left and right images nor adjust the stereo window. As a hobbyist app the user is encouraged to use [Stereo Photo Maker (English)](https://stereo.jpn.org/eng/stphmkr/) 
to align left and right images vertically, correct any horizontal perspective distortion, and set the most pleasing stereo window.

### Camera Control
Photo capture uses these camera keys: camera, volume up, or volume down after key release. There is no touch screen capture implemented.
![8BitGo Micro Bluetooth Controller](images/A3DCamera_layout_1080.png)

### Limitations
Captured images are on par in quality with the native camera app. However, with this camera images may still need adjustments for contrast, color saturation, and sharpening.

Color balance adjustments are not implemented. Exposure lock is not implemented.

There are no camera leveling, tilt, or subject distance suggestions from the app.

## Usage
1. I discovered my camera lens vertical alignment is only off by 3 pixels so that live free-viewing is possible without eye strain for me. But I cannot be too close to the subject.
2. Distance to the subject should be about 1.5 meter to match the 50mm camera lens interaxial separation distance.
3. Synchronization of the camera lens shutters is not known. However the shutter speed is automatically set by the camera so motion blur is possible.
4. I use a Bluetooth remote to take photos instead of the button keys on the camera. This requires pairing with a remote controller or keyboard.

## App Download Link

Version 1.0 https://drive.google.com/file/d/1EDfRDsRwNqKlmn5RrDLSUZ3KBl2emwbJ/view?usp=sharing

## Credits

Thanks to Wilbert Brants for his code example for 3D camera implementation. 
