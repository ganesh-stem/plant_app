package com.verdant.app.screenshots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.NightMode
import com.verdant.app.data.knowledge.InMemoryKnowledgeBase
import com.verdant.app.data.knowledge.JsonSource
import com.verdant.app.data.model.HealthStatus
import com.verdant.app.data.model.PlantMatch
import com.verdant.app.data.model.ScanRecord
import com.verdant.app.data.model.ScanStats
import com.verdant.app.data.preferences.ThemeMode
import com.verdant.app.data.preferences.UserPreferences
import com.verdant.app.domain.AnalysisStage
import com.verdant.app.domain.FailureReason
import com.verdant.app.ml.ModelInfo
import com.verdant.app.ui.analysis.AnalysisContent
import com.verdant.app.ui.analysis.AnalysisStatus
import com.verdant.app.ui.analysis.AnalysisUiState
import com.verdant.app.ui.history.HistoryContent
import com.verdant.app.ui.history.HistoryUiState
import com.verdant.app.ui.home.HomeContent
import com.verdant.app.ui.home.HomeUiState
import com.verdant.app.ui.navigation.Tab
import com.verdant.app.ui.navigation.VerdantBottomBar
import com.verdant.app.ui.onboarding.OnboardingContent
import com.verdant.app.ui.result.ResultContent
import com.verdant.app.ui.settings.SettingsContent
import com.verdant.app.ui.theme.VerdantTheme
import com.verdant.app.util.DayBucket
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class ScreenshotTest(private val dark: Boolean) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "dark={0}")
        fun params() = listOf(false, true)
    }

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6.copy(
            nightMode = if (dark) NightMode.NIGHT else NightMode.NOTNIGHT,
            softButtons = false,
        ),
        theme = "android:Theme.Material.NoActionBar",
        maxPercentDifference = 0.5,
    )

    private val kb = InMemoryKnowledgeBase {
        JsonSource(File("src/main/assets/knowledge/plants.json").readText(), File("src/main/assets/knowledge/diseases.json").readText())
    }

    private fun sample(name: String) = File("src/screenshotTest/samples/$name.jpg").absolutePath
    private val now = System.currentTimeMillis()

    private fun scan(id: Long, plant: String, image: String, disease: String?, conf: Float, diseaseConf: Float?, ageMin: Long, fav: Boolean = false) =
        ScanRecord(
            id = id,
            imagePath = sample(image),
            createdAt = now - ageMin * 60_000,
            plantLabel = plant,
            plant = kb.findPlant(plant)!!,
            hasPlantDetails = true,
            plantConfidence = conf,
            alternatives = listOf(
                PlantMatch("solanum_tuberosum", "Potato", "Solanum tuberosum", 0.07f),
                PlantMatch("capsicum_annuum", "Bell Pepper", "Capsicum annuum", 0.03f),
            ),
            health = when (disease) { null -> HealthStatus.HEALTHY; else -> HealthStatus.DISEASED },
            diseaseLabel = disease ?: "healthy",
            disease = disease?.let { kb.findDisease(it) },
            diseaseConfidence = diseaseConf,
            isDemo = true,
            isFavorite = fav,
        )

    private val scans = listOf(
        scan(1, "solanum_lycopersicum", "tomato", "early_blight", 0.91f, 0.84f, 12),
        scan(2, "monstera_deliciosa", "monstera", null, 0.96f, 0.93f, 90, fav = true),
        scan(3, "rosa", "rose", "black_spot", 0.88f, 0.48f, 60 * 26),
        scan(4, "ocimum_basilicum", "basil", null, 0.83f, 0.9f, 60 * 24 * 4),
    )

    private fun snap(content: @Composable () -> Unit) = paparazzi.snapshot {
        // Lets PlantImage decode photos synchronously, like in Android Studio previews.
        CompositionLocalProvider(LocalInspectionMode provides true) {
            VerdantTheme(themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT) { content() }
        }
    }

    @Test fun onboarding() = snap { OnboardingContent(onFinish = {}) }

    @Test fun onboardingLast() = snap { OnboardingContent(onFinish = {}, initialPage = 2) }

    @Test fun home() = snap {
        Box(Modifier.fillMaxSize()) {
            HomeContent(HomeUiState(scans, ScanStats(4, 2, 2), loading = false), {}, {}, {}, {}, {})
            Box(Modifier.align(Alignment.BottomCenter)) { VerdantBottomBar(Tab.HOME, {}, {}) }
        }
    }

    @Test fun homeEmpty() = snap {
        Box(Modifier.fillMaxSize()) {
            HomeContent(HomeUiState(loading = false), {}, {}, {}, {}, {})
            Box(Modifier.align(Alignment.BottomCenter)) { VerdantBottomBar(Tab.HOME, {}, {}) }
        }
    }

    @Test fun analysisRunning() = snap {
        AnalysisContent(
            AnalysisUiState("file://" + sample("tomato"), AnalysisStage.DIAGNOSING, setOf(AnalysisStage.PREPARING, AnalysisStage.IDENTIFYING)),
            "file://" + sample("tomato"), {}, {}, {},
        )
    }

    @Test fun analysisNoPlant() = snap {
        AnalysisContent(
            AnalysisUiState("file:///missing.jpg", status = AnalysisStatus.NoPlant("Monstera", 0.18f)),
            "file:///missing.jpg", {}, {}, {},
        )
    }

    @Test fun analysisError() = snap {
        AnalysisContent(
            AnalysisUiState("file:///missing.jpg", status = AnalysisStatus.Error(FailureReason.MODEL_ERROR)),
            "file:///missing.jpg", {}, {}, {},
        )
    }

    @Test fun resultDiseased() = snap { ResultContent(scans[0], {}, {}, {}, {}) }

    @Test fun resultHealthy() = snap { ResultContent(scans[1], {}, {}, {}, {}) }

    @Test fun resultPossible() = snap { ResultContent(scans[2], {}, {}, {}, {}) }

    @Test fun history() = snap {
        Box(Modifier.fillMaxSize()) {
            HistoryContent(
                HistoryUiState(
                    groups = listOf(DayBucket.TODAY to scans.take(2), DayBucket.YESTERDAY to listOf(scans[2]), DayBucket.THIS_WEEK to listOf(scans[3])),
                    totalCount = 4,
                    loading = false,
                ),
                SnackbarHostState(), {}, {}, {}, {}, {}, {},
            )
            Box(Modifier.align(Alignment.BottomCenter)) { VerdantBottomBar(Tab.HISTORY, {}, {}) }
        }
    }

    @Test fun historyEmpty() = snap {
        HistoryContent(HistoryUiState(loading = false), SnackbarHostState(), {}, {}, {}, {}, {}, {})
    }

    @Test fun settings() = snap {
        SettingsContent(
            prefs = UserPreferences(onboardingComplete = true, themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT),
            models = listOf(
                ModelInfo("Plant identifier", "Placeholder (simulated)", isPlaceholder = true),
                ModelInfo("Disease detector", "disease_detector.tflite", isPlaceholder = false, classCount = 38),
            ),
            onBack = {}, onReplayOnboarding = {}, onThemeChange = {}, onDynamicColorChange = {}, onClearHistory = {},
        )
    }
}
