package com.example.lottoinsight.feature.history

import com.example.lottoinsight.core.model.AnalysisResult
import com.example.lottoinsight.core.model.LottoGame

data class HistoryUiState(
    val isLoading: Boolean = false,
    val analysisRuns: List<AnalysisResult> = emptyList(),
    val expandedRunId: Long? = null,
    val expandedGames: List<LottoGame> = emptyList(),
    val userMessage: String? = null
)
