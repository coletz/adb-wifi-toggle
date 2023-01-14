package dev.coletz.adbwifi

import android.app.StatusBarManager
import android.app.StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED
import android.app.StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.database.ContentObserver
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {

    companion object {
        private const val TILE_ADDED = "sp_key.TILE_ADDED"
    }

    private lateinit var wifiAdbToggle: ToggleButton
    private lateinit var messagesTextView: TextView
    private lateinit var addTileButton: View

    private var warningMessage: Int?
        get() = throw UnsupportedOperationException()
        set(value) {
            if (value == null) {
                messagesTextView.visibility = View.GONE
                wifiAdbToggle.visibility = View.VISIBLE
            } else {
                messagesTextView.setText(value)
                messagesTextView.visibility = View.VISIBLE
                wifiAdbToggle.visibility = View.GONE
            }
        }

    private val uiHandler = Handler(Looper.getMainLooper())
    private val settingsObserver = object: ContentObserver(uiHandler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            refreshWirelessDebuggingToggle(AdbUtils.getCurrentState(this@MainActivity))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        wifiAdbToggle = findViewById(R.id.wifi_adb_toggle)
        messagesTextView = findViewById(R.id.messages_text_view)
        addTileButton = findViewById(R.id.add_tile_button)

        wifiAdbToggle.setOnClickListener { onSwitchToggled(wifiAdbToggle.isChecked) }
        addTileButton.setOnClickListener { requestAddTile(true) }
    }

    override fun onResume() {
        super.onResume()
        refreshWirelessDebuggingToggle(AdbUtils.getCurrentState(this))

        contentResolver.registerContentObserver(
            Settings.Global.getUriFor(PrivateSettings.Global.ADB_WIFI_ENABLED),
            false,
            settingsObserver
        )

        requestAddTile()
    }

    override fun onPause() {
        super.onPause()
        contentResolver.unregisterContentObserver(settingsObserver)
    }

    private fun onSwitchToggled(isChecked: Boolean) {
        val result = AdbUtils.writeAdbWifiSetting(this, isChecked)
        refreshWirelessDebuggingToggle(result)
    }

    private fun refreshWirelessDebuggingToggle(state: AdbWifiToggleState) {
        when (state) {
            is AdbWifiToggleState.NotChanged, is AdbWifiToggleState.Toggled -> {
                warningMessage = null
            }
            is AdbWifiToggleState.MissingWriteSecureSettingPermission -> {
                warningMessage = R.string.missing_write_secure_settings_permission
                messagesTextView.setOnClickListener {
                    getSystemService<ClipboardManager>()
                        ?.setPrimaryClip(ClipData.newPlainText(
                            "adb_command",
                            """adb shell "pm grant $packageName android.permission.WRITE_SECURE_SETTINGS""""
                        ))
                }
            }
            is AdbWifiToggleState.NotConnectedToWifi -> {
                warningMessage = R.string.adb_wireless_no_network_msg
            }
        }

        wifiAdbToggle.isChecked = state.enabled
    }

    private fun requestAddTile(forced: Boolean = false) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (forced || !prefs.getBoolean(TILE_ADDED, false)) {
            getSystemService<StatusBarManager>()?.requestAddTileService(
                ComponentName(packageName, AdbQstService::class.java.name),
                getString(R.string.adb_quick_settings_tile_label),
                Icon.createWithResource(this, R.drawable.ic_adb),
                Executors.newSingleThreadExecutor()
            ) { resCode ->
                if (resCode != TILE_ADD_REQUEST_RESULT_TILE_ADDED && resCode != TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
                    prefs.edit { putBoolean(TILE_ADDED, false) }
                    NotificationUtils.notifyErrorAddingTile(this, resCode)
                } else {
                    prefs.edit { putBoolean(TILE_ADDED, true) }
                }
            }
        }
    }
}