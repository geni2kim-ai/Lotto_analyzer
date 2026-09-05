package com.example.lottoinsight.feature.statistics

import com.example.lottoinsight.core.model.Draw

data class DatabaseUiState(
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val totalDrawsCount: Int = 0,
    val latestDrawNo: Int = 0,
    val recentDraws: List<Draw> = emptyList(),
    val syncMessage: String? = null,
    val userMessage: String? = null
)
