package com.verdant.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.verdant.app.data.preferences.ThemeMode

private val LightColors = lightColorScheme(
    primary = LightPrimary, onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer, onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary, onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer, onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary, tertiaryContainer = LightTertiaryContainer, onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground, onBackground = LightOnBackground,
    surface = LightSurface, onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant, onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline, outlineVariant = LightOutlineVariant,
    surfaceContainerLowest = LightSurfaceContainerLowest, surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer, surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    error = DangerLight, errorContainer = DangerContainerLight,
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary, onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer, onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary, onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer, onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary, onTertiary = Forest,
    tertiaryContainer = DarkTertiaryContainer, onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground, onBackground = DarkOnBackground,
    surface = DarkSurface, onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant, onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline, outlineVariant = DarkOutlineVariant,
    surfaceContainerLowest = DarkSurfaceContainerLowest, surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer, surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    error = DangerDark, errorContainer = DangerContainerDark,
)

/** Colours outside the Material scheme: health states and brand accents. */
@Immutable
data class VerdantColors(
    val healthy: Color,
    val healthyContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val accent: Color,
    val onAccent: Color,
    val isDark: Boolean,
)

private val LightExtended = VerdantColors(
    healthy = HealthyLight, healthyContainer = HealthyContainerLight,
    warning = WarningLight, warningContainer = WarningContainerLight,
    danger = DangerLight, dangerContainer = DangerContainerLight,
    heroStart = Forest, heroEnd = Leaf, accent = Lime, onAccent = Forest, isDark = false,
)

private val DarkExtended = VerdantColors(
    healthy = HealthyDark, healthyContainer = HealthyContainerDark,
    warning = WarningDark, warningContainer = WarningContainerDark,
    danger = DangerDark, dangerContainer = DangerContainerDark,
    heroStart = Color(0xFF0E2E22), heroEnd = Color(0xFF145A3D), accent = Lime, onAccent = Forest, isDark = true,
)

val LocalVerdantColors = staticCompositionLocalOf { LightExtended }

object VerdantTheme {
    val colors: VerdantColors
        @Composable get() = LocalVerdantColors.current
}

@Composable
fun VerdantTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalVerdantColors provides if (darkTheme) DarkExtended else LightExtended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VerdantTypography,
            shapes = VerdantShapes,
        ) {
            // Text placed directly on the background (not inside a Surface) gets the right colour.
            CompositionLocalProvider(LocalContentColor provides colorScheme.onBackground, content = content)
        }
    }
}
