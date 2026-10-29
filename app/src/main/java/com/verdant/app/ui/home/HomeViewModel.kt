package com.verdant.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.app.data.model.ScanRecord
import com.verdant.app.data.model.ScanStats
import com.verdant.app.data.repository.ScanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val recent: List<ScanRecord> = emptyList(),
    val stats: ScanStats = ScanStats(),
    val loading: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(repository: ScanRepository) : ViewModel() {
    val state: StateFlow<HomeUiState> = combine(
        repository.observeRecent(8),
        repository.observeStats(),
    ) { recent, stats -> HomeUiState(recent, stats, loading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
