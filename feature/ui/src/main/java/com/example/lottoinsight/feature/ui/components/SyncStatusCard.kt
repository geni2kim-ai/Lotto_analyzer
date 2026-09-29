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
import androidx.compose.ui.platform.testTag
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

    val presentation = resolveSyncStatusPresentation(
        isSyncing = isSyncing,
        message = message,
        isFailed = isFailed,
        failedDrawCount = failedDrawNos.size
    )

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
                        stateDescription = presentation.announcement
                    },
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when (presentation.tone) {
                        SyncStatusTone.FAILURE -> Icons.Default.ErrorOutline
                        SyncStatusTone.ACTIVE -> Icons.Default.Refresh
                        SyncStatusTone.NORMAL -> Icons.Default.CheckCircle
                    },
                    contentDescription = null,
                    tint = when (presentation.tone) {
                        SyncStatusTone.FAILURE -> MaterialTheme.colorScheme.error
                        SyncStatusTone.ACTIVE,
                        SyncStatusTone.NORMAL -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = presentation.title,
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
                val showFailureDetails =
                    failedDrawNos.size > FAILURE_PREVIEW_LIMIT && onShowFailedDetails != null
                ResponsiveActionContainer(
                    primary = { actionModifier ->
                        OutlinedButton(
                            onClick = onRetry,
                            modifier = actionModifier.testTag(SYNC_RETRY_ACTION_TAG)
                        ) {
                            Text(
                                if (failedDrawNos.isEmpty()) {
                                    "동기화 다시 시도"
                                } else {
                                    "실패한 ${failedDrawNos.size}개 회차 다시 시도"
                                }
                            )
                        }
                    },
                    secondary = if (showFailureDetails) {
                        { actionModifier ->
                            TextButton(
                                onClick = requireNotNull(onShowFailedDetails),
                                modifier = actionModifier.testTag(SYNC_FAILURE_DETAILS_ACTION_TAG)
                            ) {
                                Text("실패 목록 보기")
                            }
                        }
                    } else {
                        null
                    }
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

private const val FAILURE_PREVIEW_LIMIT = 6


internal const val SYNC_RETRY_ACTION_TAG = "sync-retry-action"
internal const val SYNC_FAILURE_DETAILS_ACTION_TAG = "sync-failure-details-action"
