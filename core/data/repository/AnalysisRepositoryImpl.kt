package com.example.lottoinsight.core.data.repository

import com.example.lottoinsight.core.common.AppError
import com.example.lottoinsight.core.common.AppResult
import com.example.lottoinsight.core.database.dao.AnalysisDao
import com.example.lottoinsight.core.database.entity.AnalysisGameEntity
import com.example.lottoinsight.core.database.entity.AnalysisRunEntity
import com.example.lottoinsight.core.model.AnalysisResult
import com.example.lottoinsight.core.model.LottoGame
import com.example.lottoinsight.core.model.WeightConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Random

class AnalysisRepositoryImpl(
    private val analysisDao: AnalysisDao
) : AnalysisRepository {

    override suspend fun saveAnalysisRun(
        config: WeightConfig,
        latestDrawNo: Int,
        startDrawNo: Int,
        endDrawNo: Int,
        games: List<LottoGame>
    ): AppResult<Long> {
        if (games.isEmpty()) {
            return AppResult.Error(AppError.InsufficientData("Cannot save empty analysis games list"))
        }

        return try {
            val runEntity = AnalysisRunEntity(
                createdAt = System.currentTimeMillis(),
                latestDrawNo = latestDrawNo,
                recentN = config.recentN,
                gameCount = games.size,
                weightFrequency = config.frequencyWeight,
                weightConsecutive = config.consecutiveWeight,
                weightParity = config.parityWeight,
                usePrizeIndex = config.usePrizeIndex,
                randomSeed = Random().nextLong(),
                algorithmVersion = "1.0.0",
                dataStartDrawNo = startDrawNo,
                dataEndDrawNo = endDrawNo
            )

            val gameEntities = games.map { AnalysisGameEntity.fromDomain(0, it) }
            val runId = analysisDao.saveAnalysisRun(runEntity, gameEntities)
            AppResult.Success(runId)
        } catch (e: Exception) {
            AppResult.Error(AppError.DatabaseError("Failed to save analysis run: ${e.localizedMessage}"))
        }
    }

    override suspend fun getRecentAnalysisRuns(limit: Int): AppResult<List<AnalysisResult>> {
        if (limit <= 0) {
            return AppResult.Error(AppError.DatabaseError("Limit must be positive integer, got $limit"))
        }

        return try {
            val runs = analysisDao.getRecentAnalysisRuns(limit)
            val results = runs.map { run ->
                val games = analysisDao.getGamesForRun(run.id).map { it.toDomain() }
                mapRunEntityToAnalysisResult(run, games)
            }
            AppResult.Success(results)
        } catch (e: Exception) {
            AppResult.Error(AppError.DatabaseError("Failed to fetch analysis runs: ${e.localizedMessage}"))
        }
    }

    override suspend fun getGamesForRun(runId: Long): AppResult<List<LottoGame>> {
        if (runId <= 0) {
            return AppResult.Error(AppError.DatabaseError("Invalid runId parameter: $runId"))
        }
        return try {
            val gameEntities = analysisDao.getGamesForRun(runId)
            AppResult.Success(gameEntities.map { it.toDomain() })
        } catch (e: Exception) {
            AppResult.Error(AppError.DatabaseError("Failed to fetch games for run $runId: ${e.localizedMessage}"))
        }
    }

    override fun observeAnalysisRuns(): Flow<List<AnalysisResult>> {
        return analysisDao.observeAnalysisRuns().map { runs ->
            runs.map { run -> mapRunEntityToAnalysisResult(run, recommendedGames = emptyList()) }
        }
    }

    private fun mapRunEntityToAnalysisResult(
        run: AnalysisRunEntity,
        recommendedGames: List<LottoGame>
    ): AnalysisResult {
        val config = WeightConfig(
            frequencyWeight = run.weightFrequency,
            consecutiveWeight = run.weightConsecutive,
            parityWeight = run.weightParity,
            usePrizeIndex = run.usePrizeIndex,
            recentN = run.recentN,
            gameCount = run.gameCount
        )
        return AnalysisResult(
            runId = run.id,
            createdAt = run.createdAt,
            latestDrawNo = run.latestDrawNo,
            config = config,
            recommendedGames = recommendedGames,
            algorithmVersion = run.algorithmVersion
        )
    }
}
