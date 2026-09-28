package com.example.lottoinsight.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lottoinsight.core.common.AppResult
import com.example.lottoinsight.core.common.Constants
import com.example.lottoinsight.core.data.repository.DrawSyncReport
import com.example.lottoinsight.core.data.repository.ExpectedValueRepository
import com.example.lottoinsight.core.data.repository.LottoRepository
import com.example.lottoinsight.core.engine.calculator.FrequencyCalculator
import com.example.lottoinsight.core.engine.calculator.PrizeIndexCalculator
import com.example.lottoinsight.core.model.Draw
import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
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

    fun retrySync() {
        viewModelScope.launch {
            val failedDrawNos = _uiState.value.syncFailedDrawNos
            if (failedDrawNos.isEmpty()) {
                syncNowInternal()
            } else {
                retryFailedDrawsInternal(failedDrawNos)
            }
        }
    }

    private suspend fun syncNowInternal() {
        if (_uiState.value.isSyncing) return

        val before = lottoRepository.observeAllDraws().first()
        beginSync(
            isLoading = before.isEmpty(),
            message = "공식 당첨번호를 확인하고 있습니다…",
            total = 0
        )

        try {
            when (val result = lottoRepository.syncDraws(::updateProgress)) {
                is AppResult.Success -> finishSync(result.data, before.size, retry = false)
                is AppResult.Error -> failSync("동기화 실패")
                is AppResult.Loading -> Unit
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            failSync("동기화 실패")
        }
    }

    private suspend fun retryFailedDrawsInternal(drawNos: List<Int>) {
        if (_uiState.value.isSyncing) return

        val before = lottoRepository.observeAllDraws().first()
        beginSync(
            isLoading = before.isEmpty(),
            message = "실패 회차를 다시 가져오고 있습니다…",
            total = drawNos.size
        )

        try {
            when (val result = lottoRepository.retryDraws(drawNos, ::updateProgress)) {
                is AppResult.Success -> finishSync(result.data, before.size, retry = true)
                is AppResult.Error -> failSync("재시도 실패", drawNos)
                is AppResult.Loading -> Unit
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            failSync("재시도 실패", drawNos)
        }
    }

    private fun beginSync(isLoading: Boolean, message: String, total: Int) {
        _uiState.update {
            it.copy(
                isSyncing = true,
                isLoading = isLoading,
                syncCompleted = 0,
                syncTotal = total,
                syncMessage = message,
                syncFailed = false,
                syncFailedDrawNos = emptyList(),
                userMessage = null
            )
        }
    }

    private fun updateProgress(completed: Int, total: Int) {
        val percent = if (total <= 0) 0 else completed * 100 / total
        _uiState.update {
            it.copy(
                syncCompleted = completed,
                syncTotal = total,
                syncMessage = "동기화 중: $completed / $total ($percent%)"
            )
        }
    }

    private suspend fun finishSync(report: DrawSyncReport, beforeCount: Int, retry: Boolean) {
        val after = lottoRepository.observeAllDraws().first()
        val added = (after.size - beforeCount).coerceAtLeast(0)
        val failed = report.failedDrawNos
        val message = if (failed.isEmpty()) {
            if (retry) {
                "재시도 완료: ${report.successfulCount}건 복구"
            } else {
                "동기화 완료: ${after.size}건 처리, 신규 ${added}건"
            }
        } else {
            "부분 완료: 총 ${report.attemptedCount}건 중 ${report.successfulCount}건 성공, " +
                    "${failed.size}건 실패 (${formatDrawNos(failed)})"
        }

        _uiState.update {
            it.copy(
                isSyncing = false,
                isLoading = false,
                latestDrawNo = after.maxOfOrNull { draw -> draw.drawNo } ?: 0,
                syncCompleted = report.attemptedCount,
                syncTotal = report.attemptedCount,
                syncMessage = message,
                syncFailed = failed.isNotEmpty(),
                syncFailedDrawNos = failed,
                userMessage = if (failed.isEmpty()) null else "실패한 회차만 다시 시도할 수 있습니다."
            )
        }
    }

    private fun failSync(message: String, failedDrawNos: List<Int> = emptyList()) {
        _uiState.update {
            it.copy(
                isSyncing = false,
                isLoading = false,
                syncMessage = message,
                syncFailed = true,
                syncFailedDrawNos = failedDrawNos,
                userMessage = "공식 당첨번호 데이터를 가져오지 못했습니다."
            )
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
            _uiState.update {
                it.copy(
                    isLoading = false,
                    totalDrawsCount = 0,
                    latestDrawNo = 0,
                    calendarStats = null
                )
            }
            return
        }

        val frequencies = FrequencyCalculator.calculateFrequencies(draws)
        val parityRatio = calculateParityTotals(draws)
        val prizeIndexes = PrizeIndexCalculator.calculateHistoricalPrizeIndexes(draws)

        _uiState.update {
            it.copy(
                isLoading = false,
                totalDrawsCount = draws.size,
                latestDrawNo = draws.maxOfOrNull { draw -> draw.drawNo } ?: 0,
                frequencyMap = frequencies,
                oddEvenRatio = parityRatio,
                prizeIndexes = prizeIndexes,
                calendarStats = null
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

    private fun formatDrawNos(drawNos: List<Int>): String {
        val visible = drawNos.take(MAX_VISIBLE_FAILED_DRAWS).joinToString(", ") { "${it}회" }
        return if (drawNos.size > MAX_VISIBLE_FAILED_DRAWS) {
            "$visible 외 ${drawNos.size - MAX_VISIBLE_FAILED_DRAWS}건"
        } else {
            visible
        }
    }

    fun userMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }

    private companion object {
        const val MAX_VISIBLE_FAILED_DRAWS = 6
    }
}
