package com.verdant.app.ml.tflite

import android.content.Context
import android.graphics.Bitmap
import com.verdant.app.data.knowledge.KnowledgeBase
import com.verdant.app.data.model.Prediction
import com.verdant.app.ml.DiseaseDetector
import com.verdant.app.ml.LabelUtils
import com.verdant.app.ml.ModelConfig
import com.verdant.app.ml.ModelInfo

/**
 * Wraps the disease model. When labels follow the PlantVillage `Plant___Condition`
 * convention, results are limited to the plant found in stage 1, which removes
 * impossible answers such as "apple scab" on a tomato.
 */
class TfLiteDiseaseDetector(
    context: Context,
    private val knowledgeBase: KnowledgeBase,
) : DiseaseDetector {

    private val classifier = TfLiteImageClassifier(
        context = context,
        modelPath = ModelConfig.DISEASE_MODEL_PATH,
        labelsPath = ModelConfig.DISEASE_LABELS_PATH,
        normalization = ModelConfig.DISEASE_NORMALIZATION,
    )

    override val info: ModelInfo
        get() = ModelInfo(
            displayName = "Disease detector",
            source = ModelConfig.DISEASE_MODEL_PATH.substringAfterLast('/'),
            isPlaceholder = false,
            classCount = classifier.classCount,
        )

    override suspend fun detect(bitmap: Bitmap, plantLabel: String?): List<Prediction> {
        val (scores, labels) = classifier.scores(bitmap)
        val all = LabelUtils.topK(scores, labels, labels.size)
        val plantId = plantLabel?.let { knowledgeBase.findPlant(it)?.id }

        val filtered = if (plantId != null) {
            all.filter { prediction ->
                val prefix = LabelUtils.splitCompound(prediction.label)?.first ?: return@filter false
                knowledgeBase.findPlant(prefix)?.id == plantId
            }
        } else {
            emptyList()
        }
        // Fall back to every class if the model doesn't cover this plant.
        val result = if (filtered.isNotEmpty()) LabelUtils.renormalize(filtered) else all
        return result.take(ModelConfig.TOP_K)
    }
}
