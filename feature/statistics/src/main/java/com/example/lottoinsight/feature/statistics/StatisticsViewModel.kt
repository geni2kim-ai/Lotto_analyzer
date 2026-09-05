package com.example.lottoinsight.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lottoinsight.core.common.AppResult
import com.example.lottoinsight.core.common.Constants
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException

class StatisticsViewModel(
    private val lottoRepository: LottoRepository,
    private val expectedValueRepository: ExpectedValueRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState(isLoading = true))
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        observeDrawStatistics()
        syncIfNeeded()
    }

    private fun syncIfNeeded() {
        viewModelScope.launch {
            val existingDraws = lottoRepository.observeAllDraws().first()
            if (existingDraws.size >= Constants.MIN_REQUIRED_DRAWS_FOR_ANALYSIS) return@launch
            syncNowInternal()
        }
    }

    fun syncNow() {
        viewModelScope.launch { syncNowInternal() }
    }

    private suspend fun syncNowInternal() {
        if (_uiState.value.isSyncing) return

        val before = lottoRepository.observeAllDraws().first()
        _uiState.update {
            it.copy(
                isSyncing = true,
                isLoading = before.isEmpty(),
                syncMessage = "공식 당첨번호를 확인하고 있습니다…",
                userMessage = null
            )
        }

        try {
            when (lottoRepository.syncDraws()) {
                is AppResult.Success -> {
                    val after = lottoRepository.observeAllDraws().first()
                    val added = (after.size - before.size).coerceAtLeast(0)
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            isLoading = false,
                            latestDrawNo = after.maxOfOrNull { draw -> draw.drawNo } ?: 0,
                            syncMessage = "동기화 완료: ${after.size}건 처리, 신규 ${added}건"
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            isLoading = false,
                            syncMessage = "동기화 실패",
                            userMessage = "공식 당첨번호 데이터를 가져오지 못했습니다."
                        )
                    }
                }
                is AppResult.Loading -> Unit
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            _uiState.update {
                it.copy(
                    isSyncing = false,
                    isLoading = false,
                    syncMessage = "동기화 실패",
                    userMessage = "공식 당첨번호 데이터를 가져오지 못했습니다."
                )
            }
        }
    }

    private fun observeDrawStatistics() {
        viewModelScope.launch {
            lottoRepository.observeAllDraws()
                .catch {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userMessage = "통계 데이터를 계산하지 못했습니다."
                        )
                    }
                }
                .collect { draws -> computeStatistics(draws) }
        }
    }

    private fun computeStatistics(draws: List<Draw>) {
        if (draws.isEmpty()) {
            _uiState.update { it.copy(isLoading = false, totalDrawsCount = 0, latestDrawNo = 0) }
            return
        }

        val frequencies = FrequencyCalculator.calculateFrequencies(draws)
        val parityRatio = calculateParityTotals(draws)
        val prizeIndexes = PrizeIndexCalculator.calculateHistoricalPrizeIndexes(draws)
        val calStats = CalendarStatistics(
            totalRounds = draws.size,
            monthRangeCorrWithPrize = 0.0,
            dayRangeCorrWithPrize = 0.0,
            evenOddCorrWithPrize = 0.0,
            consec2CorrWithPrize = 0.0,
            consec3CorrWithPrize = 0.0,
            monthRangeGroups = emptyList(),
            evenOddGroups = emptyList(),
            consecGroups = emptyList()
        )

        _uiState.update {
            it.copy(
                isLoading = false,
                totalDrawsCount = draws.size,
                latestDrawNo = draws.maxOfOrNull { draw -> draw.drawNo } ?: 0,
                frequencyMap = frequencies,
                oddEvenRatio = parityRatio,
                prizeIndexes = prizeIndexes,
                calendarStats = calStats
            )
        }
    }

    fun calculateAndSaveExpectedValues(recentN: Int = Constants.DEFAULT_RECENT_N) {
        if (_uiState.value.isExpectedValueSaving) return

        viewModelScope.launch {
            val draws = lottoRepository.observeAllDraws().first()
            if (draws.size < Constants.MIN_REQUIRED_DRAWS_FOR_ANALYSIS) {
                _uiState.update { it.copy(userMessage = "기대값 계산에는 당첨번호 데이터가 필요합니다.") }
                return@launch
            }

            val effectiveRecentN = minOf(recentN.coerceAtLeast(1), draws.size)
            val selectedDraws = draws.sortedByDescending { it.drawNo }.take(effectiveRecentN)
            val indexes = PrizeIndexCalculator.calculateHistoricalPrizeIndexes(selectedDraws)
            _uiState.update { it.copy(isExpectedValueSaving = true, userMessage = null) }

            try {
                when (
                    val saveResult = expectedValueRepository.saveExpectedValueRun(
                        recentN = effectiveRecentN,
                        latestDrawNo = selectedDraws.maxOf { it.drawNo },
                        numbers = indexes
                    )
                ) {
                    is AppResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isExpectedValueSaving = false,
                                prizeIndexes = indexes,
                                expectedValueRecentN = effectiveRecentN,
                                expectedValueRunId = saveResult.data,
                                syncMessage = "기대값 계산 완료: 최근 ${effectiveRecentN}회 기준"
                            )
                        }
                    }
                    is AppResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isExpectedValueSaving = false,
                                userMessage = "기대값 결과를 저장하지 못했습니다."
                            )
                        }
                    }
                    is AppResult.Loading -> Unit
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isExpectedValueSaving = false,
                        userMessage = "기대값 결과를 저장하지 못했습니다."
                    )
                }
            }
        }
    }

    private fun calculateParityTotals(draws: List<Draw>): Pair<Int, Int> {
        var totalOdd = 0
        var totalEven = 0
        draws.forEach { draw ->
            draw.numbers.forEach { number ->
                if (number % 2 != 0) totalOdd++ else totalEven++
            }
        }
        return totalOdd to totalEven
    }

    fun userMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
