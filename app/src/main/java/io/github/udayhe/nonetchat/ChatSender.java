package io.github.udayhe.nonetchat;

import android.util.Log;

import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatSender {
    private static final String TAG = "ChatSender";

    private final PrintWriter writer;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ChatSender(PrintWriter writer) {
        this.writer = writer;
    }

    public void send(String message) {
        executor.execute(() -> {
            try {
                writer.println(message);
                writer.flush();  // Optional, but ensures delivery
            } catch (Exception e) {
                Log.e(TAG, "Failed to send message", e);
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}
