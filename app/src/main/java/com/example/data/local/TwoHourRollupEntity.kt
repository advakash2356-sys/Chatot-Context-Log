package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "two_hour_rollups")
data class TwoHourRollupEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val twoHourBlockStart: Long,
    val matterCode: String? = null,
    val executiveSummary: String,
    val formattedBillableText: String,
    val estimatedHours: Double = 2.0,
    val generatedAt: Long = System.currentTimeMillis()
)
