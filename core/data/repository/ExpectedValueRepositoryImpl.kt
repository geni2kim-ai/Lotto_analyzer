package com.example.lottoinsight.core.data.repository

import com.example.lottoinsight.core.common.AppError
import com.example.lottoinsight.core.common.AppResult
import com.example.lottoinsight.core.database.dao.ExpectedValueDao
import com.example.lottoinsight.core.database.entity.ExpectedValueNumberEntity
import com.example.lottoinsight.core.database.entity.ExpectedValueRunEntity
import com.example.lottoinsight.core.model.HistoricalPrizeIndex

class ExpectedValueRepositoryImpl(
    private val expectedValueDao: ExpectedValueDao
) : ExpectedValueRepository {

    override suspend fun saveExpectedValueRun(
        recentN: Int,
        latestDrawNo: Int,
        numbers: List<HistoricalPrizeIndex>
    ): AppResult<Long> {
        if (recentN <= 0) {
            return AppResult.Error(AppError.InsufficientData("recentN must be positive integer, got $recentN"))
        }
        if (latestDrawNo <= 0) {
            return AppResult.Error(AppError.InvalidDraw(latestDrawNo, "latestDrawNo must be positive integer, got $latestDrawNo"))
        }
        if (numbers.isEmpty()) {
            return AppResult.Error(AppError.InsufficientData("HistoricalPrizeIndex numbers list cannot be empty"))
        }

        return try {
            val runEntity = ExpectedValueRunEntity(
                createdAt = System.currentTimeMillis(),
                latestDrawNo = latestDrawNo,
                recentN = recentN,
                algorithmVersion = "1.0.0"
            )

            val numberEntities = numbers.map { ExpectedValueNumberEntity.fromDomain(0, it) }
            val runId = expectedValueDao.saveExpectedValueRun(runEntity, numberEntities)
            AppResult.Success(runId)
        } catch (e: Exception) {
            AppResult.Error(AppError.DatabaseError("Failed to save expected value run: ${e.localizedMessage}"))
        }
    }

    override suspend fun getLatestExpectedValueNumbers(recentN: Int): AppResult<List<HistoricalPrizeIndex>> {
        if (recentN <= 0) {
            return AppResult.Error(AppError.InsufficientData("recentN must be positive integer, got $recentN"))
        }

        return try {
            val run = expectedValueDao.getLatestRunForRecentN(recentN)
                ?: return AppResult.Error(AppError.InsufficientData("No expected value run found for recentN=$recentN"))

            val numberEntities = expectedValueDao.getNumbersForRun(run.id)
            AppResult.Success(numberEntities.map { it.toDomain() })
        } catch (e: Exception) {
            AppResult.Error(AppError.DatabaseError("Failed to fetch expected value numbers for recentN=$recentN: ${e.localizedMessage}"))
        }
    }
}
