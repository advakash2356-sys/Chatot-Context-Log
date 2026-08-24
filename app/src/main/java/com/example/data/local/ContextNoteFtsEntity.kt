package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Entity(tableName = "context_notes_fts")
@Fts4
data class ContextNoteFtsEntity(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowid: Int = 0,
    val noteId: String,
    val cleanText: String,
    val rawTranscript: String,
    val matterCode: String
)
