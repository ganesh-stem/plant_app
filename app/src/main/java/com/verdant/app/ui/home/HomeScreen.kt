package com.verdant.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verdant.app.R
import com.verdant.app.data.model.ScanRecord
import com.verdant.app.data.model.ScanStats
import com.verdant.app.ui.components.HealthBadge
import com.verdant.app.ui.components.IconBubble
import com.verdant.app.ui.components.LeafCluster
import com.verdant.app.ui.components.PlantImage
import com.verdant.app.ui.components.SectionHeader
import com.verdant.app.ui.components.VerdantCard
import com.verdant.app.ui.theme.Mint
import com.verdant.app.ui.theme.VerdantTheme
import com.verdant.app.util.formatTimestamp
import java.util.Calendar

@Composable
fun HomeScreen(
    onScan: () -> Unit,
    onUpload: () -> Unit,
    onOpenScan: (Long) -> Unit,
    onSeeAll: () -> Unit,
    onSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeContent(state, onScan, onUpload, onOpenScan, onSeeAll, onSettings)
}

@Composable
internal fun HomeContent(
    state: HomeUiState,
    onScan: () -> Unit,
    onUpload: () -> Unit,
    onOpenScan: (Long) -> Unit,
    onSeeAll: () -> Unit,
    onSettings: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 140.dp),
    ) {
        item { Header(onSettings) }
        item { HeroCard(onScan, onUpload, Modifier.padding(horizontal = 20.dp)) }
        item { StatsRow(state.stats, Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) }
        item {
            SectionHeader(
                title = stringResource(R.string.home_recent_title),
                action = if (state.recent.isNotEmpty()) stringResource(R.string.action_see_all) else null,
                onAction = onSeeAll,
                modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 28.dp, bottom = 8.dp),
            )
        }
        item {
            if (state.recent.isEmpty() && !state.loading) {
                VerdantCard(Modifier.padding(horizontal = 20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBubble(Icons.Rounded.LocalFlorist, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                        Spacer(Modifier.width(14.dp))
                        Text(
                            stringResource(R.string.home_empty_recent),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(state.recent, key = { it.id }) { scan -> RecentScanCard(scan) { onOpenScan(scan.id) } }
                }
            }
        }
        item {
            SectionHeader(
                title = stringResource(R.string.home_how_title),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 12.dp),
            )
        }
        item { HowItWorks(Modifier.padding(horizontal = 20.dp)) }
        item { TipCard(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp)) }
    }
}

@Composable
private fun Header(onSettings: () -> Unit) {
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> R.string.home_greeting_morning
            in 12..17 -> R.string.home_greeting_afternoon
            else -> R.string.home_greeting_evening
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 24.dp, end = 16.dp, top = 12.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(greeting),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        FilledTonalIconButton(
            onClick = onSettings,
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.action_settings))
        }
    }
}

@Composable
private fun HeroCard(onScan: () -> Unit, onUpload: () -> Unit, modifier: Modifier = Modifier) {
    val colors = VerdantTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(listOf(colors.heroStart, colors.heroEnd))),
    ) {
        // Decorative circles + leaves
        Box(
            Modifier
                .size(220.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-50).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f)),
        )
        LeafCluster(
            modifier = Modifier
                .size(170.dp)
                .align(Alignment.TopEnd)
                .offset(x = 18.dp, y = 4.dp),
            primary = colors.accent,
            secondary = Mint.copy(alpha = 0.55f),
            vein = colors.heroStart,
        )
        Column(Modifier.padding(24.dp)) {
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.14f)) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.HealthAndSafety, null, tint = colors.accent, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.home_hero_eyebrow), style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.home_hero_title),
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                modifier = Modifier.fillMaxWidth(0.68f),
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onScan,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.home_hero_scan), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = onUpload,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = stringResource(R.string.home_hero_upload), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun StatsRow(stats: ScanStats, modifier: Modifier = Modifier) {
    val c = VerdantTheme.colors
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile(stats.total.toString(), stringResource(R.string.home_stat_scans), Icons.Rounded.Search, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer, Modifier.weight(1f))
        StatTile(stats.healthy.toString(), stringResource(R.string.home_stat_healthy), Icons.Rounded.Favorite, c.healthy, c.healthyContainer, Modifier.weight(1f))
        StatTile(stats.needsAttention.toString(), stringResource(R.string.home_stat_attention), Icons.Rounded.WarningAmber, c.warning, c.warningContainer, Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(value: String, label: String, icon: ImageVector, tint: Color, container: Color, modifier: Modifier) {
    VerdantCard(modifier, contentPadding = PaddingValues(14.dp)) {
        IconBubble(icon, tint, container, size = 32.dp, iconSize = 16.dp)
        Spacer(Modifier.height(10.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun RecentScanCard(scan: ScanRecord, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.width(168.dp),
    ) {
        Column {
            Box {
                PlantImage(
                    path = scan.imagePath,
                    contentDescription = scan.plant.commonName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(22.dp)),
                )
                HealthBadge(
                    status = scan.health,
                    severity = scan.disease?.severity,
                    onImage = true,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp),
                )
            }
            Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp, top = 4.dp)) {
                Text(scan.plant.commonName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    formatTimestamp(scan.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun HowItWorks(modifier: Modifier = Modifier) {
    val c = VerdantTheme.colors
    val steps = listOf(
        Triple(Icons.Rounded.CameraAlt, R.string.home_step_snap, R.string.home_step_snap_body),
        Triple(Icons.Rounded.LocalFlorist, R.string.home_step_identify, R.string.home_step_identify_body),
        Triple(Icons.Rounded.HealthAndSafety, R.string.home_step_diagnose, R.string.home_step_diagnose_body),
    )
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        steps.forEachIndexed { i, (icon, title, body) ->
            VerdantCard(Modifier.weight(1f), contentPadding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(icon, c.onAccent, c.accent, size = 34.dp, iconSize = 18.dp)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "0${i + 1}",
                        style = MaterialTheme.typography.titleSmall.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(2.dp))
                Text(stringResource(body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TipCard(modifier: Modifier = Modifier) {
    val tips = stringArrayResource(R.array.daily_tips)
    val tip = remember(tips) { tips[Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % tips.size] }
    Box(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), MaterialTheme.shapes.large)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            IconBubble(Icons.Rounded.Lightbulb, VerdantTheme.colors.onAccent, VerdantTheme.colors.accent, size = 40.dp, iconSize = 20.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    stringResource(R.string.home_tip_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(4.dp))
                Text(tip, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f))
            }
        }
    }
}
