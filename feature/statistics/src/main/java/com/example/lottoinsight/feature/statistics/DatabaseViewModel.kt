package com.example.lottoinsight.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lottoinsight.core.common.AppResult
import com.example.lottoinsight.core.data.repository.DrawSyncReport
import com.example.lottoinsight.core.data.repository.LottoRepository
import com.example.lottoinsight.core.model.Draw
import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DatabaseViewModel(
    private val lottoRepository: LottoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DatabaseUiState())
    val uiState: StateFlow<DatabaseUiState> = _uiState.asStateFlow()
    private var cachedDraws: List<Draw> = emptyList()

    init {
        observeDraws()
    }

    private fun observeDraws() {
        viewModelScope.launch {
            lottoRepository.observeAllDraws()
                .catch {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userMessage = "당첨번호 DB를 읽지 못했습니다."
                        )
                    }
                }
                .collect { draws ->
                    cachedDraws = draws.sortedByDescending { draw -> draw.drawNo }
                    val latest = cachedDraws.firstOrNull()?.drawNo ?: 0
                    _uiState.update { current ->
                        val refreshedSearchResult = current.searchResult?.drawNo?.let { searchedNo ->
                            cachedDraws.firstOrNull { it.drawNo == searchedNo }
                        }
                        current.copy(
                            isLoading = false,
                            totalDrawsCount = cachedDraws.size,
                            latestDrawNo = latest,
                            recentDraws = cachedDraws.take(300),
                            searchResult = refreshedSearchResult ?: current.searchResult
                        )
                    }
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query.filter(Char::isDigit).take(MAX_DRAW_QUERY_LENGTH),
                searchResult = null,
                searchMessage = null
            )
        }
    }

    fun searchDraw() {
        val drawNo = _uiState.value.searchQuery.toIntOrNull()
        if (drawNo == null || drawNo <= 0) {
            _uiState.update {
                it.copy(
                    searchResult = null,
                    searchMessage = "검색할 회차 번호를 입력해 주세요."
                )
            }
            return
        }

        val match = cachedDraws.firstOrNull { it.drawNo == drawNo }
        _uiState.update {
            it.copy(
                searchResult = match,
                searchMessage = if (match == null) {
                    "저장된 DB에 ${drawNo}회 데이터가 없습니다."
                } else {
                    "${drawNo}회 데이터를 찾았습니다."
                }
            )
        }
    }

    fun clearSearch() {
        _uiState.update {
            it.copy(searchQuery = "", searchResult = null, searchMessage = null)
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
        beginSync("공식 데이터를 확인하고 있습니다…", 0)

        try {
            when (val result = lottoRepository.syncDraws(::updateProgress)) {
                is AppResult.Success -> finishSync(result.data, before.size, retry = false)
                is AppResult.Error -> failSync("수집 실패")
                is AppResult.Loading -> failSync("수집이 완료되지 않았습니다.")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            failSync("수집 실패")
        }
    }

    private suspend fun retryFailedDrawsInternal(drawNos: List<Int>) {
        if (_uiState.value.isSyncing) return
        val before = lottoRepository.observeAllDraws().first()
        beginSync("실패 회차를 다시 가져오고 있습니다…", drawNos.size)

        try {
            when (val result = lottoRepository.retryDraws(drawNos, ::updateProgress)) {
                is AppResult.Success -> finishSync(result.data, before.size, retry = true)
                is AppResult.Error -> failSync("재시도 실패", drawNos)
                is AppResult.Loading -> failSync("재시도가 완료되지 않았습니다.", drawNos)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            failSync("재시도 실패", drawNos)
        }
    }

    private fun beginSync(message: String, total: Int) {
        _uiState.update {
            it.copy(
                isSyncing = true,
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
                syncMessage = message,
                syncFailed = true,
                syncFailedDrawNos = failedDrawNos,
                userMessage = "공식 당첨번호 데이터를 가져오지 못했습니다."
            )
        }
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
        const val MAX_DRAW_QUERY_LENGTH = 5
    }
}
