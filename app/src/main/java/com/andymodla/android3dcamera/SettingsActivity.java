package com.andymodla.android3dcamera;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Settings Activity for A3DCamera
 * Provides a GUI to view and change all application parameters.
 * Launched by pressing the 'J' key from MainActivity.
 *
 * All changes accumulate in the UI and are saved when the user presses
 * the back button to exit.
 *
 * Copyright 2025-2026 Andy Modla All Rights Reserved
 */
public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    private static final String PREFS_NAME = "Parameters";

    private Parameters parameters;

    // --- SeekBar references (integer parameters) ---
    private SeekBar seekParallax;
    private TextView tvParallaxValue;
    private SeekBar seekVertical;
    private TextView tvVerticalValue;
    private SeekBar seekCountdown;
    private TextView tvCountdownValue;

    // --- RadioGroup for focus distance ---
    private RadioGroup rgFocusDistance;
    private RadioButton rbFocusHyperfocal;
    private RadioButton rbFocusPhotobooth;
    private RadioButton rbFocusMacro;
    private RadioButton rbFocusAuto;

    // --- Switch references (boolean parameters) ---
    private Switch swSoundOn;
    private Switch swAiEdit;
    private Switch swPhotoBooth;
    private Switch swMirror;
    private Switch swBlankScreen;
    private Switch swCountDownEnabled;
    private Switch swUdpControlEnabled;
    private Switch swUdpTransmit;
    private Switch swAutoReview;
    private Switch swSbsCropPrint;

    // --- EditText references (string parameters) ---
    private EditText etReceiverIp;
    private TextView tvReceiverPort; // read-only
    private EditText etTitle1;
    private EditText etTitle2;
    private EditText etInstruction1;
    private EditText etInstruction2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportActionBar().setTitle("A3DCamera Settings");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        parameters = MainActivity.parameters;

        bindViews();
        loadValues();
        setupListeners();
    }

    @Override
    public boolean onSupportNavigateUp() {
        saveAllParameters();
        finish();
        return true;
    }

    @Override
    public void onBackPressed() {
        saveAllParameters();
        super.onBackPressed();
    }

    // ===================== BINDING =====================

    private void bindViews() {
        // SeekBars
        seekParallax = findViewById(R.id.seek_parallax);
        tvParallaxValue = findViewById(R.id.tv_parallax_value);
        seekVertical = findViewById(R.id.seek_vertical);
        tvVerticalValue = findViewById(R.id.tv_vertical_value);
        seekCountdown = findViewById(R.id.seek_countdown);
        tvCountdownValue = findViewById(R.id.tv_countdown_value);

        // RadioGroup
        rgFocusDistance = findViewById(R.id.rg_focus_distance);
        rbFocusHyperfocal = findViewById(R.id.rb_focus_hyperfocal);
        rbFocusPhotobooth = findViewById(R.id.rb_focus_photobooth);
        rbFocusMacro = findViewById(R.id.rb_focus_macro);
        rbFocusAuto = findViewById(R.id.rb_focus_auto);

        // Switches
        swSoundOn = findViewById(R.id.sw_sound_on);
        swAiEdit = findViewById(R.id.sw_ai_edit);
        swPhotoBooth = findViewById(R.id.sw_photo_booth);
        swMirror = findViewById(R.id.sw_mirror);
        swBlankScreen = findViewById(R.id.sw_blank_screen);
        swCountDownEnabled = findViewById(R.id.sw_count_down_enabled);
        swUdpControlEnabled = findViewById(R.id.sw_udp_control_enabled);
        swUdpTransmit = findViewById(R.id.sw_udp_transmit);
        swAutoReview = findViewById(R.id.sw_auto_review);
        swSbsCropPrint = findViewById(R.id.sw_sbs_crop_print);

        // Strings
        etReceiverIp = findViewById(R.id.et_receiver_ip);
        tvReceiverPort = findViewById(R.id.tv_receiver_port);
        etTitle1 = findViewById(R.id.et_title_1);
        etTitle2 = findViewById(R.id.et_title_2);
        etInstruction1 = findViewById(R.id.et_instruction_1);
        etInstruction2 = findViewById(R.id.et_instruction_2);

        // Force single-line for edit texts
        etReceiverIp.setSingleLine(true);
        etTitle1.setSingleLine(true);
        etTitle2.setSingleLine(true);
        etInstruction1.setSingleLine(true);
        etInstruction2.setSingleLine(true);
    }

    // ===================== LOAD CURRENT VALUES =====================

    private void loadValues() {
        // --- Integers ---
        seekParallax.setMin(-500);
        seekParallax.setMax(500);
        seekParallax.setProgress(parameters.getParallaxOffset());
        tvParallaxValue.setText(String.valueOf(parameters.getParallaxOffset()));

        seekVertical.setMin(-100);
        seekVertical.setMax(100);
        seekVertical.setProgress(parameters.getVerticalOffset());
        tvVerticalValue.setText(String.valueOf(parameters.getVerticalOffset()));

        seekCountdown.setMin(0);
        seekCountdown.setMax(30);
        seekCountdown.setProgress(parameters.getCountdownTimer());
        tvCountdownValue.setText(parameters.getCountdownTimer() + "s");

        // Focus distance index: 0=hyperFocal, 1=photoBooth, 2=macro, 3=auto
        int fdi = parameters.getFocusDistanceIndex();
        switch (fdi) {
            case 0: rgFocusDistance.check(R.id.rb_focus_hyperfocal); break;
            case 1: rgFocusDistance.check(R.id.rb_focus_photobooth); break;
            case 2: rgFocusDistance.check(R.id.rb_focus_macro); break;
            default: rgFocusDistance.check(R.id.rb_focus_auto); break;
        }

        // --- Booleans ---
        swSoundOn.setChecked(parameters.getIsSoundOn());
        swAiEdit.setChecked(parameters.getIsAiEdit());
        swPhotoBooth.setChecked(parameters.getIsPhotoBooth());
        swMirror.setChecked(parameters.getIsMirror());
        swBlankScreen.setChecked(parameters.getIsBlankScreen());
        swCountDownEnabled.setChecked(parameters.getCountDownEnabled());
        swUdpControlEnabled.setChecked(parameters.getUdpControlEnabled());
        swUdpTransmit.setChecked(parameters.getUdpTransmit());
        swAutoReview.setChecked(parameters.getAutoReview());
        swSbsCropPrint.setChecked(parameters.getSbsCropPrint());

        // --- Strings ---
        etReceiverIp.setText(parameters.getReceiverIp());
        tvReceiverPort.setText(String.valueOf(parameters.getReceiverPort()));
        etTitle1.setText(parameters.getTitle1());
        etTitle2.setText(parameters.getTitle2());
        etInstruction1.setText(parameters.getInst1());
        etInstruction2.setText(parameters.getInst2());
    }

    // ===================== LISTENERS (UI-only, no saving) =====================

    private void setupListeners() {

        // --- Parallax SeekBar ---
        seekParallax.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvParallaxValue.setText(String.valueOf(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        // --- Vertical Offset SeekBar ---
        seekVertical.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvVerticalValue.setText(String.valueOf(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        // --- Countdown Timer SeekBar ---
        seekCountdown.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvCountdownValue.setText(progress + "s");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        // --- EditText: save immediately when focus is lost (tab out or back press) ---
        android.view.View.OnFocusChangeListener textSaveListener = (v, hasFocus) -> {
            if (!hasFocus && v instanceof EditText) {
                String val = ((EditText) v).getText().toString().trim();
                int id = v.getId();
                if (id == R.id.et_receiver_ip) parameters.setReceiverIp(val);
                else if (id == R.id.et_title_1) parameters.setTitle1(val);
                else if (id == R.id.et_title_2) parameters.setTitle2(val);
                else if (id == R.id.et_instruction_1) parameters.setInst1(val);
                else if (id == R.id.et_instruction_2) parameters.setInst2(val);
            }
        };
        etReceiverIp.setOnFocusChangeListener(textSaveListener);
        etTitle1.setOnFocusChangeListener(textSaveListener);
        etTitle2.setOnFocusChangeListener(textSaveListener);
        etInstruction1.setOnFocusChangeListener(textSaveListener);
        etInstruction2.setOnFocusChangeListener(textSaveListener);
    }

    // ===================== SAVE ALL ON EXIT =====================

    /**
     * Read every widget's current value and persist through Parameters.
     * Called from onBackPressed() and onSupportNavigateUp().
     */
    private void saveAllParameters() {
        // Force all EditText widgets to release focus so their text is finalized
        if (getCurrentFocus() != null) {
            getCurrentFocus().clearFocus();
        }
        etReceiverIp.clearFocus();
        etTitle1.clearFocus();
        etTitle2.clearFocus();
        etInstruction1.clearFocus();
        etInstruction2.clearFocus();

        // --- Integers ---
        parameters.setParallaxOffset(seekParallax.getProgress());
        parameters.setVerticalOffset(seekVertical.getProgress());
        parameters.setCountdownTimer(seekCountdown.getProgress());

        // Focus distance from RadioGroup
        int checkedId = rgFocusDistance.getCheckedRadioButtonId();
        int focusIndex;
        if (checkedId == R.id.rb_focus_hyperfocal) {
            focusIndex = 0;
        } else if (checkedId == R.id.rb_focus_photobooth) {
            focusIndex = 1;
        } else if (checkedId == R.id.rb_focus_macro) {
            focusIndex = 2;
        } else {
            focusIndex = 3;
        }
        parameters.setFocusDistanceIndex(focusIndex);

        // --- Booleans ---
        parameters.setIsSoundOn(swSoundOn.isChecked());
        parameters.setIsAiEdit(swAiEdit.isChecked());
        parameters.setIsPhotoBooth(swPhotoBooth.isChecked());
        parameters.setIsMirror(swMirror.isChecked());
        parameters.setIsBlankScreen(swBlankScreen.isChecked());
        parameters.setCountDownEnabled(swCountDownEnabled.isChecked());
        parameters.setUdpControlEnabled(swUdpControlEnabled.isChecked());
        parameters.setUdpTransmit(swUdpTransmit.isChecked());
        parameters.setAutoReview(swAutoReview.isChecked());
        parameters.setSbsCropPrint(swSbsCropPrint.isChecked());

        // --- Strings ---
        parameters.setReceiverIp(etReceiverIp.getText().toString().trim());
        parameters.setTitle1(etTitle1.getText().toString().trim());
        parameters.setTitle2(etTitle2.getText().toString().trim());
        parameters.setInst1(etInstruction1.getText().toString().trim());
        parameters.setInst2(etInstruction2.getText().toString().trim());
    }
}
