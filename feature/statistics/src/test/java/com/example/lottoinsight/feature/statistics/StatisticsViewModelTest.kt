package com.example.lottoinsight.feature.statistics

import com.example.lottoinsight.core.common.AppError
import com.example.lottoinsight.core.common.AppResult
import com.example.lottoinsight.core.data.repository.DrawSyncReport
import com.example.lottoinsight.core.data.repository.ExpectedValueRepository
import com.example.lottoinsight.core.data.repository.LottoRepository
import com.example.lottoinsight.core.model.Draw
import com.example.lottoinsight.core.model.HistoricalPrizeIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModelTest {
    private lateinit var dispatcher: TestDispatcher

    @Before
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun savedExpectedValueBasisIsClearedWhenDrawDatasetChanges() = runTest(dispatcher) {
        val drawFlow = MutableStateFlow(draws(10))
        val expectedValueRepository = FakeExpectedValueRepository()
        val viewModel = StatisticsViewModel(
            lottoRepository = FakeLottoRepository(drawFlow),
            expectedValueRepository = expectedValueRepository
        )
        testScheduler.advanceUntilIdle()

        viewModel.calculateAndSaveExpectedValues(recentN = 5)
        viewModel.calculateAndSaveExpectedValues(recentN = 5)
        testScheduler.advanceUntilIdle()

        assertEquals(1, expectedValueRepository.saveCalls)
        assertEquals(77L, viewModel.uiState.value.expectedValueRunId)
        assertEquals(5, viewModel.uiState.value.expectedValueRecentN)
        assertEquals(
            "기대값 계산 완료: 최근 5회 기준",
            viewModel.uiState.value.syncMessage
        )

        drawFlow.value = draws(11)
        testScheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.expectedValueRunId)
        assertEquals(100, viewModel.uiState.value.expectedValueRecentN)
        assertNull(viewModel.uiState.value.syncMessage)
        assertEquals(11, viewModel.uiState.value.totalDrawsCount)
    }

    private class FakeExpectedValueRepository : ExpectedValueRepository {
        var saveCalls: Int = 0
            private set

        override suspend fun saveExpectedValueRun(
            recentN: Int,
            latestDrawNo: Int,
            numbers: List<HistoricalPrizeIndex>
        ): AppResult<Long> {
            saveCalls++
            return AppResult.Success(77L)
        }

        override suspend fun getLatestExpectedValueNumbers(
            recentN: Int
        ): AppResult<List<HistoricalPrizeIndex>> = AppResult.Success(emptyList())
    }

    private class FakeLottoRepository(
        private val drawFlow: MutableStateFlow<List<Draw>>
    ) : LottoRepository {
        override fun observeAllDraws(): Flow<List<Draw>> = drawFlow
        override fun observeLatestDraw(): Flow<Draw?> =
            flowOf(drawFlow.value.maxByOrNull { it.drawNo })

        override suspend fun getDrawByNo(drawNo: Int): AppResult<Draw> =
            drawFlow.value.firstOrNull { it.drawNo == drawNo }?.let(AppResult::Success)
                ?: AppResult.Error(AppError.InvalidDraw(drawNo, "missing"))

        override suspend fun fetchAndSaveLatestDraws(fetchCount: Int): AppResult<Int> =
            AppResult.Success(0)

        override suspend fun syncDraws(
            onProgress: ((completed: Int, total: Int) -> Unit)?
        ): AppResult<DrawSyncReport> = AppResult.Success(DrawSyncReport(0, emptyList()))

        override suspend fun retryDraws(
            drawNos: List<Int>,
            onProgress: ((completed: Int, total: Int) -> Unit)?
        ): AppResult<DrawSyncReport> = AppResult.Success(DrawSyncReport(0, emptyList()))
    }

    private fun draws(count: Int): List<Draw> =
        (1..count).map { drawNo ->
            Draw(
                drawNo = drawNo,
                drawDate = "2026-01-01",
                numbers = listOf(1, 2, 3, 4, 5, 6),
                bonus = 7,
                firstPrize = 1_000_000L + drawNo,
                source = "test",
                fetchedAt = 0L
            )
        }
}
