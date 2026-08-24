package com.example.data.ai

import com.example.data.local.DocumentChunkEntity
import com.example.data.local.DocumentEntity
import java.util.UUID

object DocumentChunker {

    /**
     * Splits document text into chunks of up to chunkSize words with overlap words overlap.
     * Default: 500 words per chunk, 50 words overlap.
     */
    fun chunkDocument(
        document: DocumentEntity,
        chunkSize: Int = 500,
        overlap: Int = 50
    ): List<DocumentChunkEntity> {
        val words = document.content.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (words.isEmpty()) {
            return listOf(
                DocumentChunkEntity(
                    id = UUID.randomUUID().toString(),
                    documentId = document.id,
                    title = document.title,
                    content = document.content,
                    pageNumber = 1
                )
            )
        }

        val chunks = mutableListOf<DocumentChunkEntity>()
        var start = 0
        var pageIndex = 1
        val wordsPerPage = 250 // Approximate 250 words per page

        while (start < words.size) {
            val end = (start + chunkSize).coerceAtMost(words.size)
            val chunkWords = words.subList(start, end)
            val chunkText = chunkWords.joinToString(" ")
            val pageNum = (start / wordsPerPage) + 1

            chunks.add(
                DocumentChunkEntity(
                    id = UUID.randomUUID().toString(),
                    documentId = document.id,
                    title = document.title,
                    content = chunkText,
                    pageNumber = pageNum
                )
            )

            if (end == words.size) break
            start += (chunkSize - overlap).coerceAtLeast(1)
            pageIndex++
        }

        return chunks
    }
}
