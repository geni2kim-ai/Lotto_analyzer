package com.example.lottoinsight.core.network.datasource

import com.example.lottoinsight.core.model.Draw

data class DrawFetchReport(
    val successful: List<Draw>,
    val failedDrawNos: List<Int>
) {
    val attemptedCount: Int
        get() = successful.size + failedDrawNos.size

    val isPartialSuccess: Boolean
        get() = successful.isNotEmpty() && failedDrawNos.isNotEmpty()
}
