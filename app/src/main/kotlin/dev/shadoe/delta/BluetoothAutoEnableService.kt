package dev.shadoe.delta

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.shadoe.delta.data.BluetoothRepository
import dev.shadoe.delta.data.FlagsRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BluetoothAutoEnableService : Service() {
  @Inject lateinit var bluetoothRepository: BluetoothRepository
  @Inject lateinit var flagsRepository: FlagsRepository

  private val bluetoothReceiver = BluetoothAutoEnableReceiver()
  private var isReceiverRegistered = false
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  override fun onCreate() {
    super.onCreate()
    Log.d(TAG, "Service created")
    createNotificationChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Log.d(TAG, "Service started")

    // CRITICAL: Call startForeground() IMMEDIATELY and SYNCHRONOUSLY
    // Android requires this within ~5 seconds of startForegroundService()
    startForeground(NOTIFICATION_ID, createInitialNotification())

    // Now do async work to check if feature is enabled and update notification
    serviceScope.launch {
      val isEnabled: Boolean = flagsRepository.isAutoEnableOnBtEnabled()

      if (isEnabled) {
        // Update notification with device info now that we can suspend
        val updatedNotification = createNotificationWithDevice()
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, updatedNotification)

        // Register Bluetooth receiver if not already registered
        if (!isReceiverRegistered) {
          val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
          }
          registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_EXPORTED)
          isReceiverRegistered = true
          Log.d(TAG, "Bluetooth receiver registered in service")
        }
      } else {
        // Feature disabled, stop the service
        Log.d(TAG, "Feature disabled, stopping service")
        stopSelf()
      }
    }

    // If the service is killed, Android will restart it
    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()
    if (isReceiverRegistered) {
      try {
        unregisterReceiver(bluetoothReceiver)
        isReceiverRegistered = false
        Log.d(TAG, "Bluetooth receiver unregistered")
      } catch (e: IllegalArgumentException) {
        Log.e(TAG, "Receiver not registered", e)
      }
    }
    Log.d(TAG, "Service destroyed")
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun createNotificationChannel() {
    val channel =
      NotificationChannel(
        CHANNEL_ID,
        "Auto Enable on Bluetooth",
        NotificationManager.IMPORTANCE_LOW,
      ).apply {
        description =
          "Keeps the app running to monitor Bluetooth connections"
        setShowBadge(false)
      }

    val notificationManager = getSystemService(NotificationManager::class.java)
    notificationManager.createNotificationChannel(channel)
  }

  // Synchronous notification for immediate startForeground() call
  private fun createInitialNotification(): Notification {
    val intent =
      Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      }
    val pendingIntent =
      PendingIntent.getActivity(
        this,
        0,
        intent,
        PendingIntent.FLAG_IMMUTABLE,
      )

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("Auto Enable on Bluetooth")
      .setContentText("Starting...")
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentIntent(pendingIntent)
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .build()
  }

  // Suspend function to create notification with device info
  private suspend fun createNotificationWithDevice(): Notification {
    val selectedDevice = bluetoothRepository.getSelectedDevice()
    val deviceName = selectedDevice?.deviceName ?: "Unknown device"

    val intent =
      Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      }
    val pendingIntent =
      PendingIntent.getActivity(
        this,
        0,
        intent,
        PendingIntent.FLAG_IMMUTABLE,
      )

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("Auto Enable on Bluetooth")
      .setContentText("Monitoring: $deviceName")
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentIntent(pendingIntent)
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .build()
  }

  companion object {
    private const val TAG = "BtAutoEnableService"
    private const val CHANNEL_ID = "auto_enable_bt_service"
    private const val NOTIFICATION_ID = 1001
  }
}
