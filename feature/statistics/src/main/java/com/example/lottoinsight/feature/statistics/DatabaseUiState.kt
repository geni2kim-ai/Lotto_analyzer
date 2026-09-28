package com.example.lottoinsight.feature.statistics

import com.example.lottoinsight.core.model.Draw

data class DatabaseUiState(
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val totalDrawsCount: Int = 0,
    val latestDrawNo: Int = 0,
    val recentDraws: List<Draw> = emptyList(),
    val syncCompleted: Int = 0,
    val syncTotal: Int = 0,
    val syncMessage: String? = null,
    val syncFailed: Boolean = false,
    val syncFailedDrawNos: List<Int> = emptyList(),
    val userMessage: String? = null
)
