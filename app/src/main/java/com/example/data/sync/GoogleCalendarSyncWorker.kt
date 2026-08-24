package com.example.data.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.ai.GeminiService
import com.example.data.calendar.GoogleCalendarService
import com.example.data.local.AppDatabase
import com.example.data.local.ContextNoteEntity
import com.example.data.local.EntryType
import com.example.data.local.NoteEmbeddingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * WorkManager service that periodically fetches upcoming Google Calendar events
 * and generates context-aware meeting notes and vector embeddings for scheduled matters.
 */
class GoogleCalendarSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "GoogleCalendarSyncWorker started: syncing upcoming events...")
            val database = AppDatabase.getDatabase(applicationContext)
            val dao = database.contextLogDao()
            val calendarService = GoogleCalendarService()
            val geminiService = GeminiService()

            // 1. Fetch upcoming events (from Google Calendar API or local zero-latency cache)
            val upcomingEvents = calendarService.fetchUpcomingEvents(
                accessToken = null,
                timeMinMillis = System.currentTimeMillis() - 3600000L * 4,
                timeMaxMillis = System.currentTimeMillis() + 86400000L * 7
            )

            if (upcomingEvents.isNotEmpty()) {
                dao.insertCalendarEvents(upcomingEvents)
                Log.d(TAG, "Inserted ${upcomingEvents.size} calendar events into local cache")
                // Schedule local 5-minute pre-event notification triggers
                try {
                    com.example.notification.CalendarNotificationScheduler.scheduleAllUpcomingEvents(
                        applicationContext,
                        upcomingEvents
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to schedule calendar event 5-minute notifications", e)
                }
            }

            // 2. Context-Aware Meeting Note Creation for events within the next 24 hours
            val now = System.currentTimeMillis()
            val next24Hours = now + 86400000L
            val existingNotes = dao.getAllNotesSync()
            val timeFormat = SimpleDateFormat("MMM dd, h:mm a", Locale.US)

            var createdNotesCount = 0
            for (event in upcomingEvents) {
                if (event.startTime in now..next24Hours) {
                    // Check if a note already exists for this event
                    val isAlreadyCreated = existingNotes.any { note ->
                        note.cleanText.contains(event.title, ignoreCase = true) ||
                        note.matterCode.equals(event.matterCode, ignoreCase = true) &&
                        Math.abs(note.twoHourBlockStart - (event.startTime - (event.startTime % 7200000L))) < 3600000L
                    }

                    if (!isAlreadyCreated) {
                        val blockStart = event.startTime - (event.startTime % 7200000L)
                        val formattedTime = timeFormat.format(Date(event.startTime))
                        val cleanText = "Upcoming Calendar Meeting: \"${event.title}\" scheduled for $formattedTime with matter code [${event.matterCode}]." +
                                (if (event.location.isNotBlank()) " Location: ${event.location}." else "") +
                                (if (event.attendees.isNotBlank()) " Attendees: ${event.attendees}." else "")

                        val newNote = ContextNoteEntity(
                            rawTranscript = "Scheduled from Google Calendar: ${event.title}",
                            cleanText = cleanText,
                            entryType = EntryType.REMINDER,
                            matterCode = if (event.matterCode.isNotBlank()) event.matterCode else "GENERAL",
                            depthLevel = 2,
                            twoHourBlockStart = blockStart,
                            isSyncedToBackend = false,
                            tags = "calendar,meeting,upcoming,${event.matterCode.lowercase()}"
                        )

                        dao.insertNote(newNote)
                        createdNotesCount++

                        // Generate semantic embedding for the new meeting note
                        try {
                            val embedding = geminiService.generateEmbedding(newNote.cleanText, newNote.matterCode)
                            if (embedding.isNotEmpty()) {
                                dao.insertNoteEmbedding(
                                    NoteEmbeddingEntity(
                                        noteId = newNote.id,
                                        matterCode = newNote.matterCode,
                                        textSnippet = newNote.cleanText,
                                        embeddingJson = NoteEmbeddingEntity.floatsToJson(embedding)
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to generate embedding for note ${newNote.id}: ${e.message}")
                        }
                    }
                }
            }

            Log.d(TAG, "GoogleCalendarSyncWorker completed successfully. Created $createdNotesCount context-aware notes.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "GoogleCalendarSyncWorker execution failed", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "CalendarSyncWorker"
        const val PERIODIC_CALENDAR_WORK_NAME = "google_calendar_periodic_sync"
        const val ONE_TIME_CALENDAR_WORK_NAME = "google_calendar_one_time_sync"

        /**
         * Schedules periodic WorkManager task to keep Google Calendar events in sync.
         */
        fun schedulePeriodicCalendarSync(context: Context, intervalMinutes: Long = 60) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<GoogleCalendarSyncWorker>(
                intervalMinutes, TimeUnit.MINUTES,
                15, TimeUnit.MINUTES // Flex interval
            )
            .setConstraints(constraints)
            .addTag(PERIODIC_CALENDAR_WORK_NAME)
            .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_CALENDAR_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
            Log.d(TAG, "Scheduled periodic Google Calendar sync every $intervalMinutes minutes")
        }

        /**
         * Triggers an immediate one-time sync of calendar events and context note creation.
         */
        fun enqueueOneTimeCalendarSync(context: Context) {
            val oneTimeRequest = OneTimeWorkRequestBuilder<GoogleCalendarSyncWorker>()
                .addTag(ONE_TIME_CALENDAR_WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_CALENDAR_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
            Log.d(TAG, "Enqueued one-time Google Calendar sync worker")
        }
    }
}
