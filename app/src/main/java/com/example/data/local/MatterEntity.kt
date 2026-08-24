package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "matters")
data class MatterEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val code: String,
    val name: String,
    val clientName: String,
    val createdAt: Long = System.currentTimeMillis()
)
