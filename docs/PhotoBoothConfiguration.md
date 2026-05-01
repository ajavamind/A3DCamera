# Parameter Command Reference

This document details the keyboard commands used to configure parameters in the Android 3D Camera application.

## ⌨️ Usage Instructions

*   **Input Method:** Commands are entered using the keyboard.
*   **Command Prefix:** Type `//` followed by the command abbreviation to initiate a command.
*   **Read Value:** Press **Enter** after the command to display the current parameter value.
*   **Set Value:** Type `=setvalue` followed by **Enter** to set the parameter.
    *   *Example:* `//pb=true` followed by Enter sets Photo Booth mode.
*   **String Parameters:**
    *   Do **not** use quotes.
    *   Spaces separate words.
    *   The **Enter** key defines the end of the string.
    *   *Example:* `//t1=3D Photo Booth` sets the title.
*   **Navigation:**
    *   **Left / Right Arrow Keys:** Position the cursor within the input.
    *   **Backspace:** Erases the previous character.
    *   **Delete:** Erases the character after the cursor.
*   **Reset to Default:**
    *   The backslash `\` key resets the parameter to its factory default value.
    *   *Example:* `//pb=\` turns off photo booth mode (resets to default `false`).

## 📷 Configuration Modes

*   **Default Values:** The default parameter values configure the device as a **3D point-and-shoot camera**.
*   **Photo Booth Mode:** When `pb=true`, the camera enters photo booth mode but can still be handheld to take pictures.
*   **Fixed Photo Booth:** With additional settings, the camera is intended to be mounted in a fixed photo booth setup.

## 📋 Parameter Table

All parameters are listed below. The full description for each parameter is provided in the section following this table.

| Abbreviation | Description | Type | Default Value |
| :--- | :--- | :--- | :--- |
| `ai` | AI Edit | boolean | `false` |
| `ar` | Auto Review | boolean | `false` |
| `bl` | Blank Screen | boolean | `false` |
| `cd` | Count Down Enabled | boolean | `false` |
| `ct` | Countdown Timer | int | `0` |
| `fdi` | Focus Distance Index | int | `0` |
| `i1` | Instruction 1 | String | `Look at Camera` |
| `i2` | Instruction 2 | String | `` |
| `mr` | Mirror | boolean | `true` |
| `pb` | Photo Booth | boolean | `false` |
| `px` | Parallax Offset | int | `0` |
| `rip` | Receiver IP | String | `192.168.8.99` |
| `sd` | Sound On | boolean | `true` |
| `sbs` | SBS Crop Print | boolean | `false` |
| `t1` | Title 1 | String | `3D Photo Booth` |
| `t2` | Title 2 | String | `` |
| `uc` | UDP Control Enabled | boolean | `false` |
| `ut` | UDP Transmit | boolean | `false` |
| `vt` | Vertical Offset | int | `0` |

## 📖 Detailed Parameter Descriptions

**//ai**: Turns on AI Edit mode to launch another application that prompts AI to edit a photo.

**//ar**: In photo booth mode after a photo capture keep the booth in review mode until changed by the operator. When false the camera is ready to shoot after showing the last photo briefly.

**//bl**: For covering the active display with black.

**//cd**: Enables or disables the countdown timer for the camera.

**//ct**: Set the camera countdown timer in seconds for shutter release.

**//fdi**: Set the focus distance index: 0 HyperFocal Focus Distance 1.7 m; 1 Photo Booth Focus Distance 550mm; 2 Macro Focus Distance 100mm; 3 Auto Focus Distance.

**//i1**: Photo booth instruction appearing on the top first line.

**//i2**: Photo booth instruction appearing on the top second line.

**//mr**: In photo booth mode it sets the display to a mirror image.

**//pb**: Configures a photo booth operation and display.

**//px**: Camera left and right image parallax offset for stereo window placement.

**//rip**: The IP Address of the device to receive a URL Link for a saved photo.

**//sd**: Turns on/off the shutter sound.

**//sbs**: When enabled the SBS photo is center cropped to fit the printer paper size 6x4.

**//t1**: Photo booth title appearing on the bottom first line.

**//t2**: Photo booth title appearing on the bottom second line.

**//uc**: Enables or disables Wi-Fi UDP broadcast message receive and transmit.

**//ut**: With Wi-Fi broadcast message control enabled, this option mutually exclusive enables transmit or receive only.

**//vt**: Camera left and right image vertical offset alignment for 3D camera correction.
