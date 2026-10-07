package com.example.lottoinsight.core.data.repository

import com.example.lottoinsight.core.common.AppResult
import com.example.lottoinsight.core.model.HistoricalPrizeIndex

interface ExpectedValueRepository {
    suspend fun saveExpectedValueRun(
        recentN: Int,
        latestDrawNo: Int,
        numbers: List<HistoricalPrizeIndex>
    ): AppResult<Long>

    suspend fun getLatestExpectedValueNumbers(recentN: Int): AppResult<List<HistoricalPrizeIndex>>

    suspend fun deleteExpectedValueRun(runId: Long): AppResult<Unit>
}
