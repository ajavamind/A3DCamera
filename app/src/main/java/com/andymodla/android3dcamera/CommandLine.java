package com.andymodla.android3dcamera;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class CommandLine extends AppCompatActivity {
    private EditText inputField;
    private TextView outputDisplay;
    private StringBuilder cmdBuffer;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "CommandLineVars";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_command_line);

        //inputField = findViewById(R.id.input_field);
        //outputDisplay = findViewById(R.id.output_display);
        cmdBuffer = new StringBuilder();
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        inputField.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    return handleKeyDown(keyCode, event);
                }
                return false;
            }
        });
    }

    private boolean handleKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            processCommand();
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_DEL) {
            if (cmdBuffer.length() > 0) {
                cmdBuffer.deleteCharAt(cmdBuffer.length() - 1);
                updateInputDisplay();
            }
            return true;
        }

        // Get the character from the key event
        char c = (char) event.getUnicodeChar();
        if (c != 0) {
            cmdBuffer.append(c);
            updateInputDisplay();
            return true;
        }

        return false;
    }

    private void updateInputDisplay() {
        inputField.setText(cmdBuffer.toString());
        inputField.setSelection(cmdBuffer.length());
    }

    private void processCommand() {
        String cmd = cmdBuffer.toString().trim();
        cmdBuffer.setLength(0);

        if (cmd.isEmpty() || cmd.charAt(0) != '/') {
            displayOutput("Error: Command must start with /");
            updateInputDisplay();
            return;
        }

        // Remove the / prefix
        String cmdContent = cmd.substring(1).trim();

        if (cmdContent.isEmpty()) {
            displayOutput("Error: No command specified");
            updateInputDisplay();
            return;
        }

        String result = parseAndExecute(cmdContent);
        displayOutput(result);
        updateInputDisplay();
    }

    private String parseAndExecute(String cmdContent) {
        int equalsPos = cmdContent.indexOf('=');

        if (equalsPos == -1) {
            // GET operation - retrieve variable value
            String varName = cmdContent.trim();
            if (varName.isEmpty()) {
                return "Error: Variable name is empty";
            }

            String value = prefs.getString(varName, null);
            if (value == null) {
                return varName + " = (undefined)";
            }
            return varName + " = " + value;

        } else {
            // SET operation - store variable value
            String varName = cmdContent.substring(0, equalsPos).trim();
            String value = cmdContent.substring(equalsPos + 1).trim();

            if (varName.isEmpty()) {
                return "Error: Variable name is empty";
            }

            // Save to SharedPreferences
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(varName, value);
            editor.apply();

            return varName + " = " + value;
        }
    }

    private void displayOutput(String output) {
        String current = outputDisplay.getText().toString();
        if (!current.isEmpty()) {
            current += "\n";
        }
        current += "> " + cmdBuffer.toString() + "\n" + output;
        outputDisplay.setText(current);

        // Scroll to bottom
        final TextView tv = outputDisplay;
        tv.post(new Runnable() {
            @Override
            public void run() {
               // tv.setSelection(tv.getText().length());
            }
        });
    }
}

/*
 * XML Layout file (res/layout/activity_command_line.xml):
 *
 * <?xml version="1.0" encoding="utf-8"?>
 * <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent"
 *     android:orientation="vertical"
 *     android:padding="16dp"
 *     android:background="#000000">
 *
 *     <TextView
 *         android:id="@+id/output_display"
 *         android:layout_width="match_parent"
 *         android:layout_height="0dp"
 *         android:layout_weight="1"
 *         android:textColor="#00FF00"
 *         android:fontFamily="monospace"
 *         android:textSize="14sp"
 *         android:scrollbars="vertical"
 *         android:padding="8dp"/>
 *
 *     <EditText
 *         android:id="@+id/input_field"
 *         android:layout_width="match_parent"
 *         android:layout_height="wrap_content"
 *         android:textColor="#00FF00"
 *         android:fontFamily="monospace"
 *         android:textSize="14sp"
 *         android:background="#222222"
 *         android:padding="8dp"
 *         android:hint="Enter command..."
 *         android:textColorHint="#006600"/>
 *
 * </LinearLayout>
 */