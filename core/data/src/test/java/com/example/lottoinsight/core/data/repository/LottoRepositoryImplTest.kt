package com.example.lottoinsight.core.data.repository

import com.example.lottoinsight.core.common.AppError
import com.example.lottoinsight.core.common.AppResult
import com.example.lottoinsight.core.database.dao.DrawDao
import com.example.lottoinsight.core.database.entity.DrawEntity
import com.example.lottoinsight.core.model.Draw
import com.example.lottoinsight.core.network.datasource.DrawFetchReport
import com.example.lottoinsight.core.network.datasource.LottoRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LottoRepositoryImplTest {

    @Test
    fun fetchAndSaveLatestDrawsReusesRangeFetchAndPersistsPartialSuccess() = runBlocking {
        val dao = FakeDrawDao(
            mutableListOf(DrawEntity.fromDomain(draw(100)))
        )
        val remote = FakeRemoteDataSource(
            rangeReport = DrawFetchReport(
                successful = listOf(draw(101), draw(103)),
                failedDrawNos = listOf(102)
            )
        )
        val repository = LottoRepositoryImpl(dao, remote)

        val result = repository.fetchAndSaveLatestDraws(fetchCount = 3)

        assertTrue(result is AppResult.Success)
        assertEquals(2, (result as AppResult.Success).data)
        assertEquals(101 to 103, remote.requestedRange)
        assertEquals(0, remote.singleDrawCalls)
        assertEquals(listOf(100, 101, 103), dao.entities.map { it.drawNo }.sorted())
    }

    @Test
    fun retryDrawsUsesAggregateReportWithoutUnreachableErrorBranch() = runBlocking {
        val dao = FakeDrawDao(mutableListOf())
        val remote = FakeRemoteDataSource(
            rangeReport = DrawFetchReport(
                successful = listOf(draw(11)),
                failedDrawNos = listOf(12)
            )
        )
        val repository = LottoRepositoryImpl(dao, remote)

        val result = repository.retryDraws(listOf(12, 11, 12, -1))

        assertTrue(result is AppResult.Success)
        val report = (result as AppResult.Success).data
        assertEquals(1, report.successfulCount)
        assertEquals(listOf(12), report.failedDrawNos)
        assertEquals(listOf(11, 12), remote.requestedDrawNos)
        assertEquals(listOf(11), dao.entities.map { it.drawNo })
    }

    private class FakeDrawDao(
        val entities: MutableList<DrawEntity>
    ) : DrawDao {
        override suspend fun upsertDraws(draws: List<DrawEntity>) {
            draws.forEach { incoming ->
                entities.removeAll { it.drawNo == incoming.drawNo }
                entities += incoming
            }
        }

        override suspend fun getDrawCount(): Int = entities.size
        override suspend fun getLatestDrawNo(): Int? = entities.maxOfOrNull { it.drawNo }
        override fun observeLatestDraw(): Flow<DrawEntity?> = flowOf(entities.maxByOrNull { it.drawNo })
        override fun observeAllDraws(): Flow<List<DrawEntity>> = flowOf(entities.sortedBy { it.drawNo })
        override suspend fun getRecentDraws(recentN: Int): List<DrawEntity> =
            entities.sortedByDescending { it.drawNo }.take(recentN)
        override suspend fun getDrawByNo(drawNo: Int): DrawEntity? =
            entities.firstOrNull { it.drawNo == drawNo }
    }

    private class FakeRemoteDataSource(
        private val rangeReport: DrawFetchReport
    ) : LottoRemoteDataSource {
        var requestedRange: Pair<Int, Int>? = null
            private set
        var requestedDrawNos: List<Int> = emptyList()
            private set
        var singleDrawCalls: Int = 0
            private set

        override suspend fun fetchDraw(drawNo: Int): AppResult<Draw> {
            singleDrawCalls++
            return AppResult.Error(AppError.NetworkUnavailable)
        }

        override suspend fun fetchAllDraws(
            existingMaxDrawNo: Int?,
            onProgress: ((completed: Int, total: Int) -> Unit)?
        ): AppResult<DrawFetchReport> =
            AppResult.Error(AppError.NetworkUnavailable)

        override suspend fun fetchDrawRange(
            startDrawNo: Int,
            endDrawNo: Int,
            onProgress: ((completed: Int, total: Int) -> Unit)?
        ): DrawFetchReport {
            requestedRange = startDrawNo to endDrawNo
            return rangeReport
        }

        override suspend fun fetchDraws(
            drawNos: List<Int>,
            onProgress: ((completed: Int, total: Int) -> Unit)?
        ): DrawFetchReport {
            requestedDrawNos = drawNos
            return rangeReport
        }

        override suspend fun fetchLatestDrawNo(existingMaxDrawNo: Int?): AppResult<Int> =
            AppResult.Error(AppError.NetworkUnavailable)
    }

    companion object {
        private fun draw(drawNo: Int): Draw {
            val start = ((drawNo - 1) % 38) + 1
            val numbers = (start until start + 6).toList()
            val bonus = (1..45).first { it !in numbers }
            return Draw(
                drawNo = drawNo,
                drawDate = "2026-09-28",
                numbers = numbers,
                bonus = bonus,
                firstPrize = 1_000_000L,
                source = "test",
                fetchedAt = 0L
            )
        }
    }
}
