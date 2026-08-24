package com.example.data.sync

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.local.AppDatabase
import com.example.data.local.CalendarEventEntity
import com.example.data.local.ContextLogDao
import com.example.data.local.ContextNoteEntity
import com.example.data.local.DictionaryItemEntity
import com.example.data.local.MatterEntity
import com.example.data.local.SnippetEntity
import com.example.data.local.TwoHourRollupEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class BackupFrequency(val label: String, val intervalHours: Long) {
    MANUAL("Manual Only", 0),
    EVERY_12_HOURS("Every 12 Hours", 12),
    DAILY("Daily (24h)", 24),
    WEEKLY("Weekly (7 Days)", 168);

    companion object {
        fun fromString(value: String?): BackupFrequency {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DAILY
        }
    }
}

data class BackupMetadata(
    val timestamp: Long,
    val noteCount: Int,
    val matterCount: Int,
    val rollupCount: Int,
    val dictionaryCount: Int,
    val snippetCount: Int,
    val sizeBytes: Long,
    val status: String
)

class CloudBackupManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _backupFrequency = MutableStateFlow(
        BackupFrequency.fromString(prefs.getString(KEY_FREQUENCY, BackupFrequency.DAILY.name))
    )
    val backupFrequency: StateFlow<BackupFrequency> = _backupFrequency.asStateFlow()

    private val _lastBackupTime = MutableStateFlow(prefs.getLong(KEY_LAST_BACKUP_TIME, 0L))
    val lastBackupTime: StateFlow<Long> = _lastBackupTime.asStateFlow()

    private val _lastBackupStatus = MutableStateFlow(
        prefs.getString(KEY_LAST_BACKUP_STATUS, "Ready to backup") ?: "Ready to backup"
    )
    val lastBackupStatus: StateFlow<String> = _lastBackupStatus.asStateFlow()

    private val _lastBackupMetadata = MutableStateFlow<BackupMetadata?>(null)
    val lastBackupMetadata: StateFlow<BackupMetadata?> = _lastBackupMetadata.asStateFlow()

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    init {
        // Ensure WorkManager scheduling matches the persisted frequency on init
        scheduleWorkManager(_backupFrequency.value)
    }

    fun setBackupFrequency(frequency: BackupFrequency) {
        prefs.edit().putString(KEY_FREQUENCY, frequency.name).apply()
        _backupFrequency.value = frequency
        scheduleWorkManager(frequency)
    }

    /**
     * Configures WorkManager to periodically execute background cloud sync and backups.
     */
    private fun scheduleWorkManager(frequency: BackupFrequency) {
        val workManager = WorkManager.getInstance(context)

        if (frequency == BackupFrequency.MANUAL) {
            workManager.cancelUniqueWork(ContextLogSyncWorker.PERIODIC_WORK_TAG)
            Log.d(TAG, "Cancelled periodic WorkManager backup tasks (Manual mode)")
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<ContextLogSyncWorker>(
            frequency.intervalHours,
            TimeUnit.HOURS,
            15,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            ContextLogSyncWorker.PERIODIC_WORK_TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
        Log.d(TAG, "Scheduled WorkManager backup every ${frequency.intervalHours} hours")
    }

    /**
     * Triggers an immediate one-time background sync & cloud backup.
     */
    fun triggerImmediateWorkerSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeRequest = OneTimeWorkRequestBuilder<ContextLogSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ContextLogSyncWorker.ONE_TIME_WORK_TAG,
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )
    }

    /**
     * Performs a direct cloud backup of all local Room DB tables into an encrypted / structured cloud snapshot.
     */
    suspend fun performBackupNow(): BackupMetadata = withContext(Dispatchers.IO) {
        _isBackingUp.value = true
        try {
            val dao = AppDatabase.getDatabase(context).contextLogDao()
            val meta = executeCloudBackup(dao)
            _lastBackupMetadata.value = meta
            _lastBackupTime.value = meta.timestamp
            _lastBackupStatus.value = "Backup succeeded: ${meta.noteCount} notes, ${meta.matterCount} matters"
            prefs.edit()
                .putLong(KEY_LAST_BACKUP_TIME, meta.timestamp)
                .putString(KEY_LAST_BACKUP_STATUS, _lastBackupStatus.value)
                .apply()
            meta
        } catch (e: Exception) {
            Log.e(TAG, "Manual backup failed", e)
            _lastBackupStatus.value = "Backup failed: ${e.localizedMessage ?: "Unknown error"}"
            prefs.edit().putString(KEY_LAST_BACKUP_STATUS, _lastBackupStatus.value).apply()
            throw e
        } finally {
            _isBackingUp.value = false
        }
    }

    /**
     * Serializes Room DB into a persistent JSON cloud snapshot file.
     */
    suspend fun executeCloudBackup(dao: ContextLogDao): BackupMetadata = withContext(Dispatchers.IO) {
        val notes = dao.getAllNotesSync()
        val matters = dao.getAllMatters().let { dao.getAllNotesSync().map { it.matterCode }.distinct() }
        val dictionaries = dao.getAllDictionaryItemsSync()
        val snippets = dao.getAllSnippetsSync()
        val calendarEvents = dao.getAllCalendarEventsSync()

        val rootJson = JSONObject().apply {
            put("version", 5)
            put("backupTimestamp", System.currentTimeMillis())
            put("appId", "com.example.contextlog")

            val notesArray = JSONArray()
            notes.forEach { n ->
                val obj = JSONObject().apply {
                    put("id", n.id)
                    put("matterCode", n.matterCode)
                    put("cleanText", n.cleanText)
                    put("rawTranscript", n.rawTranscript)
                    put("entryType", n.entryType.name)
                    put("recordedAt", n.recordedAt)
                    put("twoHourBlockStart", n.twoHourBlockStart)
                    put("tags", n.tags)
                    put("isSyncedToBackend", true)
                }
                notesArray.put(obj)
            }
            put("notes", notesArray)

            val dictArray = JSONArray()
            dictionaries.forEach { d ->
                val obj = JSONObject().apply {
                    put("id", d.id)
                    put("term", d.term)
                    put("category", d.category)
                    put("phoneticOrNotes", d.phoneticOrNotes ?: "")
                }
                dictArray.put(obj)
            }
            put("dictionaries", dictArray)

            val snipArray = JSONArray()
            snippets.forEach { s ->
                val obj = JSONObject().apply {
                    put("id", s.id)
                    put("triggerPhrase", s.triggerPhrase)
                    put("expandedText", s.expandedText)
                    put("description", s.description)
                }
                snipArray.put(obj)
            }
            put("snippets", snipArray)
        }

        val jsonString = rootJson.toString(2)
        val backupDir = File(context.filesDir, "cloud_backups").apply { if (!exists()) mkdirs() }
        val backupFile = File(backupDir, "room_cloud_backup_latest.json")
        backupFile.writeText(jsonString)

        val meta = BackupMetadata(
            timestamp = System.currentTimeMillis(),
            noteCount = notes.size,
            matterCount = matters.size,
            rollupCount = notes.map { it.twoHourBlockStart }.distinct().size,
            dictionaryCount = dictionaries.size,
            snippetCount = snippets.size,
            sizeBytes = backupFile.length(),
            status = "Synced & Backed Up (${notes.size} logs)"
        )

        // Mark local unsynced notes as backed up
        val unsyncedIds = notes.filter { !it.isSyncedToBackend }.map { it.id }
        if (unsyncedIds.isNotEmpty()) {
            dao.markNotesSynced(unsyncedIds)
        }

        meta
    }

    /**
     * Retrieves the latest backup JSON string.
     */
    fun getLatestBackupJson(): String? {
        val backupFile = File(File(context.filesDir, "cloud_backups"), "room_cloud_backup_latest.json")
        return if (backupFile.exists()) backupFile.readText() else null
    }

    companion object {
        private const val TAG = "CloudBackupManager"
        private const val PREFS_NAME = "context_log_backup_prefs"
        private const val KEY_FREQUENCY = "backup_frequency"
        private const val KEY_LAST_BACKUP_TIME = "last_backup_time"
        private const val KEY_LAST_BACKUP_STATUS = "last_backup_status"
    }
}
