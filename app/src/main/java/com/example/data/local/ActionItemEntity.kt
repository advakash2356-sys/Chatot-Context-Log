package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "action_items")
data class ActionItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val memoryId: String? = null,
    val memoryTitle: String? = null,
    val title: String,
    val owner: String = "You",
    val isAssignedToYou: Boolean = true,
    val actionVerb: String = "Follow up", // "Review", "Send", "Draft", "Schedule", "Submit", "Follow up", "Prepare"
    val targetDueDate: Long? = null,
    val dueDateFormatted: String = "Today, 5:00 PM",
    val isCompleted: Boolean = false,
    val priority: String = "HIGH", // "HIGH", "MEDIUM", "LOW"
    val extractedAt: Long = System.currentTimeMillis(),
    val externalSyncTarget: String? = null, // "Task Management", "Workspace Docs", "System Reminders", "Action Hub", "Calendar Schedule"
    val externalSyncStatus: String = "READY" // "READY", "EXPORTED", "SYNCED"
)

@Entity(tableName = "briefing_dossiers")
data class BriefingDossierEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val targetPersonOrTopic: String,
    val title: String,
    val executiveSummary: String,
    val keyDecisions: String, // Comma or newline separated
    val openActionItems: String,
    val relatedMemoryIds: String,
    val generatedAt: Long = System.currentTimeMillis()
)
