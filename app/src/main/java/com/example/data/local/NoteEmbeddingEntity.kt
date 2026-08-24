package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Entity storing vector embeddings for individual context notes and meeting minutes.
 * Enables semantic similarity search (cosine distance) across historical logs.
 */
@Entity(
    tableName = "note_embeddings",
    indices = [Index(value = ["noteId"], unique = true)]
)
data class NoteEmbeddingEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val noteId: String,
    val matterCode: String,
    val textSnippet: String,
    val embeddingJson: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getEmbedding(): List<Float> {
        if (embeddingJson.isBlank() || !embeddingJson.startsWith("[")) return emptyList()
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

data class SemanticNoteSearchResult(
    val noteId: String,
    val matterCode: String,
    val textSnippet: String,
    val similarityScore: Float,
    val note: ContextNoteEntity? = null
)
