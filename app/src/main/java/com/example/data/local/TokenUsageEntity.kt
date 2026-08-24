package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Entity storing token usage metrics recorded from Gemini API responses (headers & usageMetadata).
 * Used for detailed billing reporting, daily expenditure analytics, and cost modeling.
 */
@Entity(
    tableName = "token_usage_metrics",
    indices = [
        Index(value = ["dateString"]),
        Index(value = ["timestamp"]),
        Index(value = ["matterCode"])
    ]
)
data class TokenUsageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String, // e.g. "2026-08-23"
    val promptTokens: Int,
    val candidatesTokens: Int,
    val totalTokens: Int,
    val estimatedCostUsd: Double,
    val endpoint: String, // e.g. "gemini-3.5-flash:generateContent"
    val modelName: String = "gemini-3.5-flash",
    val matterCode: String = "GENERAL",
    val requestType: String = "NOTE_PARSE" // "NOTE_PARSE", "2H_ROLLUP", "RAG_QUESTION", "TRANSCRIPTION", "TRANSFORM"
)

data class DailyTokenAggregate(
    val dateString: String,
    val totalPromptTokens: Int,
    val totalCandidateTokens: Int,
    val totalTokens: Int,
    val totalCostUsd: Double,
    val requestCount: Int
)

data class MatterTokenAggregate(
    val matterCode: String,
    val totalTokens: Int,
    val totalCostUsd: Double,
    val requestCount: Int
)
