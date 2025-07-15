package io.github.udayhe.nonetchat;

import android.util.Log;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MultiPeerChatSender {

    private static final String TAG = "MultiPeerChatSender";
    private final Map<String, PrintWriter> userWriters = Collections.synchronizedMap(new HashMap<>());

    public void addUserWriter(String username, PrintWriter writer) {
        userWriters.put(username, writer);
        Log.d(TAG, "Writer added for: " + username);
    }

    public void removeUser(String username) {
        PrintWriter writer = userWriters.remove(username);
        if (writer != null) {
            writer.close();
        }
        Log.d(TAG, "Removed writer for: " + username);
    }

    public void sendToUser(String username, String message) {
        PrintWriter writer = userWriters.get(username);
        if (writer != null) {
            try {
                writer.println(message);
                Log.d(TAG, "Sent to " + username + ": " + message);
            } catch (Exception e) {
                Log.e(TAG, "Failed to send to " + username, e);
            }
        } else {
            Log.w(TAG, "No writer found for user: " + username);
        }
    }


    public void shutdown() {
        for (Map.Entry<String, PrintWriter> entry : userWriters.entrySet()) {
            try {
                entry.getValue().close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing writer for user: " + entry.getKey(), e);
            }
        }
        userWriters.clear();
        Log.d(TAG, "All user writers shut down");
    }
}

