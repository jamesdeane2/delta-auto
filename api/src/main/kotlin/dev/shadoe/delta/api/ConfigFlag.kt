package dev.shadoe.delta.api

/**
 * Holds certain flags used to determine the app behavior.
 *
 * Possible usage: Determining first run, app version migrations, etc.
 */
enum class ConfigFlag {
  /** True if the app has been run before. */
  NOT_FIRST_RUN,

  /**
   * True if the app uses Room for config.
   *
   * This is set either after migrating app from Datastore when upgrading from
   * older versions or when app is installed afresh.
   */
  @Deprecated("Not used anymore", level = DeprecationLevel.HIDDEN) USES_ROOM_DB,

  /**
   * Adding a permission to the receiver cannot let arbitrary apps send intents
   * to this app. Thus, a setting is added in the app for power users to enable
   * insecure receivers.
   */
  INSECURE_RECEIVER_ENABLED,

  /**
   * Enable automatic hotspot start when a specific Bluetooth device connects,
   * and automatic stop when it disconnects.
   */
  AUTO_ENABLE_ON_BT,

  /**
   * Show debug Toast notifications for Auto Enable on BT feature.
   * Useful for troubleshooting connection issues.
   */
  AUTO_ENABLE_ON_BT_DEBUG_TOASTS,
}
