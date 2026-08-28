package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "episodic_memories")
data class EpisodicMemoryEntity(
    @PrimaryKey
    val id: String, // memory_id
    val timestampRecorded: Long,
    val isoTimestamp: String,
    val timeframeReferenced: String,
    val relativeLifeStage: String,
    val peopleJson: String,
    val locationsJson: String,
    val physicalObjectsJson: String,
    val primaryTone: String,
    val emotionalValence: String, // "positive", "neutral", "negative", "bittersweet", "melancholic"
    val notableShifts: String,
    val sensoryCuesJson: String,
    val narrativeSummary: String,
    val unresolvedGapsJson: String,
    val searchKeywordsJson: String,
    val rawInputText: String,
    val imageDescription: String? = null,
    val audioPath: String? = null,
    val isEnrichedWithProbe: Boolean = false
) {
    fun getPeopleList(): List<String> = parseJsonArray(peopleJson)
    fun getLocationsList(): List<String> = parseJsonArray(locationsJson)
    fun getPhysicalObjectsList(): List<String> = parseJsonArray(physicalObjectsJson)
    fun getSensoryCuesList(): List<String> = parseJsonArray(sensoryCuesJson)
    fun getUnresolvedGapsList(): List<String> = parseJsonArray(unresolvedGapsJson)
    fun getSearchKeywordsList(): List<String> = parseJsonArray(searchKeywordsJson)

    companion object {
        private fun parseJsonArray(jsonStr: String): List<String> {
            if (jsonStr.isBlank()) return emptyList()
            return try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    val item = array.optString(i)
                    if (item.isNotBlank()) list.add(item)
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }

        fun toJsonArrayString(items: List<String>): String {
            val array = JSONArray()
            items.forEach { array.put(it) }
            return array.toString()
        }
    }
}
