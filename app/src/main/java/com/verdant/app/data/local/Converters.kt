package com.verdant.app.data.local

import androidx.room.TypeConverter
import com.verdant.app.data.model.HealthStatus
import com.verdant.app.data.model.Prediction
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun predictionsToJson(value: List<Prediction>): String = json.encodeToString(value)

    @TypeConverter
    fun jsonToPredictions(value: String): List<Prediction> =
        runCatching { json.decodeFromString<List<Prediction>>(value) }.getOrDefault(emptyList())

    @TypeConverter
    fun healthToString(value: HealthStatus): String = value.name

    @TypeConverter
    fun stringToHealth(value: String): HealthStatus =
        runCatching { HealthStatus.valueOf(value) }.getOrDefault(HealthStatus.UNKNOWN)
}
