package dev.coletz.adbwifi

sealed class AdbWifiToggleState(val enabled: Boolean) {
    class Toggled(enabled: Boolean): AdbWifiToggleState(enabled)
    class MissingWriteSecureSettingPermission(enabled: Boolean): AdbWifiToggleState(enabled)
    class NotConnectedToWifi(enabled: Boolean): AdbWifiToggleState(enabled)
    class NotChanged(enabled: Boolean) : AdbWifiToggleState(enabled)
}
