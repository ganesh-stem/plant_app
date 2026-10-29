package com.verdant.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.app.data.local.ScanEntity
import com.verdant.app.data.model.HealthStatus
import com.verdant.app.data.model.ScanRecord
import com.verdant.app.data.repository.ScanRepository
import com.verdant.app.util.DayBucket
import com.verdant.app.util.dayBucket
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HistoryFilter { ALL, HEALTHY, ATTENTION, FAVORITES }

data class HistoryUiState(
    val query: String = "",
    val filter: HistoryFilter = HistoryFilter.ALL,
    val groups: List<Pair<DayBucket, List<ScanRecord>>> = emptyList(),
    val totalCount: Int = 0,
    val loading: Boolean = true,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: ScanRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(HistoryFilter.ALL)

    /** Deleted rows kept in memory so "Undo" can restore them exactly. */
    private val pendingUndo = mutableMapOf<Long, ScanEntity>()

    val state: StateFlow<HistoryUiState> = combine(repository.observeAll(), query, filter) { all, q, f ->
        val needle = q.trim().lowercase()
        val filtered = all.filter { scan ->
            val matchesFilter = when (f) {
                HistoryFilter.ALL -> true
                HistoryFilter.HEALTHY -> scan.health == HealthStatus.HEALTHY
                HistoryFilter.ATTENTION -> scan.health == HealthStatus.DISEASED
                HistoryFilter.FAVORITES -> scan.isFavorite
            }
            val matchesQuery = needle.isEmpty() ||
                scan.plant.commonName.lowercase().contains(needle) ||
                scan.plant.scientificName.lowercase().contains(needle) ||
                (scan.disease?.name?.lowercase()?.contains(needle) == true)
            matchesFilter && matchesQuery
        }
        HistoryUiState(
            query = q,
            filter = f,
            groups = filtered.groupBy { dayBucket(it.createdAt) }.toSortedMap().toList(),
            totalCount = all.size,
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun setQuery(value: String) { query.value = value }
    fun setFilter(value: HistoryFilter) { filter.value = value }

    fun toggleFavorite(scan: ScanRecord) {
        viewModelScope.launch { repository.setFavorite(scan.id, !scan.isFavorite) }
    }

    fun delete(scan: ScanRecord) {
        viewModelScope.launch {
            val entity = repository.getEntity(scan.id) ?: return@launch
            pendingUndo[scan.id] = entity
            repository.deleteRow(scan.id)
        }
    }

    fun undoDelete(id: Long) {
        val entity = pendingUndo.remove(id) ?: return
        viewModelScope.launch { repository.save(entity) }
    }

    /** Called when the undo snackbar goes away: now the photo can be removed for good. */
    fun finalizeDelete(id: Long) {
        val entity = pendingUndo.remove(id) ?: return
        viewModelScope.launch { repository.purgeImage(entity.imagePath) }
    }

    override fun onCleared() {
        // Leftover photos are swept by the orphan clean-up on next launch.
        pendingUndo.clear()
    }
}
