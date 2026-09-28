package com.example.lottoinsight.feature.ui.database

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
import com.example.lottoinsight.feature.statistics.DatabaseViewModel
import com.example.lottoinsight.feature.ui.components.LottoBallRow

@Composable
fun DatabaseScreen(viewModel: DatabaseViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "당첨번호 DB",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = viewModel::syncNow,
                enabled = !uiState.isSyncing
            ) {
                Text(if (uiState.isSyncing) "수집 중…" else "공식 데이터 동기화")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "저장된 회차: ${uiState.totalDrawsCount}건 · 최신 ${uiState.latestDrawNo}회",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
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
            if (uiState.syncTotal > 0) {
                val progress = (uiState.syncCompleted.toFloat() / uiState.syncTotal.toFloat())
                    .coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
            Text(
                text = "최근 300회 표시",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(uiState.recentDraws, key = { it.drawNo }) { draw ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${draw.drawNo}회", fontWeight = FontWeight.Bold)
                                Text(draw.drawDate, style = MaterialTheme.typography.bodySmall)
                            }
                            LottoBallRow(numbers = draw.numbers.sorted(), ballSize = 30.dp)
                            Text(
                                text = "보너스 ${draw.bonus} · 1등 ${draw.firstWinners ?: "-"}명 · " +
                                        "${draw.firstPrize?.let { "%,d원".format(it) } ?: "-"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
