package dev.coletz.adbwifi

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationUtils.createNotificationChannel(this)
    }
}