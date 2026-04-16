# Photo Booth Key Function Guide

This document describes the functions of Photo Booth key commands handled by the `processKeyCode()` method in `PhotoBooth.java`. The functions are ordered alphabetically by the action they perform.

***

### Cycle Display Mode
*   **Key:** `A`
*   **Function:** Cycles the display mode through available options (SBS, Anaglyph, Left, Right). This allows the user to switch how the stereo images are presented on the screen.

### Decrease Parallax
*   **Key:** `Minus` (`-`)
*   **Function:** Decreases the parallax value by 2. This adjustment affects the spatial offset of the images, allowing fine-tuning of the stereo window.

### Increase Parallax
*   **Key:** `Plus` (`+`) or `Equals` (`=`)
*   **Function:** Increases the parallax value by 2. This adjustment affects the spatial offset of the images, allowing fine-tuning of the stereo window.

### Screenshot
*   **Key:** `C`
*   **Function:** Triggers a screenshot capture. When pressed, save the current display canvas to the Picture/Screenshots storage.

### Share for AI Edit
*   **Key:** `Forward` (Media Button)
*   **Function:** Shares the current image file with the AI Editing package. This allows the user to edit the photo with AI.

### Toggle Blank Screen
*   **Key:** `B`
*   **Function:** Toggles the visibility of current screen with a blank screen.

### Toggle Cross-Eye
*   **Key:** `X`
*   **Function:** Toggles the cross-eye effect where right image appears on the left and the left image appears on the right. 

### Toggle Debug Logging
*   **Key:** `Period` (`.`)
*   **Function:** Toggles the internal `DEBUG` flag. This controls the display of detailed logging information on the console during the application's operation.

### Toggle Mirror
*   **Key:** `M`
*   **Function:** Toggles the mirror effect on and off. When active, the images are horizontally flipped, simulating a mirror view.

### Toggle Test Mode
*   **Key:** `Space`
*   **Function:** Toggles the `testMode` flag. When enabled, the application displays debug information, such as current parallax, vertical offset, and mirror status, on the screen.

### Toggle Zoom State
*   **Key:** `Z`
*   **Function:** Toggles zoom on or off. This switches the image display between its normal size and a magnified view. It does not camera zoom.

### View Help/Parameters
*   **Key:** `H`
*   **Function:** Displays detailed information about the camera parameters. This function prints the available parameter details provided by the camera hardware to the console.

### Zoom In
*   **Key:** `Right Bracket` (`]`)
*   **Function:** Increases the magnification level by incrementing `magnifyIndex`. This allows the user to zoom in on the captured image, with increased magnification levels.

### Zoom Out
*   **Key:** `Left Bracket` (`[`)
*   **Function:** Decreases the magnification level by decrementing `magnifyIndex`. This allows the user to zoom out from the captured image, with lower magnification levels.
