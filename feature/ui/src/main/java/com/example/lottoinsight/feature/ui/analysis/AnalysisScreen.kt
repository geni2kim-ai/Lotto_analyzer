package com.example.lottoinsight.feature.ui.analysis

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lottoinsight.core.model.WeightConfig
import com.example.lottoinsight.feature.analysis.AnalysisViewModel
import com.example.lottoinsight.feature.ui.components.LottoBallRow

@Composable
fun AnalysisScreen(viewModel: AnalysisViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "번호 분석 및 게임 추천",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        WeightConfigCard(
            config = uiState.config,
            isLoading = uiState.isLoading,
            onConfigChange = viewModel::updateConfig,
            onGenerateClick = viewModel::runAnalysisAndGenerate
        )
        Spacer(modifier = Modifier.height(12.dp))

        uiState.userMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
        }

        uiState.latestAnalysisResult?.let { result ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "추천 결과 · 최신 ${result.latestDrawNo}회 · 최근 ${result.recentN}회",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "가중치 ${(result.weightConfig.normalizedFrequency * 100.0).formatOneDecimal()}% / " +
                                "${(result.weightConfig.normalizedConsecutive * 100.0).formatOneDecimal()}% / " +
                                "${(result.weightConfig.normalizedParity * 100.0).formatOneDecimal()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = {
                        val copied = copyAnalysisResultAsImage(context, result)
                        Toast.makeText(
                            context,
                            if (copied) "이미지가 클립보드에 복사되었습니다." else "이미지 복사에 실패했습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "이미지 복사",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("이미지 복사")
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(result.games, key = { it.gameIndex }) { game ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "GAME ${game.gameIndex}",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                LottoBallRow(numbers = game.sortedNumbers, ballSize = 34.dp)
                            }
                            Text(
                                text = "종합 ${(game.totalScore * 100.0).formatOneDecimal()} · " +
                                        "빈도 ${(game.frequencyScore * 100.0).formatOneDecimal()} · " +
                                        "연속 ${(game.consecutiveScore * 100.0).formatOneDecimal()} · " +
                                        "홀짝 ${(game.parityScore * 100.0).formatOneDecimal()}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "홀수 ${game.oddCount}개 · 연속쌍 ${game.pairCount}개 · 최대 중복 ${game.maxOverlap}개",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeightConfigCard(
    config: WeightConfig,
    isLoading: Boolean,
    onConfigChange: (WeightConfig) -> Unit,
    onGenerateClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Selector(
                    label = "게임 수",
                    value = config.gameCount,
                    options = (1..10).toList(),
                    onSelected = { onConfigChange(config.copy(gameCount = it)) },
                    modifier = Modifier.weight(1f)
                )
                Selector(
                    label = "분석 범위",
                    value = config.recentN,
                    options = listOf(10, 20, 30, 50, 100, 200, 300, 500, 1000),
                    onSelected = { onConfigChange(config.copy(recentN = it)) },
                    modifier = Modifier.weight(1f)
                )
            }

            Text("빈도 가중치: ${config.frequencyWeight}%")
            Slider(
                value = config.frequencyWeight.toFloat(),
                onValueChange = { onConfigChange(config.copy(frequencyWeight = it.toInt())) },
                valueRange = 0f..100f
            )
            Text("연속번호 가중치: ${config.consecutiveWeight}%")
            Slider(
                value = config.consecutiveWeight.toFloat(),
                onValueChange = { onConfigChange(config.copy(consecutiveWeight = it.toInt())) },
                valueRange = 0f..100f
            )
            Text("홀짝 구성 가중치: ${config.parityWeight}%")
            Slider(
                value = config.parityWeight.toFloat(),
                onValueChange = { onConfigChange(config.copy(parityWeight = it.toInt())) },
                valueRange = 0f..100f
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "기대값 지수 5% 반영")
                Switch(
                    checked = config.usePrizeIndex,
                    onCheckedChange = { onConfigChange(config.copy(usePrizeIndex = it)) }
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onGenerateClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("추천 번호 조합 생성")
                }
            }
        }
    }
}

@Composable
private fun Selector(
    label: String,
    value: Int,
    options: List<Int>,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("$label: $value")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.toString()) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun Double.formatOneDecimal(): String = "%.1f".format(this)
