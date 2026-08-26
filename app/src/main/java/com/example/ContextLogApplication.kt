package com.example

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.notification.CalendarNotificationScheduler

class ContextLogApplication : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        try {
            CalendarNotificationScheduler.createNotificationChannel(this)
        } catch (e: Exception) {
            Log.w("ContextLogApplication", "Failed to create notification channel on startup", e)
        }
    }
}
