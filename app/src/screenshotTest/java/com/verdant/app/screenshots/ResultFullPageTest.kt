package com.verdant.app.screenshots

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalInspectionMode
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.NightMode
import com.verdant.app.data.knowledge.InMemoryKnowledgeBase
import com.verdant.app.data.knowledge.JsonSource
import com.verdant.app.data.model.HealthStatus
import com.verdant.app.data.model.PlantMatch
import com.verdant.app.data.model.ScanRecord
import com.verdant.app.data.preferences.ThemeMode
import com.verdant.app.ui.result.ResultContent
import com.verdant.app.ui.theme.VerdantTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

/** Captures the result page at several scroll positions to review every section. */
@RunWith(Parameterized::class)
class ResultFullPageTest(private val dark: Boolean, private val offsetDp: String) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "dark={0},offset={1}")
        fun params() = listOf(false, true).flatMap { d -> listOf("700", "1400", "2100").map { arrayOf(d, it) } }
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

    @Test fun resultFullPage() {
        val scan = ScanRecord(
            id = 1,
            imagePath = File("src/screenshotTest/samples/tomato.jpg").absolutePath,
            createdAt = 1_730_000_000_000,
            plantLabel = "solanum_lycopersicum",
            plant = kb.findPlant("solanum_lycopersicum")!!,
            hasPlantDetails = true,
            plantConfidence = 0.91f,
            alternatives = listOf(
                PlantMatch("solanum_tuberosum", "Potato", "Solanum tuberosum", 0.06f),
                PlantMatch("capsicum_annuum", "Bell Pepper", "Capsicum annuum", 0.02f),
            ),
            health = HealthStatus.DISEASED,
            diseaseLabel = "early_blight",
            disease = kb.findDisease("early_blight"),
            diseaseConfidence = 0.84f,
            isDemo = true,
            isFavorite = false,
        )
        paparazzi.snapshot {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                VerdantTheme(themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
                    val px = with(LocalDensity.current) { offsetDp.toInt().dp.roundToPx() }
                    ResultContent(scan, {}, {}, {}, {}, rememberLazyListState(initialFirstVisibleItemIndex = 0, initialFirstVisibleItemScrollOffset = px))
                }
            }
        }
    }
}
