package io.github.udayhe.nonetchat;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;

public class ChatReceiver {
    private final BufferedReader reader;
    private final OnMessageReceivedListener listener;
    private volatile boolean running = true;
    private static final String TAG = "ChatReceiver";

    public ChatReceiver(BufferedReader reader, OnMessageReceivedListener listener) {
        this.reader = reader;
        this.listener = listener;
    }

    public void start() {
        new Thread(() -> {
            try {
                String line;
                while (running && (line = reader.readLine()) != null) {
                    Log.d(TAG, "Received: " + line);
                    deliverToMainThread(line);
                }
            } catch (IOException e) {
                Log.e(TAG, "Receiver error", e);
                deliverToMainThread("[Connection lost]");
            }
        }).start();
    }

    public void stop() {
        running = false;
    }

    private void deliverToMainThread(String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (listener != null) listener.onMessage(message);
        });
    }

    public interface OnMessageReceivedListener {
        void onMessage(String message);
    }
}
