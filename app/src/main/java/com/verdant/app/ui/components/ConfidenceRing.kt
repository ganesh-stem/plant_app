package com.verdant.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Circular gauge that animates from 0 to [value] (0..1) with the percentage in the centre. */
@Composable
fun ConfidenceRing(
    value: Float,
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    strokeWidth: Dp = 7.dp,
    colors: List<Color> = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary),
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    caption: String? = null,
) {
    val inspection = LocalInspectionMode.current
    val progress = remember { Animatable(if (inspection) value.coerceIn(0f, 1f) else 0f) }
    LaunchedEffect(value) {
        progress.animateTo(value.coerceIn(0f, 1f), tween(1100, easing = FastOutSlowInEasing))
    }
    val percent = (progress.value * 100).roundToInt()
    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "${(value * 100).roundToInt()} percent ${caption.orEmpty()}" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = strokeWidth.toPx() / 2
            val arcSize = androidx.compose.ui.geometry.Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(trackColor, -90f, 360f, false, topLeft, arcSize, style = stroke)
            drawArc(Brush.sweepGradient(colors + colors.first()), -90f, 360f * progress.value, false, topLeft, arcSize, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$percent%",
                style = if (size >= 72.dp) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (caption != null) {
                Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
