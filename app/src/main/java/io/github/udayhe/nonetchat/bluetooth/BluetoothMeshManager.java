package io.github.udayhe.nonetchat.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class BluetoothMeshManager {

    private static final String TAG = "BluetoothMesh";
    private static final UUID SERVICE_UUID = UUID.fromString("0000feed-0000-1000-8000-00805f9b34fb");

    private final BluetoothLeAdvertiser advertiser;
    private final BluetoothLeScanner scanner;

    public BluetoothMeshManager(Context context) {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        scanner = bluetoothAdapter.getBluetoothLeScanner();
    }

    public void startAdvertising(String message) {
        if (advertiser == null || message == null || message.length() > 20) {
            Log.w(TAG, "Cannot advertise: Advertiser null or message too long");
            return;
        }

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(false)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .addServiceUuid(new ParcelUuid(SERVICE_UUID))
                .addServiceData(new ParcelUuid(SERVICE_UUID), message.getBytes(StandardCharsets.UTF_8))
                .build();

        advertiser.startAdvertising(settings, data, advertiseCallback);
    }

    public void stopAdvertising() {
        if (advertiser != null) advertiser.stopAdvertising(advertiseCallback);
    }

    public void startScanning() {
        if (scanner != null) scanner.startScan(scanCallback);
    }

    public void stopScanning() {
        if (scanner != null) scanner.stopScan(scanCallback);
    }

    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.d(TAG, "Advertising started");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.e(TAG, "Advertising failed: " + errorCode);
        }
    };

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            byte[] data = result.getScanRecord() != null ? result.getScanRecord().getServiceData(new ParcelUuid(SERVICE_UUID)) : null;
            if (data != null) {
                String msg = new String(data, StandardCharsets.UTF_8);
                Log.d(TAG, "Received mesh message: " + msg);
                // TODO: Forward to UI or message handler
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "Scan failed: " + errorCode);
        }
    };
}
