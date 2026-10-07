package com.example.lottoinsight.feature.statistics

import com.example.lottoinsight.core.common.AppError
import com.example.lottoinsight.core.common.AppResult
import com.example.lottoinsight.core.data.repository.DrawSyncReport
import com.example.lottoinsight.core.data.repository.ExpectedValueRepository
import com.example.lottoinsight.core.data.repository.LottoRepository
import com.example.lottoinsight.core.model.Draw
import com.example.lottoinsight.core.model.HistoricalPrizeIndex
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.Assert.assertTrue
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

    @Test
    fun staleSaveCompletionDoesNotRestoreExpectedValueProvenanceAfterDrawRefresh() = runTest(dispatcher) {
        val drawFlow = MutableStateFlow(draws(10))
        val saveGate = CompletableDeferred<Unit>()
        val expectedValueRepository = FakeExpectedValueRepository(saveGate)
        val viewModel = StatisticsViewModel(
            lottoRepository = FakeLottoRepository(drawFlow),
            expectedValueRepository = expectedValueRepository
        )
        testScheduler.advanceUntilIdle()

        viewModel.calculateAndSaveExpectedValues(recentN = 5)
        testScheduler.runCurrent()

        assertEquals(1, expectedValueRepository.saveCalls)
        assertTrue(viewModel.uiState.value.isExpectedValueSaving)

        drawFlow.value = draws(11)
        testScheduler.runCurrent()
        assertEquals(11, viewModel.uiState.value.totalDrawsCount)
        assertNull(viewModel.uiState.value.expectedValueRunId)

        saveGate.complete(Unit)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.expectedValueRunId)
        assertEquals(100, state.expectedValueRecentN)
        assertEquals(11, state.totalDrawsCount)
        assertEquals(listOf(77L), expectedValueRepository.deletedRunIds)
        assertTrue(state.userMessage?.contains("이전 기준의 기대값 결과를 적용하지 않았습니다") == true)
    }

    private class FakeExpectedValueRepository(
        private val saveGate: CompletableDeferred<Unit>? = null
    ) : ExpectedValueRepository {
        var saveCalls: Int = 0
            private set
        val deletedRunIds = mutableListOf<Long>()

        override suspend fun saveExpectedValueRun(
            recentN: Int,
            latestDrawNo: Int,
            numbers: List<HistoricalPrizeIndex>
        ): AppResult<Long> {
            saveCalls++
            saveGate?.await()
            return AppResult.Success(77L)
        }

        override suspend fun getLatestExpectedValueNumbers(
            recentN: Int
        ): AppResult<List<HistoricalPrizeIndex>> = AppResult.Success(emptyList())

        override suspend fun deleteExpectedValueRun(runId: Long): AppResult<Unit> {
            deletedRunIds += runId
            return AppResult.Success(Unit)
        }
    }

    private class FakeLottoRepository(
        private val drawFlow: MutableStateFlow<List<Draw>>
    ) : LottoRepository {
        override fun observeAllDraws(): Flow<List<Draw>> = drawFlow
        override fun observeLatestDraw(): Flow<Draw?> =
            flowOf(drawFlow.value.maxByOrNull { it.drawNo })

        override suspend fun getDrawByNo(drawNo: Int): AppResult<Draw> =
            drawFlow.value.firstOrNull { it.drawNo == drawNo }?.let { AppResult.Success(it) }
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
