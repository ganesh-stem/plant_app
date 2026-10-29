package com.verdant.app.domain

import android.graphics.Bitmap
import com.verdant.app.data.image.ImageImportException
import com.verdant.app.data.local.ScanEntity
import com.verdant.app.data.model.HealthStatus
import com.verdant.app.data.model.Prediction
import com.verdant.app.ml.DiseaseDetector
import com.verdant.app.ml.LabelUtils
import com.verdant.app.ml.ModelConfig
import com.verdant.app.ml.PlantIdentifier
import kotlinx.coroutines.CancellationException
import java.io.File
import javax.inject.Inject

enum class AnalysisStage { PREPARING, IDENTIFYING, DIAGNOSING, SAVING }

sealed interface AnalysisOutcome {
    data class Success(val scanId: Long) : AnalysisOutcome
    /** Stage 1 wasn't confident the photo contains a plant. */
    data class NoPlantFound(val bestGuess: Prediction?) : AnalysisOutcome
    data class Failed(val reason: FailureReason, val cause: Throwable? = null) : AnalysisOutcome
}

enum class FailureReason { IMAGE_UNREADABLE, MODEL_ERROR, STORAGE_ERROR }

/** Source of the current time; injectable for tests. */
fun interface Clock {
    fun now(): Long
}

/** Persists a finished scan and returns its id. */
fun interface ScanSaver {
    suspend fun save(entity: ScanEntity): Long
}

/** Loads a photo (given as a URI string) into memory; abstracted so the pipeline can be unit tested. */
fun interface ImageImporter {
    suspend fun import(imageUri: String): Pair<File, Bitmap>
}

/**
 * The core pipeline:
 *  1. Import + normalise the photo.
 *  2. Identify the plant (stage 1 model). Stop early if it isn't a plant.
 *  3. Diagnose its health (stage 2 model), passing the plant label for context.
 *     A failure here doesn't throw away a good identification; health becomes UNKNOWN.
 *  4. Persist the scan.
 */
class AnalyzePlantUseCase @Inject constructor(
    private val importer: ImageImporter,
    private val plantIdentifier: PlantIdentifier,
    private val diseaseDetector: DiseaseDetector,
    private val saver: ScanSaver,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        imageUri: String,
        onStage: suspend (AnalysisStage) -> Unit = {},
    ): AnalysisOutcome {
        onStage(AnalysisStage.PREPARING)
        val (file, bitmap) = try {
            importer.import(imageUri)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ImageImportException) {
            return AnalysisOutcome.Failed(FailureReason.IMAGE_UNREADABLE, e)
        } catch (e: Exception) {
            return AnalysisOutcome.Failed(FailureReason.IMAGE_UNREADABLE, e)
        }

        var keepFile = false
        try {
            onStage(AnalysisStage.IDENTIFYING)
            val plantPredictions = try {
                plantIdentifier.identify(bitmap)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return AnalysisOutcome.Failed(FailureReason.MODEL_ERROR, e)
            }

            val top = plantPredictions.firstOrNull()
            val isNonPlant = top != null && LabelUtils.normalize(top.label) in ModelConfig.NON_PLANT_LABELS
            if (top == null || isNonPlant || top.confidence < ModelConfig.MIN_PLANT_CONFIDENCE) {
                return AnalysisOutcome.NoPlantFound(top?.takeUnless { isNonPlant })
            }

            onStage(AnalysisStage.DIAGNOSING)
            val diseasePredictions = try {
                diseaseDetector.detect(bitmap, top.label)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
            val topDisease = diseasePredictions.firstOrNull()
            val health = when {
                topDisease == null -> HealthStatus.UNKNOWN
                LabelUtils.isHealthy(topDisease.label) -> HealthStatus.HEALTHY
                else -> HealthStatus.DISEASED
            }

            onStage(AnalysisStage.SAVING)
            val id = try {
                saver.save(
                    ScanEntity(
                        imagePath = file.absolutePath,
                        createdAt = clock.now(),
                        plantLabel = top.label,
                        plantConfidence = top.confidence,
                        plantAlternatives = plantPredictions.drop(1)
                            .filter { LabelUtils.normalize(it.label) !in ModelConfig.NON_PLANT_LABELS }
                            .take(3),
                        healthStatus = health,
                        diseaseLabel = topDisease?.label,
                        diseaseConfidence = topDisease?.confidence,
                        diseaseAlternatives = diseasePredictions.drop(1).take(3),
                        isDemo = plantIdentifier.info.isPlaceholder || diseaseDetector.info.isPlaceholder,
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return AnalysisOutcome.Failed(FailureReason.STORAGE_ERROR, e)
            }
            keepFile = true
            return AnalysisOutcome.Success(id)
        } finally {
            if (!keepFile) file.delete()
            bitmap.recycle()
        }
    }
}
