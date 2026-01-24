package com.andymodla.android3dcamera;

import android.view.KeyEvent;
import android.view.View;
import com.google.android.material.snackbar.Snackbar;

public class CommandLine {
    private final StringBuilder cmdBuffer;
    private final Snackbar mSnackbar;
    View rootView;
    Parameters mParameters;

    /*
     * Command line instructions for Parameter values
     * Debugging feature until Graphical User Interface is implemented
     *
     *
     * Constructor
     * @param mainActivity
     * @param parameters
     * @return
     */
    public CommandLine(MainActivity mainActivity, Parameters parameters, String initialMessage) {
        cmdBuffer = new StringBuilder();
        rootView = mainActivity.findViewById(R.id.overlay_text); // Or any appropriate view
        mParameters = parameters;
        mSnackbar = Snackbar.make(rootView, initialMessage, Snackbar.LENGTH_SHORT);
        mSnackbar.show();
    }
 
    public boolean processCommandLineKey(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SLASH) {
            cmdBuffer.setLength(0); // Clear the buffer
            cmdBuffer.append('/');
            mSnackbar.setDuration(Snackbar.LENGTH_INDEFINITE);
            updateDisplay();
            return true;
        }
        if (cmdBuffer.length() == 0) {
            return false;
        }
    
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            processCommand();
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_DEL) {
            if (cmdBuffer.length() > 0) {
                cmdBuffer.deleteCharAt(cmdBuffer.length() - 1);
                updateDisplay();
            }
            return true;
        }

        // Get the character from the key event
        char c = (char) event.getUnicodeChar();
        if (c != 0) {
            cmdBuffer.append(c);
            updateDisplay();
            return true;
        }

        return false;
    }

    private void updateDisplay() {
        mSnackbar.setText(cmdBuffer.toString());
        mSnackbar.show();
    }

    private void processCommand() {
        String cmd = cmdBuffer.toString().trim();
        cmdBuffer.setLength(0);

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
        int equalsPos = cmdContent.indexOf('=');

        if (equalsPos == -1) {
            // GET operation - retrieve variable value
            String varName = cmdContent.trim();
            if (varName.isEmpty()) {
                return "Error: Variable name is empty";
            }

            String result  = mParameters.findParam(varName, null, false);
            System.out.println("result=" + result);
            return result;
        } else {
            // SET operation - store variable value
            String varName = cmdContent.substring(0, equalsPos).trim();
            String value = cmdContent.substring(equalsPos + 1).trim();

            if (varName.isEmpty()) {
                return "Error: Variable name is empty";
            }

            // Save to SharedPreferences
            return mParameters.findParam(varName, value, true);
        }
    }

    private void displayOutput(String output) {
        cmdBuffer.append(output);
        mSnackbar.setText(cmdBuffer.toString());
        cmdBuffer.setLength(0);
        mSnackbar.setDuration(Snackbar.LENGTH_SHORT);
        mSnackbar.show();
   }

}
