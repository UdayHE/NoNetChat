package io.github.udayhe.nonetchat.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import io.github.udayhe.nonetchat.activity.MainActivity;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "WiFiP2P";

    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;
    private final MainActivity activity;

    private boolean serverStarted = false;
    private boolean clientConnected = false;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, MainActivity activity) {
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (manager == null || intent == null || intent.getAction() == null) {
            Log.w(TAG, "Received null manager or intent");
            return;
        }

        String action = intent.getAction();
        Log.d(TAG, "Broadcast received: " + action);

        switch (action) {
            case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    Log.d(TAG, "Wi-Fi P2P is enabled");
                } else {
                    Log.w(TAG, "Wi-Fi P2P is not enabled");
                    Toast.makeText(context, "Please enable Wi-Fi Direct", Toast.LENGTH_SHORT).show();
                }
                break;

            case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                Log.d(TAG, "Connection state changed");

                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (networkInfo != null && networkInfo.isConnected()) {
                    Log.d(TAG, "Device connected. Requesting connection info...");

                    manager.requestConnectionInfo(channel, info -> {
                        if (info.groupFormed) {
                            if (info.isGroupOwner && !serverStarted) {
                                Log.d(TAG, "We are group owner, starting server");
                                serverStarted = true;
                                activity.startServer();
                            } else if (!info.isGroupOwner && !clientConnected) {
                                Log.d(TAG, "We are client, connecting to host: " + info.groupOwnerAddress.getHostAddress());
                                clientConnected = true;
                                activity.connectToHost(info.groupOwnerAddress);
                            }
                        } else {
                            Log.w(TAG, "Group not formed yet");
                        }
                    });
                } else {
                    Log.w(TAG, "Wi-Fi Direct disconnected");

                    // Reset flags on disconnect
                    serverStarted = false;
                    clientConnected = false;

                    activity.runOnUiThread(() ->
                            Toast.makeText(context, "Disconnected", Toast.LENGTH_SHORT).show()
                    );
                }
                break;

            case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                Log.d(TAG, "PEERS_CHANGED_ACTION received");

                boolean locationGranted = ActivityCompat.checkSelfPermission(
                        context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

                boolean nearbyGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        ActivityCompat.checkSelfPermission(
                                context, android.Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED;

                if (!locationGranted || !nearbyGranted) {
                    Log.w(TAG, "Missing permissions, skipping requestPeers");
                    return;
                }

                manager.requestPeers(channel, peers -> {
                    Log.d(TAG, "Found " + peers.getDeviceList().size() + " peer(s)");
                    for (WifiP2pDevice device : peers.getDeviceList()) {
                        Log.d(TAG, "Peer: " + device.deviceName + " - " + device.deviceAddress);
                    }
                });
                break;

            case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                Log.d(TAG, "THIS_DEVICE_CHANGED_ACTION received");
                break;

            default:
                Log.d(TAG, "Unhandled action: " + action);
                break;
        }
    }
}
