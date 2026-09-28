package com.example.lottoinsight.core.data.repository

import com.example.lottoinsight.core.common.AppResult
import com.example.lottoinsight.core.model.Draw
import kotlinx.coroutines.flow.Flow

interface LottoRepository {
    fun observeAllDraws(): Flow<List<Draw>>
    fun observeLatestDraw(): Flow<Draw?>
    suspend fun getDrawByNo(drawNo: Int): AppResult<Draw>
    suspend fun fetchAndSaveLatestDraws(fetchCount: Int = 10): AppResult<Int>

    suspend fun syncDraws(
        onProgress: ((completed: Int, total: Int) -> Unit)? = null
    ): AppResult<DrawSyncReport>

    suspend fun retryDraws(
        drawNos: List<Int>,
        onProgress: ((completed: Int, total: Int) -> Unit)? = null
    ): AppResult<DrawSyncReport>
}
