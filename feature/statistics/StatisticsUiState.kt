package com.example.lottoinsight.feature.statistics

import com.example.lottoinsight.core.model.CalendarStatistics
import com.example.lottoinsight.core.model.HistoricalPrizeIndex

data class StatisticsUiState(
    val isLoading: Boolean = false,
    val totalDrawsCount: Int = 0,
    val frequencyMap: Map<Int, Int> = emptyMap(),
    val oddEvenRatio: Pair<Int, Int> = Pair(0, 0),
    val prizeIndexes: List<HistoricalPrizeIndex> = emptyList(),
    val calendarStats: CalendarStatistics? = null,
    val userMessage: String? = null
)
