package io.github.udayhe.nonetchat.sender;

import android.util.Log;

import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiPeerChatSender {

    private static final String TAG = "MultiPeerChatSender";


    private final Map<String, PrintWriter> userWriters = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();



    public void addUserWriter(String username, PrintWriter writer) {
        userWriters.put(username, writer);
        Log.d(TAG, "Writer added for: " + username);
    }

    public void removeUser(String username) {
        PrintWriter writer = userWriters.remove(username);
        if (writer != null) {
            writer.close();
            Log.d(TAG, "Writer removed for: " + username);
        }
    }

    public void sendToUser(String username, String message) {
        PrintWriter writer = userWriters.get(username);
        if (writer != null) {
            executor.execute(() -> {
                try {
                    writer.println(message);
                    writer.flush();
                } catch (Exception e) {
                    Log.e("ChatSender", "Failed to send to " + username, e);
                }
            });
        }
    }


    public void broadcast(String message) {
        synchronized (userWriters) {
            for (Map.Entry<String, PrintWriter> entry : userWriters.entrySet()) {
                try {
                    entry.getValue().println(message);
                    Log.d(TAG, "Broadcasted to " + entry.getKey());
                } catch (Exception e) {
                    Log.e(TAG, "Failed to send to " + entry.getKey(), e);
                }
            }
        }
    }

    public void shutdown() {
        executor.shutdownNow();
        for (PrintWriter writer : userWriters.values()) {
            writer.close();
        }
        userWriters.clear();
    }
}