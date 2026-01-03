## delta-auto

A fork of [delta](https://github.com/supershadoe/delta) that adds automatic WiFi hotspot activation when connecting to a Bluetooth device.

### What this fork adds

**Auto Enable on Bluetooth** — Automatically turns on your WiFi hotspot when a specific Bluetooth device connects (e.g., your car's head unit). The hotspot turns off when the device disconnects.

This is useful for Android Auto wireless setups or any scenario where you want hands-free hotspot activation.

### Compatibility

- **Tested on:** Android 15 (API 35)
- **Target SDK:** Android 16 (API 36)
- **Requires:** [Shizuku](https://github.com/RikkaApps/Shizuku) for hotspot control

### Installation

1. Install and start [Shizuku](https://github.com/RikkaApps/Shizuku)
2. Download the APK from [Releases](../../releases)
3. Grant Shizuku permissions when prompted
4. Go to Settings → Auto Enable on Bluetooth
5. Select your trigger device (e.g., car Bluetooth)
6. Enable the feature

### Building from source

```bash
# Requires Android Studio or command line with Android SDK
./gradlew assembleDebug
```

APKs will be in `app/build/outputs/apk/*/debug/`

### Changes from upstream

- Added `BluetoothAutoEnableService` foreground service for monitoring Bluetooth connections
- Added `BluetoothAutoEnableReceiver` broadcast receiver for ACL connect/disconnect events
- Added settings UI for selecting trigger device and enabling the feature
- Fixed foreground service timing crash on Android 14+ (service must call `startForeground()` synchronously)

### Known issues

None currently. If you encounter issues, please open an issue with your Android version and logcat output.

---

## Original delta features

This app allows accessing advanced hotspot settings not exposed through the system settings app on devices that use Stock Android (Pixel, Nothing, etc.)

It provides various information and features such as:
- Usual info provided by settings app such as hotspot name, passphrase, security type, frequency bands, quick connect using QR, etc.
- List of connected devices.
- Modifying amount of devices that can connect.
- Changing MAC randomization settings.
- Setting auto turn-off timeout.
- Blocklist to prevent certain devices from connecting.
- Make hotspot's SSID hidden (device doesn't appear on WiFi scans)
- Quick setting tile to easily turn hotspot on/off from notification shade.
- Control hotspot using Tasker/other automation apps.

### Credits

- Original app by [supershadoe](https://github.com/supershadoe/delta)
- Auto-hotspot feature by [iaakki](https://github.com/iaakki)
- Foreground service fix contributed via this fork

### License

BSD-3-Clause (same as upstream)
