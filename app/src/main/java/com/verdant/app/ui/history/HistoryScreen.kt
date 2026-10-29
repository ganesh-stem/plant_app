package com.verdant.app.ui.history

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verdant.app.R
import com.verdant.app.data.model.HealthStatus
import com.verdant.app.data.model.ScanRecord
import com.verdant.app.ui.components.EmptyState
import com.verdant.app.ui.components.HealthBadge
import com.verdant.app.ui.components.PlantImage
import com.verdant.app.ui.theme.VerdantTheme
import com.verdant.app.util.DayBucket
import com.verdant.app.util.asPercent
import com.verdant.app.util.formatTimestamp
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    onOpenScan: (Long) -> Unit,
    onScan: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedText = stringResource(R.string.history_deleted)
    val undoText = stringResource(R.string.action_undo)

    val onDelete: (ScanRecord) -> Unit = { scan ->
        viewModel.delete(scan)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(deletedText, undoText, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(scan.id) else viewModel.finalizeDelete(scan.id)
        }
    }

    HistoryContent(
        state = state,
        snackbar = snackbar,
        onQueryChange = viewModel::setQuery,
        onFilterChange = viewModel::setFilter,
        onOpenScan = onOpenScan,
        onScan = onScan,
        onToggleFavorite = viewModel::toggleFavorite,
        onDelete = onDelete,
    )
}

@Composable
internal fun HistoryContent(
    state: HistoryUiState,
    snackbar: SnackbarHostState,
    onQueryChange: (String) -> Unit,
    onFilterChange: (HistoryFilter) -> Unit,
    onOpenScan: (Long) -> Unit,
    onScan: () -> Unit,
    onToggleFavorite: (ScanRecord) -> Unit,
    onDelete: (ScanRecord) -> Unit,
) {
    val focus = LocalFocusManager.current
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(contentPadding = PaddingValues(bottom = 140.dp)) {
            item {
                Column(
                    Modifier
                        .statusBarsPadding()
                        .padding(start = 24.dp, end = 24.dp, top = 16.dp),
                ) {
                    Text(stringResource(R.string.history_title), style = MaterialTheme.typography.displaySmall)
                    Text(
                        pluralStringResource(R.plurals.history_subtitle, state.totalCount, state.totalCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (state.totalCount > 0) {
                item {
                    TextField(
                        value = state.query,
                        onValueChange = onQueryChange,
                        placeholder = { Text(stringResource(R.string.history_search)) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (state.query.isNotEmpty()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.history_clear_search))
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { focus.clearFocus() }),
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(HistoryFilter.entries) { f ->
                            FilterChip(
                                selected = state.filter == f,
                                onClick = { onFilterChange(f) },
                                label = {
                                    Text(
                                        stringResource(
                                            when (f) {
                                                HistoryFilter.ALL -> R.string.history_filter_all
                                                HistoryFilter.HEALTHY -> R.string.history_filter_healthy
                                                HistoryFilter.ATTENTION -> R.string.history_filter_attention
                                                HistoryFilter.FAVORITES -> R.string.history_filter_favorites
                                            },
                                        ),
                                    )
                                },
                                shape = CircleShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            )
                        }
                    }
                }
            }

            when {
                state.loading -> Unit
                state.totalCount == 0 -> item {
                    EmptyState(
                        title = stringResource(R.string.history_empty_title),
                        body = stringResource(R.string.history_empty_body),
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        action = {
                            Button(onClick = onScan, contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)) {
                                Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.home_hero_scan))
                            }
                        },
                    )
                }
                state.groups.isEmpty() -> item {
                    EmptyState(
                        title = stringResource(R.string.history_no_results_title),
                        body = stringResource(R.string.history_no_results_body),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else -> state.groups.forEach { (bucket, scans) ->
                    item(key = "header_$bucket") {
                        Text(
                            stringResource(
                                when (bucket) {
                                    DayBucket.TODAY -> R.string.history_today
                                    DayBucket.YESTERDAY -> R.string.history_yesterday
                                    DayBucket.THIS_WEEK -> R.string.history_this_week
                                    DayBucket.EARLIER -> R.string.history_earlier
                                },
                            ).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(start = 24.dp, top = 20.dp, bottom = 8.dp)
                                .animateItem(),
                        )
                    }
                    items(scans, key = { it.id }) { scan ->
                        SwipeableScanRow(
                            scan = scan,
                            onClick = { onOpenScan(scan.id) },
                            onToggleFavorite = { onToggleFavorite(scan) },
                            onDelete = { onDelete(scan) },
                            modifier = Modifier
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                                .animateItem(),
                        )
                    }
                }
            }
        }
        SnackbarHost(
            snackbar,
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 110.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableScanRow(
    scan: ScanRecord,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        modifier = modifier,
        backgroundContent = {
            val bg by animateColorAsState(
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) VerdantTheme.colors.danger else VerdantTheme.colors.dangerContainer,
                label = "swipeBg",
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.large)
                    .background(bg)
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.action_delete), tint = androidx.compose.ui.graphics.Color.White)
            }
        },
    ) {
        ScanRow(scan, onClick, onToggleFavorite)
    }
}

@Composable
private fun ScanRow(scan: ScanRecord, onClick: () -> Unit, onToggleFavorite: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            PlantImage(
                scan.imagePath,
                contentDescription = scan.plant.commonName,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(20.dp)),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(scan.plant.commonName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val subtitle = if (scan.health == HealthStatus.DISEASED && scan.disease != null) {
                    scan.disease.name
                } else {
                    scan.plant.scientificName.ifBlank { scan.plantConfidence.asPercent() }
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HealthBadge(scan.health, severity = scan.disease?.severity)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        formatTimestamp(scan.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                    )
                }
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (scan.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = stringResource(if (scan.isFavorite) R.string.result_favorite_remove else R.string.result_favorite_add),
                    tint = if (scan.isFavorite) VerdantTheme.colors.danger else MaterialTheme.colorScheme.outline,
                )
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
