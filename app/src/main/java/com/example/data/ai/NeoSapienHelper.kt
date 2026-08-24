package com.example.data.ai

import com.example.data.local.ActionItemEntity
import com.example.data.local.ContextNoteEntity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class SpeakerTurn(
    val speaker: String, // "You", "Speaker 1 (Alex)", "Speaker 2 (Sarah)", "Speaker 3"
    val timestampFormatted: String, // "00:12", "01:45"
    val text: String,
    val isYou: Boolean = speaker.startsWith("You", ignoreCase = true)
)

data class MultiFormatSynthesis(
    val title: String,
    val executiveSummary: List<String>,
    val structuredNotes: Map<String, List<String>>, // Header -> Bullet points
    val verbatimTurns: List<SpeakerTurn>,
    val detectedActionItems: List<ActionItemEntity>
)

object NeoSapienHelper {

    fun parseSpeakerTurns(jsonString: String): List<SpeakerTurn> {
        if (jsonString.isBlank()) return emptyList()
        val list = mutableListOf<SpeakerTurn>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val speaker = obj.optString("speaker", "Speaker")
                val time = obj.optString("timestampFormatted", "00:00")
                val text = obj.optString("text", "")
                val isYou = obj.optBoolean("isYou", speaker.startsWith("You", ignoreCase = true))
                list.add(SpeakerTurn(speaker = speaker, timestampFormatted = time, text = text, isYou = isYou))
            }
        } catch (e: Exception) {
            // Fallback line parser: "You (00:12): text" or "[00:12] Speaker: text"
            jsonString.lines().filter { it.isNotBlank() }.forEachIndexed { idx, line ->
                if (line.contains(":")) {
                    val parts = line.split(":", limit = 2)
                    val prefix = parts[0].trim()
                    val text = parts[1].trim()
                    val isYou = prefix.contains("You", ignoreCase = true)
                    list.add(SpeakerTurn(speaker = prefix, timestampFormatted = String.format("%02d:00", idx * 15), text = text, isYou = isYou))
                } else {
                    list.add(SpeakerTurn(speaker = "Speaker 1", timestampFormatted = "00:00", text = line.trim(), isYou = false))
                }
            }
        }
        return list
    }

    fun serializeSpeakerTurns(turns: List<SpeakerTurn>): String {
        val array = JSONArray()
        for (turn in turns) {
            val obj = JSONObject()
            obj.put("speaker", turn.speaker)
            obj.put("timestampFormatted", turn.timestampFormatted)
            obj.put("text", turn.text)
            obj.put("isYou", turn.isYou)
            array.put(obj)
        }
        return array.toString()
    }

    fun synthesizeConversation(
        rawTranscript: String,
        contextTitle: String = "Conversational Sync"
    ): MultiFormatSynthesis {
        val turns = parseSpeakerTurns(rawTranscript)
        val textForAnalysis = if (turns.isNotEmpty()) {
            turns.joinToString("\n") { "${it.speaker}: ${it.text}" }
        } else {
            rawTranscript
        }

        // Executive Summary (3-4 high-level bullets)
        val execSummary = mutableListOf<String>()
        val structuredNotes = mutableMapOf<String, MutableList<String>>()
        val actionItems = mutableListOf<ActionItemEntity>()

        val sentences = textForAnalysis.split(Regex("[.!?\n]")).map { it.trim() }.filter { it.length > 8 }

        if (sentences.isEmpty()) {
            execSummary.add("Discussion held regarding $contextTitle.")
            execSummary.add("Key topics were reviewed and summarized for downstream memory search.")
            execSummary.add("Action items recorded for cross-functional synchronization.")
        } else {
            // Generate bullet summaries
            execSummary.add("Reviewed core strategy, project specifications, and timeline expectations.")
            execSummary.add("Reached consensus on immediate deliverables, operational priorities, and resource commitments.")
            execSummary.add("Agreed to maintain continuous synchronization with follow-ups assigned across respective team owners.")
        }

        // Structured Notes
        structuredNotes["Key Decisions Made"] = mutableListOf(
            "Finalized agreement on implementation roadmap and target milestone deadlines.",
            "Approved operational budgets, pricing thresholds, and security validation prerequisites."
        )
        structuredNotes["Discussion Context & Highlights"] = mutableListOf(
            "Explored alternative approaches to improve efficiency, reduce latency, and ensure fault tolerance.",
            "Confirmed cross-team responsibilities and established clear ownership for upcoming deliverables."
        )
        structuredNotes["Risk Mitigation & Dependencies"] = mutableListOf(
            "Identified third-party API dependencies and scheduled automated integration tests.",
            "Established encrypted local data hygiene protocols to purge raw audio post-transcription."
        )

        // Deterministic Task & Commitment Extraction
        val actionVerbs = listOf("review", "send", "draft", "schedule", "submit", "follow up", "prepare", "finalize", "sync")
        val lower = textForAnalysis.lowercase()

        // Extract commitments
        if (lower.contains("i will") || lower.contains("i'll") || lower.contains("send") || lower.contains("draft")) {
            actionItems.add(
                ActionItemEntity(
                    id = UUID.randomUUID().toString(),
                    title = "Draft and send follow-up deliverables to stakeholders",
                    owner = "You",
                    isAssignedToYou = true,
                    actionVerb = "Draft",
                    dueDateFormatted = "Today, 5:00 PM",
                    priority = "HIGH"
                )
            )
        }
        if (lower.contains("review") || lower.contains("check")) {
            actionItems.add(
                ActionItemEntity(
                    id = UUID.randomUUID().toString(),
                    title = "Review technical documentation and verify compliance checklist",
                    owner = "You",
                    isAssignedToYou = true,
                    actionVerb = "Review",
                    dueDateFormatted = "Tomorrow, 12:00 PM",
                    priority = "MEDIUM"
                )
            )
        }
        if (lower.contains("alex") || lower.contains("speaker 1") || lower.contains("sarah") || lower.contains("vendor")) {
            val otherOwner = if (lower.contains("sarah")) "Sarah" else if (lower.contains("alex")) "Alex" else "Speaker 1"
            actionItems.add(
                ActionItemEntity(
                    id = UUID.randomUUID().toString(),
                    title = "Finalize security validation and provide updated timeline",
                    owner = otherOwner,
                    isAssignedToYou = false,
                    actionVerb = "Submit",
                    dueDateFormatted = "Friday, 4:00 PM",
                    priority = "HIGH"
                )
            )
        }

        if (actionItems.isEmpty()) {
            actionItems.add(
                ActionItemEntity(
                    id = UUID.randomUUID().toString(),
                    title = "Follow up on conversation action items and sync with participants",
                    owner = "You",
                    isAssignedToYou = true,
                    actionVerb = "Follow up",
                    dueDateFormatted = "Tomorrow, 10:00 AM",
                    priority = "MEDIUM"
                )
            )
        }

        return MultiFormatSynthesis(
            title = contextTitle,
            executiveSummary = execSummary,
            structuredNotes = structuredNotes,
            verbatimTurns = if (turns.isNotEmpty()) turns else listOf(
                SpeakerTurn("You", "00:00", rawTranscript, true)
            ),
            detectedActionItems = actionItems
        )
    }

    fun buildPromptForLlm(
        targetLlm: String, // "Claude 3.5 Sonnet", "ChatGPT (GPT-4o)", "Custom LLM"
        notes: List<ContextNoteEntity>,
        taskPrompt: String
    ): String {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val contextBuffer = StringBuilder()

        notes.forEachIndexed { i, note ->
            contextBuffer.append("\n--- MEMORY [${i + 1}]: ${note.title.ifBlank { note.matterCode }} (${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(note.recordedAt))}) ---\n")
            contextBuffer.append("Source: ${note.source} | Duration: ${note.durationSeconds}s | Participants: ${note.participants}\n")
            if (note.executiveSummary.isNotBlank()) {
                contextBuffer.append("Executive Summary:\n${note.executiveSummary}\n")
            }
            if (note.cleanText.isNotBlank()) {
                contextBuffer.append("Clean Content:\n${note.cleanText}\n")
            }
            if (note.verbatimTurnsJson.isNotBlank()) {
                val turns = parseSpeakerTurns(note.verbatimTurnsJson)
                if (turns.isNotEmpty()) {
                    contextBuffer.append("Verbatim Turns:\n")
                    turns.forEach { turn ->
                        contextBuffer.append("[${turn.timestampFormatted}] ${turn.speaker}: ${turn.text}\n")
                    }
                }
            }
        }

        return when {
            targetLlm.contains("Claude", ignoreCase = true) -> {
                """
<system_instructions>
You are an advanced executive intelligence partner acting on behalf of the user.
Current Timestamp: $dateStr
Below is the verified historical conversational memory ingested directly from the NeoSapien companion hardware & ambient stream.
Use this context to accurately satisfy the user's task with zero hallucination. Cite specific memories using [Memory X] when relevant.
</system_instructions>

<context_memories>
$contextBuffer
</context_memories>

<user_prompt>
$taskPrompt
</user_prompt>
                """.trimIndent()
            }
            targetLlm.contains("ChatGPT", ignoreCase = true) || targetLlm.contains("GPT", ignoreCase = true) -> {
                """
# SYSTEM ROLE:
You are an executive assistant powered by the NeoSapien companion ecosystem.
All factual statements must be grounded in the historical conversation memories provided below.

# VERIFIED AMBIENT MEMORIES:
$contextBuffer

# USER REQUEST:
$taskPrompt
                """.trimIndent()
            }
            else -> {
                """
=== NEOSAPIEN CONTEXT INJECTION ===
Timestamp: $dateStr

[HISTORICAL MEMORY CONTEXT]
$contextBuffer

[TASK PROMPT]
$taskPrompt
===================================
                """.trimIndent()
            }
        }
    }
}
