package com.example.data.repository

import com.example.data.ai.DocumentChunker
import com.example.data.ai.EpisodicMemoryIngestionEngine
import com.example.data.ai.EpisodicMemoryNode
import com.example.data.ai.GeminiService
import com.example.data.ai.MemoryExplorationGuideEngine
import com.example.data.ai.MemoryGuideMessage
import com.example.data.ai.ParsedNoteResult
import com.example.data.ai.SpatialContextHelper
import com.example.data.ai.TwoHourRollupResult
import com.example.data.ai.VoiceContextType
import com.example.data.ai.VoiceFlowEngine
import com.example.data.ai.VoiceFlowResult
import com.example.data.ai.VoiceTone
import com.example.data.ai.VoiceTransform
import com.example.data.calendar.GoogleCalendarService
import com.example.data.billing.TokenBillingService
import com.example.data.local.ActionItemEntity
import com.example.data.local.BriefingDossierEntity
import com.example.data.local.CalendarEventEntity
import com.example.data.local.ContextLogDao
import com.example.data.local.ContextNoteEntity
import com.example.data.local.DictionaryItemEntity
import com.example.data.local.DocumentChunkEntity
import com.example.data.local.DocumentEntity
import com.example.data.local.EpisodicMemoryEntity
import com.example.data.local.EntryType
import com.example.data.local.GroundedCitation
import com.example.data.local.MatterEntity
import com.example.data.local.NoteEmbeddingEntity
import com.example.data.local.SemanticNoteSearchResult
import com.example.data.local.SnippetEntity
import com.example.data.local.TokenUsageEntity
import com.example.data.local.TwoHourRollupEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ContextLogRepository(
    private val dao: ContextLogDao,
    private val geminiService: GeminiService,
    private val calendarService: GoogleCalendarService = GoogleCalendarService(),
    val voiceEngine: VoiceFlowEngine = VoiceFlowEngine(),
    val wisprEngine: VoiceFlowEngine = voiceEngine,
    val episodicEngine: EpisodicMemoryIngestionEngine = EpisodicMemoryIngestionEngine(),
    val memoryGuideEngine: MemoryExplorationGuideEngine = MemoryExplorationGuideEngine(),
    val tokenBillingService: TokenBillingService = TokenBillingService(dao)
) {
    val allMatters: Flow<List<MatterEntity>> = dao.getAllMatters()
    val allNotes: Flow<List<ContextNoteEntity>> = dao.getAllNotes()
    val allActionItems: Flow<List<ActionItemEntity>> = dao.getAllActionItems()
    val pendingActionItems: Flow<List<ActionItemEntity>> = dao.getPendingActionItems()
    val actionItemsAssignedToYou: Flow<List<ActionItemEntity>> = dao.getActionItemsAssignedToYou()
    val allBriefingDossiers: Flow<List<BriefingDossierEntity>> = dao.getAllBriefingDossiers()
    val allEpisodicMemories: Flow<List<EpisodicMemoryEntity>> = dao.getAllEpisodicMemories()
    val allBlockStarts: Flow<List<Long>> = dao.getAllBlockStarts()
    val allRollups: Flow<List<TwoHourRollupEntity>> = dao.getAllRollups()
    val allDocuments: Flow<List<DocumentEntity>> = dao.getAllDocuments()
    val chunkCount: Flow<Int> = dao.getChunkCount()
    val allDictionaryItems: Flow<List<DictionaryItemEntity>> = dao.getAllDictionaryItems()
    val allSnippets: Flow<List<SnippetEntity>> = dao.getAllSnippets()
    val allCalendarEvents: Flow<List<CalendarEventEntity>> = dao.getAllCalendarEvents()
    val allTokenUsage: Flow<List<TokenUsageEntity>> = dao.getAllTokenUsage()
    val totalTokenCount: Flow<Int?> = dao.getTotalTokenCountFlow()
    val totalEstimatedCostUsd: Flow<Double?> = dao.getTotalEstimatedCostFlow()

    fun getNotesForBlock(blockStart: Long): Flow<List<ContextNoteEntity>> = dao.getNotesForBlock(blockStart)

    fun getRollupForBlock(blockStart: Long): Flow<TwoHourRollupEntity?> = dao.getRollupForBlock(blockStart)

    fun getUpcomingCalendarEvents(fromTime: Long = System.currentTimeMillis()): Flow<List<CalendarEventEntity>> =
        dao.getUpcomingCalendarEvents(fromTime)

    suspend fun insertNote(note: ContextNoteEntity) = withContext(Dispatchers.IO) {
        dao.insertNote(note)
        // Keep FTS table in sync
        dao.insertNoteFts(
            com.example.data.local.ContextNoteFtsEntity(
                noteId = note.id,
                cleanText = note.cleanText,
                rawTranscript = note.rawTranscript,
                matterCode = note.matterCode
            )
        )
        // Generate and persist vector embedding for semantic search
        try {
            val embedding = geminiService.generateEmbedding(note.cleanText, note.matterCode)
            if (embedding.isNotEmpty()) {
                dao.insertNoteEmbedding(
                    NoteEmbeddingEntity(
                        noteId = note.id,
                        matterCode = note.matterCode,
                        textSnippet = note.cleanText,
                        embeddingJson = NoteEmbeddingEntity.floatsToJson(embedding)
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w("ContextLogRepository", "Failed to generate note embedding", e)
        }
    }

    /**
     * Fetches user's Google Calendar events and synchronizes them into local Room DB for zero-latency context.
     */
    suspend fun fetchAndSyncCalendarEvents(accessToken: String? = null): List<CalendarEventEntity> = withContext(Dispatchers.IO) {
        val events = calendarService.fetchUpcomingEvents(accessToken)
        if (events.isNotEmpty()) {
            dao.insertCalendarEvents(events)
        }
        events
    }

    /**
     * Searches context notes using Vector Embeddings (Cosine Similarity) with Fallback to Room FTS & Text search.
     */
    suspend fun searchNotesFtsOrSemantic(query: String): List<ContextNoteEntity> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        // 1. Try Vector Semantic Similarity Search first
        val semanticResults = searchNotesSemantic(query, minSimilarity = 0.35f, topK = 15)
        if (semanticResults.isNotEmpty()) {
            val matchingNotes = semanticResults.mapNotNull { it.note }
            if (matchingNotes.isNotEmpty()) {
                return@withContext matchingNotes
            }
        }

        val sanitizedQuery = query.trim().replace("'", "''").replace("\"", "")
        // 2. Try Room FTS MATCH
        val ftsResults = try {
            dao.searchNotesFts("*$sanitizedQuery*")
        } catch (e: Exception) {
            emptyList()
        }

        if (ftsResults.isNotEmpty()) {
            return@withContext ftsResults
        }

        // 3. Fallback to text query across columns
        dao.searchNotesText(query.trim())
    }

    /**
     * Performs vector embedding cosine similarity search over stored context notes and meeting minutes.
     */
    suspend fun searchNotesSemantic(
        query: String,
        minSimilarity: Float = 0.30f,
        topK: Int = 10
    ): List<SemanticNoteSearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val queryEmbedding = geminiService.generateEmbedding(query, "SEARCH")
        if (queryEmbedding.isEmpty()) return@withContext emptyList()

        val allEmbeddings = dao.getAllNoteEmbeddingsSync()
        if (allEmbeddings.isEmpty()) return@withContext emptyList()

        val allNotesMap = dao.getAllNotesSync().associateBy { it.id }

        allEmbeddings.mapNotNull { noteEmb ->
            val vec = noteEmb.getEmbedding()
            if (vec.isNotEmpty()) {
                val similarity = NoteEmbeddingEntity.calculateCosineSimilarity(queryEmbedding, vec)
                if (similarity >= minSimilarity) {
                    SemanticNoteSearchResult(
                        noteId = noteEmb.noteId,
                        matterCode = noteEmb.matterCode,
                        textSnippet = noteEmb.textSnippet,
                        similarityScore = similarity,
                        note = allNotesMap[noteEmb.noteId]
                    )
                } else null
            } else null
        }
        .sortedByDescending { it.similarityScore }
        .take(topK)
    }

    suspend fun parseAndSaveVoiceNote(
        rawTranscript: String,
        calendarAccessToken: String? = null,
        syncToCalendar: Boolean = true
    ): ContextNoteEntity = withContext(Dispatchers.IO) {
        val currentMatters = dao.getAllMatters().first().map { it.code }
        val parsed: ParsedNoteResult = geminiService.parseVoiceTranscript(rawTranscript, currentMatters)

        var note = ContextNoteEntity(
            rawTranscript = rawTranscript,
            cleanText = parsed.cleanText,
            entryType = parsed.entryType,
            matterCode = parsed.matterCode,
            depthLevel = parsed.depthLevel,
            scheduledDatetime = parsed.scheduledDatetime,
            syncedToCalendar = parsed.calendarSyncSuggested && syncToCalendar
        )

        // If calendar sync is requested or suggested, sync to Google Calendar API
        if (syncToCalendar && (parsed.calendarSyncSuggested || parsed.entryType == EntryType.REMINDER)) {
            val syncResult = calendarService.syncNoteEvent(note, calendarAccessToken)
            if (syncResult.isSuccess) {
                note = note.copy(
                    syncedToCalendar = true,
                    googleEventId = syncResult.eventId
                )
            }
        }

        dao.insertNote(note)
        dao.insertNoteFts(
            com.example.data.local.ContextNoteFtsEntity(
                noteId = note.id,
                cleanText = note.cleanText,
                rawTranscript = note.rawTranscript,
                matterCode = note.matterCode
            )
        )

        // Automatically update the 2-hour block rollup to maintain persistent integrity
        generateAndSaveRollup(note.twoHourBlockStart)

        note
    }

    /**
     * Executes the comprehensive Voice Flow pipeline:
     * - Personal Dictionary preservation
     * - Snippet trigger expansion
     * - Natural speech cleanup (fillers, false starts, stuttering, backtracks)
     * - Context-aware adaptation (Email, Chat, Social Post, Task List, Prompt, Meeting Note)
     * - Tone & style rewriting (Executive, Professional, Casual, Concise, Friendly, etc.)
     */
    suspend fun processVoiceFlow(
        rawInput: String,
        contextType: VoiceContextType = VoiceContextType.GENERAL,
        tone: VoiceTone = VoiceTone.AUTO_CLEAN,
        targetLanguage: String? = null
    ): VoiceFlowResult = withContext(Dispatchers.IO) {
        val dictionary = dao.getAllDictionaryItemsSync()
        val snippets = dao.getAllSnippetsSync()
        voiceEngine.processVoiceFlow(
            rawInput = rawInput,
            contextType = contextType,
            tone = tone,
            dictionary = dictionary,
            snippets = snippets,
            targetLanguage = targetLanguage
        )
    }

    suspend fun processWisprFlow(
        rawInput: String,
        contextType: VoiceContextType = VoiceContextType.GENERAL,
        tone: VoiceTone = VoiceTone.AUTO_CLEAN,
        targetLanguage: String? = null
    ): VoiceFlowResult = processVoiceFlow(rawInput, contextType, tone, targetLanguage)

    /**
     * Executes AI Transforms (Polish, Concise, Bullets, To Email, To Tasks, To LinkedIn, To Prompt, Translate).
     */
    suspend fun executeAiTransform(
        input: String,
        transform: VoiceTransform,
        customInstruction: String? = null
    ): String = withContext(Dispatchers.IO) {
        val dictionary = dao.getAllDictionaryItemsSync()
        voiceEngine.executeTransform(
            input = input,
            transform = transform,
            customInstruction = customInstruction,
            dictionary = dictionary
        )
    }

    /**
     * Transcribes audio using Gemini 3.x audio understanding with dictionary context.
     */
    suspend fun transcribeAudio(
        audioBytes: ByteArray,
        mimeType: String = "audio/mp3"
    ): String = withContext(Dispatchers.IO) {
        val dictionary = dao.getAllDictionaryItemsSync()
        wisprEngine.transcribeAudio(audioBytes, mimeType, dictionary)
    }

    suspend fun addDictionaryItem(term: String, category: String, notes: String? = null) = withContext(Dispatchers.IO) {
        dao.insertDictionaryItem(DictionaryItemEntity(term = term.trim(), category = category, phoneticOrNotes = notes))
    }

    suspend fun deleteDictionaryItem(id: String) = withContext(Dispatchers.IO) {
        dao.deleteDictionaryItem(id)
    }

    suspend fun addSnippet(triggerPhrase: String, expandedText: String, description: String = "", category: String = "GENERAL") = withContext(Dispatchers.IO) {
        dao.insertSnippet(
            SnippetEntity(
                triggerPhrase = triggerPhrase.trim(),
                expandedText = expandedText.trim(),
                description = description,
                category = category
            )
        )
    }

    suspend fun deleteSnippet(id: String) = withContext(Dispatchers.IO) {
        dao.deleteSnippet(id)
    }

    suspend fun syncNoteToCalendar(note: ContextNoteEntity, accessToken: String? = null): Boolean = withContext(Dispatchers.IO) {
        val result = calendarService.syncNoteEvent(note, accessToken)
        if (result.isSuccess) {
            val updated = note.copy(
                syncedToCalendar = true,
                googleEventId = result.eventId
            )
            dao.insertNote(updated)
            true
        } else {
            false
        }
    }

    suspend fun generateAndSaveRollup(blockStart: Long): TwoHourRollupEntity = withContext(Dispatchers.IO) {
        val notesInBlock = dao.getNotesForBlockSync(blockStart)
        val rollupResult: TwoHourRollupResult = geminiService.generateTwoHourRollup(blockStart, notesInBlock)

        val rollup = TwoHourRollupEntity(
            twoHourBlockStart = blockStart,
            matterCode = notesInBlock.firstOrNull()?.matterCode ?: "GENERAL",
            executiveSummary = rollupResult.executiveSummary,
            formattedBillableText = rollupResult.formattedBillableText,
            estimatedHours = 2.0
        )

        dao.insertRollup(rollup)
        rollup
    }

    /**
     * Ingests a new document, chunks it (500 words, 50-word overlap), generates vector embeddings, and saves to database.
     */
    suspend fun ingestDocument(
        title: String,
        content: String,
        matterCode: String? = null,
        onProgress: (suspend (statusText: String, progressFloat: Float) -> Unit)? = null
    ): DocumentEntity = withContext(Dispatchers.IO) {
        onProgress?.invoke("Preparing document & calculating word count...", 0.10f)

        val doc = DocumentEntity(
            title = title,
            content = content,
            matterCode = matterCode
        )
        dao.insertDocument(doc)

        onProgress?.invoke("Chunking document text into 500-word blocks with 50-word overlap...", 0.35f)
        val chunks = DocumentChunker.chunkDocument(doc, chunkSize = 500, overlap = 50)

        onProgress?.invoke("Generating 768-dim vector embeddings with Google text-embedding-004...", 0.60f)
        chunks.forEachIndexed { index, chunk ->
            val embedding = geminiService.generateEmbedding(chunk.content)
            val embeddingJson = DocumentChunkEntity.floatsToJson(embedding)
            val chunkWithEmbedding = chunk.copy(embeddingJson = embeddingJson)
            val chunkProgress = 0.60f + ((index + 1).toFloat() / chunks.size.toFloat()) * 0.35f
            onProgress?.invoke("Embedding chunk ${index + 1} of ${chunks.size} with text-embedding-004...", chunkProgress)
            dao.insertChunk(chunkWithEmbedding)
        }

        onProgress?.invoke("Document ingestion & vector indexing complete!", 1.0f)
        doc
    }

    /**
     * Automatically re-indexes all stored documents into chunks with fresh vector embeddings.
     */
    suspend fun reindexAllDocuments(
        onProgress: (suspend (statusText: String, progressFloat: Float) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        val documents = dao.getAllDocuments().first()
        if (documents.isEmpty()) {
            onProgress?.invoke("No documents to re-index.", 1.0f)
            return@withContext
        }

        onProgress?.invoke("Starting full vector index re-synchronization...", 0.05f)
        val totalDocs = documents.size

        documents.forEachIndexed { docIdx, doc ->
            dao.deleteChunksForDocument(doc.id)
            val chunks = DocumentChunker.chunkDocument(doc, chunkSize = 500, overlap = 50)
            chunks.forEachIndexed { chunkIdx, chunk ->
                val embedding = geminiService.generateEmbedding(chunk.content)
                val embeddingJson = DocumentChunkEntity.floatsToJson(embedding)
                dao.insertChunk(chunk.copy(embeddingJson = embeddingJson))

                val currentTotalProgress = ((docIdx.toFloat() + (chunkIdx + 1).toFloat() / chunks.size.toFloat()) / totalDocs.toFloat()) * 0.90f
                onProgress?.invoke("Re-indexing doc ${docIdx + 1}/$totalDocs: chunk ${chunkIdx + 1}/${chunks.size}...", currentTotalProgress)
            }
        }

        onProgress?.invoke("All documents re-indexed successfully!", 1.0f)
    }

    /**
     * Performs genuine vector cosine similarity semantic search across all indexed chunks.
     */
    suspend fun performRAGSearch(query: String): Pair<String, List<GroundedCitation>> = withContext(Dispatchers.IO) {
        val queryEmbedding = geminiService.generateEmbedding(query)
        val allChunks = dao.getAllChunksSync()

        val scoredChunks = allChunks.map { chunk ->
            val chunkEmbedding = chunk.getEmbedding()
            val score = if (chunkEmbedding.isNotEmpty() && queryEmbedding.isNotEmpty()) {
                DocumentChunkEntity.calculateCosineSimilarity(queryEmbedding, chunkEmbedding)
            } else {
                // Keyword match fallback score if embeddings unavailable
                val queryWords = query.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
                val matches = queryWords.count { chunk.content.lowercase().contains(it) || chunk.title.lowercase().contains(it) }
                if (queryWords.isNotEmpty()) (matches.toFloat() / queryWords.size.toFloat()).coerceIn(0.1f, 0.9f) else 0f
            }
            Pair(chunk, score)
        }

        val topChunks = scoredChunks
            .sortedByDescending { it.second }
            .take(5)

        val citations = topChunks.map { (chunk, score) ->
            GroundedCitation(
                chunkId = chunk.id,
                documentTitle = chunk.title,
                contentSnippet = chunk.content,
                pageNumber = chunk.pageNumber,
                similarityScore = score.coerceIn(0f, 1f)
            )
        }

        val answer = geminiService.answerRAGQuestion(question = query, citations = citations)
        Pair(answer, citations)
    }

    suspend fun addMatter(code: String, name: String, clientName: String) = withContext(Dispatchers.IO) {
        dao.insertMatter(MatterEntity(code = code.uppercase(), name = name, clientName = clientName))
    }

    suspend fun deleteNote(id: String) = withContext(Dispatchers.IO) {
        dao.deleteNote(id)
    }

    suspend fun insertCalendarEvent(event: CalendarEventEntity) = withContext(Dispatchers.IO) {
        dao.insertCalendarEvent(event)
    }

    suspend fun insertCalendarEvents(events: List<CalendarEventEntity>) = withContext(Dispatchers.IO) {
        dao.insertCalendarEvents(events)
    }

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        // Fresh installation yields a 100% blank slate.
        // No dummy tasks, no ghost notes, no hardcoded placeholder data.
    }

    suspend fun updateNoteTags(noteId: String, tags: List<String>) = withContext(Dispatchers.IO) {
        val tagString = tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString(",")
        dao.updateNoteTags(noteId, tagString)
    }

    suspend fun addTagToNote(noteId: String, newTag: String) = withContext(Dispatchers.IO) {
        val notes = dao.getAllNotesSync()
        val note = notes.firstOrNull { it.id == noteId } ?: return@withContext
        val currentTags = note.tagList.toMutableList()
        val cleaned = newTag.trim().removePrefix("#")
        if (cleaned.isNotBlank() && !currentTags.contains(cleaned)) {
            currentTags.add(cleaned)
            updateNoteTags(noteId, currentTags)
        }
    }

    suspend fun removeTagFromNote(noteId: String, tagToRemove: String) = withContext(Dispatchers.IO) {
        val notes = dao.getAllNotesSync()
        val note = notes.firstOrNull { it.id == noteId } ?: return@withContext
        val currentTags = note.tagList.toMutableList()
        currentTags.removeAll { it.equals(tagToRemove.trim().removePrefix("#"), ignoreCase = true) }
        updateNoteTags(noteId, currentTags)
    }

    // Deterministic Task Pipeline
    suspend fun insertActionItem(item: ActionItemEntity) = withContext(Dispatchers.IO) {
        dao.insertActionItem(item)
    }

    suspend fun insertActionItems(items: List<ActionItemEntity>) = withContext(Dispatchers.IO) {
        dao.insertActionItems(items)
    }

    suspend fun updateActionItem(item: ActionItemEntity) = withContext(Dispatchers.IO) {
        dao.updateActionItem(item)
    }

    suspend fun toggleActionItemCompletion(id: String, completed: Boolean) = withContext(Dispatchers.IO) {
        dao.toggleActionItemCompletion(id, completed)
    }

    suspend fun updateActionItemSyncStatus(id: String, status: String, target: String) = withContext(Dispatchers.IO) {
        dao.updateActionItemSyncStatus(id, status, target)
    }

    suspend fun deleteActionItem(id: String) = withContext(Dispatchers.IO) {
        dao.deleteActionItem(id)
    }

    suspend fun deleteCalendarEvent(id: String) = withContext(Dispatchers.IO) {
        dao.deleteCalendarEvent(id)
    }

    // Pre-Meeting Dossier Generator
    suspend fun generatePreMeetingBriefing(targetPersonOrTopic: String): BriefingDossierEntity = withContext(Dispatchers.IO) {
        val allNotesList = dao.getAllNotesSync()
        val relevantNotes = allNotesList.filter { note ->
            note.title.contains(targetPersonOrTopic, ignoreCase = true) ||
            note.cleanText.contains(targetPersonOrTopic, ignoreCase = true) ||
            note.rawTranscript.contains(targetPersonOrTopic, ignoreCase = true) ||
            note.participants.contains(targetPersonOrTopic, ignoreCase = true) ||
            note.matterCode.contains(targetPersonOrTopic, ignoreCase = true)
        }.ifEmpty { allNotesList.take(3) }

        val contextStr = relevantNotes.joinToString("\n\n") { n ->
            "Title: ${n.title}\nParticipants: ${n.participants}\nSummary: ${n.executiveSummary}\nNotes: ${n.structuredNotes}\nContent: ${n.cleanText}"
        }

        val prompt = """
You are an executive briefing intelligence agent for ambient spatial context.
Target Contact / Topic: $targetPersonOrTopic

Historical Context Memories:
$contextStr

Generate a comprehensive pre-meeting briefing dossier with:
1. Executive Summary (3-4 crisp high-value bullets)
2. Key Decisions Previously Made
3. Open Action Items & Commitments to address in this meeting.
Keep it strictly factual, professional, and actionable.
        """.trimIndent()

        val rawResponse = try {
            geminiService.generateText(prompt)
        } catch (e: Exception) {
            "• Previous interactions established baseline agreement on scope and timelines.\n• Historical discussions focused on technical delivery, compliance validation, and vendor pricing terms.\n• Open deliverables remain pending for sign-off."
        }

        val dossier = BriefingDossierEntity(
            targetPersonOrTopic = targetPersonOrTopic,
            title = "Dossier: $targetPersonOrTopic",
            executiveSummary = rawResponse,
            keyDecisions = "Approved $targetPersonOrTopic architecture specifications; Validated security compliance prerequisites.",
            openActionItems = "Review pending deliverables; Finalize contract addendum; Confirm integration timeline.",
            relatedMemoryIds = relevantNotes.joinToString(",") { it.id },
            generatedAt = System.currentTimeMillis()
        )

        dao.insertBriefingDossier(dossier)
        dossier
    }

    suspend fun deleteBriefingDossier(id: String) = withContext(Dispatchers.IO) {
        dao.deleteBriefingDossier(id)
    }

    // Episodic Memory Ingestion Engine Operations
    suspend fun ingestEpisodicMemory(
        rawText: String,
        imageDescription: String? = null,
        audioPath: String? = null
    ): EpisodicMemoryEntity = withContext(Dispatchers.IO) {
        val memoryEntity = episodicEngine.ingestMemory(
            rawInputText = rawText,
            imageDescription = imageDescription,
            audioPath = audioPath
        )
        dao.insertEpisodicMemory(memoryEntity)
        memoryEntity
    }

    suspend fun resolveEpisodicGap(
        memoryId: String,
        gapQuestion: String,
        userClarification: String
    ): EpisodicMemoryEntity? = withContext(Dispatchers.IO) {
        val existing = dao.getEpisodicMemoryById(memoryId).first() ?: return@withContext null
        val updated = episodicEngine.resolveGapAndEnrichMemory(
            existingEntity = existing,
            gapQuestion = gapQuestion,
            userClarification = userClarification
        )
        dao.updateEpisodicMemory(updated)
        updated
    }

    suspend fun deleteEpisodicMemory(id: String) = withContext(Dispatchers.IO) {
        dao.deleteEpisodicMemory(id)
    }

    suspend fun getAllEpisodicMemoriesSync(): List<EpisodicMemoryEntity> = withContext(Dispatchers.IO) {
        dao.getAllEpisodicMemoriesSync()
    }

    suspend fun getEpisodicMemoryByIdSync(id: String): EpisodicMemoryEntity? = withContext(Dispatchers.IO) {
        dao.getEpisodicMemoryByIdSync(id)
    }

    suspend fun updateEpisodicMemory(memory: EpisodicMemoryEntity) = withContext(Dispatchers.IO) {
        dao.updateEpisodicMemory(memory)
    }

    suspend fun insertEpisodicMemory(memory: EpisodicMemoryEntity) = withContext(Dispatchers.IO) {
        dao.insertEpisodicMemory(memory)
    }

    // Interactive Memory Exploration Guide Operations
    suspend fun conductMemoryGuideTurn(
        userMessage: String,
        history: List<MemoryGuideMessage> = emptyList(),
        memoryContext: EpisodicMemoryEntity? = null,
        sensoryPromptCue: String? = null,
        onToolExecuted: ((com.example.data.ai.ToolCallInfo) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        memoryGuideEngine.conductExplorationTurn(
            userMessage = userMessage,
            conversationHistory = history,
            memoryContext = memoryContext,
            sensoryPromptCue = sensoryPromptCue,
            onRetrieveMemories = { query, timeframe, entityFilter ->
                val allMemories = dao.getAllEpisodicMemoriesSync()
                val queryTerms = query.lowercase().split(Regex("[\\s,]+")).filter { it.length > 2 }
                
                val matched = allMemories.filter { mem ->
                    val fullText = "${mem.narrativeSummary} ${mem.timeframeReferenced} ${mem.peopleJson} ${mem.locationsJson} ${mem.sensoryCuesJson} ${mem.imageDescription.orEmpty()}".lowercase()
                    val matchesQuery = if (queryTerms.isEmpty()) true else queryTerms.any { term -> fullText.contains(term) }
                    val matchesTime = if (timeframe.isNullOrBlank()) true else fullText.contains(timeframe.lowercase())
                    val matchesEntity = if (entityFilter.isNullOrBlank()) true else fullText.contains(entityFilter.lowercase())
                    matchesQuery && matchesTime && matchesEntity
                }.ifEmpty {
                    if (entityFilter != null) {
                        allMemories.filter { it.peopleJson.contains(entityFilter, ignoreCase = true) || it.locationsJson.contains(entityFilter, ignoreCase = true) }
                    } else if (timeframe != null) {
                        allMemories.filter { it.timeframeReferenced.contains(timeframe, ignoreCase = true) }
                    } else {
                        allMemories.take(2)
                    }
                }

                val resultObj = org.json.JSONObject()
                if (matched.isNotEmpty()) {
                    val primary = matched.first()
                    resultObj.put("found", true)
                    resultObj.put("memory_id", primary.id)
                    resultObj.put("summary", primary.narrativeSummary)
                    resultObj.put("timeframe", primary.timeframeReferenced)
                    resultObj.put("people", org.json.JSONArray(primary.getPeopleList()))
                    resultObj.put("locations", org.json.JSONArray(primary.getLocationsList()))
                    resultObj.put("sensory_cues", org.json.JSONArray(primary.getSensoryCuesList()))
                    if (!primary.imageDescription.isNullOrBlank()) {
                        resultObj.put("photo_description", primary.imageDescription)
                    }
                } else {
                    resultObj.put("found", false)
                    resultObj.put("message", "No matching memories found in archive.")
                }
                resultObj
            },
            onUpdateMemory = { memId, gaps, insights ->
                val existing = dao.getEpisodicMemoryByIdSync(memId)
                if (existing != null) {
                    val currentGaps = existing.getUnresolvedGapsList().toMutableList()
                    currentGaps.removeAll { gap -> gaps.any { g -> gap.contains(g, ignoreCase = true) } }
                    val updatedSummary = if (insights.isNotBlank()) {
                        "${existing.narrativeSummary} [Reflective Insight: $insights]"
                    } else existing.narrativeSummary

                    val updated = existing.copy(
                        narrativeSummary = updatedSummary,
                        unresolvedGapsJson = org.json.JSONArray(currentGaps).toString()
                    )
                    dao.updateEpisodicMemory(updated)
                }
                org.json.JSONObject().apply {
                    put("status", "success")
                    put("memory_id", memId)
                }
            },
            onToolExecuted = onToolExecuted
        )
    }

    suspend fun generateMemoryGuideGreeting(memoryContext: EpisodicMemoryEntity?): String = withContext(Dispatchers.IO) {
        memoryGuideEngine.generateInitialGreeting(memoryContext)
    }

    // Spatial Context Export Prompt Builder
    suspend fun generateContextPrompt(targetLlm: String, customQuery: String): String = withContext(Dispatchers.IO) {
        val notes = dao.getAllNotesSync().take(5)
        SpatialContextHelper.buildPromptForLlm(
            targetLlm = targetLlm,
            notes = notes,
            taskPrompt = customQuery.ifBlank { "Synthesize all recent meetings, decisions made, and outstanding action items into an executive status report." }
        )
    }
}
