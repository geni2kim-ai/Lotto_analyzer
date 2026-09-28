package com.example.lottoinsight.feature.ui.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lottoinsight.feature.statistics.StatisticsViewModel
import com.example.lottoinsight.feature.ui.components.LottoBall

@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "기대값·통계",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = viewModel::syncNow,
                enabled = !uiState.isSyncing,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (uiState.isSyncing) "수집 중…" else "데이터 새로고침")
            }
            Button(
                onClick = { viewModel.calculateAndSaveExpectedValues() },
                enabled = !uiState.isExpectedValueSaving,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (uiState.isExpectedValueSaving) "계산 중…" else "기대값 계산")
            }
        }

        uiState.syncMessage?.let {
            Text(
                text = it,
                color = if (uiState.syncFailed) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        }

        if (uiState.isSyncing) {
            Spacer(modifier = Modifier.height(6.dp))
            val progress = if (uiState.syncTotal > 0) {
                (uiState.syncCompleted.toFloat() / uiState.syncTotal.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth()
            )
        }

        uiState.userMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        if (uiState.syncFailed && !uiState.isSyncing) {
            OutlinedButton(
                onClick = viewModel::retrySync,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (uiState.syncFailedDrawNos.isEmpty()) {
                        "동기화 다시 시도"
                    } else {
                        "실패한 ${uiState.syncFailedDrawNos.size}개 회차 다시 시도"
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "저장된 당첨번호: ${uiState.totalDrawsCount}회 · 최신 ${uiState.latestDrawNo}회",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "누적 홀짝 비율: 홀수 ${uiState.oddEvenRatio.first}개 vs 짝수 ${uiState.oddEvenRatio.second}개",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    uiState.expectedValueRunId?.let { runId ->
                        Text(
                            text = "기대값 저장 완료: #$runId · 최근 ${uiState.expectedValueRecentN}회 기준",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "번호별 기대값 지수",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    items = uiState.prizeIndexes.sortedByDescending { it.normalizedScore },
                    key = { it.number }
                ) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LottoBall(number = item.number, size = 32.dp)
                            Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${item.rank}위 · 출현 ${item.appearanceCount}회 (${String.format("%.2f%%", item.appearanceRate * 100.0)})",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "평균 1등 ${item.averageFirstPrize.toLong().let { "%,d원".format(it) }} · " +
                                            "유효 표본 ${item.prizeSampleCount}회 · 점수 ${"%.3f".format(item.normalizedScore)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
