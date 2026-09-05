package com.example.lottoinsight.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lottoinsight.core.data.repository.ExpectedValueRepository
import com.example.lottoinsight.core.data.repository.LottoRepository
import com.example.lottoinsight.core.engine.calculator.FrequencyCalculator
import com.example.lottoinsight.core.engine.calculator.PrizeIndexCalculator
import com.example.lottoinsight.core.model.CalendarStatistics
import com.example.lottoinsight.core.model.Draw
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StatisticsViewModel(
    private val lottoRepository: LottoRepository,
    private val expectedValueRepository: ExpectedValueRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState(isLoading = true))
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        observeDrawStatistics()
    }

    private fun observeDrawStatistics() {
        viewModelScope.launch {
            lottoRepository.observeAllDraws()
                .catch { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userMessage = "Failed to compute statistics: ${exception.localizedMessage}"
                        )
                    }
                }
                .collect { draws ->
                    computeStatistics(draws)
                }
        }
    }

    private fun computeStatistics(draws: List<Draw>) {
        if (draws.isEmpty()) {
            _uiState.update { it.copy(isLoading = false, totalDrawsCount = 0) }
            return
        }

        val frequencies = FrequencyCalculator.calculateFrequencies(draws)
        val parityRatio = calculateParityTotals(draws)
        val prizeIndexes = PrizeIndexCalculator.calculateHistoricalPrizeIndexes(draws)
        val monthCountMap = extractMonthCountMap(draws)

        val calStats = CalendarStatistics(
            yearFilter = null,
            monthCountMap = monthCountMap
        )

        _uiState.update {
            it.copy(
                isLoading = false,
                totalDrawsCount = draws.size,
                frequencyMap = frequencies,
                oddEvenRatio = parityRatio,
                prizeIndexes = prizeIndexes,
                calendarStats = calStats
            )
        }
    }

    private fun calculateParityTotals(draws: List<Draw>): Pair<Int, Int> {
        var totalOdd = 0
        var totalEven = 0
        draws.forEach { draw ->
            draw.numbers.forEach { num ->
                if (num % 2 != 0) totalOdd++ else totalEven++
            }
        }
        return Pair(totalOdd, totalEven)
    }

    private fun extractMonthCountMap(draws: List<Draw>): Map<Int, Int> {
        val monthCountMap = mutableMapOf<Int, Int>()
        draws.forEach { draw ->
            if (draw.drawDate.length >= 7) {
                val monthStr = draw.drawDate.substring(5, 7)
                val monthInt = monthStr.toIntOrNull() ?: 1
                monthCountMap[monthInt] = (monthCountMap[monthInt] ?: 0) + 1
            }
        }
        return monthCountMap
    }

    fun userMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
