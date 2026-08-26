package com.example.data.calendar

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.TimeZone

data class DeviceCalendarResult(
    val isSuccess: Boolean,
    val eventUri: Uri? = null,
    val eventId: Long? = null,
    val errorMessage: String? = null
)

class DeviceCalendarManager(private val context: Context) {

    companion object {
        private const val TAG = "DeviceCalendarManager"
    }

    /**
     * Checks if the app has READ and WRITE permissions for device calendar.
     */
    fun hasCalendarPermissions(): Boolean {
        val writeGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        val readGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        return writeGranted && readGranted
    }

    /**
     * Finds the primary or first available writeable calendar ID on the device.
     */
    fun getPrimaryCalendarId(): Long? {
        if (!hasCalendarPermissions()) return null
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )
        val contentResolver: ContentResolver = context.contentResolver
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )
            if (cursor != null) {
                var firstCalendarId: Long? = null
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val isPrimary = if (cursor.isNull(1)) 0 else cursor.getInt(1)
                    val accessLevel = if (cursor.isNull(2)) 0 else cursor.getInt(2)
                    
                    // Access level >= 500 means contributor or owner (writeable)
                    if (accessLevel >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) {
                        if (isPrimary == 1) {
                            return id
                        }
                        if (firstCalendarId == null) {
                            firstCalendarId = id
                        }
                    }
                }
                return firstCalendarId
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying device calendars", e)
        } finally {
            cursor?.close()
        }
        return null
    }

    /**
     * Inserts an event directly into the device's default/primary calendar via ContentResolver.
     */
    fun insertEvent(
        title: String,
        description: String,
        startEpochMs: Long,
        endEpochMs: Long = startEpochMs + 3600000L,
        location: String? = null
    ): DeviceCalendarResult {
        if (!hasCalendarPermissions()) {
            return DeviceCalendarResult(
                isSuccess = false,
                errorMessage = "Calendar permission not granted"
            )
        }

        val calendarId = getPrimaryCalendarId() ?: 1L
        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startEpochMs)
            put(CalendarContract.Events.DTEND, endEpochMs)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            if (!location.isNullOrBlank()) {
                put(CalendarContract.Events.EVENT_LOCATION, location)
            }
            put(CalendarContract.Events.HAS_ALARM, 1)
        }

        return try {
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            if (uri != null) {
                val eventId = uri.lastPathSegment?.toLongOrNull()
                
                // Add a 15-minute notification reminder
                if (eventId != null) {
                    val reminderValues = ContentValues().apply {
                        put(CalendarContract.Reminders.EVENT_ID, eventId)
                        put(CalendarContract.Reminders.MINUTES, 15)
                        put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                    }
                    try {
                        context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
                    } catch (re: Exception) {
                        Log.w(TAG, "Failed to add reminder for event $eventId", re)
                    }
                }
                
                Log.d(TAG, "Successfully inserted event into Device Calendar: $uri")
                DeviceCalendarResult(isSuccess = true, eventUri = uri, eventId = eventId)
            } else {
                DeviceCalendarResult(isSuccess = false, errorMessage = "ContentResolver returned null URI")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting event into Device Calendar", e)
            DeviceCalendarResult(isSuccess = false, errorMessage = e.localizedMessage)
        }
    }

    /**
     * Creates an Intent to open the device's native calendar app with pre-filled event parameters.
     */
    fun createCalendarIntent(
        title: String,
        description: String,
        startEpochMs: Long,
        endEpochMs: Long = startEpochMs + 3600000L,
        location: String? = null
    ): Intent {
        return Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startEpochMs)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endEpochMs)
            if (!location.isNullOrBlank()) {
                putExtra(CalendarContract.Events.EVENT_LOCATION, location)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
