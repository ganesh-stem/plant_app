package com.verdant.app.ml

import android.graphics.Bitmap
import com.verdant.app.data.model.Prediction

/** Describes a model so the UI can show what is powering results. */
data class ModelInfo(
    val displayName: String,
    val source: String,
    val isPlaceholder: Boolean,
    val classCount: Int? = null,
)

/**
 * Stage 1: works out which plant is in the photo.
 *
 * Implementations must return predictions sorted by confidence (highest first)
 * with confidences in the 0..1 range.
 */
interface PlantIdentifier {
    val info: ModelInfo
    suspend fun identify(bitmap: Bitmap): List<Prediction>
}

/**
 * Stage 2: checks the identified plant for diseases, pests or deficiencies.
 *
 * [plantLabel] is the label from stage 1, so crop-specific models (e.g. PlantVillage
 * labels like `Tomato___Early_blight`) can restrict results to the right plant.
 */
interface DiseaseDetector {
    val info: ModelInfo
    suspend fun detect(bitmap: Bitmap, plantLabel: String?): List<Prediction>
}

class ModelException(message: String, cause: Throwable? = null) : Exception(message, cause)
