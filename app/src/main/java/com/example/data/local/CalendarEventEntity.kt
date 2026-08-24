package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val googleEventId: String? = null,
    val title: String,
    val description: String = "",
    val startTime: Long,
    val endTime: Long,
    val location: String = "",
    val attendees: String = "",
    val matterCode: String = "GENERAL",
    val isSynced: Boolean = true,
    val htmlLink: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
