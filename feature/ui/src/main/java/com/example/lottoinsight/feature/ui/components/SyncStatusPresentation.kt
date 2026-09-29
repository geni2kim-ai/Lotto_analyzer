package com.example.lottoinsight.feature.ui.components

internal enum class SyncStatusTone {
    FAILURE,
    ACTIVE,
    NORMAL
}

internal data class SyncStatusPresentation(
    val title: String,
    val announcement: String,
    val tone: SyncStatusTone
)

internal fun resolveSyncStatusPresentation(
    isSyncing: Boolean,
    message: String?,
    isFailed: Boolean,
    failedDrawCount: Int
): SyncStatusPresentation {
    val tone = when {
        isFailed -> SyncStatusTone.FAILURE
        isSyncing -> SyncStatusTone.ACTIVE
        else -> SyncStatusTone.NORMAL
    }
    val title = when (tone) {
        SyncStatusTone.FAILURE -> "동기화 확인 필요"
        SyncStatusTone.ACTIVE -> "데이터 동기화 중"
        SyncStatusTone.NORMAL -> "동기화 상태"
    }
    val announcement = buildString {
        append(title)
        message?.let { append(". ").append(it) }
        if (failedDrawCount > 0) {
            append(". 실패 회차 ").append(failedDrawCount).append("건")
        }
    }
    return SyncStatusPresentation(
        title = title,
        announcement = announcement,
        tone = tone
    )
}
