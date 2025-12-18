package dev.shadoe.delta

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint
import dev.shadoe.delta.api.ShizukuStates
import dev.shadoe.delta.data.BluetoothRepository
import dev.shadoe.delta.data.FlagsRepository
import dev.shadoe.delta.data.shizuku.ShizukuRepository
import dev.shadoe.delta.data.softap.SoftApController
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

/**
 * BroadcastReceiver that listens for Bluetooth device connection/disconnection
 * events and automatically enables/disables WiFi hotspot based on the selected
 * Bluetooth device.
 *
 * Note: This receiver is registered dynamically in Application.onCreate()
 * because implicit broadcasts like ACL_CONNECTED cannot be received via
 * manifest registration since Android 8.0 (API 26).
 */
@AndroidEntryPoint
class BluetoothAutoEnableReceiver : BroadcastReceiver() {
  @Inject lateinit var bluetoothRepository: BluetoothRepository
  @Inject lateinit var flagsRepository: FlagsRepository
  @Inject lateinit var softApController: SoftApController
  @Inject lateinit var shizukuRepository: ShizukuRepository

  companion object {
    private const val TAG = "BluetoothAutoEnable"
  }

  override fun onReceive(context: Context, intent: Intent) {
    // Check if debug toasts are enabled
    val showDebugToasts = runBlocking {
      flagsRepository.isAutoEnableOnBtDebugToastsEnabled()
    }

    // Debug: Show that receiver was triggered
    if (showDebugToasts) {
      Toast.makeText(context, "BT Event: ${intent.action}", Toast.LENGTH_SHORT)
        .show()
    }

    // Check if the feature is enabled
    val isEnabled = runBlocking { flagsRepository.isAutoEnableOnBtEnabled() }

    if (!isEnabled) {
      Log.d(TAG, "Feature is disabled, ignoring BT event")
      if (showDebugToasts) {
        Toast.makeText(
            context,
            "Auto BT: Feature disabled",
            Toast.LENGTH_SHORT,
          )
          .show()
      }
      return
    }

    // Get the Bluetooth device from the intent
    val device: BluetoothDevice? =
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        intent.getParcelableExtra(
          BluetoothDevice.EXTRA_DEVICE,
          BluetoothDevice::class.java,
        )
      } else {
        @Suppress("DEPRECATION")
        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
      }

    val deviceAddress = device?.address
    if (deviceAddress == null) {
      Log.w(TAG, "No device address in intent")
      return
    }

    // Get the selected device from the database
    val selectedDevice = runBlocking { bluetoothRepository.getSelectedDevice() }

    if (selectedDevice == null) {
      Log.d(TAG, "No device selected, ignoring BT event")
      if (showDebugToasts) {
        Toast.makeText(
            context,
            "Auto BT: No device selected",
            Toast.LENGTH_SHORT,
          )
          .show()
      }
      return
    }

    // Check if this is the device we're monitoring
    if (selectedDevice.macAddress != deviceAddress) {
      Log.d(
        TAG,
        "Device $deviceAddress doesn't match selected ${selectedDevice.macAddress}",
      )
      if (showDebugToasts) {
        Toast.makeText(
            context,
            "Auto BT: Wrong device ($deviceAddress)",
            Toast.LENGTH_SHORT,
          )
          .show()
      }
      return
    }

    // Check Shizuku status
    if (shizukuRepository.shizukuState.value != ShizukuStates.CONNECTED) {
      Log.w(TAG, "Shizuku not connected, cannot toggle hotspot")
      if (showDebugToasts) {
        Toast.makeText(
            context,
            "Auto BT: Shizuku not connected!",
            Toast.LENGTH_LONG,
          )
          .show()
      }
      return
    }

    // Handle the Bluetooth event
    when (intent.action) {
      BluetoothDevice.ACTION_ACL_CONNECTED -> {
        Log.i(
          TAG,
          "Device ${selectedDevice.deviceName} connected, enabling hotspot",
        )
        if (showDebugToasts) {
          Toast.makeText(
              context,
              "Auto BT: Enabling hotspot for ${selectedDevice.deviceName}",
              Toast.LENGTH_SHORT,
            )
            .show()
        }
        softApController.startSoftAp(forceStart = true)
      }
      BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
        Log.i(
          TAG,
          "Device ${selectedDevice.deviceName} disconnected, disabling hotspot",
        )
        if (showDebugToasts) {
          Toast.makeText(
              context,
              "Auto BT: Disabling hotspot",
              Toast.LENGTH_SHORT,
            )
            .show()
        }
        softApController.stopSoftAp(forceStop = true)
      }
    }
  }
}
