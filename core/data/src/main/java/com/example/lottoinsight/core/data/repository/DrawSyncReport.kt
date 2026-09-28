package com.example.lottoinsight.core.data.repository

data class DrawSyncReport(
    val successfulCount: Int,
    val failedDrawNos: List<Int>
) {
    val attemptedCount: Int
        get() = successfulCount + failedDrawNos.size

    val isPartialSuccess: Boolean
        get() = successfulCount > 0 && failedDrawNos.isNotEmpty()
}
