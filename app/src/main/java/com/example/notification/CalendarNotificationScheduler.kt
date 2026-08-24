package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.CalendarEventEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CalendarNotificationScheduler {
    private const val TAG = "CalendarNotification"

    const val NOTIFICATION_CHANNEL_ID = "calendar_5min_alert_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Meeting & Calendar Alerts"
    const val NOTIFICATION_CHANNEL_DESC = "Alerts 5 minutes before scheduled calendar events with quick-action context logging."

    // Intent Action & Extras
    const val ACTION_ALARM_TRIGGER = "com.example.notification.ACTION_CALENDAR_5MIN_ALARM"
    const val ACTION_OPEN_LOG_ENTRY = "com.example.notification.ACTION_OPEN_LOG_ENTRY"

    const val EXTRA_EVENT_ID = "extra_event_id"
    const val EXTRA_EVENT_TITLE = "extra_event_title"
    const val EXTRA_EVENT_START_TIME = "extra_event_start_time"
    const val EXTRA_MATTER_CODE = "extra_matter_code"
    const val EXTRA_EVENT_LOCATION = "extra_event_location"
    const val EXTRA_AUTO_START_RECORDING = "extra_auto_start_recording"

    /**
     * Initializes the High-Priority Notification Channel with sound, vibration, and lights.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = NOTIFICATION_CHANNEL_DESC
                enableLights(true)
                lightColor = Color.parseColor("#6750A4") // Purple Primary
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Created notification channel: $NOTIFICATION_CHANNEL_ID")
        }
    }

    /**
     * Schedules a local alarm exactly 5 minutes (300,000 ms) before the event begins.
     */
    fun schedule5MinBeforeEventAlert(context: Context, event: CalendarEventEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val fiveMinutesBeforeMs = event.startTime - (5 * 60 * 1000L)
        val now = System.currentTimeMillis()

        if (fiveMinutesBeforeMs <= now) {
            Log.d(TAG, "Event '${event.title}' is in the past or starts in less than 5 minutes. Skipping future alarm schedule.")
            return
        }

        val intent = Intent(context, CalendarAlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra(EXTRA_EVENT_ID, event.id)
            putExtra(EXTRA_EVENT_TITLE, event.title)
            putExtra(EXTRA_EVENT_START_TIME, event.startTime)
            putExtra(EXTRA_MATTER_CODE, event.matterCode)
            putExtra(EXTRA_EVENT_LOCATION, event.location)
        }

        val requestCode = event.id.hashCode()
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    fiveMinutesBeforeMs,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    fiveMinutesBeforeMs,
                    pendingIntent
                )
            }
            val timeFormat = SimpleDateFormat("MMM dd, h:mm a", Locale.US)
            Log.d(TAG, "Scheduled 5-min alert for '${event.title}' [${event.matterCode}] at ${timeFormat.format(Date(fiveMinutesBeforeMs))}")
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission not granted, falling back to standard alarm: ${e.message}")
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                fiveMinutesBeforeMs,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm for event '${event.title}'", e)
        }
    }

    /**
     * Schedules 5-minute pre-event alarms for all upcoming calendar events.
     */
    fun scheduleAllUpcomingEvents(context: Context, events: List<CalendarEventEntity>) {
        createNotificationChannel(context)
        for (event in events) {
            schedule5MinBeforeEventAlert(context, event)
        }
    }

    /**
     * Cancels any scheduled alarm for a given event ID.
     */
    fun cancelEventAlert(context: Context, eventId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, CalendarAlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra(EXTRA_EVENT_ID, eventId)
        }
        val requestCode = eventId.hashCode()
        val flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled 5-min alarm for event ID: $eventId")
        }
    }

    /**
     * Dispatches the 5-Minute Pre-Meeting Notification with interactive quick-link action buttons.
     */
    fun show5MinEventNotification(
        context: Context,
        eventId: String,
        eventTitle: String,
        matterCode: String,
        startTime: Long,
        location: String
    ) {
        createNotificationChannel(context)

        val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
        val formattedTime = if (startTime > 0) timeFormat.format(Date(startTime)) else "in 5 minutes"

        val requestCode = (eventId.ifBlank { eventTitle }).hashCode()

        // 1. Primary Tap Intent -> Opens App and opens the Log Entry Modal pre-populated
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_LOG_ENTRY
            putExtra(ACTION_OPEN_LOG_ENTRY, true)
            putExtra(EXTRA_EVENT_ID, eventId)
            putExtra(EXTRA_EVENT_TITLE, eventTitle)
            putExtra(EXTRA_MATTER_CODE, matterCode)
            putExtra(EXTRA_AUTO_START_RECORDING, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Quick Action 1 -> Direct "📝 Quick Log Entry" button
        val quickLogIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_LOG_ENTRY
            putExtra(ACTION_OPEN_LOG_ENTRY, true)
            putExtra(EXTRA_EVENT_ID, eventId)
            putExtra(EXTRA_EVENT_TITLE, eventTitle)
            putExtra(EXTRA_MATTER_CODE, matterCode)
            putExtra(EXTRA_AUTO_START_RECORDING, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val quickLogPendingIntent = PendingIntent.getActivity(
            context,
            requestCode + 1,
            quickLogIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Quick Action 2 -> "🎙️ Voice Dictate" button
        val voiceDictateIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_LOG_ENTRY
            putExtra(ACTION_OPEN_LOG_ENTRY, true)
            putExtra(EXTRA_EVENT_ID, eventId)
            putExtra(EXTRA_EVENT_TITLE, eventTitle)
            putExtra(EXTRA_MATTER_CODE, matterCode)
            putExtra(EXTRA_AUTO_START_RECORDING, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val voiceDictatePendingIntent = PendingIntent.getActivity(
            context,
            requestCode + 2,
            voiceDictateIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val bigText = buildString {
            append("Meeting starts in 5 minutes ($formattedTime).\n")
            append("Matter: [$matterCode]\n")
            if (location.isNotBlank()) {
                append("Location: $location\n")
            }
            append("Tap below to quickly log context or dictate voice notes before the meeting starts.")
        }

        val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏱️ 5 Mins to: $eventTitle")
            .setContentText("[$matterCode] Starts at $formattedTime. Tap to open Context Log.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("⏱️ Starting in 5 Mins: $eventTitle")
                    .setSummaryText("[$matterCode] Meeting Reminder")
                    .bigText(bigText)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFF6750A4.toInt())
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_edit,
                "📝 Quick Log Entry",
                quickLogPendingIntent
            )
            .addAction(
                android.R.drawable.ic_btn_speak_now,
                "🎙️ Voice Dictate",
                voiceDictatePendingIntent
            )

        try {
            val notificationManagerCompat = NotificationManagerCompat.from(context)
            notificationManagerCompat.notify(requestCode, notificationBuilder.build())
            Log.d(TAG, "Dispatched 5-min notification for '$eventTitle' [NotificationId=$requestCode]")
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission not granted: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying 5-min calendar notification", e)
        }
    }

    /**
     * Immediately triggers a live test 5-minute pre-meeting notification.
     */
    fun triggerImmediateTest5MinAlert(
        context: Context,
        eventTitle: String = "Acme Settlement Deposition",
        matterCode: String = "LGL-9021"
    ) {
        val now = System.currentTimeMillis()
        val mockStartTime = now + (5 * 60 * 1000L)
        show5MinEventNotification(
            context = context,
            eventId = "test-alert-${System.currentTimeMillis()}",
            eventTitle = eventTitle,
            matterCode = matterCode,
            startTime = mockStartTime,
            location = "Virtual Conference Room / Google Meet"
        )
    }
}
