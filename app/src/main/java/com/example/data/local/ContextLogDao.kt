package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ContextLogDao {
    // Matters
    @Query("SELECT * FROM matters ORDER BY code ASC")
    fun getAllMatters(): Flow<List<MatterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatter(matter: MatterEntity)

    @Query("SELECT * FROM matters WHERE code = :code LIMIT 1")
    suspend fun getMatterByCode(code: String): MatterEntity?

    // Context Notes
    @Query("SELECT * FROM context_notes ORDER BY recordedAt DESC")
    fun getAllNotes(): Flow<List<ContextNoteEntity>>

    @Query("SELECT * FROM context_notes WHERE twoHourBlockStart = :blockStart ORDER BY recordedAt ASC")
    fun getNotesForBlock(blockStart: Long): Flow<List<ContextNoteEntity>>

    @Query("SELECT * FROM context_notes WHERE twoHourBlockStart = :blockStart ORDER BY recordedAt ASC")
    suspend fun getNotesForBlockSync(blockStart: Long): List<ContextNoteEntity>

    @Query("SELECT * FROM context_notes WHERE matterCode = :matterCode ORDER BY recordedAt DESC")
    fun getNotesForMatter(matterCode: String): Flow<List<ContextNoteEntity>>

    @Query("SELECT DISTINCT twoHourBlockStart FROM context_notes ORDER BY twoHourBlockStart DESC")
    fun getAllBlockStarts(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: ContextNoteEntity)

    @Update
    suspend fun updateNote(note: ContextNoteEntity)

    @Query("UPDATE context_notes SET tags = :tags WHERE id = :id")
    suspend fun updateNoteTags(id: String, tags: String)

    @Query("SELECT * FROM context_notes WHERE tags LIKE '%' || :tag || '%' ORDER BY recordedAt DESC")
    fun getNotesByTag(tag: String): Flow<List<ContextNoteEntity>>

    @Query("SELECT * FROM context_notes WHERE isSyncedToBackend = 0 ORDER BY recordedAt ASC")
    suspend fun getUnsyncedNotes(): List<ContextNoteEntity>

    @Query("UPDATE context_notes SET isSyncedToBackend = 1 WHERE id IN (:ids)")
    suspend fun markNotesSynced(ids: List<String>)

    @Query("SELECT * FROM context_notes ORDER BY recordedAt DESC")
    suspend fun getAllNotesSync(): List<ContextNoteEntity>

    @Query("DELETE FROM context_notes WHERE id = :id")
    suspend fun deleteNote(id: String)

    // Two Hour Rollups
    @Query("SELECT * FROM two_hour_rollups WHERE twoHourBlockStart = :blockStart ORDER BY generatedAt DESC LIMIT 1")
    fun getRollupForBlock(blockStart: Long): Flow<TwoHourRollupEntity?>

    @Query("SELECT * FROM two_hour_rollups ORDER BY twoHourBlockStart DESC")
    fun getAllRollups(): Flow<List<TwoHourRollupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRollup(rollup: TwoHourRollupEntity)

    // Documents and Chunks
    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: DocumentEntity)

    @Update
    suspend fun updateDocument(doc: DocumentEntity)

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: String): DocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunk(chunk: DocumentChunkEntity)

    @Query("SELECT * FROM document_chunks WHERE content LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%'")
    suspend fun searchChunks(query: String): List<DocumentChunkEntity>

    @Query("SELECT * FROM document_chunks ORDER BY createdAt DESC")
    fun getAllChunksFlow(): Flow<List<DocumentChunkEntity>>

    @Query("SELECT * FROM document_chunks ORDER BY createdAt DESC")
    suspend fun getAllChunksSync(): List<DocumentChunkEntity>

    @Query("SELECT * FROM document_chunks ORDER BY createdAt DESC LIMIT 20")
    suspend fun getAllChunks(): List<DocumentChunkEntity>

    @Query("SELECT COUNT(*) FROM document_chunks")
    fun getChunkCount(): Flow<Int>

    @Query("DELETE FROM document_chunks WHERE documentId = :documentId")
    suspend fun deleteChunksForDocument(documentId: String)

    // Personal Dictionary
    @Query("SELECT * FROM personal_dictionary ORDER BY term ASC")
    fun getAllDictionaryItems(): Flow<List<DictionaryItemEntity>>

    @Query("SELECT * FROM personal_dictionary ORDER BY term ASC")
    suspend fun getAllDictionaryItemsSync(): List<DictionaryItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDictionaryItem(item: DictionaryItemEntity)

    @Query("DELETE FROM personal_dictionary WHERE id = :id")
    suspend fun deleteDictionaryItem(id: String)

    // Snippets
    @Query("SELECT * FROM snippets ORDER BY triggerPhrase ASC")
    fun getAllSnippets(): Flow<List<SnippetEntity>>

    @Query("SELECT * FROM snippets ORDER BY triggerPhrase ASC")
    suspend fun getAllSnippetsSync(): List<SnippetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnippet(snippet: SnippetEntity)

    @Query("DELETE FROM snippets WHERE id = :id")
    suspend fun deleteSnippet(id: String)

    // Calendar Events (Local zero-latency caching)
    @Query("SELECT * FROM calendar_events ORDER BY startTime ASC")
    fun getAllCalendarEvents(): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events ORDER BY startTime ASC")
    suspend fun getAllCalendarEventsSync(): List<CalendarEventEntity>

    @Query("SELECT * FROM calendar_events WHERE startTime >= :fromTime ORDER BY startTime ASC")
    fun getUpcomingCalendarEvents(fromTime: Long): Flow<List<CalendarEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendarEvents(events: List<CalendarEventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendarEvent(event: CalendarEventEntity)

    @Query("DELETE FROM calendar_events WHERE id = :id OR googleEventId = :id")
    suspend fun deleteCalendarEvent(id: String)

    @Query("DELETE FROM calendar_events")
    suspend fun clearAllCalendarEvents()

    // FTS & Full-Text Search across Context Notes
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteFts(fts: ContextNoteFtsEntity)

    @Query("SELECT * FROM context_notes WHERE id IN (SELECT noteId FROM context_notes_fts WHERE context_notes_fts MATCH :query)")
    suspend fun searchNotesFts(query: String): List<ContextNoteEntity>

    @Query("SELECT * FROM context_notes WHERE cleanText LIKE '%' || :query || '%' OR rawTranscript LIKE '%' || :query || '%' OR matterCode LIKE '%' || :query || '%' ORDER BY recordedAt DESC")
    suspend fun searchNotesText(query: String): List<ContextNoteEntity>

    // Note Vector Embeddings (Semantic Search)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteEmbedding(embedding: NoteEmbeddingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteEmbeddings(embeddings: List<NoteEmbeddingEntity>)

    @Query("SELECT * FROM note_embeddings")
    fun getAllNoteEmbeddings(): Flow<List<NoteEmbeddingEntity>>

    @Query("SELECT * FROM note_embeddings")
    suspend fun getAllNoteEmbeddingsSync(): List<NoteEmbeddingEntity>

    @Query("SELECT * FROM note_embeddings WHERE noteId = :noteId LIMIT 1")
    suspend fun getEmbeddingForNote(noteId: String): NoteEmbeddingEntity?

    @Query("DELETE FROM note_embeddings WHERE noteId = :noteId")
    suspend fun deleteEmbeddingForNote(noteId: String)

    @Query("DELETE FROM note_embeddings")
    suspend fun clearAllNoteEmbeddings()

    // Token Usage Metrics & Billing
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTokenUsage(metric: TokenUsageEntity)

    @Query("SELECT * FROM token_usage_metrics ORDER BY timestamp DESC")
    fun getAllTokenUsage(): Flow<List<TokenUsageEntity>>

    @Query("SELECT * FROM token_usage_metrics ORDER BY timestamp DESC")
    suspend fun getAllTokenUsageSync(): List<TokenUsageEntity>

    @Query("SELECT * FROM token_usage_metrics WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    fun getTokenUsageSince(sinceTimestamp: Long): Flow<List<TokenUsageEntity>>

    @Query("SELECT * FROM token_usage_metrics WHERE dateString = :dateString ORDER BY timestamp DESC")
    fun getTokenUsageForDate(dateString: String): Flow<List<TokenUsageEntity>>

    @Query("SELECT SUM(totalTokens) FROM token_usage_metrics")
    fun getTotalTokenCountFlow(): Flow<Int?>

    @Query("SELECT SUM(estimatedCostUsd) FROM token_usage_metrics")
    fun getTotalEstimatedCostFlow(): Flow<Double?>

    @Query("DELETE FROM token_usage_metrics WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOldTokenUsage(beforeTimestamp: Long)

    // Deterministic Action Items / Tasks
    @Query("SELECT * FROM action_items ORDER BY targetDueDate ASC, extractedAt DESC")
    fun getAllActionItems(): Flow<List<ActionItemEntity>>

    @Query("SELECT * FROM action_items ORDER BY targetDueDate ASC, extractedAt DESC")
    suspend fun getAllActionItemsSync(): List<ActionItemEntity>

    @Query("SELECT * FROM action_items WHERE isCompleted = 0 ORDER BY targetDueDate ASC")
    fun getPendingActionItems(): Flow<List<ActionItemEntity>>

    @Query("SELECT * FROM action_items WHERE isAssignedToYou = 1 ORDER BY targetDueDate ASC")
    fun getActionItemsAssignedToYou(): Flow<List<ActionItemEntity>>

    @Query("SELECT * FROM action_items WHERE memoryId = :memoryId ORDER BY extractedAt ASC")
    fun getActionItemsForMemory(memoryId: String): Flow<List<ActionItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActionItem(item: ActionItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActionItems(items: List<ActionItemEntity>)

    @Update
    suspend fun updateActionItem(item: ActionItemEntity)

    @Query("UPDATE action_items SET isCompleted = :completed WHERE id = :id")
    suspend fun toggleActionItemCompletion(id: String, completed: Boolean)

    @Query("UPDATE action_items SET externalSyncStatus = :status, externalSyncTarget = :target WHERE id = :id")
    suspend fun updateActionItemSyncStatus(id: String, status: String, target: String)

    @Query("DELETE FROM action_items WHERE id = :id")
    suspend fun deleteActionItem(id: String)

    // Pre-Meeting Briefing Dossiers
    @Query("SELECT * FROM briefing_dossiers ORDER BY generatedAt DESC")
    fun getAllBriefingDossiers(): Flow<List<BriefingDossierEntity>>

    @Query("SELECT * FROM briefing_dossiers WHERE targetPersonOrTopic = :target LIMIT 1")
    suspend fun getBriefingDossier(target: String): BriefingDossierEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBriefingDossier(dossier: BriefingDossierEntity)

    @Query("DELETE FROM briefing_dossiers WHERE id = :id")
    suspend fun deleteBriefingDossier(id: String)
}
