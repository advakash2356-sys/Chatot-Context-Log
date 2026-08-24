package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val matterCode: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "document_chunks")
data class DocumentChunkEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val documentId: String,
    val title: String,
    val content: String,
    val pageNumber: Int = 1,
    val embeddingJson: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getEmbedding(): List<Float> {
        if (embeddingJson.isNullOrBlank() || !embeddingJson.startsWith("[")) return emptyList()
        val clean = embeddingJson.removePrefix("[").removeSuffix("]")
        if (clean.isBlank()) return emptyList()
        return clean.split(",").mapNotNull { it.trim().toFloatOrNull() }
    }

    companion object {
        fun floatsToJson(floats: List<Float>): String {
            val sb = StringBuilder("[")
            for (i in floats.indices) {
                sb.append(floats[i])
                if (i < floats.size - 1) sb.append(",")
            }
            sb.append("]")
            return sb.toString()
        }

        fun calculateCosineSimilarity(vecA: List<Float>, vecB: List<Float>): Float {
            if (vecA.isEmpty() || vecB.isEmpty() || vecA.size != vecB.size) return 0f
            var dotProduct = 0.0
            var normA = 0.0
            var normB = 0.0
            for (i in vecA.indices) {
                val a = vecA[i].toDouble()
                val b = vecB[i].toDouble()
                dotProduct += a * b
                normA += a * a
                normB += b * b
            }
            if (normA <= 0.0 || normB <= 0.0) return 0f
            return (dotProduct / (Math.sqrt(normA) * Math.sqrt(normB))).toFloat()
        }
    }
}

data class GroundedCitation(
    val chunkId: String,
    val documentTitle: String,
    val contentSnippet: String,
    val pageNumber: Int,
    val similarityScore: Float
)

