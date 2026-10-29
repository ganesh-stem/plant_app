package com.verdant.app.ui.result

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Coronavirus
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verdant.app.R
import com.verdant.app.data.model.DiseaseInfo
import com.verdant.app.data.model.HealthStatus
import com.verdant.app.data.model.PlantInfo
import com.verdant.app.data.model.PlantMatch
import com.verdant.app.data.model.ScanRecord
import com.verdant.app.ml.ModelConfig
import com.verdant.app.ui.components.ConfidenceRing
import com.verdant.app.ui.components.EmptyState
import com.verdant.app.ui.components.IconBubble
import com.verdant.app.ui.components.InfoChip
import com.verdant.app.ui.components.PlantImage
import com.verdant.app.ui.components.SectionHeader
import com.verdant.app.ui.components.SeverityMeter
import com.verdant.app.ui.components.VerdantCard
import com.verdant.app.ui.components.icon
import com.verdant.app.ui.components.labelRes
import com.verdant.app.ui.components.style
import com.verdant.app.ui.theme.VerdantTheme
import com.verdant.app.util.asPercent
import com.verdant.app.util.formatFullDate

private val HeaderHeight = 420.dp

@Composable
fun ResultScreen(
    onBack: () -> Unit,
    onScanAnother: () -> Unit,
    viewModel: ResultViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    when (val s = state) {
        ResultUiState.Loading -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), Alignment.Center) {
            CircularProgressIndicator()
        }
        ResultUiState.NotFound -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), Alignment.Center) {
            EmptyState(
                title = stringResource(R.string.result_not_found),
                body = "",
                action = { Button(onClick = onBack) { Text(stringResource(R.string.action_back)) } },
            )
        }
        is ResultUiState.Loaded -> ResultContent(
            scan = s.scan,
            onBack = onBack,
            onToggleFavorite = viewModel::toggleFavorite,
            onDelete = { confirmDelete = true },
            onScanAnother = onScanAnother,
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
            title = { Text(stringResource(R.string.result_delete_title)) },
            text = { Text(stringResource(R.string.result_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onBack)
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
internal fun ResultContent(
    scan: ScanRecord,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onScanAnother: () -> Unit,
    listState: LazyListState = rememberLazyListState(),
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val headerPx = with(density) { HeaderHeight.toPx() }
    val collapsed by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > headerPx * 0.7f }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 32.dp)) {
            item(key = "header") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(HeaderHeight)
                        .graphicsLayer {
                            // Parallax: image moves at half speed and fades as it scrolls away.
                            val offset = if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset.toFloat() else headerPx
                            translationY = offset * 0.5f
                            alpha = 1f - (offset / headerPx).coerceIn(0f, 1f) * 0.4f
                        },
                ) {
                    PlantImage(scan.imagePath, scan.plant.commonName, Modifier.fillMaxSize())
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Black.copy(alpha = 0.35f),
                                    0.3f to Color.Transparent,
                                    1f to Color.Black.copy(alpha = 0.25f),
                                ),
                            ),
                    )
                }
            }
            item(key = "sheet") {
                Column(
                    Modifier
                        .offset(y = (-36).dp)
                        .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 20.dp),
                ) {
                    Box(
                        Modifier
                            .padding(top = 12.dp)
                            .size(width = 40.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                            .align(Alignment.CenterHorizontally),
                    )
                    Spacer(Modifier.height(20.dp))
                    if (scan.isDemo) {
                        DemoBanner()
                        Spacer(Modifier.height(16.dp))
                    }
                    TitleBlock(scan)
                    Spacer(Modifier.height(20.dp))
                    HealthCard(scan)
                    if (scan.health == HealthStatus.DISEASED && scan.disease != null) {
                        Spacer(Modifier.height(16.dp))
                        DiseaseGuide(scan.disease)
                    }
                    Spacer(Modifier.height(28.dp))
                    AboutSection(scan.plant, scan.hasPlantDetails)
                    scan.plant.care?.let {
                        Spacer(Modifier.height(28.dp))
                        CareGuideSection(scan.plant)
                    }
                    if (scan.alternatives.isNotEmpty()) {
                        Spacer(Modifier.height(28.dp))
                        AlternativesSection(scan.alternatives)
                    }
                    Spacer(Modifier.height(24.dp))
                    Disclaimer()
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = onScanAnother,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 18.dp),
                    ) {
                        Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.result_scan_another))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.result_alternatives_scanned_on, formatFullDate(scan.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.align(Alignment.CenterHorizontally).navigationBarsPadding(),
                    )
                }
            }
        }

        // Solid bar that appears once the photo scrolls away
        AnimatedVisibility(collapsed, enter = fadeIn() + slideInVertically { -it / 2 }, exit = fadeOut()) {
            Surface(color = MaterialTheme.colorScheme.background, shadowElevation = 2.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(64.dp),
                ) {}
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopAction(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back), collapsed, onBack)
            Box(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                androidx.compose.animation.AnimatedVisibility(collapsed, enter = fadeIn(), exit = fadeOut()) {
                    Text(scan.plant.commonName, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            TopAction(
                if (scan.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                stringResource(if (scan.isFavorite) R.string.result_favorite_remove else R.string.result_favorite_add),
                collapsed,
                onToggleFavorite,
                tint = if (scan.isFavorite) VerdantTheme.colors.danger else null,
            )
            Spacer(Modifier.width(8.dp))
            TopAction(Icons.Rounded.Share, stringResource(R.string.action_share), collapsed, { shareScan(context, scan) })
            Spacer(Modifier.width(8.dp))
            TopAction(Icons.Outlined.DeleteOutline, stringResource(R.string.action_delete), collapsed, onDelete)
        }
    }
}

@Composable
private fun TopAction(icon: ImageVector, description: String, solid: Boolean, onClick: () -> Unit, tint: Color? = null) {
    val bg by animateColorAsState(
        if (solid) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Black.copy(alpha = 0.35f),
        label = "bg",
    )
    val fg = tint ?: if (solid) MaterialTheme.colorScheme.onSurface else Color.White
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(bg),
    ) {
        Icon(icon, contentDescription = description, tint = fg)
    }
}

@Composable
private fun DemoBanner() {
    val c = VerdantTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(c.warningContainer)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Science, contentDescription = null, tint = c.warning, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.result_demo_banner), style = MaterialTheme.typography.bodySmall, color = c.warning)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TitleBlock(scan: ScanRecord) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(scan.plant.commonName, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
            if (scan.plant.scientificName.isNotBlank()) {
                Text(
                    scan.plant.scientificName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = com.verdant.app.ui.theme.Fraunces,
                        fontStyle = FontStyle.Italic,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (scan.hasPlantDetails) {
                    InfoChip(scan.plant.family, icon = Icons.Rounded.Spa)
                    InfoChip(stringResource(scan.plant.difficulty.labelRes()), icon = Icons.Rounded.Lightbulb)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        ConfidenceRing(value = scan.plantConfidence, size = 84.dp, caption = stringResource(R.string.result_match))
    }
}

@Composable
private fun HealthCard(scan: ScanRecord) {
    val disease = scan.disease
    val style = scan.health.style(disease?.severity)
    val c = VerdantTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(style.container, style.container.copy(alpha = 0.55f))))
            .border(1.dp, style.color.copy(alpha = 0.18f), MaterialTheme.shapes.large)
            .padding(20.dp),
    ) {
        Text(
            stringResource(R.string.result_health_title).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = style.color,
        )
        Spacer(Modifier.height(12.dp))
        when (scan.health) {
            HealthStatus.HEALTHY -> HealthRow(
                icon = Icons.Rounded.CheckCircle,
                color = style.color,
                title = stringResource(R.string.result_healthy_title),
                body = stringResource(R.string.result_healthy_body),
                confidence = scan.diseaseConfidence,
            )
            HealthStatus.UNKNOWN -> HealthRow(
                icon = Icons.AutoMirrored.Rounded.HelpOutline,
                color = style.color,
                title = stringResource(R.string.result_unknown_title),
                body = stringResource(R.string.result_unknown_body),
                confidence = null,
            )
            HealthStatus.DISEASED -> {
                val confidence = scan.diseaseConfidence ?: 0f
                val uncertain = confidence < ModelConfig.CONFIDENT_DISEASE_THRESHOLD
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble((disease?.pathogen ?: com.verdant.app.data.model.Pathogen.UNKNOWN).icon(), MaterialTheme.colorScheme.surface, style.color, size = 52.dp, iconSize = 26.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        if (uncertain) {
                            Text(stringResource(R.string.result_possible), style = MaterialTheme.typography.labelMedium, color = style.color)
                        }
                        Text(
                            disease?.name ?: stringResource(R.string.status_attention),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            stringResource(R.string.result_confidence, confidence.asPercent()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (disease != null) {
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoChip(
                            stringResource(disease.pathogen.labelRes()),
                            icon = disease.pathogen.icon(),
                            color = style.color,
                            container = Color.White.copy(alpha = if (c.isDark) 0.08f else 0.6f),
                        )
                        InfoChip(
                            stringResource(if (disease.contagious) R.string.result_contagious else R.string.result_not_contagious),
                            icon = if (disease.contagious) Icons.Rounded.Coronavirus else Icons.Rounded.Shield,
                            color = style.color,
                            container = Color.White.copy(alpha = if (c.isDark) 0.08f else 0.6f),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.result_severity), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.weight(1f))
                        Text(stringResource(disease.severity.labelRes()), style = MaterialTheme.typography.labelLarge, color = style.color)
                    }
                    Spacer(Modifier.height(8.dp))
                    SeverityMeter(disease.severity, style.color, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(16.dp))
                    Text(disease.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
                }
            }
        }
    }
}

@Composable
private fun HealthRow(icon: ImageVector, color: Color, title: String, body: String, confidence: Float?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBubble(icon, MaterialTheme.colorScheme.surface, color, size = 52.dp, iconSize = 28.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            if (confidence != null) {
                Text(
                    stringResource(R.string.result_confidence, confidence.asPercent()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
}

private enum class GuideTab { SYMPTOMS, TREATMENT, PREVENTION }

@Composable
private fun DiseaseGuide(disease: DiseaseInfo) {
    val tabs = buildList {
        if (disease.treatment.isNotEmpty()) add(GuideTab.TREATMENT)
        if (disease.symptoms.isNotEmpty()) add(GuideTab.SYMPTOMS)
        if (disease.prevention.isNotEmpty()) add(GuideTab.PREVENTION)
    }
    if (tabs.isEmpty()) return
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val tab = tabs[selected.coerceIn(0, tabs.lastIndex)]

    VerdantCard(contentPadding = PaddingValues(8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(4.dp),
        ) {
            tabs.forEachIndexed { i, t ->
                val isSelected = t == tab
                val bg by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, label = "tabBg")
                val fg by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, label = "tabFg")
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(bg)
                        .selectable(selected = isSelected, onClick = { selected = i }, role = Role.Tab)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(
                            when (t) {
                                GuideTab.SYMPTOMS -> R.string.result_tab_symptoms
                                GuideTab.TREATMENT -> R.string.result_tab_treatment
                                GuideTab.PREVENTION -> R.string.result_tab_prevention
                            },
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = fg,
                        maxLines = 1,
                    )
                }
            }
        }
        AnimatedContent(tab, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "guide") { current ->
            val items = when (current) {
                GuideTab.SYMPTOMS -> disease.symptoms
                GuideTab.TREATMENT -> disease.treatment
                GuideTab.PREVENTION -> disease.prevention
            }
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items.forEachIndexed { i, text ->
                    Row(verticalAlignment = Alignment.Top) {
                        when (current) {
                            GuideTab.TREATMENT -> Box(
                                Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            GuideTab.SYMPTOMS -> Icon(Icons.Rounded.Visibility, null, tint = VerdantTheme.colors.warning, modifier = Modifier.size(22.dp).padding(top = 2.dp))
                            GuideTab.PREVENTION -> Icon(Icons.Rounded.Shield, null, tint = VerdantTheme.colors.healthy, modifier = Modifier.size(22.dp).padding(top = 2.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(top = 2.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutSection(plant: PlantInfo, hasDetails: Boolean) {
    SectionHeader(stringResource(R.string.result_about))
    Spacer(Modifier.height(10.dp))
    Text(
        if (hasDetails) plant.description else stringResource(R.string.result_no_details),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    plant.funFact?.let { fact ->
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(VerdantTheme.colors.accent.copy(alpha = 0.22f))
                .padding(16.dp),
        ) {
            Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(stringResource(R.string.result_fun_fact), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(2.dp))
                Text(fact, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CareGuideSection(plant: PlantInfo) {
    val care = plant.care ?: return
    val c = VerdantTheme.colors
    val items = listOf(
        CareItem(Icons.Rounded.WaterDrop, R.string.result_care_water, care.water, Color(0xFF3B82C4)),
        CareItem(Icons.Rounded.WbSunny, R.string.result_care_light, care.light, Color(0xFFE0A100)),
        CareItem(Icons.Rounded.Thermostat, R.string.result_care_temperature, care.temperature, Color(0xFFE0603A)),
        CareItem(Icons.Rounded.Cloud, R.string.result_care_humidity, care.humidity, Color(0xFF5BA3A8)),
        CareItem(Icons.Rounded.Grass, R.string.result_care_soil, care.soil, Color(0xFF8B6B4A)),
        CareItem(Icons.Rounded.Healing, R.string.result_care_fertilizer, care.fertilizer, MaterialTheme.colorScheme.primary),
    )
    SectionHeader(stringResource(R.string.result_care_title))
    Spacer(Modifier.height(12.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(2).forEach { row ->
            // Tiles in a row share the height of the tallest one, so text never gets cut off.
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { item -> CareTile(item, Modifier.weight(1f).fillMaxHeight()) }
            }
        }
        plant.toxicity?.let { tox ->
            VerdantCard(Modifier.fillMaxWidth(), color = c.dangerContainer.copy(alpha = 0.6f), contentPadding = PaddingValues(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(Icons.Rounded.Pets, c.danger, MaterialTheme.colorScheme.surfaceContainer, size = 40.dp, iconSize = 20.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(stringResource(R.string.result_toxicity), style = MaterialTheme.typography.labelMedium, color = c.danger)
                        Text(tox, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

private data class CareItem(val icon: ImageVector, val label: Int, val value: String, val tint: Color)

@Composable
private fun CareTile(item: CareItem, modifier: Modifier = Modifier) {
    VerdantCard(modifier, contentPadding = PaddingValues(16.dp)) {
        IconBubble(item.icon, item.tint, item.tint.copy(alpha = 0.14f), size = 38.dp, iconSize = 20.dp)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(item.label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(item.value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun AlternativesSection(alternatives: List<PlantMatch>) {
    SectionHeader(stringResource(R.string.result_alternatives))
    Spacer(Modifier.height(12.dp))
    VerdantCard(contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp)) {
        alternatives.forEach { match ->
            Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(match.commonName, style = MaterialTheme.typography.titleSmall)
                    match.scientificName?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { match.confidence },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        strokeCap = StrokeCap.Round,
                        drawStopIndicator = {},
                    )
                }
                Spacer(Modifier.width(16.dp))
                Text(match.confidence.asPercent(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun Disclaimer() {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(14.dp),
    ) {
        Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.result_disclaimer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
