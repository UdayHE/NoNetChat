package io.github.udayhe.nonetchat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

public class ConnectActivity extends AppCompatActivity {

    private static final String TAG = "WiFiP2P";
    private static final int PERMISSION_REQUEST_CODE = 1001;

    WifiP2pManager manager;
    WifiP2pManager.Channel channel;
    BroadcastReceiver receiver;
    IntentFilter intentFilter;

    ListView deviceListView;
    ArrayAdapter<String> adapter;
    List<WifiP2pDevice> peers = new ArrayList<>();

    WifiP2pManager.ConnectionInfoListener connectionInfoListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        deviceListView = findViewById(R.id.deviceList);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        deviceListView.setAdapter(adapter);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        setupIntentFilter();
        setupConnectionListener();
        setupReceiver();

        findViewById(R.id.discoverBtn).setOnClickListener(v -> {
            adapter.clear();
            if (!hasPermissions(this)) {
                requestRequiredPermissions();
                return;
            }

            Log.d(TAG, "Initiating discoverPeers()...");
            manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "discoverPeers() started");
                    Toast.makeText(ConnectActivity.this, "Discovering Peers...", Toast.LENGTH_SHORT).show();

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        Log.d(TAG, "Running fallback requestPeers()");
                        manager.requestPeers(channel, peerListListener);
                    }, 4000);
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(TAG, "discoverPeers() failed with reason: " + reason);
                    Toast.makeText(ConnectActivity.this, "Discovery Failed", Toast.LENGTH_SHORT).show();
                }
            });
        });

        deviceListView.setOnItemClickListener((parent, view, position, id) -> {
            WifiP2pDevice device = peers.get(position);
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = device.deviceAddress;

            if (!hasPermissions(this)) {
                requestRequiredPermissions();
                return;
            }

            Log.d(TAG, "Connecting to: " + device.deviceName);
            manager.connect(channel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Connection initiated");
                    Toast.makeText(ConnectActivity.this, "Connecting to " + device.deviceName, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(TAG, "Connection failed: " + reason);
                    Toast.makeText(ConnectActivity.this, "Connection failed", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void setupIntentFilter() {
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    private void setupConnectionListener() {
        connectionInfoListener = info -> {
            Log.d(TAG, "ConnectionInfo available: " + info);
            if (info.groupFormed) {
                Intent intent = new Intent(ConnectActivity.this, MainActivity.class);
                intent.putExtra("isGroupOwner", info.isGroupOwner);
                intent.putExtra("groupOwnerAddress", info.groupOwnerAddress.getHostAddress());
                startActivity(intent);
                finish();
            }
        };
    }

    private void setupReceiver() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(TAG, "Broadcast received: " + action);

                switch (action) {
                    case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                        if (hasPermissions(context)) {
                            manager.requestPeers(channel, peerListListener);
                        }
                        break;

                    case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                        if (!hasPermissions(context)) return;
                        manager.requestConnectionInfo(channel, connectionInfoListener);
                        break;
                }
            }
        };
    }

    WifiP2pManager.PeerListListener peerListListener = peerList -> {
        Log.d(TAG, "peerListListener triggered");
        peers.clear();
        peers.addAll(peerList.getDeviceList());
        adapter.clear();

        if (peers.isEmpty()) {
            adapter.add("No peers found");
        } else {
            for (WifiP2pDevice device : peers) {
                String deviceInfo = device.deviceName + " - " + device.deviceAddress;
                adapter.add(deviceInfo);
                Log.d(TAG, "Found peer: " + deviceInfo);
            }
        }
    };

    private boolean hasPermissions(Context context) {
        boolean fine = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean nearby = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES)
                        == PackageManager.PERMISSION_GRANTED;
        return fine && nearby;
    }

    private void requestRequiredPermissions() {
        List<String> permissions = new ArrayList<>();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES)
                        != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES);
        }
        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && hasPermissions(this)) {
            Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Required permissions not granted", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (receiver != null) registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (receiver != null) unregisterReceiver(receiver);
    }
}
