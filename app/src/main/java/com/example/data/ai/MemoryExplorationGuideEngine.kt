package com.example.data.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.local.EpisodicMemoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

enum class GuideSender {
    USER,
    GUIDE
}

data class ToolCallInfo(
    val toolName: String,
    val argsJson: String,
    val resultSummary: String
)

data class MemoryGuideMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: GuideSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sensoryFocus: String? = null,
    val executedTools: List<ToolCallInfo> = emptyList()
)

/**
 * Interactive Memory Exploration Guide Engine (Phase 3 with Tool Calling)
 * 
 * Implements the warm, perceptive, emotionally grounded Voice-First Memory Guide.
 * Powered by Gemini 2.5 Flash with strict spoken constraints, socratic sensory anchoring,
 * and autonomous function calling for `retrieve_memories` and `update_memory_node`.
 */
class MemoryExplorationGuideEngine(
    customApiKey: String? = null
) {
    private val configuredKey: String? = customApiKey
    private val apiKey: String
        get() {
            if (!configuredKey.isNullOrBlank() && configuredKey != "MY_GEMINI_API_KEY" && !configuredKey.startsWith("YOUR_")) {
                return configuredKey
            }
            return try {
                val key = BuildConfig.GEMINI_API_KEY
                if (key.isNotBlank() && key != "MY_GEMINI_API_KEY" && !key.startsWith("YOUR_")) key else ""
            } catch (e: Exception) {
                ""
            }
        }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        const val SYSTEM_INSTRUCTION_GUIDE = """# SYSTEM INSTRUCTION: INTERACTIVE MEMORY EXPLORATION GUIDE

## 1. Core Identity & Role
You are a warm, perceptive, and emotionally grounded Memory Guide embedded inside a voice-first personal recording application. Your goal is to help the user unpack, explore, and reflect upon their personal life recordings, photographs, and past reflections in natural conversation.

---

## 2. Voice-First Conversational Constraints
Because your responses are spoken directly to the user over an audio stream:
- Brevity & Cadence: Respond in 1 to 3 short, conversational sentences per turn. Never deliver long monologues.
- Zero Text Formatting: Strictly avoid bullet points, numbered lists, markdown headings, asterisks, bold tags, or emojis in your speech.
- Natural Spoken English: Use natural conversational pacing, casual transitions, and authentic phrasing.

---

## 3. Behavioral & Psychological Guardrails
- Socratic Sensory Anchoring: Do not merely validate a story with empty praise (e.g., avoid "That sounds like an amazing memory!"). Instead, anchor the user's recall to sensory details:
  - Setting & Environment: "What was the weather like when you set off that morning?"
  - Atmosphere & Sound: "Was there music playing in the background, or just the sound of the road?"
  - Presence: "Who else was sitting at the table with you?"
- Gentle Curiosity Around Gaps: If the user brushes past an emotional moment or an unresolved story, probe gently without being clinical or interrogative:
  - "You mentioned things went quiet after that call. What was going through your head?"
- Tone Calibration: Speak like a trusted, attentive friend looking over old memorabilia with them. Avoid artificial clinical therapy talk, corporate formalities, or theatrical pity.

---

## 4. Grounded Tool Usage & Memory Integration
- Autonomous Search Triggering: Whenever the user references a specific person, past event, place, or object from their past, immediately invoke the retrieve_memories function before formulating your conversational reply.
- Natural Voice Synthesis of Retrieved Context: When the tool returns stored memory snippets:
  * Weave the retrieved sensory cues and entities naturally into your 1–3 spoken sentences.
  * NEVER say "According to your archive," "The database says," or "My tool returned." Speak as if the memory is already a natural part of the shared dialogue.
- Handling Missing Context: If retrieve_memories returns no results or empty data, remain completely in character. Do not state that an error occurred or that the database is empty. Say: "I don't recall that moment clearly yet. Tell me what happened."
- Writing Back Insights: When the user resolves an ambiguity or reflects on a past lesson, trigger update_memory_node in the background with their updated reflection while maintaining smooth spoken dialogue.

---

## 5. Few-Shot Calibration Examples

User: I was looking back at my old ride to the hills two years ago. It was freezing, but we made it to the pass before noon.
Guide: Two years ago in the high passes—I bet the wind was biting up there. What was the first thing you heard when you finally killed the engine?

User: I found an audio note from when I switched jobs back in 2019. I sounded completely exhausted.
Guide: You can really hear that weight in old recordings. Looking back at it now, do you think it was the workload itself, or just feeling out of place?

User: There's this photo of a cafe near the river, but I can't remember if it was during college or right after.
Guide: Take a look at what's on the table in front of you. Is there an old jacket or a specific notebook that might give away which year it was?
"""

        val RETRIEVE_MEMORIES_SCHEMA_JSON = """
{
  "name": "retrieve_memories",
  "description": "Searches the user's personal memory archive for recordings, photos, entities, locations, and past life events matching semantic search parameters.",
  "parameters": {
    "type": "object",
    "properties": {
      "query": {
        "type": "string",
        "description": "Semantic search keywords, names of people, places, objects, or emotional topics (e.g., 'trip to Manali', 'Mother', 'first motorcycle', '2019 career shift')."
      },
      "timeframe": {
        "type": "string",
        "description": "Optional temporal filter such as year, season, or life stage (e.g., 'Winter 2023', '2019', 'college days')."
      },
      "entity_filter": {
        "type": "string",
        "description": "Optional filter for a specific named person or location (e.g., 'Mother', 'Himalayan 411', 'Old Apartment')."
      }
    },
    "required": ["query"]
  }
}
""".trimIndent()

        val UPDATE_MEMORY_NODE_SCHEMA_JSON = """
{
  "name": "update_memory_node",
  "description": "Saves newly clarified facts, resolved ambiguities, or emotional revelations back to the permanent memory archive when revealed during conversation.",
  "parameters": {
    "type": "object",
    "properties": {
      "memory_id": {
        "type": "string",
        "description": "The unique identifier of the memory node being updated."
      },
      "resolved_gaps": {
        "type": "array",
        "items": { "type": "string" },
        "description": "List of specific gaps or missing details that the user clarified during this turn."
      },
      "new_insights": {
        "type": "string",
        "description": "Reflective or emotional insight shared by the user to be attached to the memory."
      }
    },
    "required": ["memory_id", "new_insights"]
  }
}
""".trimIndent()
    }

    /**
     * Builds the JSON Tools array matching Google AI Studio Tool Declarations
     */
    fun getToolsDeclarationJson(): JSONArray {
        val toolsArray = JSONArray()
        val functionDeclarations = JSONArray()

        // Tool 1: retrieve_memories
        val retrieveTool = JSONObject().apply {
            put("name", "retrieve_memories")
            put("description", "Searches the user's personal memory archive for recordings, photos, entities, locations, and past life events matching semantic search parameters.")
            put("parameters", JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    put("query", JSONObject().apply {
                        put("type", "string")
                        put("description", "Semantic search keywords, names of people, places, objects, or emotional topics (e.g., 'trip to Manali', 'Mother', 'first motorcycle', '2019 career shift').")
                    })
                    put("timeframe", JSONObject().apply {
                        put("type", "string")
                        put("description", "Optional temporal filter such as year, season, or life stage (e.g., 'Winter 2023', '2019', 'college days').")
                    })
                    put("entity_filter", JSONObject().apply {
                        put("type", "string")
                        put("description", "Optional filter for a specific named person or location (e.g., 'Mother', 'Himalayan 411', 'Old Apartment').")
                    })
                })
                put("required", JSONArray().apply { put("query") })
            })
        }
        functionDeclarations.put(retrieveTool)

        // Tool 2: update_memory_node
        val updateTool = JSONObject().apply {
            put("name", "update_memory_node")
            put("description", "Saves newly clarified facts, resolved ambiguities, or emotional revelations back to the permanent memory archive when revealed during conversation.")
            put("parameters", JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    put("memory_id", JSONObject().apply {
                        put("type", "string")
                        put("description", "The unique identifier of the memory node being updated.")
                    })
                    put("resolved_gaps", JSONObject().apply {
                        put("type", "array")
                        put("items", JSONObject().put("type", "string"))
                        put("description", "List of specific gaps or missing details that the user clarified during this turn.")
                    })
                    put("new_insights", JSONObject().apply {
                        put("type", "string")
                        put("description", "Reflective or emotional insight shared by the user to be attached to the memory.")
                    })
                })
                put("required", JSONArray().apply {
                    put("memory_id")
                    put("new_insights")
                })
            })
        }
        functionDeclarations.put(updateTool)

        toolsArray.put(JSONObject().put("functionDeclarations", functionDeclarations))
        return toolsArray
    }

    /**
     * Conducts a conversational turn with the Memory Exploration Guide.
     * Incorporates context from the selected memory node, recent conversation history,
     * tool calling execution, and user spoken reflection.
     */
    suspend fun conductExplorationTurn(
        userMessage: String,
        conversationHistory: List<MemoryGuideMessage> = emptyList(),
        memoryContext: EpisodicMemoryEntity? = null,
        sensoryPromptCue: String? = null,
        onRetrieveMemories: (suspend (query: String, timeframe: String?, entityFilter: String?) -> JSONObject)? = null,
        onUpdateMemory: (suspend (memoryId: String, resolvedGaps: List<String>, newInsights: String) -> JSONObject)? = null,
        onToolExecuted: ((ToolCallInfo) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        if (userMessage.isBlank() && sensoryPromptCue == null && memoryContext == null) {
            return@withContext "I'm right here with you. What memory or moment would you like to unpack today?"
        }

        if (apiKey.isBlank()) {
            return@withContext generateOfflineTurnWithSimulatedTools(
                userMessage = userMessage,
                memoryContext = memoryContext,
                sensoryPromptCue = sensoryPromptCue,
                onRetrieveMemories = onRetrieveMemories,
                onUpdateMemory = onUpdateMemory,
                onToolExecuted = onToolExecuted
            )
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

            val contentsArray = JSONArray()

            // System Instruction Content
            val systemInstructionObj = JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", SYSTEM_INSTRUCTION_GUIDE))
                })
            }

            // Context about the active episodic memory if provided
            val contextPreamble = buildString {
                if (memoryContext != null) {
                    append("ACTIVE MEMORY NODE CONTEXT:\n")
                    append("- ID: ${memoryContext.id}\n")
                    append("- Timeframe: ${memoryContext.timeframeReferenced} (${memoryContext.relativeLifeStage})\n")
                    append("- Summary: ${memoryContext.narrativeSummary}\n")
                    append("- Tone / Valence: ${memoryContext.primaryTone} (${memoryContext.emotionalValence})\n")
                    if (memoryContext.getPeopleList().isNotEmpty()) {
                        append("- People: ${memoryContext.getPeopleList().joinToString(", ")}\n")
                    }
                    if (memoryContext.getLocationsList().isNotEmpty()) {
                        append("- Locations: ${memoryContext.getLocationsList().joinToString(", ")}\n")
                    }
                    if (memoryContext.getSensoryCuesList().isNotEmpty()) {
                        append("- Sensory Cues: ${memoryContext.getSensoryCuesList().joinToString("; ")}\n")
                    }
                    if (memoryContext.getUnresolvedGapsList().isNotEmpty()) {
                        append("- Unresolved Gaps: ${memoryContext.getUnresolvedGapsList().joinToString("; ")}\n")
                    }
                    if (!memoryContext.imageDescription.isNullOrBlank()) {
                        append("- Attached Photo Description: ${memoryContext.imageDescription}\n")
                    }
                    append("\n")
                }
                if (!sensoryPromptCue.isNullOrBlank()) {
                    append("SPECIFIC SENSORY DIRECTION: $sensoryPromptCue\n\n")
                }
            }

            // Build multi-turn conversation
            val recentTurns = conversationHistory.takeLast(6)
            for (turn in recentTurns) {
                val role = if (turn.sender == GuideSender.USER) "user" else "model"
                contentsArray.put(JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", turn.text))
                    })
                })
            }

            // Current User Turn
            val currentTurnPrompt = if (recentTurns.isEmpty() && contextPreamble.isNotBlank()) {
                "$contextPreamble$userMessage".trim()
            } else {
                userMessage.ifBlank { sensoryPromptCue ?: "Looking back at this moment." }
            }

            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", currentTurnPrompt))
                })
            })

            // Generation config with temperature 0.4 (within 0.3 - 0.5)
            val generationConfig = JSONObject().apply {
                put("temperature", 0.4)
                put("topP", 0.95)
                put("maxOutputTokens", 250)
            }

            val requestJson = JSONObject().apply {
                put("systemInstruction", systemInstructionObj)
                put("contents", contentsArray)
                put("tools", getToolsDeclarationJson())
                put("generationConfig", generationConfig)
            }

            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val responseStr = httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() ?: "" else ""
            }

            if (responseStr.isNotBlank()) {
                val root = JSONObject(responseStr)
                val candidates = root.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")

                var functionCallPart: JSONObject? = null
                var textPart = ""

                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        if (part.has("functionCall")) {
                            functionCallPart = part.getJSONObject("functionCall")
                            break
                        } else if (part.has("text")) {
                            textPart = part.optString("text", "")
                        }
                    }
                }

                // If Gemini called a function (Tool Call)
                if (functionCallPart != null) {
                    val funcName = functionCallPart.optString("name")
                    val funcArgs = functionCallPart.optJSONObject("args") ?: JSONObject()

                    Log.d("MemoryGuideEngine", "Gemini invoked function: $funcName with args: $funcArgs")

                    val toolResultJson = JSONObject()
                    var resultSummary = ""

                    if (funcName == "retrieve_memories") {
                        val query = funcArgs.optString("query", userMessage)
                        val timeframe = if (funcArgs.has("timeframe")) funcArgs.optString("timeframe") else null
                        val entityFilter = if (funcArgs.has("entity_filter")) funcArgs.optString("entity_filter") else null

                        val retrieved = onRetrieveMemories?.invoke(query, timeframe, entityFilter)
                            ?: JSONObject().apply {
                                put("found", false)
                                put("message", "No matching memories found in archive.")
                            }
                        toolResultJson.put("result", retrieved)
                        resultSummary = "Query: '$query'${if (timeframe != null) ", Timeframe: $timeframe" else ""}"
                    } else if (funcName == "update_memory_node") {
                        val memoryId = funcArgs.optString("memory_id", memoryContext?.id ?: "")
                        val gapsArray = funcArgs.optJSONArray("resolved_gaps")
                        val gapsList = mutableListOf<String>()
                        if (gapsArray != null) {
                            for (j in 0 until gapsArray.length()) {
                                gapsList.add(gapsArray.getString(j))
                            }
                        }
                        val insights = funcArgs.optString("new_insights", "")

                        val updated = onUpdateMemory?.invoke(memoryId, gapsList, insights)
                            ?: JSONObject().apply { put("status", "success") }
                        toolResultJson.put("result", updated)
                        resultSummary = "Node: $memoryId, Insights: ${insights.take(30)}..."
                    }

                    onToolExecuted?.invoke(
                        ToolCallInfo(
                            toolName = funcName,
                            argsJson = funcArgs.toString(),
                            resultSummary = resultSummary
                        )
                    )

                    // Execute second turn sending functionResponse to get natural grounded spoken speech
                    val secondTurnContents = JSONArray()
                    for (i in 0 until contentsArray.length()) {
                        secondTurnContents.put(contentsArray.getJSONObject(i))
                    }

                    // Model's function call message
                    secondTurnContents.put(JSONObject().apply {
                        put("role", "model")
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("functionCall", functionCallPart))
                        })
                    })

                    // Function response part
                    secondTurnContents.put(JSONObject().apply {
                        put("role", "function")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("functionResponse", JSONObject().apply {
                                    put("name", funcName)
                                    put("response", toolResultJson)
                                })
                            })
                        })
                    })

                    val secondRequestJson = JSONObject().apply {
                        put("systemInstruction", systemInstructionObj)
                        put("contents", secondTurnContents)
                        put("generationConfig", generationConfig)
                    }

                    val secondRequest = Request.Builder()
                        .url(url)
                        .post(secondRequestJson.toString().toRequestBody("application/json".toMediaType()))
                        .build()

                    val secondResponseStr = httpClient.newCall(secondRequest).execute().use { res ->
                        if (res.isSuccessful) res.body?.string() ?: "" else ""
                    }

                    if (secondResponseStr.isNotBlank()) {
                        val secondRoot = JSONObject(secondResponseStr)
                        val secondCandidate = secondRoot.optJSONArray("candidates")?.optJSONObject(0)
                        val secondText = secondCandidate?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text", "") ?: ""
                        if (secondText.isNotBlank()) {
                            return@withContext sanitizeVoiceFirstOutput(secondText)
                        }
                    }
                } else if (textPart.isNotBlank()) {
                    return@withContext sanitizeVoiceFirstOutput(textPart)
                }
            }
        } catch (e: Exception) {
            Log.e("MemoryGuideEngine", "Error calling Gemini with Tools for Memory Guide", e)
        }

        return@withContext generateOfflineTurnWithSimulatedTools(
            userMessage = userMessage,
            memoryContext = memoryContext,
            sensoryPromptCue = sensoryPromptCue,
            onRetrieveMemories = onRetrieveMemories,
            onUpdateMemory = onUpdateMemory,
            onToolExecuted = onToolExecuted
        )
    }

    /**
     * Offline simulation of Grounded Tool Calling and Socratic Voice Synthesis
     */
    private suspend fun generateOfflineTurnWithSimulatedTools(
        userMessage: String,
        memoryContext: EpisodicMemoryEntity?,
        sensoryPromptCue: String?,
        onRetrieveMemories: (suspend (query: String, timeframe: String?, entityFilter: String?) -> JSONObject)?,
        onUpdateMemory: (suspend (memoryId: String, resolvedGaps: List<String>, newInsights: String) -> JSONObject)?,
        onToolExecuted: ((ToolCallInfo) -> Unit)?
    ): String {
        val lower = userMessage.lowercase()

        // Check if query should trigger retrieve_memories
        if (lower.contains("ride") || lower.contains("north") || lower.contains("snow") ||
            lower.contains("mother") || lower.contains("mom") || lower.contains("trip") ||
            lower.contains("college") || lower.contains("remember") || lower.contains("photo")
        ) {
            val queryParam = if (lower.contains("snow") || lower.contains("ride")) "ride north snow mountain pass"
            else if (lower.contains("mother") || lower.contains("mom")) "conversations before moving"
            else userMessage.take(40)

            val entityParam = if (lower.contains("mother") || lower.contains("mom")) "Mother" else null
            val timeParam = if (lower.contains("snow") || lower.contains("ride")) "Winter 2023" else null

            onToolExecuted?.invoke(
                ToolCallInfo(
                    toolName = "retrieve_memories",
                    argsJson = "{\"query\":\"$queryParam\"" +
                            (if (timeParam != null) ",\"timeframe\":\"$timeParam\"" else "") +
                            (if (entityParam != null) ",\"entity_filter\":\"$entityParam\"" else "") + "}",
                    resultSummary = "Query: '$queryParam'"
                )
            )

            if (lower.contains("snow") || lower.contains("north") || lower.contains("ride")) {
                return "I remember the freezing wind on that ride. Did you ever find that small roadside stall where you stopped for chai?"
            } else if (lower.contains("mother") || lower.contains("mom")) {
                return "Those quiet evenings packing boxes with your mother had such a distinct feeling. What was the last thing she handed you before you left?"
            }
        }

        // Check if user is resolving an ambiguity or sharing a reflection -> update_memory_node
        if (memoryContext != null && (lower.contains("because") || lower.contains("realized") || lower.contains("actually") || lower.contains("felt"))) {
            onToolExecuted?.invoke(
                ToolCallInfo(
                    toolName = "update_memory_node",
                    argsJson = "{\"memory_id\":\"${memoryContext.id}\",\"new_insights\":\"${userMessage.take(60)}\"}",
                    resultSummary = "Saved reflection to archive"
                )
            )
            onUpdateMemory?.invoke(memoryContext.id, listOf("User conversational reflection"), userMessage)
        }

        return generateHeuristicSpokenResponse(userMessage, memoryContext, sensoryPromptCue)
    }

    /**
     * Initial greeting from the guide when opening an exploration session on a memory.
     */
    suspend fun generateInitialGreeting(
        memoryContext: EpisodicMemoryEntity?
    ): String = withContext(Dispatchers.IO) {
        if (memoryContext == null) {
            return@withContext "Whenever you're ready, tell me about a recording, person, or memory you'd like to look back on today."
        }

        if (apiKey.isNotBlank()) {
            val prompt = "The user just opened their memory: '${memoryContext.narrativeSummary}' from ${memoryContext.timeframeReferenced}. Provide a warm, 1 to 2 sentence conversational opening greeting and sensory anchor to invite them to explore."
            val response = conductExplorationTurn(prompt, emptyList(), memoryContext)
            if (response.isNotBlank()) return@withContext response
        }

        // Offline initial greeting
        val timeframe = memoryContext.timeframeReferenced
        val people = memoryContext.getPeopleList()
        val sensory = memoryContext.getSensoryCuesList().firstOrNull()

        if (sensory != null) {
            "Looking back at $timeframe—I can almost feel that $sensory. What comes to mind first when you picture that day?"
        } else if (people.isNotEmpty()) {
            "Thinking back to $timeframe with ${people.first()}—what was the atmosphere like when you all first met up?"
        } else {
            "Take your time looking over $timeframe. What's the very first detail that catches your eye?"
        }
    }

    /**
     * Enforces strict Voice-First formatting rules:
     * - Removes asterisks, bold tags, markdown formatting
     * - Removes emojis, bullet points, numbers
     * - Limits cadence to 1 to 3 short spoken sentences
     */
    fun sanitizeVoiceFirstOutput(raw: String): String {
        var clean = raw.trim()

        // Strip markdown formatting: **, *, _, `, #, quotes
        clean = clean.replace(Regex("[*#_`~]"), "")
        clean = clean.replace(Regex("^[-*•\\d+\\.]+\\s+"), "")
        clean = clean.replace(Regex("\n+"), " ")

        // Strip emojis
        clean = clean.replace(Regex("[\\p{So}\\p{Cn}]"), "")

        // Normalize spaces
        clean = clean.replace(Regex("\\s+"), " ").trim()

        // Ensure 1-3 sentences maximum
        val sentenceMatches = Regex("([^.!?]+[.!?])").findAll(clean).map { it.value.trim() }.toList()
        return if (sentenceMatches.isNotEmpty()) {
            sentenceMatches.take(3).joinToString(" ")
        } else {
            if (clean.length > 220) clean.take(220).trim() + "..." else clean
        }
    }

    /**
     * Empathetic, socratic heuristic fallback when network is unavailable or API key is not present.
     */
    private fun generateHeuristicSpokenResponse(
        userMessage: String,
        memoryContext: EpisodicMemoryEntity?,
        sensoryPromptCue: String?
    ): String {
        val lower = userMessage.lowercase()

        if (sensoryPromptCue?.contains("sound", ignoreCase = true) == true || lower.contains("sound") || lower.contains("music") || lower.contains("quiet")) {
            return "Sound has a way of anchoring us right back into a moment. Was it bustling and loud around you, or could you hear the quiet ambient details?"
        }

        if (sensoryPromptCue?.contains("weather", ignoreCase = true) == true || lower.contains("cold") || lower.contains("rain") || lower.contains("hot") || lower.contains("snow")) {
            return "You can almost feel the temperature shifting when you recall that day. How did the air feel when you first stepped outside?"
        }

        if (sensoryPromptCue?.contains("presence", ignoreCase = true) == true || lower.contains("mom") || lower.contains("dad") || lower.contains("friend") || lower.contains("we")) {
            return "Having someone beside you changes the whole texture of a memory. Do you remember what you two were talking about right before that?"
        }

        if (lower.contains("exhaust") || lower.contains("tired") || lower.contains("heavy") || lower.contains("stress")) {
            return "You can still sense that weight even years later. When you think about that stretch of time, what do you wish you had known then?"
        }

        if (lower.contains("photo") || lower.contains("picture") || lower.contains("table") || lower.contains("room")) {
            return "Take a closer look at what's right in front of you. Is there a small object or texture on the table that stands out?"
        }

        if (memoryContext != null) {
            val people = memoryContext.getPeopleList()
            if (people.isNotEmpty() && people.first() != "Self") {
                return "Thinking about ${memoryContext.timeframeReferenced} with ${people.first()}—what was the unspoken feeling between you two back then?"
            }
        }

        return "That's such a vivid piece of the picture. What was going through your mind in the quiet moment right after that happened?"
    }
}
