package com.verdant.app.data.model

import kotlinx.serialization.Serializable

/** A single class score produced by a classifier. */
@Serializable
data class Prediction(
    val label: String,
    val confidence: Float,
)

enum class HealthStatus { HEALTHY, DISEASED, UNKNOWN }

enum class Severity { LOW, MODERATE, HIGH }

enum class Difficulty { EASY, MODERATE, EXPERT }

enum class Pathogen { FUNGAL, BACTERIAL, VIRAL, OOMYCETE, PEST, NUTRIENT, ENVIRONMENTAL, UNKNOWN }

@Serializable
data class CareGuide(
    val water: String,
    val light: String,
    val temperature: String,
    val humidity: String,
    val soil: String,
    val fertilizer: String,
)

@Serializable
data class PlantInfo(
    val id: String,
    val commonName: String,
    val scientificName: String,
    val family: String,
    val aliases: List<String> = emptyList(),
    val description: String,
    val difficulty: Difficulty = Difficulty.MODERATE,
    val care: CareGuide? = null,
    val toxicity: String? = null,
    val funFact: String? = null,
)

@Serializable
data class DiseaseInfo(
    val id: String,
    val name: String,
    val pathogen: Pathogen = Pathogen.UNKNOWN,
    val severity: Severity = Severity.MODERATE,
    val contagious: Boolean = false,
    val aliases: List<String> = emptyList(),
    val hosts: List<String> = emptyList(),
    val description: String,
    val symptoms: List<String> = emptyList(),
    val treatment: List<String> = emptyList(),
    val prevention: List<String> = emptyList(),
)

/** An alternative match shown under "Other possible matches". */
data class PlantMatch(
    val label: String,
    val commonName: String,
    val scientificName: String?,
    val confidence: Float,
)

/** A fully resolved scan, ready for display. */
data class ScanRecord(
    val id: Long,
    val imagePath: String,
    val createdAt: Long,
    val plantLabel: String,
    val plant: PlantInfo,
    /** False when the model returned a label we have no care data for. */
    val hasPlantDetails: Boolean,
    val plantConfidence: Float,
    val alternatives: List<PlantMatch>,
    val health: HealthStatus,
    val diseaseLabel: String?,
    val disease: DiseaseInfo?,
    val diseaseConfidence: Float?,
    val isDemo: Boolean,
    val isFavorite: Boolean,
) {
    val needsAttention: Boolean get() = health == HealthStatus.DISEASED
}

data class ScanStats(
    val total: Int = 0,
    val healthy: Int = 0,
    val needsAttention: Int = 0,
)
