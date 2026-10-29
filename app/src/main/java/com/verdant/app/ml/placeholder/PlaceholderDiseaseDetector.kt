package com.verdant.app.ml.placeholder

import android.graphics.Bitmap
import com.verdant.app.data.knowledge.KnowledgeBase
import com.verdant.app.data.model.Prediction
import com.verdant.app.ml.DiseaseDetector
import com.verdant.app.ml.ModelInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Stand-in for the real disease model. About 40% of photos come back healthy; the
 * rest get a condition that actually affects the identified plant.
 *
 * Replace by adding `models/disease_detector.tflite` + `models/disease_labels.txt`.
 */
class PlaceholderDiseaseDetector(
    private val knowledgeBase: KnowledgeBase,
    private val simulatedLatencyMs: Long = 1_300,
) : DiseaseDetector {

    override val info = ModelInfo(
        displayName = "Disease detector",
        source = "Placeholder (simulated)",
        isPlaceholder = true,
    )

    override suspend fun detect(bitmap: Bitmap, plantLabel: String?): List<Prediction> =
        withContext(Dispatchers.Default) {
            val signature = ImageSignature.of(bitmap)
            delay(simulatedLatencyMs)

            val random = Random(signature.hash xor 0x5EED)
            val plantId = plantLabel?.let { knowledgeBase.findPlant(it)?.id }
            val candidates = knowledgeBase.diseases.filter { plantId == null || plantId in it.hosts }
            val healthy = random.nextFloat() < 0.4f || candidates.isEmpty()

            if (healthy) {
                val score = 0.80f + random.nextFloat() * 0.18f
                buildList {
                    add(Prediction("healthy", score))
                    candidates.shuffled(random).firstOrNull()?.let { add(Prediction(it.id, 1f - score)) }
                }
            } else {
                val picks = candidates.shuffled(random).take(2)
                val score = 0.58f + random.nextFloat() * 0.36f
                buildList {
                    add(Prediction(picks[0].id, score))
                    add(Prediction("healthy", (1f - score) * 0.6f))
                    picks.getOrNull(1)?.let { add(Prediction(it.id, (1f - score) * 0.4f)) }
                }
            }
        }
}
