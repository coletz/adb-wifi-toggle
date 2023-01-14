package dev.coletz.adbwifi

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import androidx.core.content.PermissionChecker
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.core.content.getSystemService
import dev.coletz.adbwifi.PrivateSettings.Global.ADB_WIFI_ENABLED

object AdbUtils {

    private const val ADB_SETTING_OFF = 0
    private const val ADB_SETTING_ON = 1

    fun canWriteSecureSettings(context: Context): Boolean =
        PermissionChecker.checkSelfPermission(context, Manifest.permission.WRITE_SECURE_SETTINGS) == PERMISSION_GRANTED

    fun isWifiConnected(context: Context): Boolean {
        val cm: ConnectivityManager = context.getSystemService() ?: return false
        return cm.allNetworks.any {
            cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }
    }

    fun writeAdbWifiSetting(context: Context, enabled: Boolean): AdbWifiToggleState {
        val currentlyEnabled = isAdbWifiEnabled(context)

        if (!isWifiConnected(context)) {
            NotificationUtils.notifyNotConnectedToWifiNetwork(context)
            return AdbWifiToggleState.NotConnectedToWifi(currentlyEnabled)
        }
        if (!canWriteSecureSettings(context)) {
            NotificationUtils.notifyMissingWriteSecureSettingsPermission(context)
            return AdbWifiToggleState.MissingWriteSecureSettingPermission(currentlyEnabled)
        }

        val changed = Settings.Global.putInt(
            context.contentResolver,
            ADB_WIFI_ENABLED,
            if (enabled) ADB_SETTING_ON else ADB_SETTING_OFF
        )

        if (!changed) {
            return AdbWifiToggleState.NotChanged(currentlyEnabled)
        }

        return AdbWifiToggleState.Toggled(enabled)
    }

    fun getCurrentState(context: Context): AdbWifiToggleState {
        val currentlyEnabled = isAdbWifiEnabled(context)

        if (!isWifiConnected(context)) {
            return AdbWifiToggleState.NotConnectedToWifi(currentlyEnabled)
        }
        if (!canWriteSecureSettings(context)) {
            return AdbWifiToggleState.MissingWriteSecureSettingPermission(currentlyEnabled)
        }

        return AdbWifiToggleState.Toggled(currentlyEnabled)
    }

    fun isAdbWifiEnabled(context: Context): Boolean =
        (Settings.Global.getInt(context.contentResolver, ADB_WIFI_ENABLED, ADB_SETTING_OFF) != ADB_SETTING_OFF)
}