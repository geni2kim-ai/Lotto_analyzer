package com.example.lottoinsight.core.network.datasource

import com.example.lottoinsight.core.common.AppResult
import com.example.lottoinsight.core.model.Draw

interface LottoRemoteDataSource {
    suspend fun fetchDraw(drawNo: Int): AppResult<Draw>

    suspend fun fetchAllDraws(
        existingMaxDrawNo: Int? = null,
        onProgress: ((completed: Int, total: Int) -> Unit)? = null
    ): AppResult<DrawFetchReport>

    suspend fun fetchDrawRange(
        startDrawNo: Int,
        endDrawNo: Int,
        onProgress: ((completed: Int, total: Int) -> Unit)? = null
    ): AppResult<DrawFetchReport>

    suspend fun fetchDraws(
        drawNos: List<Int>,
        onProgress: ((completed: Int, total: Int) -> Unit)? = null
    ): AppResult<DrawFetchReport>

    suspend fun fetchLatestDrawNo(existingMaxDrawNo: Int? = null): AppResult<Int>
}
