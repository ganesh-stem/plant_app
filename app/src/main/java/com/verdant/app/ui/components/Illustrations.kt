package com.verdant.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

/** Draws a single stylised leaf with a midrib, pointing up, centred at [center]. */
fun DrawScope.drawLeaf(
    center: Offset,
    length: Float,
    width: Float,
    rotation: Float,
    fill: Brush,
    vein: Color,
) {
    rotate(rotation, pivot = center) {
        val top = Offset(center.x, center.y - length / 2)
        val bottom = Offset(center.x, center.y + length / 2)
        val path = Path().apply {
            moveTo(bottom.x, bottom.y)
            cubicTo(center.x - width, center.y + length * 0.15f, center.x - width * 0.8f, center.y - length * 0.35f, top.x, top.y)
            cubicTo(center.x + width * 0.8f, center.y - length * 0.35f, center.x + width, center.y + length * 0.15f, bottom.x, bottom.y)
            close()
        }
        drawPath(path, fill)
        drawLine(vein, bottom, Offset(top.x, top.y + length * 0.12f), strokeWidth = width * 0.06f, cap = StrokeCap.Round)
        for (i in 1..3) {
            val y = bottom.y - length * (0.22f * i)
            val spread = width * (0.55f - i * 0.08f)
            drawLine(vein, Offset(center.x, y), Offset(center.x - spread, y - length * 0.1f), strokeWidth = width * 0.035f, cap = StrokeCap.Round)
            drawLine(vein, Offset(center.x, y), Offset(center.x + spread, y - length * 0.1f), strokeWidth = width * 0.035f, cap = StrokeCap.Round)
        }
    }
}

/** A gently swaying cluster of leaves used in the hero card and onboarding. */
@Composable
fun LeafCluster(
    modifier: Modifier = Modifier,
    primary: Color,
    secondary: Color,
    vein: Color,
    animate: Boolean = true,
) {
    val transition = rememberInfiniteTransition(label = "sway")
    val sway by transition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sway",
    )
    val angle = if (animate) sway else 0f
    Canvas(modifier) {
        val s = size.minDimension
        val c = Offset(size.width / 2, size.height / 2)
        val stem = Offset(c.x, c.y + s * 0.42f)
        drawLeaf(Offset(c.x - s * 0.2f, c.y + s * 0.05f), s * 0.55f, s * 0.2f, -38f + angle, Brush.verticalGradient(listOf(secondary, secondary.copy(alpha = 0.75f))), vein.copy(alpha = 0.5f))
        drawLeaf(Offset(c.x + s * 0.2f, c.y + s * 0.02f), s * 0.6f, s * 0.22f, 36f - angle, Brush.verticalGradient(listOf(secondary, secondary.copy(alpha = 0.8f))), vein.copy(alpha = 0.5f))
        drawLeaf(Offset(c.x, c.y - s * 0.08f), s * 0.72f, s * 0.26f, angle * 0.6f, Brush.verticalGradient(listOf(primary, primary.copy(alpha = 0.85f))), vein)
        drawLine(vein.copy(alpha = 0.6f), stem, Offset(c.x, c.y + s * 0.22f), strokeWidth = s * 0.02f, cap = StrokeCap.Round)
    }
}

/** Soft, slowly drifting colour blobs for atmospheric backgrounds. */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    colors: List<Color>,
    content: @Composable () -> Unit = {},
) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "aurora",
    )
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            colors.forEachIndexed { i, color ->
                val phase = (t + i * 0.33f) % 1f
                val cx = w * (0.15f + 0.7f * ((i * 0.37f + phase * 0.3f) % 1f))
                val cy = h * (0.1f + 0.25f * i + 0.08f * kotlin.math.sin(phase * 6.28f))
                val r = maxOf(w, h) * 0.45f
                drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), Offset(cx, cy), r), r, Offset(cx, cy))
            }
        }
        content()
    }
}

/** Animated concentric rings, e.g. behind icons on the onboarding pages. */
@Composable
fun PulseRings(modifier: Modifier = Modifier, color: Color) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val p by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "pulse")
    Canvas(modifier) {
        val max = size.minDimension / 2
        for (i in 0 until 3) {
            val f = (p + i / 3f) % 1f
            drawCircle(color.copy(alpha = (1f - f) * 0.35f), radius = max * (0.45f + 0.55f * f), style = Stroke(width = 2f))
        }
    }
}

/** Rounded corner brackets that frame the subject in the camera. */
fun DrawScope.drawCornerBrackets(color: Color, bounds: Rect, arm: Float, stroke: Float, radius: Float) {
    val l = bounds.left; val t = bounds.top; val r = bounds.right; val b = bounds.bottom
    val style = Stroke(width = stroke, cap = StrokeCap.Round)
    fun corner(path: Path) = drawPath(path, color, style = style)
    corner(Path().apply { moveTo(l, t + arm); lineTo(l, t + radius); quadraticTo(l, t, l + radius, t); lineTo(l + arm, t) })
    corner(Path().apply { moveTo(r - arm, t); lineTo(r - radius, t); quadraticTo(r, t, r, t + radius); lineTo(r, t + arm) })
    corner(Path().apply { moveTo(r, b - arm); lineTo(r, b - radius); quadraticTo(r, b, r - radius, b); lineTo(r - arm, b) })
    corner(Path().apply { moveTo(l + arm, b); lineTo(l + radius, b); quadraticTo(l, b, l, b - radius); lineTo(l, b - arm) })
}
