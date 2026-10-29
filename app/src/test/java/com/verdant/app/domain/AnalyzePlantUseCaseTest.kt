package com.verdant.app.domain

import android.graphics.Bitmap
import com.google.common.truth.Truth.assertThat
import com.verdant.app.data.image.ImageImportException
import com.verdant.app.data.local.ScanEntity
import com.verdant.app.data.model.HealthStatus
import com.verdant.app.data.model.Prediction
import com.verdant.app.ml.DiseaseDetector
import com.verdant.app.ml.ModelInfo
import com.verdant.app.ml.PlantIdentifier
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AnalyzePlantUseCaseTest {

    @get:Rule val tmp = TemporaryFolder()

    private lateinit var photo: File
    private val saved = mutableListOf<ScanEntity>()
    private val stages = mutableListOf<AnalysisStage>()

    private var plantResult: () -> List<Prediction> = { listOf(Prediction("rosa", 0.9f), Prediction("ocimum_basilicum", 0.05f)) }
    private var diseaseResult: () -> List<Prediction> = { listOf(Prediction("healthy", 0.88f)) }
    private var importer = ImageImporter { photo to fakeBitmap() }
    private var saver = ScanSaver { entity -> saved += entity; 42L }
    private var placeholder = false
    private var diseasePlantLabel: String? = null

    @Before fun setUp() {
        photo = tmp.newFile("scan.jpg")
    }

    private fun useCase() = AnalyzePlantUseCase(
        importer = importer,
        plantIdentifier = object : PlantIdentifier {
            override val info = ModelInfo("plant", "test", isPlaceholder = placeholder)
            override suspend fun identify(bitmap: Bitmap) = plantResult()
        },
        diseaseDetector = object : DiseaseDetector {
            override val info = ModelInfo("disease", "test", isPlaceholder = false)
            override suspend fun detect(bitmap: Bitmap, plantLabel: String?): List<Prediction> {
                diseasePlantLabel = plantLabel
                return diseaseResult()
            }
        },
        saver = saver,
        clock = Clock { 1_000L },
    )

    private suspend fun run() = useCase()("file:///scan.jpg") { stages += it }

    @Test fun `healthy plant is saved with all stages in order`() = runTest {
        val outcome = run()

        assertThat(outcome).isEqualTo(AnalysisOutcome.Success(42L))
        assertThat(stages).containsExactlyElementsIn(AnalysisStage.entries).inOrder()
        val entity = saved.single()
        assertThat(entity.plantLabel).isEqualTo("rosa")
        assertThat(entity.healthStatus).isEqualTo(HealthStatus.HEALTHY)
        assertThat(entity.createdAt).isEqualTo(1_000L)
        assertThat(entity.plantAlternatives.map { it.label }).containsExactly("ocimum_basilicum")
        assertThat(entity.isDemo).isFalse()
        assertThat(photo.exists()).isTrue()
    }

    @Test fun `disease model receives plant label and diseased result is stored`() = runTest {
        diseaseResult = { listOf(Prediction("black_spot", 0.7f), Prediction("healthy", 0.2f)) }

        run()

        assertThat(diseasePlantLabel).isEqualTo("rosa")
        val entity = saved.single()
        assertThat(entity.healthStatus).isEqualTo(HealthStatus.DISEASED)
        assertThat(entity.diseaseLabel).isEqualTo("black_spot")
        assertThat(entity.diseaseConfidence).isEqualTo(0.7f)
    }

    @Test fun `low confidence stops before diagnosis and discards photo`() = runTest {
        plantResult = { listOf(Prediction("rosa", 0.12f)) }

        val outcome = run()

        assertThat(outcome).isEqualTo(AnalysisOutcome.NoPlantFound(Prediction("rosa", 0.12f)))
        assertThat(stages).doesNotContain(AnalysisStage.DIAGNOSING)
        assertThat(saved).isEmpty()
        assertThat(photo.exists()).isFalse()
    }

    @Test fun `background class is reported as no plant without a guess`() = runTest {
        plantResult = { listOf(Prediction("background", 0.95f)) }

        assertThat(run()).isEqualTo(AnalysisOutcome.NoPlantFound(null))
    }

    @Test fun `empty predictions are reported as no plant`() = runTest {
        plantResult = { emptyList() }

        assertThat(run()).isEqualTo(AnalysisOutcome.NoPlantFound(null))
    }

    @Test fun `disease model failure keeps identification with unknown health`() = runTest {
        diseaseResult = { error("boom") }

        val outcome = run()

        assertThat(outcome).isInstanceOf(AnalysisOutcome.Success::class.java)
        assertThat(saved.single().healthStatus).isEqualTo(HealthStatus.UNKNOWN)
    }

    @Test fun `plant model failure is a model error`() = runTest {
        plantResult = { error("boom") }

        val outcome = run() as AnalysisOutcome.Failed

        assertThat(outcome.reason).isEqualTo(FailureReason.MODEL_ERROR)
        assertThat(photo.exists()).isFalse()
    }

    @Test fun `unreadable image is reported`() = runTest {
        importer = ImageImporter { throw ImageImportException("bad") }

        val outcome = run() as AnalysisOutcome.Failed

        assertThat(outcome.reason).isEqualTo(FailureReason.IMAGE_UNREADABLE)
        assertThat(stages).containsExactly(AnalysisStage.PREPARING)
    }

    @Test fun `storage failure discards photo`() = runTest {
        saver = ScanSaver { error("disk full") }

        val outcome = run() as AnalysisOutcome.Failed

        assertThat(outcome.reason).isEqualTo(FailureReason.STORAGE_ERROR)
        assertThat(photo.exists()).isFalse()
    }

    @Test fun `placeholder models mark the scan as demo`() = runTest {
        placeholder = true

        run()

        assertThat(saved.single().isDemo).isTrue()
    }

    /**
     * android.jar only contains stubs on the JVM. With `unitTests.isReturnDefaultValues`
     * every Bitmap method is a no-op, so an uninitialised instance is enough here.
     */
    private fun fakeBitmap(): Bitmap {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val unsafe = unsafeClass.getDeclaredField("theUnsafe").apply { isAccessible = true }.get(null)
        return unsafeClass.getMethod("allocateInstance", Class::class.java).invoke(unsafe, Bitmap::class.java) as Bitmap
    }
}
