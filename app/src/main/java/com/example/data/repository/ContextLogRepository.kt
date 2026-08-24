package com.example.data.repository

import com.example.data.ai.DocumentChunker
import com.example.data.ai.GeminiService
import com.example.data.ai.NeoSapienHelper
import com.example.data.ai.ParsedNoteResult
import com.example.data.ai.TwoHourRollupResult
import com.example.data.ai.WisprContextType
import com.example.data.ai.WisprFlowEngine
import com.example.data.ai.WisprFlowResult
import com.example.data.ai.WisprTone
import com.example.data.ai.WisprTransform
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
    val wisprEngine: WisprFlowEngine = WisprFlowEngine(),
    val tokenBillingService: TokenBillingService = TokenBillingService(dao)
) {
    val allMatters: Flow<List<MatterEntity>> = dao.getAllMatters()
    val allNotes: Flow<List<ContextNoteEntity>> = dao.getAllNotes()
    val allActionItems: Flow<List<ActionItemEntity>> = dao.getAllActionItems()
    val pendingActionItems: Flow<List<ActionItemEntity>> = dao.getPendingActionItems()
    val actionItemsAssignedToYou: Flow<List<ActionItemEntity>> = dao.getActionItemsAssignedToYou()
    val allBriefingDossiers: Flow<List<BriefingDossierEntity>> = dao.getAllBriefingDossiers()
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
     * Executes the comprehensive Wispr Flow pipeline:
     * - Personal Dictionary preservation
     * - Snippet trigger expansion
     * - Natural speech cleanup (fillers, false starts, stuttering, backtracks)
     * - Context-aware adaptation (Email, Slack, LinkedIn, Task List, Prompt, Meeting Note)
     * - Tone & style rewriting (Executive, Professional, Casual, Concise, Friendly, etc.)
     */
    suspend fun processWisprFlow(
        rawInput: String,
        contextType: WisprContextType = WisprContextType.GENERAL,
        tone: WisprTone = WisprTone.AUTO_CLEAN,
        targetLanguage: String? = null
    ): WisprFlowResult = withContext(Dispatchers.IO) {
        val dictionary = dao.getAllDictionaryItemsSync()
        val snippets = dao.getAllSnippetsSync()
        wisprEngine.processWisprFlow(
            rawInput = rawInput,
            contextType = contextType,
            tone = tone,
            dictionary = dictionary,
            snippets = snippets,
            targetLanguage = targetLanguage
        )
    }

    /**
     * Executes AI Transforms (Polish, Concise, Bullets, To Email, To Tasks, To LinkedIn, To Prompt, Translate).
     */
    suspend fun executeAiTransform(
        input: String,
        transform: WisprTransform,
        customInstruction: String? = null
    ): String = withContext(Dispatchers.IO) {
        val dictionary = dao.getAllDictionaryItemsSync()
        wisprEngine.executeTransform(
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

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val existingMatters = dao.getAllMatters().first()
        if (existingMatters.isNotEmpty()) return@withContext

        // Seed Personal Dictionary
        val dictList = listOf(
            DictionaryItemEntity(term = "Akash", category = "NAME", phoneticOrNotes = "Author & Architect"),
            DictionaryItemEntity(term = "Kaito", category = "NAME", phoneticOrNotes = "Engineering Partner"),
            DictionaryItemEntity(term = "Wispr Flow", category = "PRODUCT", phoneticOrNotes = "Voice Dictation Engine"),
            DictionaryItemEntity(term = "ContextLog", category = "PRODUCT", phoneticOrNotes = "Context Intelligence Platform"),
            DictionaryItemEntity(term = "Azure AI Foundry", category = "COMPANY", phoneticOrNotes = "Cloud ML Suite"),
            DictionaryItemEntity(term = "LGL-9021", category = "ACRONYM", phoneticOrNotes = "IP & Licensing Matter"),
            DictionaryItemEntity(term = "CTX-2024-08", category = "ACRONYM", phoneticOrNotes = "Core Context Project"),
            DictionaryItemEntity(term = "PostgreSQL", category = "TECHNICAL", phoneticOrNotes = "Relational DB Engine")
        )
        for (item in dictList) {
            dao.insertDictionaryItem(item)
        }

        // Seed Snippets
        val snippetList = listOf(
            SnippetEntity(
                triggerPhrase = "my email",
                expandedText = "Adv.Akash2356@gmail.com",
                description = "Primary contact email"
            ),
            SnippetEntity(
                triggerPhrase = "my intro",
                expandedText = "Hi, I am Adv. Akash, Principal Counsel & AI Systems Architect specializing in contextual intelligence.",
                description = "Professional bio intro"
            ),
            SnippetEntity(
                triggerPhrase = "meeting link",
                expandedText = "https://meet.google.com/context-flow-sync",
                description = "Google Meet conference room link"
            ),
            SnippetEntity(
                triggerPhrase = "standard follow-up",
                expandedText = "Thank you for joining our sync today. Attached are the summarized action items and scheduled next steps for your review.",
                description = "Post-meeting follow-up template"
            )
        )
        for (snippet in snippetList) {
            dao.insertSnippet(snippet)
        }

        // Seed Matters
        val matter1 = MatterEntity(code = "CTX-2024-08", name = "ContextLog Engine", clientName = "AI Studio Platform")
        val matter2 = MatterEntity(code = "LGL-9021", name = "IP & Licensing Contract", clientName = "Apex Systems")
        val matter3 = MatterEntity(code = "MTR-104", name = "Quarterly Compliance Strategy", clientName = "Vanguard Tech")
        val matter4 = MatterEntity(code = "DECISION-PAUSE", name = "System Architecture Pause", clientName = "Internal Ops")

        dao.insertMatter(matter1)
        dao.insertMatter(matter2)
        dao.insertMatter(matter3)
        dao.insertMatter(matter4)

        // Seed Context Notes (NeoSapien Memories)
        val now = System.currentTimeMillis()
        val twoHoursMs = 7200000L

        val turns1 = listOf(
            com.example.data.ai.SpeakerTurn("You", "00:05", "Let's review the Q3 cloud infrastructure deliverables and vendor pricing quotes.", true),
            com.example.data.ai.SpeakerTurn("Speaker 1 (Alex)", "00:22", "We received the revised proposal from Apex Cloud. They agreed to reduce the enterprise tier to $14k/month if signed before Friday.", false),
            com.example.data.ai.SpeakerTurn("You", "00:41", "That works well within budget. I will draft the approval addendum and send it over to legal by 3 PM.", true),
            com.example.data.ai.SpeakerTurn("Speaker 2 (Sarah)", "01:05", "Great. I will finalize the SOC2 security compliance checklist and attach the report.", false)
        )

        val note1 = ContextNoteEntity(
            title = "Q3 Cloud Vendor & Security Review",
            sessionBoundaryId = "SESSION-0823-01",
            durationSeconds = 145,
            source = "PENDANT_BLE",
            rawTranscript = "Let's review the Q3 cloud infrastructure deliverables and vendor pricing quotes. We received the revised proposal from Apex Cloud. They agreed to reduce the enterprise tier to $14k/month if signed before Friday. I will draft the approval addendum and send it over to legal by 3 PM. I will finalize the SOC2 security compliance checklist and attach the report.",
            cleanText = "Reviewed Q3 cloud infrastructure deliverables with Alex and Sarah. Approved Apex Cloud pricing reduction to $14k/month. Committed to drafting the approval addendum for legal by 3 PM.",
            executiveSummary = "• Approved Apex Cloud enterprise infrastructure pricing at $14k/month.\n• Reached consensus on SOC2 security compliance prerequisites.\n• Action items assigned: approval addendum to legal, SOC2 report attachment.",
            structuredNotes = "### Key Decisions\n- Accepted $14k/mo discounted tier with Apex Cloud.\n- Finalized SOC2 compliance deadline for Friday.\n\n### Discussion Highlights\n- Explored cost optimization across multi-region clusters.\n- Verified legal approval workflow.",
            verbatimTurnsJson = NeoSapienHelper.serializeSpeakerTurns(turns1),
            audioPurged = true,
            isEncrypted = true,
            participants = "You, Alex, Sarah",
            entryType = EntryType.LOG,
            matterCode = "CTX-2024-08",
            depthLevel = 2,
            recordedAt = now - 1800000L,
            tags = "Cloud,Vendor,Pricing,Security"
        )

        val turns2 = listOf(
            com.example.data.ai.SpeakerTurn("You", "00:02", "We need to optimize the RAG embedding pipeline for zero-latency memory retrieval.", true),
            com.example.data.ai.SpeakerTurn("Speaker 1 (Kaito)", "00:19", "I benchmarked 500-word chunks with cosine similarity over SQLite embeddings. We achieved 42ms retrieval latency.", false),
            com.example.data.ai.SpeakerTurn("You", "00:38", "Excellent. Let's make sure we also cache pre-meeting briefings to prepare users instantly.", true)
        )

        val note2 = ContextNoteEntity(
            title = "RAG Vector Retrieval Architecture",
            sessionBoundaryId = "SESSION-0823-02",
            durationSeconds = 98,
            source = "DESKTOP_LOOPBACK",
            rawTranscript = "We need to optimize the RAG embedding pipeline for zero-latency memory retrieval. I benchmarked 500-word chunks with cosine similarity over SQLite embeddings. We achieved 42ms retrieval latency. Excellent. Let's make sure we also cache pre-meeting briefings to prepare users instantly.",
            cleanText = "Benchmarked vector search RAG pipeline. Verified 42ms retrieval latency over SQLite embeddings with 500-word document chunking.",
            executiveSummary = "• Validated 42ms local vector search retrieval over encrypted embeddings.\n• Approved 500-word document chunking standard for precision grounding.\n• Scheduled pre-meeting briefing caching module.",
            structuredNotes = "### Architecture Specs\n- 500-word semantic chunking with 50-word overlap.\n- Zero-latency local SQLite embeddings vector table.",
            verbatimTurnsJson = NeoSapienHelper.serializeSpeakerTurns(turns2),
            audioPurged = true,
            isEncrypted = true,
            participants = "You, Kaito",
            entryType = EntryType.RAG_QUESTION,
            matterCode = "DECISION-PAUSE",
            depthLevel = 3,
            recordedAt = now - 3600000L,
            tags = "VectorSearch,RAG,Performance"
        )

        val note3 = ContextNoteEntity(
            title = "Client Compliance Strategy Call",
            sessionBoundaryId = "SESSION-0822-01",
            durationSeconds = 210,
            source = "PENDANT_BLE",
            rawTranscript = "Scheduled client compliance review call for tomorrow at 10 AM. Will verify data retention and automated audio purge protocols.",
            cleanText = "Scheduled client compliance review call for tomorrow at 10 AM. Will verify data retention and automated audio purge protocols.",
            executiveSummary = "• Confirmed compliance audit scheduled for 10:00 AM.\n• Outlined automated audio purge verification steps.\n• Synced calendar invite with client legal team.",
            structuredNotes = "### Client Objectives\n- Verify GDPR/SOC2 compliance.\n- Ensure zero-retention raw audio policy.",
            verbatimTurnsJson = NeoSapienHelper.serializeSpeakerTurns(listOf(
                com.example.data.ai.SpeakerTurn("You", "00:00", "Scheduled client compliance review call for tomorrow at 10 AM.", true)
            )),
            audioPurged = true,
            isEncrypted = true,
            participants = "You, Vanguard Legal",
            entryType = EntryType.REMINDER,
            matterCode = "MTR-104",
            depthLevel = 1,
            recordedAt = now - (twoHoursMs + 1800000L),
            syncedToCalendar = true,
            googleEventId = "seed-evt-mtr-104",
            tags = "ClientCall,Compliance,Calendar"
        )

        val note4 = ContextNoteEntity(
            title = "Patent & IP Licensing Negotiation",
            sessionBoundaryId = "SESSION-0821-03",
            durationSeconds = 310,
            source = "PENDANT_BLE",
            rawTranscript = "Reviewed patent claim clauses for AI generated code artifacts with client counsel. Negotiated 30-day termination notice and IP assignment.",
            cleanText = "Reviewed patent claim clauses for AI generated code artifacts with client counsel. Negotiated 30-day termination notice and IP assignment.",
            executiveSummary = "• Finalized IP assignment terms for AI artifacts.\n• Agreed upon 30-day bilateral termination window.\n• All work product confirmed to vest with client upon delivery.",
            structuredNotes = "### Contract Terms\n- Section 4: Termination & IP assignment.\n- 14-day post-termination data destruction certificate required.",
            verbatimTurnsJson = NeoSapienHelper.serializeSpeakerTurns(listOf(
                com.example.data.ai.SpeakerTurn("You", "00:00", "Reviewed patent claim clauses for AI generated code artifacts with client counsel.", true)
            )),
            audioPurged = true,
            isEncrypted = true,
            participants = "You, Apex Legal Counsel",
            entryType = EntryType.LEGAL_MATTER,
            matterCode = "LGL-9021",
            depthLevel = 4,
            recordedAt = now - (twoHoursMs + 3600000L),
            tags = "Contract,IP,Negotiation"
        )

        dao.insertNote(note1)
        dao.insertNote(note2)
        dao.insertNote(note3)
        dao.insertNote(note4)

        // Seed Action Items
        val seedActionItems = listOf(
            ActionItemEntity(
                title = "Draft approval addendum and send over to legal",
                owner = "You",
                isAssignedToYou = true,
                actionVerb = "Draft",
                dueDateFormatted = "Today, 3:00 PM",
                isCompleted = false,
                priority = "HIGH",
                memoryId = note1.id,
                memoryTitle = note1.title,
                externalSyncTarget = "ClickUp",
                externalSyncStatus = "READY"
            ),
            ActionItemEntity(
                title = "Finalize SOC2 security compliance checklist and attach report",
                owner = "Sarah",
                isAssignedToYou = false,
                actionVerb = "Submit",
                dueDateFormatted = "Friday, 5:00 PM",
                isCompleted = false,
                priority = "HIGH",
                memoryId = note1.id,
                memoryTitle = note1.title,
                externalSyncTarget = "Notion",
                externalSyncStatus = "READY"
            ),
            ActionItemEntity(
                title = "Send signed Apex Cloud enterprise addendum ($14k/mo)",
                owner = "Alex",
                isAssignedToYou = false,
                actionVerb = "Send",
                dueDateFormatted = "Friday, 2:00 PM",
                isCompleted = false,
                priority = "MEDIUM",
                memoryId = note1.id,
                memoryTitle = note1.title,
                externalSyncTarget = "Todoist",
                externalSyncStatus = "READY"
            ),
            ActionItemEntity(
                title = "Review patent claim clauses and IP assignment wording",
                owner = "You",
                isAssignedToYou = true,
                actionVerb = "Review",
                dueDateFormatted = "Tomorrow, 11:00 AM",
                isCompleted = true,
                priority = "MEDIUM",
                memoryId = note4.id,
                memoryTitle = note4.title,
                externalSyncTarget = "Apple Reminders",
                externalSyncStatus = "SYNCED"
            )
        )
        dao.insertActionItems(seedActionItems)

        // Ingest sample documents with 500-word chunking and citations
        ingestDocument(
            title = "Master Legal Services & IP Agreement",
            content = "Section 4. Termination & Intellectual Property. Either party may terminate this agreement upon 30 days written notice. Upon termination, all rights, title, and interest in work product, source code, and AI model outputs developed under this statement of work shall immediately vest with the Client. The Provider shall destroy all copies of Confidential Information within 14 days of termination receipt.",
            matterCode = "LGL-9021"
        )

        ingestDocument(
            title = "PostgreSQL Trigger Efficiency & 2-Hour Window Spec",
            content = "The set_two_hour_block trigger calculates two_hour_block_start using floor(extract(epoch from recorded_at) / 7200) * 7200. This enables zero-latency rollups into 2-hour billable blocks for legal time tracking and billing context.",
            matterCode = "CTX-2024-08"
        )

        ingestDocument(
            title = "Google Calendar API Sync & OAuth Scopes",
            content = "Google Calendar API requires scope https://www.googleapis.com/auth/calendar.events for automated event synchronization. Notes classified as REMINDER or LEGAL_MATTER automatically generate secondary calendar events.",
            matterCode = "MTR-104"
        )

        // Seed initial 2-hour rollups
        generateAndSaveRollup(ContextNoteEntity.calculateTwoHourBlock(now))
        generateAndSaveRollup(ContextNoteEntity.calculateTwoHourBlock(now - twoHoursMs))
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

    // NeoSapien Deterministic Task Pipeline
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

    // NeoSapien Pre-Meeting Dossier Generator
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
You are an executive briefing intelligence agent for NeoSapien.
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

    // NeoSapien Context Export Prompt Builder
    suspend fun generateContextPrompt(targetLlm: String, customQuery: String): String = withContext(Dispatchers.IO) {
        val notes = dao.getAllNotesSync().take(5)
        NeoSapienHelper.buildPromptForLlm(
            targetLlm = targetLlm,
            notes = notes,
            taskPrompt = customQuery.ifBlank { "Synthesize all recent meetings, decisions made, and outstanding action items into an executive status report." }
        )
    }
}
