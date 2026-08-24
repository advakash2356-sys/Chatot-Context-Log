package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class CalendarAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getStringExtra(CalendarNotificationScheduler.EXTRA_EVENT_ID) ?: ""
        val eventTitle = intent.getStringExtra(CalendarNotificationScheduler.EXTRA_EVENT_TITLE) ?: "Upcoming Meeting"
        val startTime = intent.getLongExtra(CalendarNotificationScheduler.EXTRA_EVENT_START_TIME, 0L)
        val matterCode = intent.getStringExtra(CalendarNotificationScheduler.EXTRA_MATTER_CODE) ?: "GENERAL"
        val location = intent.getStringExtra(CalendarNotificationScheduler.EXTRA_EVENT_LOCATION) ?: ""

        Log.d("CalendarAlarmReceiver", "Received 5-minute pre-event alarm for: '$eventTitle' [$matterCode]")

        CalendarNotificationScheduler.show5MinEventNotification(
            context = context,
            eventId = eventId,
            eventTitle = eventTitle,
            matterCode = matterCode,
            startTime = startTime,
            location = location
        )
    }
}
