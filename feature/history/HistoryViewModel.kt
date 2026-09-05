package com.example.lottoinsight.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lottoinsight.core.common.AppResult
import com.example.lottoinsight.core.data.repository.AnalysisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val analysisRepository: AnalysisRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState(isLoading = true))
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        observeHistory()
    }

    private fun observeHistory() {
        viewModelScope.launch {
            analysisRepository.observeAnalysisRuns()
                .catch { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userMessage = "Failed to load history: ${exception.localizedMessage}"
                        )
                    }
                }
                .collect { runs ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            analysisRuns = runs
                        )
                    }
                }
        }
    }

    fun toggleRunDetails(runId: Long) {
        if (_uiState.value.expandedRunId == runId) {
            _uiState.update { it.copy(expandedRunId = null, expandedGames = emptyList()) }
            return
        }
        fetchAndExpandRunGames(runId)
    }

    private fun fetchAndExpandRunGames(runId: Long) {
        viewModelScope.launch {
            try {
                when (val gamesResult = analysisRepository.getGamesForRun(runId)) {
                    is AppResult.Success -> {
                        _uiState.update {
                            it.copy(
                                expandedRunId = runId,
                                expandedGames = gamesResult.data
                            )
                        }
                    }
                    is AppResult.Error -> {
                        _uiState.update {
                            it.copy(
                                userMessage = "Failed to load run games: ${gamesResult.error.message}"
                            )
                        }
                    }
                    is AppResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        userMessage = "Unexpected error loading games for run $runId: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun userMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
