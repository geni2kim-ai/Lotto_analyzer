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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lottoinsight.core.model.WeightConfig
import com.example.lottoinsight.feature.analysis.AnalysisViewModel
import com.example.lottoinsight.feature.ui.components.LottoBallRow
import com.example.lottoinsight.feature.ui.components.ResponsiveActionContainer

@Composable
fun AnalysisScreen(viewModel: AnalysisViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val result = uiState.latestAnalysisResult
    var configExpanded by rememberSaveable { mutableStateOf(true) }
    var expandedGameIds by rememberSaveable(result?.randomSeed) {
        mutableStateOf(intArrayOf())
    }

    LaunchedEffect(result?.randomSeed) {
        if (result != null) configExpanded = false
    }

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

        if (result == null || configExpanded) {
            WeightConfigCard(
                config = uiState.config,
                isLoading = uiState.isLoading,
                onConfigChange = viewModel::updateConfig,
                onGenerateClick = viewModel::runAnalysisAndGenerate,
                onCollapse = if (result != null) {
                    { configExpanded = false }
                } else {
                    null
                }
            )
        } else {
            ConfigSummaryCard(
                config = uiState.config,
                onExpand = { configExpanded = true },
                onGenerateClick = viewModel::runAnalysisAndGenerate,
                isLoading = uiState.isLoading
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        uiState.userMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
        }

        result?.let { analysis ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "추천 결과",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = {
                        val copied = copyAnalysisResultAsImage(context, analysis)
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

            AnalysisStatusChips(
                latestDrawNo = analysis.latestDrawNo,
                recentN = analysis.recentN,
                usePrizeIndex = analysis.weightConfig.usePrizeIndex,
                algorithmVersion = analysis.algorithmVersion
            )
            ReproducibilityInfo(
                algorithmVersion = analysis.algorithmVersion,
                seed = analysis.randomSeed
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(analysis.games, key = { it.gameIndex }) { game ->
                    val detailsExpanded = game.gameIndex in expandedGameIds
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
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "종합 점수 ${(game.totalScore * 100.0).formatOneDecimal()}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = {
                                    expandedGameIds = if (detailsExpanded) {
                                        expandedGameIds.filterNot { it == game.gameIndex }.toIntArray()
                                    } else {
                                        expandedGameIds + game.gameIndex
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (detailsExpanded) {
                                        Icons.Default.ExpandLess
                                    } else {
                                        Icons.Default.ExpandMore
                                    },
                                    contentDescription = null
                                )
                                Text(if (detailsExpanded) "세부 점수 닫기" else "세부 점수 보기")
                            }
                            if (detailsExpanded) {
                                Text(
                                    text = "빈도 ${(game.frequencyScore * 100.0).formatOneDecimal()} · " +
                                            "연속 ${(game.consecutiveScore * 100.0).formatOneDecimal()} · " +
                                            "홀짝 ${(game.parityScore * 100.0).formatOneDecimal()} · " +
                                            "EV ${(game.prizeScore * 100.0).formatOneDecimal()}",
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
}

@Composable
private fun ReproducibilityInfo(
    algorithmVersion: String,
    seed: Long
) {
    var expanded by rememberSaveable(seed) { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "재현 정보",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "닫기" else "보기")
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "재현 정보 닫기" else "재현 정보 열기"
                    )
                }
            }
            if (expanded) {
                Text(
                    text = "알고리즘: $algorithmVersion",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Seed: $seed",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "동일 데이터·적용 설정·알고리즘 버전은 같은 seed와 추천 결과를 생성합니다.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(
                            AnnotatedString("algorithm=$algorithmVersion\nseed=$seed")
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("재현 정보 복사")
                }
            }
        }
    }
}

@Composable
private fun AnalysisStatusChips(
    latestDrawNo: Int,
    recentN: Int,
    usePrizeIndex: Boolean,
    algorithmVersion: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatusChip(
                text = "최신 ${latestDrawNo}회",
                modifier = Modifier.weight(1f)
            )
            StatusChip(
                text = "분석 ${recentN}회",
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatusChip(
                text = if (usePrizeIndex) "EV 사용" else "EV 미사용",
                modifier = Modifier.weight(1f)
            )
            StatusChip(
                text = "알고리즘 $algorithmVersion",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatusChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ConfigSummaryCard(
    config: WeightConfig,
    onExpand: () -> Unit,
    onGenerateClick: () -> Unit,
    isLoading: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "분석 설정",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${config.gameCount}게임 · 최근 ${config.recentN}회 · " +
                        if (config.usePrizeIndex) "EV 사용" else "EV 미사용",
                style = MaterialTheme.typography.bodySmall
            )
            ResponsiveActionContainer(
                primary = { actionModifier ->
                    Button(
                        onClick = onGenerateClick,
                        enabled = !isLoading,
                        modifier = actionModifier
                    ) {
                        Text(if (isLoading) "분석 중…" else "다시 생성")
                    }
                },
                secondary = { actionModifier ->
                    OutlinedButton(
                        onClick = onExpand,
                        modifier = actionModifier
                    ) {
                        Text("설정 변경")
                    }
                }
            )
        }
    }
}

@Composable
private fun WeightConfigCard(
    config: WeightConfig,
    isLoading: Boolean,
    onConfigChange: (WeightConfig) -> Unit,
    onGenerateClick: () -> Unit,
    onCollapse: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (onCollapse != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("분석 설정", fontWeight = FontWeight.Bold)
                    TextButton(onClick = onCollapse) {
                        Text("접기")
                    }
                }
            }

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
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("분석 중…")
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
