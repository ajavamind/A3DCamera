package com.andymodla.android3dcamera;

import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

public class ToastHelper {

    public static void showToast(Context context, String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                toast.cancel();
            }
        }, 500); // 500 milliseconds = 0.5 seconds

        toast.show();
    }
}
