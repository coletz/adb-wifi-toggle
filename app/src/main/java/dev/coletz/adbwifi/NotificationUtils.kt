package dev.coletz.adbwifi

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService

object NotificationUtils {

    private const val NOTIFICATION_CHANNEL_ID = "notification.missing_permission"

    fun createNotificationChannel(context: Context) {
        with(context) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.channel_name_missing_write_secure_settings_permission),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_description_missing_write_secure_settings_permission)
                setBypassDnd(true)
            }
            val notificationManager: NotificationManager? = getSystemService()
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun notifyMissingWriteSecureSettingsPermission(context: Context){
        context.notify(context.getString(R.string.missing_write_secure_settings_permission))
    }

    fun notifyNotConnectedToWifiNetwork(context: Context){
        context.notify(context.getString(R.string.adb_wireless_no_network_msg))
    }

    fun notifyErrorAddingTile(context: Context, errorCode: Int) {
        context.notify("An error occurred trying to add the tile; Code: $errorCode")
    }

    private fun Context.notify(message: String) {
        // Use both notification and toast so even if user disabled notification something will happen
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        val pendingIntent = Intent(this, MainActivity::class.java)
            .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
            .let { PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE) }

        NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
            .run { getSystemService<NotificationManager>()?.notify(0, this) }
    }
}
