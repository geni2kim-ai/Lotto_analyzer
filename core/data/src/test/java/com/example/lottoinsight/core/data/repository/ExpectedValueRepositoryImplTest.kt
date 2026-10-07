package com.example.lottoinsight.core.data.repository

import com.example.lottoinsight.core.common.AppResult
import com.example.lottoinsight.core.common.Constants
import com.example.lottoinsight.core.database.dao.ExpectedValueDao
import com.example.lottoinsight.core.database.entity.ExpectedValueNumberEntity
import com.example.lottoinsight.core.database.entity.ExpectedValueRunEntity
import com.example.lottoinsight.core.model.HistoricalPrizeIndex
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpectedValueRepositoryImplTest {

    @Test
    fun saveExpectedValueRunUsesCurrentAlgorithmVersion() = runBlocking {
        val dao = FakeExpectedValueDao()
        val repository = ExpectedValueRepositoryImpl(dao)

        val result = repository.saveExpectedValueRun(
            recentN = 100,
            latestDrawNo = 1200,
            numbers = listOf(
                HistoricalPrizeIndex(
                    number = 1,
                    appearanceCount = 10,
                    appearanceRate = 0.1,
                    prizeSampleCount = 5,
                    averageFirstPrize = 1_000_000.0,
                    rawIndex = 100_000.0,
                    normalizedScore = 0.7,
                    rank = 1
                )
            )
        )

        assertTrue(result is AppResult.Success)
        assertEquals(Constants.ALGORITHM_VERSION, dao.lastRun?.algorithmVersion)
        assertEquals(42L, (result as AppResult.Success).data)
    }

    private class FakeExpectedValueDao : ExpectedValueDao {
        var lastRun: ExpectedValueRunEntity? = null
            private set

        override suspend fun insertRun(run: ExpectedValueRunEntity): Long {
            lastRun = run
            return 42L
        }

        override suspend fun insertNumbers(numbers: List<ExpectedValueNumberEntity>) = Unit

        override suspend fun getLatestRun(): ExpectedValueRunEntity? = lastRun

        override suspend fun getLatestRunForRecentN(recentN: Int): ExpectedValueRunEntity? =
            lastRun?.takeIf { it.recentN == recentN }

        override suspend fun getNumbersForRun(runId: Long): List<ExpectedValueNumberEntity> =
            emptyList()
    }
}
