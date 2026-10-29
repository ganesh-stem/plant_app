package com.verdant.app.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import java.io.File
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalInspectionMode

/** Rounded surface card used across the app. */
@Composable
fun VerdantCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, tonalElevation = 0.dp) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(modifier = modifier, shape = shape, color = color, tonalElevation = 0.dp) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) { Text(action, style = MaterialTheme.typography.labelLarge) }
        }
    }
}

/** Loads a locally stored scan photo with a botanical placeholder while loading or on error. */
@Composable
fun PlantImage(
    path: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    // Previews and screenshot tests can't load images asynchronously, so decode inline there.
    if (LocalInspectionMode.current) {
        val bitmap = remember(path) { runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull() }
        Box(modifier) {
            if (bitmap != null) {
                Image(bitmap, contentDescription, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                ImageFallback()
            }
        }
        return
    }
    val context = LocalContext.current
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context).data(File(path)).crossfade(true).build(),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier,
        loading = { ImageFallback() },
        error = { ImageFallback() },
    )
}

@Composable
private fun ImageFallback() {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Eco, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    }
}

/** Icon inside a soft tinted circle. */
@Composable
fun IconBubble(
    icon: ImageVector,
    tint: Color,
    background: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** Friendly empty / error state with an illustration. */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.background),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            LeafCluster(
                modifier = Modifier.size(96.dp),
                primary = MaterialTheme.colorScheme.primary,
                secondary = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                vein = MaterialTheme.colorScheme.primaryContainer,
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}

/** Small rounded label chip. */
@Composable
fun InfoChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}
