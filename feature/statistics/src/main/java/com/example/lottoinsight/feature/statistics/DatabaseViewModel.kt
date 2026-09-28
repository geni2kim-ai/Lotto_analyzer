package com.example.lottoinsight.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lottoinsight.core.common.AppResult
import com.example.lottoinsight.core.data.repository.LottoRepository
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
                    val latest = draws.maxOfOrNull { it.drawNo } ?: 0
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            totalDrawsCount = draws.size,
                            latestDrawNo = latest,
                            recentDraws = draws.sortedByDescending { draw -> draw.drawNo }.take(300)
                        )
                    }
                }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            if (_uiState.value.isSyncing) return@launch
            val before = lottoRepository.observeAllDraws().first()
            _uiState.update {
                it.copy(
                    isSyncing = true,
                    syncCompleted = 0,
                    syncTotal = 0,
                    syncMessage = "공식 데이터를 확인하고 있습니다…",
                    userMessage = null
                )
            }

            try {
                when (
                    lottoRepository.syncDraws { completed, total ->
                        val percent = if (total <= 0) 0 else completed * 100 / total
                        _uiState.update {
                            it.copy(
                                syncCompleted = completed,
                                syncTotal = total,
                                syncMessage = "동기화 중: $completed / $total ($percent%)"
                            )
                        }
                    }
                ) {
                    is AppResult.Success -> {
                        val after = lottoRepository.observeAllDraws().first()
                        val added = (after.size - before.size).coerceAtLeast(0)
                        _uiState.update {
                            it.copy(
                                isSyncing = false,
                                syncMessage = "동기화 완료: ${after.size}건 처리, 신규 ${added}건"
                            )
                        }
                    }
                    is AppResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isSyncing = false,
                                syncMessage = "수집 실패",
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
                        syncMessage = "수집 실패",
                        userMessage = "공식 당첨번호 데이터를 가져오지 못했습니다."
                    )
                }
            }
        }
    }

    fun userMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
