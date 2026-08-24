package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            Log.d("BootCompletedReceiver", "Device rebooted / app updated: rescheduling calendar 5-min alert alarms...")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val events = db.contextLogDao().getAllCalendarEventsSync()
                    val now = System.currentTimeMillis()
                    val upcoming = events.filter { it.startTime > now }
                    CalendarNotificationScheduler.scheduleAllUpcomingEvents(context, upcoming)
                    Log.d("BootCompletedReceiver", "Successfully rescheduled ${upcoming.size} upcoming calendar event alerts.")
                } catch (e: Exception) {
                    Log.e("BootCompletedReceiver", "Failed to reschedule calendar alarms after reboot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
