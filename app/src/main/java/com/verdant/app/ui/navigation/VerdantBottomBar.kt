package com.verdant.app.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.CenterFocusWeak
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.verdant.app.R
import com.verdant.app.ui.theme.VerdantTheme

/** Floating pill navigation with a raised scan button in the centre. */
@Composable
fun VerdantBottomBar(
    selected: Tab,
    onSelect: (Tab) -> Unit,
    onScan: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 40.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(24.dp, RoundedCornerShape(34.dp), ambientColor = MaterialTheme.colorScheme.primary, spotColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(34.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                BarItem(
                    label = stringResource(R.string.nav_home),
                    icon = if (selected == Tab.HOME) Icons.Rounded.Home else Icons.Outlined.Home,
                    selected = selected == Tab.HOME,
                    onClick = { onSelect(Tab.HOME) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(76.dp))
                BarItem(
                    label = stringResource(R.string.nav_history),
                    icon = if (selected == Tab.HISTORY) Icons.Rounded.History else Icons.Outlined.History,
                    selected = selected == Tab.HISTORY,
                    onClick = { onSelect(Tab.HISTORY) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        ScanButton(onClick = onScan)
    }
}

@Composable
private fun ScanButton(onClick: () -> Unit) {
    val colors = VerdantTheme.colors
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = colors.accent,
        modifier = Modifier
            .size(72.dp)
            .shadow(16.dp, CircleShape, spotColor = colors.accent),
    ) {
        Box(
            Modifier.background(Brush.linearGradient(listOf(colors.accent, com.verdant.app.ui.theme.LimeDeep))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.CenterFocusWeak,
                contentDescription = stringResource(R.string.nav_scan),
                tint = colors.onAccent,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@Composable
private fun BarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "tint",
    )
    val scale by animateFloatAsState(if (selected) 1f else 0.92f, label = "scale")
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(vertical = 8.dp)
            .scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Text(label, style = MaterialTheme.typography.labelMedium, color = tint)
        Box(
            Modifier
                .padding(top = 3.dp)
                .size(width = if (selected) 16.dp else 0.dp, height = 3.dp)
                .clip(CircleShape)
                .background(tint),
        )
    }
}
