package com.example.lottoinsight.feature.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lottoinsight.core.common.AppResult
import com.example.lottoinsight.core.common.Constants
import com.example.lottoinsight.core.data.repository.AnalysisRepository
import com.example.lottoinsight.core.data.repository.LottoRepository
import com.example.lottoinsight.core.engine.AnalysisEngine
import com.example.lottoinsight.core.model.WeightConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException

class AnalysisViewModel(
    private val lottoRepository: LottoRepository,
    private val analysisRepository: AnalysisRepository,
    private val analysisEngine: AnalysisEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    fun updateConfig(newConfig: WeightConfig) {
        _uiState.update { it.copy(config = newConfig) }
    }

    fun runAnalysisAndGenerate() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, userMessage = null, isSavedSuccess = false) }

            try {
                val draws = ensureDrawsAvailable()
                if (draws.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userMessage = "No lottery draws available for analysis"
                        )
                    }
                    return@launch
                }
                when (val engineResult = analysisEngine.analyzeAndGenerate(draws, _uiState.value.config)) {
                    is AppResult.Success -> {
                        val result = engineResult.data
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                latestAnalysisResult = result,
                                userMessage = "Generated ${result.games.size} games successfully!"
                            )
                        }
                        saveAnalysisRun(result)
                    }
                    is AppResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                userMessage = "Analysis failed: ${engineResult.error}"
                            )
                        }
                    }
                    is AppResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = "Unexpected error during analysis"
                    )
                }
            }
        }
    }

    private suspend fun saveAnalysisRun(result: com.example.lottoinsight.core.model.AnalysisResult) {
        val saveResult = analysisRepository.saveAnalysisRun(
            config = result.weightConfig,
            latestDrawNo = result.latestDrawNo,
            startDrawNo = result.dataStartDrawNo,
            endDrawNo = result.dataEndDrawNo,
            randomSeed = result.randomSeed,
            algorithmVersion = result.algorithmVersion,
            games = result.games
        )
        if (saveResult is AppResult.Success) {
            _uiState.update { it.copy(isSavedSuccess = true) }
        }
    }

    private suspend fun ensureDrawsAvailable(): List<com.example.lottoinsight.core.model.Draw> {
        val initialDraws = lottoRepository.observeAllDraws().firstOrNull() ?: emptyList()
        if (initialDraws.size >= Constants.MIN_REQUIRED_DRAWS_FOR_ANALYSIS) {
            return initialDraws
        }
        val syncResult = lottoRepository.syncDraws()
        if (syncResult is AppResult.Error) {
            return emptyList()
        }
        return lottoRepository.observeAllDraws().firstOrNull() ?: emptyList()
    }

    fun userMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
