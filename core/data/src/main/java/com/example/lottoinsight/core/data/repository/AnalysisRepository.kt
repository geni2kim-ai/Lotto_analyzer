package com.example.lottoinsight.core.data.repository

import com.example.lottoinsight.core.common.AppResult
import com.example.lottoinsight.core.model.AnalysisResult
import com.example.lottoinsight.core.model.LottoGame
import com.example.lottoinsight.core.model.WeightConfig
import kotlinx.coroutines.flow.Flow

interface AnalysisRepository {
    suspend fun saveAnalysisRun(
        config: WeightConfig,
        latestDrawNo: Int,
        startDrawNo: Int,
        endDrawNo: Int,
        randomSeed: Long,
        algorithmVersion: String,
        games: List<LottoGame>
    ): AppResult<Long>

    suspend fun getRecentAnalysisRuns(limit: Int = 20): AppResult<List<AnalysisResult>>
    suspend fun getGamesForRun(runId: Long): AppResult<List<LottoGame>>
    fun observeAnalysisRuns(): Flow<List<AnalysisResult>>
}
