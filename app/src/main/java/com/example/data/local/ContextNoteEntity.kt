package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class EntryType {
    LOG,
    REMINDER,
    LEGAL_MATTER,
    DECISION_PAUSE,
    RAG_QUESTION
}

@Entity(tableName = "context_notes")
data class ContextNoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val matterId: String? = null,
    val matterCode: String = "GENERAL",
    val title: String = "",
    val sessionBoundaryId: String = "",
    val durationSeconds: Int = 60,
    val source: String = "PENDANT_BLE", // "PENDANT_BLE", "DESKTOP_LOOPBACK", "MANUAL_MIC"
    val rawTranscript: String,
    val cleanText: String,
    val executiveSummary: String = "",
    val structuredNotes: String = "",
    val verbatimTurnsJson: String = "",
    val audioPurged: Boolean = true,
    val isEncrypted: Boolean = true,
    val participants: String = "You",
    val entryType: EntryType = EntryType.LOG,
    val depthLevel: Int = 1,
    val recordedAt: Long = System.currentTimeMillis(),
    val twoHourBlockStart: Long = calculateTwoHourBlock(recordedAt),
    val scheduledDatetime: Long? = null,
    val googleEventId: String? = null,
    val syncedToCalendar: Boolean = false,
    val tags: String = "",
    val isSyncedToBackend: Boolean = false
) {
    val tagList: List<String>
        get() = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim().removePrefix("#") }.filter { it.isNotEmpty() }.distinct()

    companion object {
        fun calculateTwoHourBlock(timestampMs: Long): Long {
            val epochSeconds = timestampMs / 1000
            val blockStartSeconds = (epochSeconds / 7200) * 7200
            return blockStartSeconds * 1000
        }
    }
}
