package com.andymodla.android3dcamera;

import android.os.Handler;

import java.util.concurrent.Executor;

public class HandlerExecutor implements Executor {
    private final Handler handler;

    public HandlerExecutor(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void execute(Runnable command) {
        handler.post(command);
    }
}
