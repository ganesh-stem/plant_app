package com.verdant.app.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verdant.app.BuildConfig
import com.verdant.app.R
import com.verdant.app.data.preferences.ThemeMode
import com.verdant.app.data.preferences.UserPreferences
import com.verdant.app.ml.ModelInfo
import com.verdant.app.ui.components.IconBubble
import com.verdant.app.ui.theme.VerdantTheme
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onReplayOnboarding: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    SettingsContent(
        prefs = prefs,
        models = viewModel.models,
        onBack = onBack,
        onReplayOnboarding = onReplayOnboarding,
        onThemeChange = { viewModel.setTheme(it) },
        onDynamicColorChange = { viewModel.setDynamicColor(it) },
        onClearHistory = { done -> viewModel.clearHistory(done) },
    )
}

@Composable
internal fun SettingsContent(
    prefs: UserPreferences,
    models: List<ModelInfo>,
    onBack: () -> Unit,
    onReplayOnboarding: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onClearHistory: (onDone: () -> Unit) -> Unit,
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clearedText = stringResource(R.string.settings_cleared)
    val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)
            }

            Section(stringResource(R.string.settings_appearance)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBubble(Icons.Rounded.Palette, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer, size = 40.dp, iconSize = 20.dp)
                        Spacer(Modifier.width(14.dp))
                        Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(14.dp))
                    ThemeSelector(prefs.themeMode, onThemeChange)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingRow(
                    icon = Icons.Rounded.AutoAwesome,
                    title = stringResource(R.string.settings_dynamic_color),
                    subtitle = stringResource(if (dynamicSupported) R.string.settings_dynamic_color_body else R.string.settings_dynamic_color_unsupported),
                    enabled = dynamicSupported,
                    onClick = { onDynamicColorChange(!prefs.dynamicColor) },
                    trailing = {
                        Switch(
                            checked = prefs.dynamicColor && dynamicSupported,
                            onCheckedChange = onDynamicColorChange,
                            enabled = dynamicSupported,
                        )
                    },
                )
            }

            Section(stringResource(R.string.settings_models)) {
                models.forEachIndexed { i, model ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ModelRow(model, if (i == 0) Icons.Rounded.LocalFlorist else Icons.Rounded.HealthAndSafety)
                }
                if (models.any { it.isPlaceholder }) {
                    Text(
                        stringResource(R.string.settings_models_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(16.dp),
                    )
                }
            }

            Section(stringResource(R.string.settings_data)) {
                SettingRow(
                    icon = Icons.Outlined.DeleteOutline,
                    title = stringResource(R.string.settings_clear_history),
                    subtitle = stringResource(R.string.settings_clear_history_body),
                    tint = VerdantTheme.colors.danger,
                    container = VerdantTheme.colors.dangerContainer,
                    onClick = { confirmClear = true },
                )
            }

            Section(stringResource(R.string.settings_about)) {
                SettingRow(Icons.Rounded.Lock, stringResource(R.string.settings_privacy), stringResource(R.string.settings_privacy_body))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingRow(Icons.Rounded.Replay, stringResource(R.string.settings_replay_onboarding), null, onClick = onReplayOnboarding, chevron = true)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingRow(Icons.Rounded.Info, stringResource(R.string.settings_version), BuildConfig.VERSION_NAME)
            }

            Text(
                stringResource(R.string.settings_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(vertical = 24.dp, horizontal = 8.dp),
            )
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
            title = { Text(stringResource(R.string.settings_clear_confirm_title)) },
            text = { Text(stringResource(R.string.settings_clear_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClearHistory { scope.launch { snackbar.showSnackbar(clearedText) } }
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 20.dp, bottom = 8.dp),
    )
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer),
        content = content,
    )
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.primary,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    chevron: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBubble(icon, tint, container, size = 40.dp, iconSize = 20.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing != null) trailing()
        if (chevron) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun ModelRow(model: ModelInfo, icon: ImageVector) {
    val c = VerdantTheme.colors
    val statusColor = if (model.isPlaceholder) c.warning else c.healthy
    val subtitle = buildString {
        append(
            if (model.isPlaceholder) stringResource(R.string.settings_model_placeholder)
            else stringResource(R.string.settings_model_loaded, model.source),
        )
        model.classCount?.let { append(" · ").append(pluralStringResource(R.plurals.settings_model_classes, it, it)) }
    }
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBubble(icon, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer, size = 40.dp, iconSize = 20.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(model.displayName, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(statusColor),
        )
    }
}

@Composable
private fun ThemeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = listOf(
        Triple(ThemeMode.SYSTEM, Icons.Rounded.PhoneAndroid, R.string.settings_theme_system),
        Triple(ThemeMode.LIGHT, Icons.Rounded.LightMode, R.string.settings_theme_light),
        Triple(ThemeMode.DARK, Icons.Rounded.DarkMode, R.string.settings_theme_dark),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { (mode, icon, label) ->
            val isSelected = mode == selected
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .selectable(selected = isSelected, onClick = { onSelect(mode) }, role = Role.RadioButton)
                    .padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val fg = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                Icon(icon, contentDescription = null, tint = fg)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(label), style = MaterialTheme.typography.labelLarge, color = fg)
            }
        }
    }
}
