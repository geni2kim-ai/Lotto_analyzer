package com.example.lottoinsight.feature.ui.database

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.lottoinsight.core.model.Draw
import com.example.lottoinsight.feature.statistics.DatabaseViewModel
import com.example.lottoinsight.feature.ui.components.LottoBallRow
import com.example.lottoinsight.feature.ui.components.SyncStatusCard

@Composable
fun DatabaseScreen(viewModel: DatabaseViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var showFailedDetails by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.searchResult?.drawNo, uiState.recentDraws) {
        val drawNo = uiState.searchResult?.drawNo ?: return@LaunchedEffect
        val index = uiState.recentDraws.indexOfFirst { it.drawNo == drawNo }
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    if (showFailedDetails) {
        AlertDialog(
            onDismissRequest = { showFailedDetails = false },
            title = { Text("실패 회차 ${uiState.syncFailedDrawNos.size}건") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(uiState.syncFailedDrawNos, key = { it }) { drawNo ->
                        Text(
                            text = "${drawNo}회",
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFailedDetails = false }) {
                    Text("닫기")
                }
            }
        )
    }

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
        Spacer(modifier = Modifier.height(6.dp))

        SyncStatusCard(
            isSyncing = uiState.isSyncing,
            completed = uiState.syncCompleted,
            total = uiState.syncTotal,
            message = uiState.syncMessage,
            isFailed = uiState.syncFailed,
            failedDrawNos = uiState.syncFailedDrawNos,
            onRetry = viewModel::retrySync,
            onShowFailedDetails = if (uiState.syncFailedDrawNos.size > 6) {
                { showFailedDetails = true }
            } else {
                null
            }
        )

        uiState.userMessage?.let {
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "회차 검색/점프",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                label = { Text("회차 번호") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = viewModel::clearSearch) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "검색어 지우기"
                            )
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = viewModel::searchDraw,
                enabled = uiState.searchQuery.isNotBlank()
            ) {
                Text("검색/점프")
            }
        }
        uiState.searchMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = if (uiState.searchResult == null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        }

        uiState.searchResult?.let { draw ->
            Spacer(modifier = Modifier.height(6.dp))
            DrawCard(draw = draw, label = "검색 결과")
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
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(uiState.recentDraws, key = { it.drawNo }) { draw ->
                    DrawCard(draw = draw)
                }
            }
        }
    }
}

@Composable
private fun DrawCard(
    draw: Draw,
    label: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            label?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
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
