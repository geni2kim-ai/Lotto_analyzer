package com.example.lottoinsight.core.network.datasource

import com.example.lottoinsight.core.common.AppResult
import com.example.lottoinsight.core.model.Draw

interface LottoRemoteDataSource {
    suspend fun fetchDraw(drawNo: Int): AppResult<Draw>

    suspend fun fetchAllDraws(
        existingMaxDrawNo: Int? = null,
        onProgress: ((completed: Int, total: Int) -> Unit)? = null
    ): AppResult<DrawFetchReport>

    /**
     * Aggregates per-draw failures into [DrawFetchReport.failedDrawNos].
     * Once the range is known this operation does not use AppResult.Error;
     * callers can persist successes and retry failed draw numbers explicitly.
     */
    suspend fun fetchDrawRange(
        startDrawNo: Int,
        endDrawNo: Int,
        onProgress: ((completed: Int, total: Int) -> Unit)? = null
    ): DrawFetchReport

    /**
     * Same aggregate contract as [fetchDrawRange] for an explicit target set.
     */
    suspend fun fetchDraws(
        drawNos: List<Int>,
        onProgress: ((completed: Int, total: Int) -> Unit)? = null
    ): DrawFetchReport

    suspend fun fetchLatestDrawNo(existingMaxDrawNo: Int? = null): AppResult<Int>
}
