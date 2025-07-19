# NoNetChat: Offline Chat App

NoNetChat is an offline messaging app that allows Android users to communicate nearby using **Wi-Fi Direct (P2P)** and **Bluetooth Low Energy (BLE) Mesh**, without relying on internet connectivity.

---

## Features

* Multi-peer messaging via **Wi-Fi Direct** (socket-based)
* Message relaying using **Bluetooth Mesh** (BLE advertisement)
* **Sender-Receiver name tagging**
* **Selective messaging** via dropdown/spinner
* Chat bubble UI with timestamps and source (WiFi or BLE)

---

## How It Works (End-to-End Lifecycle)

### Entry Point: `MainActivity`

When the app starts:

1. **`onCreate()`**

   * Loads `activity_main.xml`
   * Initializes UI, Wi-Fi Direct, BLE Mesh, and permissions
   * Sets up broadcast receivers for Wi-Fi events

2. **Permissions**

   * Calls `checkAndRequestPermissions()` for:

     * Location, BLE Scan/Advertise/Connect, Wi-Fi Direct

3. **Wi-Fi P2P Setup**

   * Calls `manager.discoverPeers()`
   * `WiFiDirectBroadcastReceiver` listens for:

     * `WIFI_P2P_CONNECTION_CHANGED_ACTION`
     * On group formation:

       * Group owner → `startServer()` (accepts clients)
       * Client → `connectToHost()`

4. **Socket Communication**

   * Clients send username to group owner
   * Group owner maps username to PrintWriter (via `chatSender.addUserWriter()`)
   * Incoming messages handled by `receiveFromClient()`

5. **Bluetooth Mesh Setup**

   * `BluetoothMeshManager.startMesh()` starts BLE scanner and advertiser
   * On scanning, decodes messages and triggers `onMessageReceived()` callback

6. **Send Message Flow**

   * On Send Button click:

     * Gets content and selected recipient from Spinner
     * Adds message to RecyclerView using `MessageAdapter`
     * Sends via:

       * Wi-Fi (using `chatSender.sendToUser()`)
       * BLE (using `meshManager.sendMessage()`)

7. **Receive Message Flow**

   * Wi-Fi: Message received via socket thread → `Message` object → added to list
   * BLE: Message decoded → `onMessageReceived()` callback → added to list

8. **Lifecycle Cleanup**

   * `onDestroy()`

     * Closes all sockets
     * Stops BLE scan/advertising

---

## Application Structure

### 📁 Java Classes

| Class                         | Responsibility                                               |
| ----------------------------- | ------------------------------------------------------------ |
| `MainActivity`                | Core UI, lifecycle, WiFi/BLE integration                     |
| `Message`                     | Chat model with sender, receiver, content, timestamp, source |
| `MessageAdapter`              | RecyclerView adapter for displaying messages                 |
| `MultiPeerChatSender`         | Thread-safe Wi-Fi message dispatcher                         |
| `BluetoothMeshManager`        | BLE advertiser and scanner                                   |
| `WiFiDirectBroadcastReceiver` | Handles Wi-Fi P2P system broadcasts                          |

### Key Methods in `MainActivity`

* `onCreate()` - Initializes everything
* `startServer()` - Accepts socket clients (group owner)
* `connectToHost()` - Connects to group owner
* `receiveFromClient()` - Handles messages from connected sockets
* `updateSpinner()` - Updates dropdown user list
* `checkAndRequestPermissions()` - Ensures all runtime permissions are granted

---

## UI Layouts

### `activity_main.xml`

* `RecyclerView` for messages
* `EditText` for typing
* `Button` for sending
* `Spinner` for choosing recipient

### `item_message.xml`

* Chat bubble layout
* Shows sender, receiver, time, and source (WiFi/BLE)

---

## Permissions

### `AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
<uses-permission android:name="android.permission.INTERNET" />
```

Register broadcast receiver:

```xml
<receiver android:name=".WiFiDirectBroadcastReceiver" android:exported="true">
    <intent-filter>
        <action android:name="android.net.wifi.p2p.PEERS_CHANGED" />
        <action android:name="android.net.wifi.p2p.CONNECTION_STATE_CHANGE" />
        <action android:name="android.net.wifi.p2p.STATE_CHANGED" />
        <action android:name="android.net.wifi.p2p.THIS_DEVICE_CHANGED" />
    </intent-filter>
</receiver>
```

---

## Chat Logic

### Sending

```java
sendButton.setOnClickListener(v -> {
    String msg = messageInput.getText().toString().trim();
    String recipient = recipientSpinner.getSelectedItem().toString();

    messages.add(new Message(myUsername, recipient, msg, now(), "WiFi"));
    chatSender.sendToUser(recipient, myUsername + ": " + msg);
    meshManager.sendMessage(myUsername, msg);
});
```

### Receiving (WiFi)

```java
receiveFromClient() → new Message(sender, you, content, time, "WiFi")
```

### Receiving (BLE)

```java
BluetoothMeshManager callback → new Message(sender, you, content, time, "BLE")
```

---

## Message Transport

| Feature              | Wi-Fi Direct | BLE Mesh |
| -------------------- | ------------ | -------- |
| One-to-One           | ✅            | ❌        |
| One-to-Many          | ✅            | ✅        |
| Bi-directional       | ✅            | ❌        |
| Data Size            | High         | Low      |
| Permissions Required | ✅            | ✅        |

---

## Scenarios

* ✅ Multi-device group chat via Wi-Fi
* ✅ Cross-peer BLE message relay
* ✅ Dynamic reconnection with P2P broadcast
* ✅ Username-based selective routing

---

## Lifecycle

* **onCreate** → permissions → scanning/server setup
* **onConnect** → start client/server socket
* **onSend** → send via socket + BLE
* **onDestroy** → close sockets, stop BLE

---

## Ideas for Future

* 🔒 End-to-end encryption
* 📎 File/image transfer
* 🔁 BLE to WiFi bridge
* 🔍 QR sharing of usernames

---

## Folder Structure

```bash
Directory structure:
└── udayhe-nonetchat/
    ├── gradle.properties
    ├── gradlew
    ├── gradlew.bat
    ├── app/
    │   ├── proguard-rules.pro
    │   └── src/
    │       ├── androidTest/
    │       │   └── java/
    │       │       └── io/
    │       │           └── github/
    │       │               └── udayhe/
    │       │                   └── nonetchat/
    │       │                       └── ExampleInstrumentedTest.kt
    │       ├── main/
    │       │   ├── AndroidManifest.xml
    │       │   ├── java/
    │       │   │   └── io/
    │       │   │       └── github/
    │       │   │           └── udayhe/
    │       │   │               └── nonetchat/
    │       │   │                   ├── activity/
    │       │   │                   │   ├── ConnectActivity.java
    │       │   │                   │   └── MainActivity.java
    │       │   │                   ├── adapter/
    │       │   │                   │   └── MessageAdapter.java
    │       │   │                   ├── bluetooth/
    │       │   │                   │   └── BluetoothMeshManager.java
    │       │   │                   ├── payload/
    │       │   │                   │   └── Message.java
    │       │   │                   ├── sender/
    │       │   │                   │   └── MultiPeerChatSender.java
    │       │   │                   └── wifi/
    │       │   │                       └── WiFiDirectBroadcastReceiver.java
    │       │   └── res/
    │       │       ├── drawable/
    │       │       │   ├── ic_launcher_background.xml
    │       │       │   └── message_bubble.xml
    │       │       ├── drawable-v24/
    │       │       │   └── ic_launcher_foreground.xml
    │       │       ├── layout/
    │       │       │   ├── activity_connect.xml
    │       │       │   ├── activity_main.xml
    │       │       │   └── item_message.xml
    │       │       ├── mipmap-anydpi-v26/
    │       │       │   ├── ic_launcher.xml
    │       │       │   └── ic_launcher_round.xml
    │       │       ├── mipmap-hdpi/
    │       │       │   ├── ic_launcher.webp
    │       │       │   ├── ic_launcher_foreground.webp
    │       │       │   └── ic_launcher_round.webp
    │       │       ├── mipmap-mdpi/
    │       │       │   ├── ic_launcher.webp
    │       │       │   ├── ic_launcher_foreground.webp
    │       │       │   └── ic_launcher_round.webp
    │       │       ├── mipmap-xhdpi/
    │       │       │   ├── ic_launcher.webp
    │       │       │   ├── ic_launcher_foreground.webp
    │       │       │   └── ic_launcher_round.webp
    │       │       ├── mipmap-xxhdpi/
    │       │       │   ├── ic_launcher.webp
    │       │       │   ├── ic_launcher_foreground.webp
    │       │       │   └── ic_launcher_round.webp
    │       │       ├── mipmap-xxxhdpi/
    │       │       │   ├── ic_launcher.webp
    │       │       │   ├── ic_launcher_foreground.webp
    │       │       │   └── ic_launcher_round.webp
    │       │       ├── values/
    │       │       │   ├── colors.xml
    │       │       │   ├── ic_launcher_background.xml
    │       │       │   ├── ids.xml
    │       │       │   ├── strings.xml
    │       │       │   └── themes.xml
    │       │       ├── values-v31/
    │       │       │   └── themes.xml
    │       │       └── xml/
    │       │           ├── backup_rules.xml
    │       │           └── data_extraction_rules.xml
    │       └── test/
    │           └── java/
    │               └── io/
    │                   └── github/
    │                       └── udayhe/
    │                           └── nonetchat/
    │                               └── ExampleUnitTest.kt
    └── gradle/
        ├── libs.versions.toml
        └── wrapper/
            └── gradle-wrapper.properties

```

---

## Build & Run

* Target: Android 6.0+ (API 23+)
* Enable location, BLE, WiFi permissions
* Build using Android Studio

---

## Status

* Fully functional offline chat across nearby devices using both transport layers.
