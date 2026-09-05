package com.example.lottoinsight.feature.analysis

import com.example.lottoinsight.core.model.AnalysisResult
import com.example.lottoinsight.core.model.WeightConfig

data class AnalysisUiState(
    val isLoading: Boolean = false,
    val config: WeightConfig = WeightConfig(),
    val latestAnalysisResult: AnalysisResult? = null,
    val userMessage: String? = null,
    val isSavedSuccess: Boolean = false
)
