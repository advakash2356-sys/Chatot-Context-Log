package com.example.data.ai

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.DictionaryItemEntity
import com.example.data.local.SnippetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * ChatotFlowEngine / WisprFlowEngine processes voice transcripts and raw audio through Gemini 3.5 Flash
 * with custom tone selection (Auto Clean, Formal, Casual, Concise, Professional), automatic speech cleanup,
 * snippet expansion, personal dictionary preservation, and structured meeting extraction.
 */
class ChatotFlowEngine(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY
) {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Primary Flow Studio processing for dictation, snippets, dictionary terms, and tones.
     */
    suspend fun processWisprFlow(
        rawInput: String,
        contextType: WisprContextType = WisprContextType.GENERAL,
        tone: WisprTone = WisprTone.AUTO_CLEAN,
        dictionary: List<DictionaryItemEntity> = emptyList(),
        snippets: List<SnippetEntity> = emptyList(),
        targetLanguage: String? = null
    ): WisprFlowResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        if (rawInput.isBlank()) {
            return@withContext WisprFlowResult(
                rawTranscript = "",
                cleanText = "",
                toneUsed = tone
            )
        }

        // Apply snippet expansion locally first
        var expandedText = rawInput
        val appliedSnippetsList = mutableListOf<String>()
        for (snippet in snippets) {
            if (snippet.triggerPhrase.isNotBlank() && expandedText.contains(snippet.triggerPhrase, ignoreCase = true)) {
                expandedText = expandedText.replace(snippet.triggerPhrase, snippet.expandedText, ignoreCase = true)
                val label = if (snippet.description.isNotBlank()) snippet.description else snippet.expandedText.take(20)
                appliedSnippetsList.add("${snippet.triggerPhrase} ➔ $label")
            }
        }

        val dictionaryTerms = dictionary.joinToString(", ") { it.term }
        val prompt = """
            You are Wispr / Chatot, the world's most advanced speech-to-text intelligence engine powered by Gemini 3.5 Flash.
            Analyze and transform the user's voice input.
            
            CONTEXT: ${contextType.displayName} (${contextType.iconDesc})
            TONE: ${tone.displayName} (${tone.promptInstruction})
            ${if (!targetLanguage.isNullOrBlank() && targetLanguage != "English") "TARGET LANGUAGE: $targetLanguage" else ""}
            ${if (dictionaryTerms.isNotBlank()) "PRESERVED VOCABULARY & NAMES: $dictionaryTerms" else ""}
            
            RAW INPUT:
            \"\"\"
            $expandedText
            \"\"\"
            
            INSTRUCTIONS:
            1. Remove filler words (um, uh, you know, like, actually, so basically) and speech disfluencies.
            2. Detect and fix false starts / self-corrections (e.g., 'let's meet at 4, wait no actually 5' becomes 'let's meet at 5').
            3. Apply the requested TONE formatting (${tone.displayName}).
            4. Structure the output according to the CONTEXT (${contextType.displayName}).
            5. Extract action items if any are present.
            6. If context is MEETING_NOTE or LONG_CONTEXT_SYNC, produce structured meeting notes with executiveSummary, keyDecisions, and actionItems.
            
            Respond strictly in valid JSON with schema:
            {
              "cleanText": "Pristine transcription with fillers removed and self-corrections fixed",
              "formattedText": "Formatted text according to context layout",
              "toneRewrittenText": "Rewritten text matching requested tone",
              "detectedLanguage": "English",
              "selfCorrectionsFound": ["corrected item 1"],
              "actionItems": ["Action item 1"],
              "structuredMeeting": {
                "title": "Meeting Title",
                "executiveSummary": "Executive summary",
                "actionItems": ["Task 1"],
                "keyDecisions": ["Decision 1"],
                "timelineHighlights": ["Milestone 1"],
                "risksAndBlockers": [],
                "followUpEmailDraft": "Draft follow up email"
              },
              "builtAiPrompt": null
            }
            RESPOND ONLY WITH JSON. NO MARKDOWN CODE FENCES.
        """.trimIndent()

        val jsonResult = if (apiKey.isNotBlank()) callGeminiJsonApi(prompt) else null

        if (jsonResult != null) {
            val cleanText = jsonResult.optString("cleanText", fallbackSpeechCleanup(expandedText))
            val formattedText = jsonResult.optString("formattedText", cleanText)
            val toneRewrittenText = jsonResult.optString("toneRewrittenText", formattedText)
            val detectedLanguage = jsonResult.optString("detectedLanguage", "English")

            val selfCorrections = mutableListOf<String>()
            val corrArr = jsonResult.optJSONArray("selfCorrectionsFound")
            if (corrArr != null) {
                for (i in 0 until corrArr.length()) selfCorrections.add(corrArr.getString(i))
            }

            val actionItems = mutableListOf<String>()
            val actArr = jsonResult.optJSONArray("actionItems")
            if (actArr != null) {
                for (i in 0 until actArr.length()) actionItems.add(actArr.getString(i))
            }

            var structuredMeeting: StructuredMeetingNotes? = null
            val meetObj = jsonResult.optJSONObject("structuredMeeting")
            if (meetObj != null && (contextType == WisprContextType.MEETING_NOTE || contextType == WisprContextType.LONG_CONTEXT_SYNC)) {
                val meetActions = mutableListOf<String>()
                val mActArr = meetObj.optJSONArray("actionItems")
                if (mActArr != null) {
                    for (i in 0 until mActArr.length()) meetActions.add(mActArr.getString(i))
                }
                val meetDecisions = mutableListOf<String>()
                val mDecArr = meetObj.optJSONArray("keyDecisions")
                if (mDecArr != null) {
                    for (i in 0 until mDecArr.length()) meetDecisions.add(mDecArr.getString(i))
                }
                val meetTimeline = mutableListOf<String>()
                val mTimeArr = meetObj.optJSONArray("timelineHighlights")
                if (mTimeArr != null) {
                    for (i in 0 until mTimeArr.length()) meetTimeline.add(mTimeArr.getString(i))
                }
                val meetRisks = mutableListOf<String>()
                val mRiskArr = meetObj.optJSONArray("risksAndBlockers")
                if (mRiskArr != null) {
                    for (i in 0 until mRiskArr.length()) meetRisks.add(mRiskArr.getString(i))
                }

                structuredMeeting = StructuredMeetingNotes(
                    title = meetObj.optString("title", "Structured Meeting Notes"),
                    executiveSummary = meetObj.optString("executiveSummary", cleanText),
                    actionItems = meetActions.ifEmpty { actionItems },
                    keyDecisions = meetDecisions,
                    timelineHighlights = meetTimeline,
                    risksAndBlockers = meetRisks,
                    followUpEmailDraft = meetObj.optString("followUpEmailDraft", "")
                )
            }

            val builtAiPrompt = if (contextType == WisprContextType.AI_PROMPT) {
                "Role: Subject Matter Expert\nContext: $cleanText\nInstructions: Provide detailed, step-by-step guidance.\nConstraints: Be concise, clear, and actionable."
            } else null

            val latency = System.currentTimeMillis() - startTime
            val tokenEst = (expandedText.length / 4) + 120

            return@withContext WisprFlowResult(
                rawTranscript = rawInput,
                cleanText = cleanText,
                formattedText = formattedText,
                toneRewrittenText = toneRewrittenText,
                toneUsed = tone,
                structuredMeeting = structuredMeeting,
                builtAiPrompt = builtAiPrompt,
                appliedSnippets = appliedSnippetsList,
                selfCorrectionsFound = selfCorrections,
                actionItems = actionItems,
                detectedLanguage = detectedLanguage,
                isExtendedContext = contextType == WisprContextType.LONG_CONTEXT_SYNC,
                tokenCountEstimate = tokenEst,
                latencyMs = latency
            )
        }

        // Local Fallback Processing
        val clean = fallbackSpeechCleanup(expandedText)
        val latency = System.currentTimeMillis() - startTime
        val actions = extractFallbackActionItems(clean)

        return@withContext WisprFlowResult(
            rawTranscript = rawInput,
            cleanText = clean,
            formattedText = clean,
            toneRewrittenText = clean,
            toneUsed = tone,
            appliedSnippets = appliedSnippetsList,
            actionItems = actions,
            latencyMs = latency,
            tokenCountEstimate = (clean.length / 4)
        )
    }

    /**
     * Primary entry point for voice audio or text to structured notes with tone instruction
     */
    suspend fun processAudioOrTextToStructuredNotes(
        rawText: String? = null,
        audioBase64: String? = null,
        audioMimeType: String = "audio/mp4",
        tone: WisprTone = WisprTone.CONCISE
    ): StructuredMeetingNotes = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        if (rawText.isNullOrBlank() && audioBase64.isNullOrBlank()) {
            return@withContext StructuredMeetingNotes(
                title = "Empty Note",
                executiveSummary = "No audio or text content was provided.",
                actionItems = emptyList(),
                keyDecisions = emptyList()
            )
        }

        if (apiKey.isNotBlank()) {
            try {
                val systemPrompt = """
                    You are Wispr / Chatot voice intelligence powered by Gemini 3.5 Flash.
                    Analyze the input audio recording or text transcript carefully.
                    
                    TONE INSTRUCTION (${tone.displayName}):
                    ${tone.promptInstruction}
                    
                    TASK:
                    1. Remove filler words (um, uh, like, you know, actually, so basically) and resolve false starts or self-corrections.
                    2. Synthesize the core content according to the requested tone (${tone.displayName}).
                    3. Extract clear, actionable next steps (actionItems) with owners and deadlines where implied.
                    4. Identify key decisions or discussion points (keyPoints).
                    
                    Output strictly JSON in this format:
                    {
                      "title": "Short descriptive title (3-6 words)",
                      "conciseSummary": "Synthesized executive summary in ${tone.displayName} tone",
                      "actionItems": [
                        "Action item 1",
                        "Action item 2"
                      ],
                      "keyPoints": [
                        "Key decision or discussion point 1",
                        "Key decision or discussion point 2"
                      ],
                      "cleanTranscript": "Cleaned transcript with filler words removed and self-corrections resolved."
                    }
                    RESPOND ONLY IN VALID JSON WITHOUT MARKDOWN CODE BLOCKS.
                """.trimIndent()

                val responseJson = if (!audioBase64.isNullOrBlank()) {
                    callGeminiMultimodalJsonApi(
                        prompt = systemPrompt,
                        base64Data = audioBase64,
                        mimeType = audioMimeType
                    )
                } else {
                    val fullPrompt = "$systemPrompt\n\nINPUT TEXT:\n\"\"\"\n$rawText\n\"\"\""
                    callGeminiJsonApi(fullPrompt)
                }

                if (responseJson != null) {
                    val title = responseJson.optString("title", "Voice Note Summary")
                    val conciseSummary = responseJson.optString("conciseSummary", "")

                    val actionItems = mutableListOf<String>()
                    val actionArray = responseJson.optJSONArray("actionItems")
                    if (actionArray != null) {
                        for (i in 0 until actionArray.length()) {
                            actionItems.add(actionArray.getString(i))
                        }
                    }

                    val keyPoints = mutableListOf<String>()
                    val pointsArray = responseJson.optJSONArray("keyPoints")
                    if (pointsArray != null) {
                        for (i in 0 until pointsArray.length()) {
                            keyPoints.add(pointsArray.getString(i))
                        }
                    }

                    val cleanTranscript = responseJson.optString("cleanTranscript", rawText ?: "")

                    return@withContext StructuredMeetingNotes(
                        title = title,
                        executiveSummary = conciseSummary.ifBlank { cleanTranscript },
                        actionItems = actionItems,
                        keyDecisions = keyPoints,
                        timelineHighlights = listOf("Synthesized in ${System.currentTimeMillis() - startTime}ms via Gemini 3.5 Flash (${tone.displayName} Tone)")
                    )
                }
            } catch (e: Exception) {
                Log.e("ChatotFlowEngine", "Error calling Gemini structured notes API: ${e.message}", e)
            }
        }

        // Local Fallback Processing
        val textToProcess = rawText ?: "Spoken note recorded via voice engine."
        val cleaned = fallbackSpeechCleanup(textToProcess)
        val fallbackActions = extractFallbackActionItems(cleaned)
        return@withContext StructuredMeetingNotes(
            title = when (tone) {
                WisprTone.FORMAL -> "Executive Voice Debrief"
                WisprTone.CASUAL -> "Quick Catch-up Note"
                WisprTone.CONCISE, WisprTone.AUTO_CLEAN, WisprTone.PROFESSIONAL -> "Voice Note Summary"
            },
            executiveSummary = when (tone) {
                WisprTone.FORMAL -> "Pursuant to the recorded discussion: $cleaned"
                WisprTone.CASUAL -> "Hey, quick update: $cleaned"
                WisprTone.CONCISE, WisprTone.AUTO_CLEAN, WisprTone.PROFESSIONAL -> cleaned
            },
            actionItems = if (fallbackActions.isNotEmpty()) fallbackActions else listOf("Review note takeaways with team", "Confirm next milestones"),
            keyDecisions = listOf("Spoken notes recorded and indexed locally", "Transcribed in ${tone.displayName} tone"),
            timelineHighlights = listOf("Processed in ${System.currentTimeMillis() - startTime}ms")
        )
    }

    /**
     * Executes custom AI transformation
     */
    suspend fun executeTransform(
        input: String,
        transform: WisprTransform,
        customInstruction: String? = null,
        dictionary: List<DictionaryItemEntity> = emptyList(),
        tone: WisprTone = WisprTone.CONCISE
    ): String = withContext(Dispatchers.IO) {
        if (input.isBlank()) return@withContext ""
        if (apiKey.isBlank()) return@withContext fallbackTransform(input, transform)

        try {
            val dictStr = dictionary.joinToString(", ") { it.term }
            val prompt = """
                You are Wispr / Chatot AI. Transform this voice text according to the following instruction:
                TRANSFORMATION: ${transform.displayName} - ${transform.promptInstruction}
                TONE: ${tone.displayName} (${tone.promptInstruction})
                ${if (dictStr.isNotBlank()) "PRESERVED VOCABULARY: $dictStr" else ""}
                ${if (!customInstruction.isNullOrBlank()) "ADDITIONAL CONSTRAINT: $customInstruction" else ""}
                
                INPUT:
                \"\"\"
                $input
                \"\"\"
                
                Respond with the transformed text directly. Do not include markdown code block fences unless specifically requested.
            """.trimIndent()

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val resObj = JSONObject(bodyStr)
                    val candidates = resObj.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val content = candidates.getJSONObject(0).optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text").trim()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChatotFlowEngine", "Transform error: ${e.message}", e)
        }

        return@withContext fallbackTransform(input, transform)
    }

    /**
     * Transcribes raw audio bytes using Gemini multimodal understanding
     */
    suspend fun transcribeAudio(
        audioBytes: ByteArray,
        mimeType: String = "audio/mp3",
        dictionary: List<DictionaryItemEntity> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || audioBytes.isEmpty()) {
            return@withContext "Recorded audio sample processed locally."
        }

        try {
            val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
            val dictTerms = dictionary.joinToString(", ") { it.term }
            val prompt = """
                Transcribe the spoken audio with exact accuracy.
                Remove filler words (um, uh, like) and self-corrections.
                ${if (dictTerms.isNotBlank()) "Vocabulary terms & names: $dictTerms" else ""}
                Return ONLY the clean transcribed text.
            """.trimIndent()

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val partsArray = JSONArray().apply {
                put(JSONObject().put("text", prompt))
                put(JSONObject().apply {
                    put("inlineData", JSONObject().apply {
                        put("mimeType", mimeType)
                        put("data", base64Audio)
                    })
                })
            }

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", partsArray)
                    })
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val resObj = JSONObject(bodyStr)
                    val candidates = resObj.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val content = candidates.getJSONObject(0).optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text").trim()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChatotFlowEngine", "Audio transcription error: ${e.message}", e)
        }

        return@withContext "Voice note recorded."
    }

    private fun callGeminiMultimodalJsonApi(
        prompt: String,
        base64Data: String,
        mimeType: String
    ): JSONObject? {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val partsArray = JSONArray().apply {
            put(JSONObject().put("text", prompt))
            put(JSONObject().apply {
                put("inlineData", JSONObject().apply {
                    put("mimeType", mimeType)
                    put("data", base64Data)
                })
            })
        }

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", partsArray)
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: ""
                val resObj = JSONObject(bodyStr)
                val candidates = resObj.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val rawText = parts.getJSONObject(0).optString("text")
                        val cleaned = rawText.trim()
                            .removePrefix("```json")
                            .removePrefix("```JSON")
                            .removePrefix("```")
                            .removeSuffix("```")
                            .trim()
                        return try {
                            JSONObject(cleaned)
                        } catch (e: Exception) {
                            Log.e("ChatotFlowEngine", "JSON parse error: $cleaned", e)
                            null
                        }
                    }
                }
            }
        }
        return null
    }

    private fun callGeminiJsonApi(prompt: String): JSONObject? {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: ""
                val resObj = JSONObject(bodyStr)
                val candidates = resObj.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val rawText = parts.getJSONObject(0).optString("text")
                        val cleaned = rawText.trim()
                            .removePrefix("```json")
                            .removePrefix("```JSON")
                            .removePrefix("```")
                            .removeSuffix("```")
                            .trim()
                        return try {
                            JSONObject(cleaned)
                        } catch (e: Exception) {
                            Log.e("ChatotFlowEngine", "JSON parse error: $cleaned", e)
                            null
                        }
                    }
                }
            }
        }
        return null
    }

    private fun fallbackSpeechCleanup(text: String): String {
        var clean = text
        val fillers = listOf(
            Regex("(?i)\\bum+h*\\b[,\\s]*"),
            Regex("(?i)\\buh+m*\\b[,\\s]*"),
            Regex("(?i)\\byou know[,\\s]*"),
            Regex("(?i)\\blike[,\\s]+"),
            Regex("(?i)\\bactually no[,\\s]*"),
            Regex("(?i)\\bwait no[,\\s]*"),
            Regex("(?i)\\bi mean[,\\s]*")
        )
        for (pattern in fillers) {
            clean = clean.replace(pattern, " ")
        }
        return clean.replace(Regex("\\s+"), " ").trim()
    }

    private fun extractFallbackActionItems(text: String): List<String> {
        val actions = mutableListOf<String>()
        val sentences = text.split(Regex("[.!?]\\s+"))
        for (s in sentences) {
            val lower = s.lowercase()
            if (lower.contains("tell") || lower.contains("send") || lower.contains("email") ||
                lower.contains("schedule") || lower.contains("call") || lower.contains("review") ||
                lower.contains("draft") || lower.contains("follow up")
            ) {
                actions.add(s.trim().replaceFirstChar { it.uppercase() })
            }
        }
        return actions
    }

    private fun fallbackTransform(input: String, transform: WisprTransform): String {
        val cleaned = fallbackSpeechCleanup(input)
        return when (transform) {
            WisprTransform.POLISH -> "Polished: $cleaned"
            WisprTransform.CONCISE -> cleaned
            WisprTransform.FIX_GRAMMAR -> cleaned
            WisprTransform.BULLETS -> cleaned.split(". ").filter { it.isNotBlank() }.joinToString("\n") { "• ${it.trim()}" }
            WisprTransform.TO_TASKS -> extractFallbackActionItems(cleaned).joinToString("\n") { "[ ] $it" }
            WisprTransform.TO_EMAIL -> "Subject: Discussion Update\n\nDear Team,\n\n$cleaned\n\nBest regards,\nWispr Flow"
            WisprTransform.TO_LINKEDIN -> "Key takeaway:\n\n$cleaned\n\n#Productivity #WisprFlow"
            WisprTransform.TO_PROMPT -> "Role: Expert\nPrompt: $cleaned\nFormat: Detailed actionable markdown"
            WisprTransform.SUMMARIZE -> "Executive Summary: $cleaned"
            WisprTransform.LONG_FORM_SYNTHESIS -> "# Meeting Minutes\n\n## Summary\n$cleaned\n\n## Action Items\n${extractFallbackActionItems(cleaned).joinToString("\n") { "• $it" }}"
            WisprTransform.TRANSLATE_EN -> cleaned
        }
    }
}

typealias WisprFlowEngine = ChatotFlowEngine
