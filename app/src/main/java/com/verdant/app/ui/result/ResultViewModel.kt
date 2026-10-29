package com.verdant.app.ui.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.verdant.app.data.model.ScanRecord
import com.verdant.app.data.repository.ScanRepository
import com.verdant.app.ui.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ResultUiState {
    data object Loading : ResultUiState
    data object NotFound : ResultUiState
    data class Loaded(val scan: ScanRecord) : ResultUiState
}

@HiltViewModel
class ResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ScanRepository,
) : ViewModel() {

    private val scanId = savedStateHandle.toRoute<Route.Result>().scanId

    private val deleting = MutableStateFlow(false)

    val state: StateFlow<ResultUiState> = combine(repository.observe(scanId), deleting) { scan, isDeleting -> scan to isDeleting }
        .runningFold<Pair<ScanRecord?, Boolean>, ResultUiState>(ResultUiState.Loading) { previous, (scan, isDeleting) ->
            when {
                scan != null -> ResultUiState.Loaded(scan)
                // Keep showing the scan while we navigate away after deleting it.
                isDeleting && previous is ResultUiState.Loaded -> previous
                else -> ResultUiState.NotFound
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResultUiState.Loading)

    fun toggleFavorite() {
        val scan = (state.value as? ResultUiState.Loaded)?.scan ?: return
        viewModelScope.launch { repository.setFavorite(scan.id, !scan.isFavorite) }
    }

    fun delete(onDeleted: () -> Unit) {
        deleting.value = true
        viewModelScope.launch {
            repository.delete(scanId)
            onDeleted()
        }
    }
}
