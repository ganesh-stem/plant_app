package com.verdant.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Coronavirus
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.verdant.app.R
import com.verdant.app.data.model.Difficulty
import com.verdant.app.data.model.HealthStatus
import com.verdant.app.data.model.Pathogen
import com.verdant.app.data.model.Severity
import com.verdant.app.ui.theme.VerdantTheme

data class StatusStyle(val color: Color, val container: Color, val icon: ImageVector, @StringRes val label: Int)

@Composable
fun HealthStatus.style(severity: Severity? = null): StatusStyle {
    val c = VerdantTheme.colors
    return when (this) {
        HealthStatus.HEALTHY -> StatusStyle(c.healthy, c.healthyContainer, Icons.Rounded.CheckCircle, R.string.status_healthy)
        HealthStatus.DISEASED -> if (severity == Severity.HIGH) {
            StatusStyle(c.danger, c.dangerContainer, Icons.Rounded.WarningAmber, R.string.status_attention)
        } else {
            StatusStyle(c.warning, c.warningContainer, Icons.Rounded.WarningAmber, R.string.status_attention)
        }
        HealthStatus.UNKNOWN -> StatusStyle(
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.surfaceContainerHighest,
            Icons.AutoMirrored.Rounded.HelpOutline,
            R.string.status_unknown,
        )
    }
}

@StringRes
fun Severity.labelRes(): Int = when (this) {
    Severity.LOW -> R.string.severity_low
    Severity.MODERATE -> R.string.severity_moderate
    Severity.HIGH -> R.string.severity_high
}

@StringRes
fun Difficulty.labelRes(): Int = when (this) {
    Difficulty.EASY -> R.string.difficulty_easy
    Difficulty.MODERATE -> R.string.difficulty_moderate
    Difficulty.EXPERT -> R.string.difficulty_expert
}

@StringRes
fun Pathogen.labelRes(): Int = when (this) {
    Pathogen.FUNGAL -> R.string.pathogen_fungal
    Pathogen.BACTERIAL -> R.string.pathogen_bacterial
    Pathogen.VIRAL -> R.string.pathogen_viral
    Pathogen.OOMYCETE -> R.string.pathogen_oomycete
    Pathogen.PEST -> R.string.pathogen_pest
    Pathogen.NUTRIENT -> R.string.pathogen_nutrient
    Pathogen.ENVIRONMENTAL -> R.string.pathogen_environmental
    Pathogen.UNKNOWN -> R.string.pathogen_unknown
}

fun Pathogen.icon(): ImageVector = when (this) {
    Pathogen.FUNGAL, Pathogen.OOMYCETE -> Icons.Rounded.Grass
    Pathogen.BACTERIAL -> Icons.Rounded.Science
    Pathogen.VIRAL -> Icons.Rounded.Coronavirus
    Pathogen.PEST -> Icons.Rounded.BugReport
    Pathogen.NUTRIENT -> Icons.Rounded.WaterDrop
    Pathogen.ENVIRONMENTAL -> Icons.Rounded.WbSunny
    Pathogen.UNKNOWN -> Icons.AutoMirrored.Rounded.HelpOutline
}

/** Compact pill showing the health of a scan. */
@Composable
fun HealthBadge(status: HealthStatus, modifier: Modifier = Modifier, severity: Severity? = null, onImage: Boolean = false) {
    val style = status.style(severity)
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(if (onImage) Color.Black.copy(alpha = 0.45f) else style.container)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(style.color),
        )
        Text(
            text = stringResource(style.label),
            style = MaterialTheme.typography.labelMedium,
            color = if (onImage) Color.White else style.color,
        )
    }
}

/** Three-segment meter for disease severity. */
@Composable
fun SeverityMeter(severity: Severity, color: Color, modifier: Modifier = Modifier) {
    val filled = severity.ordinal + 1
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (i < filled) color else color.copy(alpha = 0.18f)),
            )
        }
    }
}
