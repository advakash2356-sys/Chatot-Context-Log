package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "personal_dictionary")
data class DictionaryItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val term: String,
    val category: String = "NAME", // "NAME", "ACRONYM", "COMPANY", "PRODUCT", "TECHNICAL"
    val phoneticOrNotes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "snippets")
data class SnippetEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val triggerPhrase: String,
    val expandedText: String,
    val description: String = "",
    val category: String = "GENERAL",
    val createdAt: Long = System.currentTimeMillis()
)
