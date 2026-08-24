package com.example.data.billing

import android.util.Log
import com.example.data.local.ContextLogDao
import com.example.data.local.DailyTokenAggregate
import com.example.data.local.MatterTokenAggregate
import com.example.data.local.TokenUsageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.Response
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Service that extracts token usage metrics from Gemini API response bodies and HTTP headers,
 * calculates cost estimates based on production pricing, and records aggregates for billing.
 */
class TokenBillingService(
    private val dao: ContextLogDao? = null
) {
    companion object {
        private const val TAG = "TokenBillingService"

        // Gemini 3.5 Flash pricing tier: $0.075 per 1M input tokens, $0.30 per 1M output tokens
        const val COST_PER_MILLION_PROMPT_TOKENS = 0.075
        const val COST_PER_MILLION_CANDIDATE_TOKENS = 0.30

        // Text Embedding 004 pricing tier: $0.025 per 1M tokens
        const val COST_PER_MILLION_EMBEDDING_TOKENS = 0.025

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Extracts token usage from Gemini JSON response (`usageMetadata`) or headers,
     * computes estimated cost in USD, and saves the record to the local database.
     */
    fun recordGeminiUsage(
        responseJson: JSONObject?,
        httpResponse: Response? = null,
        endpoint: String = "gemini-3.5-flash:generateContent",
        modelName: String = "gemini-3.5-flash",
        matterCode: String = "GENERAL",
        requestType: String = "NOTE_PARSE",
        fallbackPromptText: String = "",
        fallbackResponseText: String = ""
    ) {
        val now = System.currentTimeMillis()
        val dateString = dateFormat.format(Date(now))

        var promptTokens = 0
        var candidatesTokens = 0
        var totalTokens = 0

        // 1. Try extracting usageMetadata from the JSON body
        val usageMetadata = responseJson?.optJSONObject("usageMetadata")
        if (usageMetadata != null) {
            promptTokens = usageMetadata.optInt("promptTokenCount", 0)
            candidatesTokens = usageMetadata.optInt("candidatesTokenCount", 0)
            totalTokens = usageMetadata.optInt("totalTokenCount", promptTokens + candidatesTokens)
        }

        // 2. Check HTTP headers for quota/token telemetry if present
        if (httpResponse != null && totalTokens == 0) {
            val headerPrompt = httpResponse.header("x-goog-prompt-tokens")?.toIntOrNull()
            val headerCandidate = httpResponse.header("x-goog-candidates-tokens")?.toIntOrNull()
            if (headerPrompt != null && headerCandidate != null) {
                promptTokens = headerPrompt
                candidatesTokens = headerCandidate
                totalTokens = promptTokens + candidatesTokens
            }
        }

        // 3. Fallback calculation using standard ~4 chars/token heuristic
        if (totalTokens == 0) {
            promptTokens = if (fallbackPromptText.isNotBlank()) (fallbackPromptText.length / 4).coerceAtLeast(10) else 45
            candidatesTokens = if (fallbackResponseText.isNotBlank()) (fallbackResponseText.length / 4).coerceAtLeast(5) else 30
            totalTokens = promptTokens + candidatesTokens
        }

        val estimatedCostUsd = calculateEstimatedCost(promptTokens, candidatesTokens, modelName)

        val metric = TokenUsageEntity(
            timestamp = now,
            dateString = dateString,
            promptTokens = promptTokens,
            candidatesTokens = candidatesTokens,
            totalTokens = totalTokens,
            estimatedCostUsd = estimatedCostUsd,
            endpoint = endpoint,
            modelName = modelName,
            matterCode = matterCode,
            requestType = requestType
        )

        Log.d(TAG, "Recorded token metric: $totalTokens tokens ($promptTokens in, $candidatesTokens out) -> $${String.format(Locale.US, "%.5f", estimatedCostUsd)} for matter $matterCode")

        if (dao != null) {
            scope.launch {
                try {
                    dao.insertTokenUsage(metric)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to persist token usage metric", e)
                }
            }
        }
    }

    /**
     * Records token usage for embedding generation (text-embedding-004)
     */
    fun recordEmbeddingUsage(
        text: String,
        matterCode: String = "GENERAL",
        modelName: String = "text-embedding-004"
    ) {
        val now = System.currentTimeMillis()
        val dateString = dateFormat.format(Date(now))
        val tokenCount = (text.length / 4).coerceAtLeast(1)
        val estimatedCostUsd = (tokenCount.toDouble() / 1_000_000.0) * COST_PER_MILLION_EMBEDDING_TOKENS

        val metric = TokenUsageEntity(
            timestamp = now,
            dateString = dateString,
            promptTokens = tokenCount,
            candidatesTokens = 0,
            totalTokens = tokenCount,
            estimatedCostUsd = estimatedCostUsd,
            endpoint = "text-embedding-004:embedContent",
            modelName = modelName,
            matterCode = matterCode,
            requestType = "VECTOR_EMBEDDING"
        )

        if (dao != null) {
            scope.launch {
                try {
                    dao.insertTokenUsage(metric)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to persist embedding token usage", e)
                }
            }
        }
    }

    /**
     * Computes the estimated USD cost based on token counts and model tier.
     */
    fun calculateEstimatedCost(promptTokens: Int, candidatesTokens: Int, modelName: String): Double {
        return when {
            modelName.contains("embedding", ignoreCase = true) -> {
                ((promptTokens + candidatesTokens).toDouble() / 1_000_000.0) * COST_PER_MILLION_EMBEDDING_TOKENS
            }
            else -> {
                val promptCost = (promptTokens.toDouble() / 1_000_000.0) * COST_PER_MILLION_PROMPT_TOKENS
                val candidateCost = (candidatesTokens.toDouble() / 1_000_000.0) * COST_PER_MILLION_CANDIDATE_TOKENS
                promptCost + candidateCost
            }
        }
    }

    /**
     * Reactive stream of daily aggregates for charting & reporting.
     */
    fun getDailyAggregatesFlow(): Flow<List<DailyTokenAggregate>>? {
        if (dao == null) return null
        return dao.getAllTokenUsage().map { list ->
            list.groupBy { it.dateString }
                .map { (date, items) ->
                    DailyTokenAggregate(
                        dateString = date,
                        totalPromptTokens = items.sumOf { it.promptTokens },
                        totalCandidateTokens = items.sumOf { it.candidatesTokens },
                        totalTokens = items.sumOf { it.totalTokens },
                        totalCostUsd = items.sumOf { it.estimatedCostUsd },
                        requestCount = items.size
                    )
                }
                .sortedBy { it.dateString }
        }
    }

    /**
     * Reactive stream of matter token distribution aggregates.
     */
    fun getMatterAggregatesFlow(): Flow<List<MatterTokenAggregate>>? {
        if (dao == null) return null
        return dao.getAllTokenUsage().map { list ->
            list.groupBy { it.matterCode }
                .map { (matter, items) ->
                    MatterTokenAggregate(
                        matterCode = matter,
                        totalTokens = items.sumOf { it.totalTokens },
                        totalCostUsd = items.sumOf { it.estimatedCostUsd },
                        requestCount = items.size
                    )
                }
                .sortedByDescending { it.totalTokens }
        }
    }
}
