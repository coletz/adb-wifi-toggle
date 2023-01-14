package dev.coletz.adbwifi

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class AdbQstService: TileService() {

    private val uiHandler = Handler(Looper.getMainLooper())

    private val settingsObserver = object: ContentObserver(uiHandler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            refreshWirelessDebuggingToggle(AdbUtils.getCurrentState(this@AdbQstService))
        }
    }

    // Called when your app can update your tile.
    override fun onStartListening() {
        super.onStartListening()

        refreshWirelessDebuggingToggle(AdbUtils.getCurrentState(this))

        contentResolver.registerContentObserver(
            Settings.Global.getUriFor(PrivateSettings.Global.ADB_WIFI_ENABLED),
            false,
            settingsObserver
        )
    }

    // Called when your app can no longer update your tile.
    override fun onStopListening() {
        super.onStopListening()
        contentResolver.unregisterContentObserver(settingsObserver)
    }

    // Called when the user taps on your tile in an active or inactive state.
    override fun onClick() {
        super.onClick()

        onSwitchToggled(qsTile.state == Tile.STATE_INACTIVE)
    }

    private fun onSwitchToggled(isChecked: Boolean) {
        val result = AdbUtils.writeAdbWifiSetting(this, isChecked)
        refreshWirelessDebuggingToggle(result)
    }

    private fun refreshWirelessDebuggingToggle(state: AdbWifiToggleState) {
        qsTile.label = when (state) {
            is AdbWifiToggleState.NotChanged, is AdbWifiToggleState.Toggled -> {
                getString(R.string.adb_quick_settings_tile_label)
            }
            is AdbWifiToggleState.MissingWriteSecureSettingPermission -> {
                getString(R.string.adb_quick_settings_tile_missing_permission)
            }
            is AdbWifiToggleState.NotConnectedToWifi -> {
                getString(R.string.adb_quick_settings_tile_no_wifi)
            }
        }
        qsTile.state = if (state.enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile.updateTile()
    }
}