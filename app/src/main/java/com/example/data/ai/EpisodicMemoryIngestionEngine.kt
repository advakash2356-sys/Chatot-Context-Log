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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit

data class EpisodicMemoryNode(
    val memoryId: String,
    val timestampRecorded: String,
    val timeframeReferenced: String,
    val relativeLifeStage: String,
    val people: List<String>,
    val locations: List<String>,
    val physicalObjects: List<String>,
    val primaryTone: String,
    val emotionalValence: String,
    val notableShifts: String,
    val sensoryCues: List<String>,
    val narrativeSummary: String,
    val unresolvedGaps: List<String>,
    val searchKeywords: List<String>,
    val rawInputText: String,
    val imageDescription: String? = null
) {
    fun toEntity(epochMs: Long = System.currentTimeMillis()): EpisodicMemoryEntity {
        return EpisodicMemoryEntity(
            id = memoryId,
            timestampRecorded = epochMs,
            isoTimestamp = timestampRecorded,
            timeframeReferenced = timeframeReferenced,
            relativeLifeStage = relativeLifeStage,
            peopleJson = EpisodicMemoryEntity.toJsonArrayString(people),
            locationsJson = EpisodicMemoryEntity.toJsonArrayString(locations),
            physicalObjectsJson = EpisodicMemoryEntity.toJsonArrayString(physicalObjects),
            primaryTone = primaryTone,
            emotionalValence = emotionalValence,
            notableShifts = notableShifts,
            sensoryCuesJson = EpisodicMemoryEntity.toJsonArrayString(sensoryCues),
            narrativeSummary = narrativeSummary,
            unresolvedGapsJson = EpisodicMemoryEntity.toJsonArrayString(unresolvedGaps),
            searchKeywordsJson = EpisodicMemoryEntity.toJsonArrayString(searchKeywords),
            rawInputText = rawInputText,
            imageDescription = imageDescription
        )
    }
}

class EpisodicMemoryIngestionEngine(
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
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Ingests multimodal memory input (raw text notes, speech transcript, optional image description or audio reference)
     * and processes it according to the Episodic Memory Ingestion Engine specification.
     */
    suspend fun ingestMemory(
        rawInputText: String,
        imageDescription: String? = null,
        audioPath: String? = null
    ): EpisodicMemoryEntity = withContext(Dispatchers.IO) {
        val memoryId = "mem_${UUID.randomUUID().toString().take(12)}"
        val nowIso = isoDateFormat.format(Date())
        val epochNow = System.currentTimeMillis()

        if (apiKey.isBlank()) {
            Log.w("EpisodicEngine", "Gemini API key is blank. Falling back to local offline heuristic engine.")
            val fallbackNode = generateFallbackMemoryNode(memoryId, nowIso, rawInputText, imageDescription)
            return@withContext fallbackNode.toEntity(epochNow)
        }

        try {
            val systemInstruction = """
                # SYSTEM INSTRUCTION: EPISODIC MEMORY INGESTION ENGINE

                ## Role & Purpose
                You are an episodic memory extraction and indexing engine for a personal recording application. Your task is to process raw multimodal inputs (audio recordings, speech transcripts, attached photos, and user notes) and convert them into a structured, queryable episodic memory object.

                ## Processing Objectives
                1. Temporal Anchoring: Identify the exact date, season, relative era (e.g., "Winter 2023", "College years"), or approximate time period.
                2. Entity & Relationship Graphing: Extract all people, relationships (e.g., mother, childhood friend), geographic locations, vehicles, pets, and significant physical objects.
                3. Emotional Undercurrents: Detect the dominant emotional tone (e.g., nostalgia, unresolved grief, pride, quiet contemplation) and note any emotional shifts occurring during the recording.
                4. Sensory Details: Isolate sensory descriptions mentioned in audio or visible in photos (ambient sounds, weather, lighting, smells, background music).
                5. Unresolved Gaps: Flag ambiguous statements, missing names, or incomplete stories that an interactive conversational guide should probe later.
                6. Core Narrative Summary: Generate a 2-3 sentence grounded summary capturing the essence of the memory.

                ## Output Schema (Strict JSON)
                Return ONLY a JSON object matching this schema:
                {
                  "memory_id": "$memoryId",
                  "temporal_anchor": {
                    "timestamp_recorded": "$nowIso",
                    "timeframe_referenced": "string (e.g., Late Autumn 2021, Summer of '16, 2 weeks ago)",
                    "relative_life_stage": "string (e.g., Childhood, High School, College, Early Career, Recent Years)"
                  },
                  "entities": {
                    "people": ["string"],
                    "locations": ["string"],
                    "physical_objects": ["string"]
                  },
                  "emotional_profile": {
                    "primary_tone": "string (e.g., Nostalgia, Quiet Contemplation, Joyful Reverie)",
                    "emotional_valence": "positive | neutral | negative | bittersweet | melancholic",
                    "notable_shifts": "string (e.g., Started with regret but transitioned into gratitude)"
                  },
                  "sensory_cues": ["string (e.g., sound of rain on tin roof, golden hour light, smell of old paper)"],
                  "narrative_summary": "string (2-3 sentence grounded summary)",
                  "unresolved_gaps": ["string (Ambiguities or unexplored details suitable for conversational exploration)"],
                  "search_keywords": ["string (Semantic keywords for vector search and graph database indexing)"]
                }
            """.trimIndent()

            val userContent = StringBuilder().apply {
                append("Raw User Input / Spoken Memory Transcript:\n")
                append(rawInputText.ifBlank { "No spoken transcript provided." })
                if (!imageDescription.isNullOrBlank()) {
                    append("\n\nAttached Photo Context / Visual Description:\n")
                    append(imageDescription)
                }
            }.toString()

            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", userContent))
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemInstruction))
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                Log.w("EpisodicEngine", "Gemini API error ($response.code): $errBody")
                val fallbackNode = generateFallbackMemoryNode(memoryId, nowIso, rawInputText, imageDescription)
                return@withContext fallbackNode.toEntity(epochNow)
            }

            val respBody = response.body?.string() ?: ""
            val jsonResponse = JSONObject(respBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val textPart = firstCandidate?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: ""

            val cleanedJson = cleanJsonText(textPart)
            val node = parseEpisodicJson(cleanedJson, memoryId, nowIso, rawInputText, imageDescription)
            return@withContext node.toEntity(epochNow)

        } catch (e: Exception) {
            Log.e("EpisodicEngine", "Exception during episodic memory ingestion", e)
            val fallbackNode = generateFallbackMemoryNode(memoryId, nowIso, rawInputText, imageDescription)
            return@withContext fallbackNode.toEntity(epochNow)
        }
    }

    /**
     * Enriches an existing episodic memory with the user's answer to an unresolved probe gap.
     */
    suspend fun resolveGapAndEnrichMemory(
        existingEntity: EpisodicMemoryEntity,
        gapQuestion: String,
        userClarification: String
    ): EpisodicMemoryEntity = withContext(Dispatchers.IO) {
        val remainingGaps = existingEntity.getUnresolvedGapsList().filter { it != gapQuestion }
        val updatedSummary = "${existingEntity.narrativeSummary} Further clarified: $userClarification."

        // Check if there are new entities mentioned in user clarification
        val currentPeople = existingEntity.getPeopleList().toMutableList()
        val currentLocations = existingEntity.getLocationsList().toMutableList()
        val currentObjects = existingEntity.getPhysicalObjectsList().toMutableList()

        val words = userClarification.split(" ", ",", ".", ";")
        for (w in words) {
            val clean = w.trim()
            if (clean.length > 3 && clean.first().isUpperCase() && !currentPeople.contains(clean) && !currentLocations.contains(clean)) {
                if (gapQuestion.lowercase().contains("who") || gapQuestion.lowercase().contains("person")) {
                    currentPeople.add(clean)
                } else if (gapQuestion.lowercase().contains("where") || gapQuestion.lowercase().contains("location")) {
                    currentLocations.add(clean)
                }
            }
        }

        return@withContext existingEntity.copy(
            narrativeSummary = updatedSummary,
            unresolvedGapsJson = EpisodicMemoryEntity.toJsonArrayString(remainingGaps),
            peopleJson = EpisodicMemoryEntity.toJsonArrayString(currentPeople),
            locationsJson = EpisodicMemoryEntity.toJsonArrayString(currentLocations),
            physicalObjectsJson = EpisodicMemoryEntity.toJsonArrayString(currentObjects),
            isEnrichedWithProbe = true
        )
    }

    private fun cleanJsonText(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```json")) {
            text = text.removePrefix("```json")
        } else if (text.startsWith("```")) {
            text = text.removePrefix("```")
        }
        if (text.endsWith("```")) {
            text = text.removeSuffix("```")
        }
        return text.trim()
    }

    private fun parseEpisodicJson(
        jsonStr: String,
        fallbackId: String,
        fallbackIso: String,
        rawInputText: String,
        imageDesc: String?
    ): EpisodicMemoryNode {
        return try {
            val root = JSONObject(jsonStr)

            val memoryId = root.optString("memory_id", fallbackId).ifBlank { fallbackId }
            val tempAnchorObj = root.optJSONObject("temporal_anchor")
            val timestampRecorded = tempAnchorObj?.optString("timestamp_recorded", fallbackIso) ?: fallbackIso
            val timeframeReferenced = tempAnchorObj?.optString("timeframe_referenced", "Recent context") ?: "Recent context"
            val relativeLifeStage = tempAnchorObj?.optString("relative_life_stage", "Adulthood") ?: "Adulthood"

            val entitiesObj = root.optJSONObject("entities")
            val people = jsonArrayToList(entitiesObj?.optJSONArray("people"))
            val locations = jsonArrayToList(entitiesObj?.optJSONArray("locations"))
            val physicalObjects = jsonArrayToList(entitiesObj?.optJSONArray("physical_objects"))

            val emoObj = root.optJSONObject("emotional_profile")
            val primaryTone = emoObj?.optString("primary_tone", "Reflective") ?: "Reflective"
            val emotionalValence = emoObj?.optString("emotional_valence", "neutral") ?: "neutral"
            val notableShifts = emoObj?.optString("notable_shifts", "Consistent steady mood") ?: "Consistent steady mood"

            val sensoryCues = jsonArrayToList(root.optJSONArray("sensory_cues"))
            val narrativeSummary = root.optString("narrative_summary", rawInputText.take(200))
            val unresolvedGaps = jsonArrayToList(root.optJSONArray("unresolved_gaps"))
            val searchKeywords = jsonArrayToList(root.optJSONArray("search_keywords"))

            EpisodicMemoryNode(
                memoryId = memoryId,
                timestampRecorded = timestampRecorded,
                timeframeReferenced = timeframeReferenced,
                relativeLifeStage = relativeLifeStage,
                people = people,
                locations = locations,
                physicalObjects = physicalObjects,
                primaryTone = primaryTone,
                emotionalValence = emotionalValence,
                notableShifts = notableShifts,
                sensoryCues = sensoryCues,
                narrativeSummary = narrativeSummary,
                unresolvedGaps = unresolvedGaps,
                searchKeywords = searchKeywords,
                rawInputText = rawInputText,
                imageDescription = imageDesc
            )
        } catch (e: Exception) {
            Log.e("EpisodicEngine", "Failed to parse JSON into EpisodicMemoryNode", e)
            generateFallbackMemoryNode(fallbackId, fallbackIso, rawInputText, imageDesc)
        }
    }

    private fun jsonArrayToList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            val s = array.optString(i)
            if (s.isNotBlank()) list.add(s)
        }
        return list
    }

    /**
     * Rule-based heuristic extraction fallback ensuring complete schema adherence without external network.
     */
    private fun generateFallbackMemoryNode(
        memoryId: String,
        isoNow: String,
        rawInputText: String,
        imageDesc: String?
    ): EpisodicMemoryNode {
        val lower = rawInputText.lowercase()

        // 1. Temporal Anchoring
        val timeframe = when {
            lower.contains("childhood") || lower.contains("when i was young") || lower.contains("elementary") -> "Childhood era"
            lower.contains("college") || lower.contains("university") || lower.contains("dorm") -> "College years"
            lower.contains("high school") || lower.contains("teenage") -> "High school period"
            lower.contains("winter") -> "Winter season"
            lower.contains("summer") -> "Summer season"
            lower.contains("autumn") || lower.contains("fall") -> "Autumn season"
            lower.contains("spring") -> "Spring season"
            lower.contains("yesterday") || lower.contains("last week") || lower.contains("earlier today") -> "Recent days"
            lower.contains("202") || lower.contains("199") -> "Specific calendar year"
            else -> "Circa ${SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())}"
        }

        val lifeStage = when {
            lower.contains("childhood") || lower.contains("kid") -> "Childhood"
            lower.contains("college") || lower.contains("campus") -> "College"
            lower.contains("high school") -> "Adolescence"
            lower.contains("first job") || lower.contains("early career") -> "Early Career"
            else -> "Current Era"
        }

        // 2. Entities & Graphing
        val people = mutableListOf<String>()
        val locations = mutableListOf<String>()
        val physicalObjects = mutableListOf<String>()

        val commonPeopleKeywords = listOf("mom", "mother", "dad", "father", "sister", "brother", "alex", "sarah", "michael", "emma", "grandma", "grandfather", "friend", "boss", "mentor", "colleague")
        for (p in commonPeopleKeywords) {
            if (lower.contains(p)) people.add(p.replaceFirstChar { it.uppercase() })
        }
        if (people.isEmpty()) people.add("Self")

        val commonLocationKeywords = listOf("home", "cabin", "beach", "lake", "office", "san francisco", "new york", "london", "mountains", "kitchen", "backyard", "library", "coffee shop", "paris", "tokyo", "garage")
        for (loc in commonLocationKeywords) {
            if (lower.contains(loc)) locations.add(loc.replaceFirstChar { it.uppercase() })
        }
        if (locations.isEmpty()) locations.add("Local environment")

        val commonObjectKeywords = listOf("car", "guitar", "journal", "photograph", "bicycle", "watch", "record player", "tin roof", "laptop", "fireplace", "coffee mug", "jacket", "letter")
        for (obj in commonObjectKeywords) {
            if (lower.contains(obj)) physicalObjects.add(obj.replaceFirstChar { it.uppercase() })
        }
        if (physicalObjects.isEmpty()) physicalObjects.add("Personal keepsakes")

        // 3. Emotional Profile
        val (tone, valence, shifts) = when {
            lower.contains("sad") || lower.contains("grief") || lower.contains("miss") || lower.contains("lost") ->
                Triple("Melancholic contemplation", "melancholic", "Deep emotional resonance with lingering longing")
            lower.contains("happy") || lower.contains("joy") || lower.contains("excited") || lower.contains("celebrat") || lower.contains("proud") ->
                Triple("Joyful pride & warmth", "positive", "Uplifting and energized recollection")
            lower.contains("bitter") || lower.contains("nostalgi") || lower.contains("remember when") ->
                Triple("Sweet nostalgia", "bittersweet", "Wistful appreciation intertwined with passing time")
            lower.contains("angry") || lower.contains("regret") || lower.contains("mistake") ->
                Triple("Unresolved tension", "negative", "Initial frustration softening into retrospective learning")
            else ->
                Triple("Quiet contemplative focus", "neutral", "Grounded, objective memory recording")
        }

        // 4. Sensory Cues
        val sensoryList = mutableListOf<String>()
        if (lower.contains("rain") || lower.contains("thunder") || lower.contains("storm")) sensoryList.add("Sound of rain and ambient weather")
        if (lower.contains("smell") || lower.contains("scent") || lower.contains("coffee") || lower.contains("pine")) sensoryList.add("Aroma of fresh brew and ambient air")
        if (lower.contains("light") || lower.contains("sun") || lower.contains("sunset") || lower.contains("dusk")) sensoryList.add("Warm golden hour light filtration")
        if (lower.contains("music") || lower.contains("sound") || lower.contains("voice") || lower.contains("laugh")) sensoryList.add("Background acoustic voices & reverberation")
        if (sensoryList.isEmpty()) {
            sensoryList.add("Quiet ambient room atmosphere")
            sensoryList.add("Tactile sensation of physical presence")
        }

        // 5. Narrative Summary
        val sentences = rawInputText.split(Regex("[.!?\\n]+")).filter { it.isNotBlank() }
        val summary = if (sentences.size >= 2) {
            sentences.take(2).joinToString(". ").trim() + "."
        } else if (rawInputText.isNotBlank()) {
            rawInputText.trim()
        } else {
            "An episodic memory recording capturing personal reflection, spatial details, and temporal context."
        }

        // 6. Unresolved Gaps
        val gaps = mutableListOf<String>()
        if (people.size == 1 && people.first() == "Self") {
            gaps.add("Were there other people present during this event?")
        } else {
            gaps.add("What was the reaction of ${people.firstOrNull() ?: "the others"} during that moment?")
        }
        gaps.add("What specific year or month did this milestone happen?")

        // 7. Search Keywords
        val keywords = mutableListOf<String>()
        keywords.addAll(people)
        keywords.addAll(locations)
        keywords.addAll(physicalObjects)
        keywords.add(lifeStage)
        keywords.add(tone)

        return EpisodicMemoryNode(
            memoryId = memoryId,
            timestampRecorded = isoNow,
            timeframeReferenced = timeframe,
            relativeLifeStage = lifeStage,
            people = people.distinct(),
            locations = locations.distinct(),
            physicalObjects = physicalObjects.distinct(),
            primaryTone = tone,
            emotionalValence = valence,
            notableShifts = shifts,
            sensoryCues = sensoryList,
            narrativeSummary = summary,
            unresolvedGaps = gaps,
            searchKeywords = keywords.distinct().filter { it.isNotBlank() },
            rawInputText = rawInputText,
            imageDescription = imageDesc
        )
    }
}
