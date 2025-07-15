package io.github.udayhe.nonetchat;

import android.util.Log;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MultiPeerChatSender {

    private static final String TAG = "MultiPeerChatSender";

    // Maps username → writer
    private final Map<String, PrintWriter> userWriters = Collections.synchronizedMap(new HashMap<>());

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
            writer.println(message);
            Log.d(TAG, "Message sent to " + username + ": " + message);
        } else {
            Log.w(TAG, "Writer not found for user: " + username);
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
        synchronized (userWriters) {
            for (Map.Entry<String, PrintWriter> entry : userWriters.entrySet()) {
                try {
                    entry.getValue().close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing writer for " + entry.getKey(), e);
                }
            }
            userWriters.clear();
            Log.d(TAG, "All writers shut down");
        }
    }
}