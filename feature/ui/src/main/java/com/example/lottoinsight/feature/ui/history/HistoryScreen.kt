package com.example.lottoinsight.feature.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lottoinsight.feature.history.HistoryViewModel
import com.example.lottoinsight.feature.ui.components.LottoBallRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "추천 이력 관리",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (uiState.analysisRuns.isEmpty()) {
            Text(text = "저장된 분석 추천 이력이 없습니다.")
        } else {
            LazyColumn {
                items(uiState.analysisRuns, key = { it.runId }) { run ->
                    val isExpanded = uiState.expandedRunId == run.runId
                    val dateStr = SimpleDateFormat(
                        "yyyy-MM-dd HH:mm",
                        Locale.getDefault()
                    ).format(Date(run.createdAt))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { viewModel.toggleRunDetails(run.runId) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dateStr,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "기준 ${run.latestDrawNo}회 · 최근 ${run.recentN}회 · ${run.gameCount}게임",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                TextButton(
                                    onClick = { viewModel.toggleRunDetails(run.runId) }
                                ) {
                                    Text(if (isExpanded) "닫기" else "상세")
                                    Icon(
                                        imageVector = if (isExpanded) {
                                            Icons.Default.ExpandLess
                                        } else {
                                            Icons.Default.ExpandMore
                                        },
                                        contentDescription = if (isExpanded) "상세 닫기" else "상세 열기"
                                    )
                                }
                            }
                            Text(
                                text = "실행 #${run.runId} · " +
                                        if (run.weightConfig.usePrizeIndex) "EV 사용" else "EV 미사용",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Seed ${run.randomSeed} · 알고리즘 ${run.algorithmVersion}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "가중치 - 빈도: ${run.weightConfig.frequencyWeight}, " +
                                        "연속: ${run.weightConfig.consecutiveWeight}, " +
                                        "홀짝: ${run.weightConfig.parityWeight}",
                                style = MaterialTheme.typography.bodySmall
                            )

                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "추천 게임 리스트",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                uiState.expandedGames.forEach { game ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "게임 ${game.gameIndex}: ",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        LottoBallRow(numbers = game.numbers)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
