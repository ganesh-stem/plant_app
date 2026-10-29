package com.verdant.app.ui.analysis

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.verdant.app.data.image.ImageStorage
import com.verdant.app.data.knowledge.KnowledgeBase
import com.verdant.app.domain.AnalysisOutcome
import com.verdant.app.domain.AnalysisStage
import com.verdant.app.domain.AnalyzePlantUseCase
import com.verdant.app.domain.FailureReason
import com.verdant.app.ml.LabelUtils
import com.verdant.app.ui.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AnalysisStatus {
    data object Running : AnalysisStatus
    data class Done(val scanId: Long) : AnalysisStatus
    data class NoPlant(val guessName: String?, val guessConfidence: Float?) : AnalysisStatus
    data class Error(val reason: FailureReason) : AnalysisStatus
}

data class AnalysisUiState(
    val imageUri: String,
    val stage: AnalysisStage = AnalysisStage.PREPARING,
    val completed: Set<AnalysisStage> = emptySet(),
    val status: AnalysisStatus = AnalysisStatus.Running,
)

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val analyze: AnalyzePlantUseCase,
    private val knowledgeBase: KnowledgeBase,
    private val imageStorage: ImageStorage,
) : ViewModel() {

    private val imageUri = savedStateHandle.toRoute<Route.Analysis>().imageUri
    private val _state = MutableStateFlow(AnalysisUiState(imageUri))
    val state: StateFlow<AnalysisUiState> = _state.asStateFlow()
    private var job: Job? = null

    init {
        // After process death, don't re-run (and duplicate) a scan that already finished.
        val savedId = savedStateHandle.get<Long>(KEY_SCAN_ID)
        if (savedId != null) {
            _state.update { it.copy(status = AnalysisStatus.Done(savedId), completed = AnalysisStage.entries.toSet()) }
        } else {
            start()
        }
    }

    fun retry() = start()

    fun cancel() {
        job?.cancel()
    }

    private fun start() {
        job?.cancel()
        _state.value = AnalysisUiState(imageUri)
        job = viewModelScope.launch {
            var stageStartedAt = System.currentTimeMillis()
            val outcome = analyze(imageUri) { stage ->
                // Give each step a moment on screen so fast models still read clearly.
                holdMinimum(stageStartedAt)
                stageStartedAt = System.currentTimeMillis()
                _state.update { s ->
                    s.copy(stage = stage, completed = AnalysisStage.entries.filter { it.ordinal < stage.ordinal }.toSet())
                }
            }
            holdMinimum(stageStartedAt)
            when (outcome) {
                is AnalysisOutcome.Success -> {
                    savedStateHandle[KEY_SCAN_ID] = outcome.scanId
                    _state.update { it.copy(completed = AnalysisStage.entries.toSet()) }
                    delay(350)
                    _state.update { it.copy(status = AnalysisStatus.Done(outcome.scanId)) }
                }
                is AnalysisOutcome.NoPlantFound -> _state.update {
                    it.copy(
                        status = AnalysisStatus.NoPlant(
                            guessName = outcome.bestGuess?.label?.let { label ->
                                knowledgeBase.findPlant(label)?.commonName ?: LabelUtils.prettify(label)
                            },
                            guessConfidence = outcome.bestGuess?.confidence,
                        ),
                    )
                }
                is AnalysisOutcome.Failed -> _state.update { it.copy(status = AnalysisStatus.Error(outcome.reason)) }
            }
        }
    }

    private suspend fun holdMinimum(since: Long) {
        val elapsed = System.currentTimeMillis() - since
        if (elapsed < MIN_STAGE_MS) delay(MIN_STAGE_MS - elapsed)
    }

    override fun onCleared() {
        imageStorage.discardCapture(Uri.parse(imageUri))
    }

    private companion object {
        const val KEY_SCAN_ID = "completed_scan_id"
        const val MIN_STAGE_MS = 550L
    }
}
