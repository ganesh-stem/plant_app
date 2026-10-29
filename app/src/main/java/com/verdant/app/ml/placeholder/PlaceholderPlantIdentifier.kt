package com.verdant.app.ml.placeholder

import android.graphics.Bitmap
import com.verdant.app.data.knowledge.KnowledgeBase
import com.verdant.app.data.model.Prediction
import com.verdant.app.ml.ModelInfo
import com.verdant.app.ml.PlantIdentifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Stand-in for the real plant model. Results are simulated but deterministic per
 * photo and always map to plants in the knowledge base, so every screen can be
 * exercised end-to-end before a model exists.
 *
 * Replace by adding `models/plant_identifier.tflite` + `models/plant_labels.txt`.
 */
class PlaceholderPlantIdentifier(
    private val knowledgeBase: KnowledgeBase,
    private val simulatedLatencyMs: Long = 1_100,
) : PlantIdentifier {

    override val info = ModelInfo(
        displayName = "Plant identifier",
        source = "Placeholder (simulated)",
        isPlaceholder = true,
    )

    override suspend fun identify(bitmap: Bitmap): List<Prediction> = withContext(Dispatchers.Default) {
        val signature = ImageSignature.of(bitmap)
        delay(simulatedLatencyMs)

        // Very dark photos (e.g. covered lens) demonstrate the "no plant found" path.
        if (signature.brightness < 18f) {
            return@withContext listOf(Prediction("background", 0.91f), Prediction("monstera_deliciosa", 0.05f))
        }

        val plants = knowledgeBase.plants
        val random = Random(signature.hash)
        val picks = plants.shuffled(random).take(3)
        val top = 0.72f + random.nextFloat() * 0.24f
        val second = (1f - top) * (0.55f + random.nextFloat() * 0.25f)
        val third = (1f - top - second) * 0.7f
        listOf(top, second, third).zip(picks) { score, plant -> Prediction(plant.id, score) }
    }
}
