package com.verdant.app.ui.analysis

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.verdant.app.R
import com.verdant.app.domain.AnalysisStage
import com.verdant.app.domain.FailureReason
import com.verdant.app.ui.components.PlantImage
import com.verdant.app.ui.theme.VerdantTheme
import com.verdant.app.util.asPercent

@Composable
fun AnalysisScreen(
    imageUri: String,
    onCancel: () -> Unit,
    onNewPhoto: () -> Unit,
    onComplete: (Long) -> Unit,
    viewModel: AnalysisViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(state.status) {
        val status = state.status
        if (status is AnalysisStatus.Done) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onComplete(status.scanId)
        }
    }
    val cancel = {
        viewModel.cancel()
        onCancel()
    }
    BackHandler(onBack = cancel)
    AnalysisContent(state, imageUri, cancel, onNewPhoto, viewModel::retry)
}

@Composable
internal fun AnalysisContent(
    state: AnalysisUiState,
    imageUri: String,
    cancel: () -> Unit,
    onNewPhoto: () -> Unit,
    onRetry: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.End) {
                FilledTonalIconButton(onClick = cancel) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_cancel))
                }
            }
            ScanningImage(
                uri = imageUri,
                scanning = state.status is AnalysisStatus.Running,
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .aspectRatio(0.85f),
            )
            Spacer(Modifier.height(32.dp))

            AnimatedContent(
                targetState = state.status,
                // Only animate when the kind of status changes, not on every progress update.
                contentKey = { it::class },
                transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.96f)) togetherWith fadeOut() },
                label = "status",
            ) { status ->
                when (status) {
                    is AnalysisStatus.Running, is AnalysisStatus.Done -> Progress(state)
                    is AnalysisStatus.NoPlant -> Problem(
                        icon = Icons.Rounded.SearchOff,
                        title = stringResource(R.string.analysis_no_plant_title),
                        body = stringResource(R.string.analysis_no_plant_body),
                        footnote = if (status.guessName != null && status.guessConfidence != null) {
                            stringResource(R.string.analysis_no_plant_guess, status.guessName, status.guessConfidence.asPercent())
                        } else {
                            null
                        },
                        onPrimary = onNewPhoto,
                        onSecondary = null,
                    )
                    is AnalysisStatus.Error -> Problem(
                        icon = Icons.Rounded.ErrorOutline,
                        title = stringResource(
                            when (status.reason) {
                                FailureReason.IMAGE_UNREADABLE -> R.string.analysis_error_image
                                FailureReason.MODEL_ERROR -> R.string.analysis_error_model
                                FailureReason.STORAGE_ERROR -> R.string.analysis_error_storage
                            },
                        ),
                        body = null,
                        footnote = null,
                        onPrimary = onNewPhoto,
                        onSecondary = if (status.reason != FailureReason.IMAGE_UNREADABLE) onRetry else null,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ScanningImage(uri: String, scanning: Boolean, modifier: Modifier = Modifier) {
    val accent = VerdantTheme.colors.accent
    val transition = rememberInfiniteTransition(label = "scan")
    val sweep by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse), label = "sweep")
    val glow by transition.animateFloat(0.25f, 0.7f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "glow")

    Box(
        modifier
            .clip(RoundedCornerShape(36.dp))
            .border(2.dp, if (scanning) accent.copy(alpha = glow) else Color.Transparent, RoundedCornerShape(36.dp)),
    ) {
        if (uri.startsWith("file://")) {
            PlantImage(
                path = uri.removePrefix("file://"),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
        }
        if (scanning) {
            Canvas(Modifier.fillMaxSize()) {
                // Subtle grid
                val step = size.width / 6
                for (i in 1 until 6) {
                    drawLine(Color.White.copy(alpha = 0.12f), Offset(step * i, 0f), Offset(step * i, size.height), 1f)
                }
                var y = step
                while (y < size.height) {
                    drawLine(Color.White.copy(alpha = 0.12f), Offset(0f, y), Offset(size.width, y), 1f)
                    y += step
                }
                // Sweeping beam
                val beamY = size.height * sweep
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, accent.copy(alpha = 0.45f), Color.Transparent),
                        startY = beamY - 90f,
                        endY = beamY + 90f,
                    ),
                    topLeft = Offset(0f, beamY - 90f),
                    size = androidx.compose.ui.geometry.Size(size.width, 180f),
                )
                drawLine(accent, Offset(0f, beamY), Offset(size.width, beamY), 3f)
            }
        }
    }
}

@Composable
private fun Progress(state: AnalysisUiState) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.analysis_title), style = MaterialTheme.typography.headlineMedium)
        }
        Text(
            stringResource(R.string.analysis_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(vertical = 8.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            StageRow(Icons.Rounded.Image, stringResource(R.string.analysis_stage_preparing), null, AnalysisStage.PREPARING, state)
            StageRow(Icons.Rounded.LocalFlorist, stringResource(R.string.analysis_stage_identifying), stringResource(R.string.analysis_stage_identifying_detail), AnalysisStage.IDENTIFYING, state)
            StageRow(Icons.Rounded.HealthAndSafety, stringResource(R.string.analysis_stage_diagnosing), stringResource(R.string.analysis_stage_diagnosing_detail), AnalysisStage.DIAGNOSING, state)
            StageRow(Icons.Rounded.AutoAwesome, stringResource(R.string.analysis_stage_saving), null, AnalysisStage.SAVING, state)
        }
    }
}

@Composable
private fun StageRow(icon: ImageVector, title: String, detail: String?, stage: AnalysisStage, state: AnalysisUiState) {
    val done = stage in state.completed
    val active = !done && state.stage == stage
    val c = VerdantTheme.colors
    val bubble by animateColorAsState(
        when {
            done -> c.healthy
            active -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHighest
        },
        label = "bubble",
    )
    val textColor = if (done || active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(bubble),
            contentAlignment = Alignment.Center,
        ) {
            when {
                done -> Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                active -> CircularProgressIndicator(strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
                else -> Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = textColor)
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (done || active) 1f else 0.6f))
            }
        }
    }
}

@Composable
private fun Problem(
    icon: ImageVector,
    title: String,
    body: String?,
    footnote: String?,
    onPrimary: () -> Unit,
    onSecondary: (() -> Unit)?,
) {
    val c = VerdantTheme.colors
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(c.warningContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = c.warning, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        if (body != null) {
            Spacer(Modifier.height(8.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        if (footnote != null) {
            Spacer(Modifier.height(8.dp))
            Text(footnote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onPrimary, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 16.dp)) {
            Text(stringResource(R.string.analysis_new_photo))
        }
        if (onSecondary != null) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onSecondary, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 16.dp)) {
                Text(stringResource(R.string.action_retry))
            }
        }
    }
}
