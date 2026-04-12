package com.andymodla.android3dcamera;

import android.view.KeyEvent;
import android.view.View;
import com.google.android.material.snackbar.Snackbar;

public class CommandLine {
    private final StringBuilder cmdBuffer;
    private final Snackbar mSnackbar;
    View rootView;
    Parameters mParameters;
    
    // New feature: Tracks the current cursor position (0 to cmdBuffer.length())
    private int cursorPosition = 0; 

    /*
     * Command line instructions for Parameter values
     * Debugging feature until Graphical User Interface is implemented
     *
     *
     * Constructor
     * @param mainActivity
     * @param parameters
     * @param initialMessage
     * @return
     */
    public CommandLine(MainActivity mainActivity, Parameters parameters, String initialMessage) {
        cmdBuffer = new StringBuilder();
        //rootView = mainActivity.findViewById(R.id.overlay_text); // Or any appropriate view
        rootView = mainActivity.decorView; // Or any appropriate view
        mParameters = parameters;
        mSnackbar = Snackbar.make(rootView, initialMessage, Snackbar.LENGTH_SHORT);
        mSnackbar.show();
    }
 
    /**
     * Processes key events, handling text input, cursor movement, and commands.
     * 
     * @param keyCode The key code of the event.
     * @param ch The Unicode character.
     * @return true if the key was handled.
     */
    public boolean processCommandLineKey(int keyCode, char ch) {
        // --- Command and Action Keys ---
        if (keyCode == KeyEvent.KEYCODE_SLASH) {
            cmdBuffer.setLength(0); // Clear the buffer
            cmdBuffer.append('/');
            cursorPosition = 1; // Start cursor after the /
            mSnackbar.setDuration(Snackbar.LENGTH_INDEFINITE);
            updateDisplay();
            return true;
        }

        // --- Cursor Movement Keys ---
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            // Move cursor left, but not before position 0
            if (cursorPosition > 0) {
                cursorPosition--;
                updateDisplay();
                return true;
            }
            return false;
        }
        
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            // Move cursor right, but not past the end of the buffer
            if (cursorPosition < cmdBuffer.length()) {
                cursorPosition++;
                updateDisplay();
                return true;
            }
            return false;
        }
        
        if (cmdBuffer.length() == 0 && keyCode != KeyEvent.KEYCODE_DPAD_LEFT && keyCode != KeyEvent.KEYCODE_DPAD_RIGHT) {
             // If buffer is empty and we aren't navigating, ignore input
            return false;
        }

        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            processCommand();
            return true;
        }

        // --- Delete Key (Backspace: Remove the character BEFORE the cursor) ---
        if (keyCode == KeyEvent.KEYCODE_DEL) {
            // Only delete if the cursor is not at the beginning
            if (cursorPosition > 0) {
                cmdBuffer.deleteCharAt(cursorPosition - 1);
                // Move cursor back one position since a character was deleted before it
                cursorPosition--;
                updateDisplay();
                return true;
            }
            // If cursor is at the start, nothing happens
            return true;
        }

        // --- Delete Key (Forward Delete: Remove the NEXT character) ---
        if (keyCode == KeyEvent.KEYCODE_FORWARD_DEL) {
            // Delete the character located at the current cursorPosition
            if (cursorPosition < cmdBuffer.length()) {
                cmdBuffer.deleteCharAt(cursorPosition);
                // The cursor stays in place relative to the characters, but since one was removed,
                // the cursorPosition now points to the new character at that spot.
                updateDisplay();
                return true;
            }
            // If cursor is at the end, DEL does nothing (Forward Delete behavior)
            return true; 
        }


        // --- Character Insertion ---
        if (ch != 0) {
            // Insert the character at the cursorPosition
            cmdBuffer.insert(cursorPosition, ch);
            // Move the cursor forward by one position after insertion
            cursorPosition++;
            updateDisplay();
            return true;
        }

        return false;
    }

    /**
     * Updates the display (Snackbar) reflecting the current buffer and cursor position.
     * Note: Since Snackbar doesn't natively support a cursor, we use a visual indicator (e.g., '|') 
     * to represent the cursor location.
     */
    private void updateDisplay() {
        // Generate a string with a cursor indicator '|'
        StringBuilder display = new StringBuilder(cmdBuffer);
        display.insert(cursorPosition, "|");
        
        mSnackbar.setText(display.toString());
        mSnackbar.show();
    }

    private void processCommand() {
        String cmd = cmdBuffer.toString().trim();
        
        // Clear the buffer and reset cursor before processing
        cmdBuffer.setLength(0);
        cursorPosition = 0;

        if (cmd.isEmpty() || cmd.charAt(0) != '/') {
            displayOutput("--> Error: Command must start with /");
            return;
        }

        // Remove the / prefix
        String cmdContent = cmd.substring(1).trim();
        if (cmdContent.isEmpty()) {
            displayOutput("--> Error: No command specified");
            return;
        }
        String result = parseAndExecute(cmdContent);
        displayOutput(result);
    }

    private String parseAndExecute(String cmdContent) {
        int equalsPos = cmdContent.indexOf(" ");

        if (equalsPos == -1) {
            // GET operation - retrieve variable value
            String varName = cmdContent.trim();
            if (varName.isEmpty()) {
                return "--> Error: Variable name is empty";
            }

            String result  = mParameters.findParam(varName, null, false);
            System.out.println("result=" + result);
            return result;
        } else {
            // SET operation - store variable value
            String varName = cmdContent.substring(0, equalsPos).trim();
            String value = cmdContent.substring(equalsPos + 1).trim();

            if (varName.isEmpty()) {
                return "--> Error: Store failed because variable name is empty";
            }

            // Save to SharedPreferences
            return mParameters.findParam(varName, value, true);
        }
    }

    /**
     * Displays output, clears the buffer, and resets the cursor.
     */
    private void displayOutput(String output) {
        // Append output to the buffer for display purposes, but treat it as non-editable results
        cmdBuffer.append(output);
        
        // Since this is output, we reset the cursor position to the end of the new output
        cursorPosition = cmdBuffer.length(); 
        
        mSnackbar.setText(cmdBuffer.toString());
        
        // Reset buffer and cursor after a short duration to clear the screen
        //mSnackbar.setDuration(Snackbar.LENGTH_SHORT);
        mSnackbar.setDuration(Snackbar.LENGTH_LONG);
        mSnackbar.show();
        
        // Clear the buffer immediately after showing the transient output
        cmdBuffer.setLength(0);
        cursorPosition = 0;
    }
}
