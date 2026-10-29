package com.verdant.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.verdant.app.data.model.HealthStatus
import com.verdant.app.data.model.Prediction

/** Stores raw model labels; display text is resolved from the knowledge base at read time. */
@Entity(tableName = "scans", indices = [Index("created_at")])
data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "image_path") val imagePath: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "plant_label") val plantLabel: String,
    @ColumnInfo(name = "plant_confidence") val plantConfidence: Float,
    @ColumnInfo(name = "plant_alternatives") val plantAlternatives: List<Prediction>,
    @ColumnInfo(name = "health_status") val healthStatus: HealthStatus,
    @ColumnInfo(name = "disease_label") val diseaseLabel: String?,
    @ColumnInfo(name = "disease_confidence") val diseaseConfidence: Float?,
    @ColumnInfo(name = "disease_alternatives") val diseaseAlternatives: List<Prediction>,
    @ColumnInfo(name = "is_demo") val isDemo: Boolean,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = false,
)
