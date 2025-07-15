package io.github.udayhe.nonetchat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "WiFiChat";
    private static final int REQUEST_PERMISSIONS_CODE = 1001;
    private static final int PORT = 8888;

    WifiP2pManager manager;
    WifiP2pManager.Channel channel;
    BroadcastReceiver receiver;
    IntentFilter intentFilter;

    RecyclerView recyclerView;
    EditText messageInput;
    Button sendButton;
    Spinner recipientSpinner;

    List<Message> messages = new ArrayList<>();
    MessageAdapter adapter;

    ServerSocket serverSocket;
    List<Socket> clientSockets = new ArrayList<>();
    Map<String, PrintWriter> userWriters = new HashMap<>();
    List<String> userList = new ArrayList<>();
    Socket socket; // For client mode

    MultiPeerChatSender chatSender;

    String myUsername = "User" + new Random().nextInt(1000);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        recipientSpinner = findViewById(R.id.recipientSpinner);

        adapter = new MessageAdapter(this, messages);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        sendButton.setOnClickListener(v -> {
            String msg = messageInput.getText().toString().trim();
            String recipient = recipientSpinner.getSelectedItem() != null ? recipientSpinner.getSelectedItem().toString() : null;
            if (!msg.isEmpty() && recipient != null) {
                messages.add(new Message(myUsername, recipient, msg, System.currentTimeMillis()));
                adapter.notifyItemInserted(messages.size() - 1);
                recyclerView.smoothScrollToPosition(messages.size() - 1);
                messageInput.setText("");

                if (chatSender != null) chatSender.sendToUser(recipient, myUsername + ": " + msg);
            }
        });

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        checkAndRequestPermissions();

        boolean isGroupOwner = getIntent().getBooleanExtra("isGroupOwner", false);
        String hostAddress = getIntent().getStringExtra("groupOwnerAddress");

        if (isGroupOwner) {
            startServer();
        } else {
            try {
                connectToHost(InetAddress.getByName(hostAddress));
            } catch (IOException e) {
                Toast.makeText(this, "Invalid host", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkAndRequestPermissions() {
        List<String> permissions = new ArrayList<>();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.NEARBY_WIFI_DEVICES)
                        != PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.NEARBY_WIFI_DEVICES);
        }
        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissions.toArray(new String[0]), REQUEST_PERMISSIONS_CODE);
        }
    }

    void startServer() {
        chatSender = new MultiPeerChatSender();
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                while (!serverSocket.isClosed()) {
                    Socket client = serverSocket.accept();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())), true);

                    String username = reader.readLine();
                    if (username == null || username.trim().isEmpty()) {
                        Log.w(TAG, "Username is null or empty. Skipping client.");
                        client.close();
                        continue;
                    }

                    Log.d(TAG, "Received username: " + username);
                    userWriters.put(username, writer);
                    userList.add(username);
                    runOnUiThread(this::updateSpinner);
                    chatSender.addUserWriter(username, writer);

                    new Thread(() -> receiveFromClient(client, username, reader)).start();
                }
            } catch (IOException e) {
                Log.e(TAG, "Server error", e);
            }
        }).start();
    }

    private void receiveFromClient(Socket client, String username, BufferedReader reader) {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String finalLine = line;
                runOnUiThread(() -> {
                    messages.add(new Message(username, "You", finalLine, System.currentTimeMillis()));
                    adapter.notifyItemInserted(messages.size() - 1);
                    recyclerView.smoothScrollToPosition(messages.size() - 1);
                });
            }
        } catch (IOException e) {
            Log.w(TAG, "Client disconnected: " + username);
        } finally {
            chatSender.removeUser(username);
            userWriters.remove(username);
            userList.remove(username);
            runOnUiThread(this::updateSpinner);
        }
    }

    private void updateSpinner() {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>(userList));
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        recipientSpinner.setAdapter(spinnerAdapter);
    }

    void connectToHost(InetAddress hostAddress) {
        new Thread(() -> {
            try {
                Log.d(TAG, "Attempting to connect to host: " + hostAddress);
                socket = new Socket();
                socket.connect(new InetSocketAddress(hostAddress, PORT), 8000);

                PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                writer.println(myUsername);
                writer.flush();

                chatSender = new MultiPeerChatSender();
                chatSender.addUserWriter("Host", writer);

                runOnUiThread(() -> {
                    userList.clear();
                    userList.add("Host");
                    updateSpinner();
                });

                String line;
                while ((line = reader.readLine()) != null) {
                    String finalLine = line;
                    runOnUiThread(() -> {
                        messages.add(new Message("Host", myUsername, finalLine, System.currentTimeMillis()));
                        adapter.notifyItemInserted(messages.size() - 1);
                        recyclerView.smoothScrollToPosition(messages.size() - 1);
                    });
                }

            } catch (IOException e) {
                Log.e(TAG, "Client connection failed", e);
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Failed to connect to server", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            for (Socket s : clientSockets) s.close();
            if (socket != null) socket.close();
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Cleanup failed", e);
        }
    }
}