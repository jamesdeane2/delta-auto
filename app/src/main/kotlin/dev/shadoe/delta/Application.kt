package dev.shadoe.delta

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.IntentFilter
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import dev.shadoe.delta.crash.CrashHandlerUtils
import kotlin.system.exitProcess
import org.lsposed.hiddenapibypass.HiddenApiBypass

@HiltAndroidApp
class Application : Application() {
  private val bluetoothReceiver = BluetoothAutoEnableReceiver()

  override fun onCreate() {
    super.onCreate()

    Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
      Log.e(packageName, "Uncaught exception", throwable)
      CrashHandlerUtils.sendCrashNotification(applicationContext, throwable)
      exitProcess(1)
    }

    HiddenApiBypass.setHiddenApiExemptions("L")

    // Register Bluetooth receiver dynamically (required since Android 8.0)
    val filter = IntentFilter().apply {
      addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
      addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
    }
    registerReceiver(bluetoothReceiver, filter)
    Log.d("Application", "Bluetooth receiver registered")
  }
}
