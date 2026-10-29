package com.verdant.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.app.data.preferences.ThemeMode
import com.verdant.app.data.preferences.UserPreferences
import com.verdant.app.data.preferences.UserPreferencesRepository
import com.verdant.app.data.repository.ScanRepository
import com.verdant.app.ml.DiseaseDetector
import com.verdant.app.ml.ModelInfo
import com.verdant.app.ml.PlantIdentifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val scanRepository: ScanRepository,
    plantIdentifier: PlantIdentifier,
    diseaseDetector: DiseaseDetector,
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    val models: List<ModelInfo> = listOf(plantIdentifier.info, diseaseDetector.info)

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { preferencesRepository.setThemeMode(mode) }

    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { preferencesRepository.setDynamicColor(enabled) }

    fun clearHistory(onDone: () -> Unit) = viewModelScope.launch {
        scanRepository.deleteAll()
        onDone()
    }
}
