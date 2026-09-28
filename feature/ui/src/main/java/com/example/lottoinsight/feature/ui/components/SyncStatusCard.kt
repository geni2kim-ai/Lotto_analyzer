package com.example.lottoinsight.feature.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun SyncStatusCard(
    isSyncing: Boolean,
    completed: Int,
    total: Int,
    message: String?,
    isFailed: Boolean,
    failedDrawNos: List<Int>,
    onRetry: () -> Unit,
    onShowFailedDetails: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (!isSyncing && message == null && !isFailed) return

    val title = when {
        isFailed -> "동기화 확인 필요"
        isSyncing -> "데이터 동기화 중"
        else -> "동기화 상태"
    }
    val announcement = buildString {
        append(title)
        message?.let { append(". ").append(it) }
        if (failedDrawNos.isNotEmpty()) {
            append(". 실패 회차 ").append(failedDrawNos.size).append("건")
        }
    }

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        liveRegion = LiveRegionMode.Polite
                        stateDescription = announcement
                    },
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when {
                        isFailed -> Icons.Default.ErrorOutline
                        isSyncing -> Icons.Default.Refresh
                        else -> Icons.Default.CheckCircle
                    },
                    contentDescription = null,
                    tint = when {
                        isFailed -> MaterialTheme.colorScheme.error
                        isSyncing -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall
                )
            }

            message?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isFailed) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            if (isSyncing) {
                if (total > 0) {
                    val progress = (completed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "$completed / $total",
                        style = MaterialTheme.typography.labelSmall
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = "동기화 범위를 확인하는 중",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            }

            if (isFailed && !isSyncing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onRetry,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (failedDrawNos.isEmpty()) {
                                "동기화 다시 시도"
                            } else {
                                "실패한 ${failedDrawNos.size}개 회차 다시 시도"
                            }
                        )
                    }
                    if (failedDrawNos.size > FAILURE_PREVIEW_LIMIT && onShowFailedDetails != null) {
                        TextButton(onClick = onShowFailedDetails) {
                            Text("실패 목록 보기")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

private const val FAILURE_PREVIEW_LIMIT = 6
