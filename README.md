# A3DCamera
A personal 3D Camera Android App Project

This is a personal camera project intended for 3D photography hobbyists and experimenters.
The app runs on the Xreal Beam Pro device and takes 3D photos exclusively. It is not intended to replace the native app.
It is a starting point for special purpose experimental 3D camera apps with the Beam Pro camera.

## Functions
1. Only photos captured, no video.
2. The camera is set to fixed focus  approximately 166 cm, which is the hyper focal distance of the lens.
Note the code sets the hyper focal distance of 0.60356647f in diopters.
The hyper focal distance in cm is one divided by this value.
So it is considered sharp from 83 cm (half the hyperfocal distance) and beyond.
Note that the camera LENS_FOCUS_DISTANCE_CALIBRATION is APPROXIMATE.
4. Auto exposure adjusts to subject lighting that may not be what the photographer would like. There is no photographer control.
5. The app stores image files in the "Pictures/A3DCamera" folder.
6. Back Left Camera and Right Camera images are stored as "_l" and "_r" suffix filename jpg files.
7. Analglyph conversion stored as "_ana" suffix jpg file.
8. Side-by-side stereo images are not stored by the camera.
9. Photo capture is made with these camera keys: camera, volume up, or volume down after key release. No touch screen capture.
10. The app display is a centered viewfinder sized to permit use of stereoscopic "free-viewing". This is a learned eye relaxing technique you can use to help see your subject in 3D. See 
[Learning To Free View](https://stereoscopy.blog/2022/03/11/learning-to-free-view-see-stereoscopic-images-with-the-naked-eye/).
11. Images have some EXIF capture information: example- IMG20250904_r.jpg f2.2, 1/3 second, 2.16mm, ISO413
12. Each camera image captured is 4080 x 3072 pixels (4/3 ratio).
13. The app does not align or adjust the stereo window.
14. As a hobbyist app the user is encouraged to use [Stereo Photo Maker (English)](https://stereo.jpn.org/eng/stphmkr/) to align left and right images vertically, correct any horizontal perspective distortion, and set the most pleasing stereo window. It also creates side-by-side photos and other formats.
15. Images may also need more contrast, color saturation and sharpening. Use Photo editors.
16. There are no camera leveling or tilt suggestions from the app - what you see is what you get.

## Usage
1. I discovered my camera lens vertical alignment is only off by 3 pixels so that live free-viewing is possible without eye strain for me. But I cannot be too close to the subject.
2. Distance to the subject should be about 1.5 meter to match the 50mm camera lens interaxial separation distance.
3. Synchronization of the camera lens shutters is not known. However the shutter speed is automatically set by the camera so motion blur is possible.
4. I use a Bluetooth remote to take photos instead of the button keys on the camera. This requires pairing with a remote controller or keyboard.

## App Download Link

Version 1.0 https://drive.google.com/file/d/1EDfRDsRwNqKlmn5RrDLSUZ3KBl2emwbJ/view?usp=sharing

## Credits

Thanks to Wilbert Brants for a code sample example for 3D camera control. 
