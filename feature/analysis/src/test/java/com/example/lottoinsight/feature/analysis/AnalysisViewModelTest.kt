package com.example.lottoinsight.feature.analysis

import com.example.lottoinsight.core.common.AppError
import com.example.lottoinsight.core.common.AppResult
import com.example.lottoinsight.core.data.repository.AnalysisRepository
import com.example.lottoinsight.core.data.repository.DrawSyncReport
import com.example.lottoinsight.core.data.repository.LottoRepository
import com.example.lottoinsight.core.engine.AnalysisEngine
import com.example.lottoinsight.core.model.AnalysisResult
import com.example.lottoinsight.core.model.Draw
import com.example.lottoinsight.core.model.HistoricalPrizeIndex
import com.example.lottoinsight.core.model.LottoGame
import com.example.lottoinsight.core.model.WeightConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisViewModelTest {
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
    fun saveFailureIsSurfacedWithoutDiscardingGeneratedResult() = runTest(dispatcher) {
        val expected = analysisResult()
        val repository = FakeAnalysisRepository(
            saveResult = AppResult.Error(AppError.DatabaseError("simulated"))
        )
        val engine = FakeAnalysisEngine(expected)
        val viewModel = AnalysisViewModel(
            lottoRepository = FakeLottoRepository(draws(10)),
            analysisRepository = repository,
            analysisEngine = engine
        )

        viewModel.runAnalysisAndGenerate()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(expected, state.latestAnalysisResult)
        assertFalse(state.isSavedSuccess)
        assertTrue(state.userMessage?.contains("failed to save analysis history") == true)
        assertEquals(1, repository.saveCalls)
    }

    @Test
    fun duplicateRequestBeforeWorkerRunsIsIgnored() = runTest(dispatcher) {
        val repository = FakeAnalysisRepository(saveResult = AppResult.Success(1L))
        val engine = FakeAnalysisEngine(analysisResult())
        val viewModel = AnalysisViewModel(
            lottoRepository = FakeLottoRepository(draws(10)),
            analysisRepository = repository,
            analysisEngine = engine
        )

        viewModel.runAnalysisAndGenerate()
        viewModel.runAnalysisAndGenerate()
        testScheduler.advanceUntilIdle()

        assertEquals(1, engine.calls)
        assertEquals(1, repository.saveCalls)
        assertTrue(viewModel.uiState.value.isSavedSuccess)
    }

    private class FakeAnalysisEngine(
        private val result: AnalysisResult
    ) : AnalysisEngine {
        var calls: Int = 0
            private set

        override fun analyzeAndGenerate(
            draws: List<Draw>,
            config: WeightConfig
        ): AppResult<AnalysisResult> {
            calls++
            return AppResult.Success(result)
        }

        override fun calculatePrizeIndexes(
            draws: List<Draw>
        ): AppResult<List<HistoricalPrizeIndex>> = AppResult.Success(emptyList())
    }

    private class FakeAnalysisRepository(
        private val saveResult: AppResult<Long>
    ) : AnalysisRepository {
        var saveCalls: Int = 0
            private set

        override suspend fun saveAnalysisRun(
            config: WeightConfig,
            latestDrawNo: Int,
            startDrawNo: Int,
            endDrawNo: Int,
            randomSeed: Long,
            algorithmVersion: String,
            games: List<LottoGame>
        ): AppResult<Long> {
            saveCalls++
            return saveResult
        }

        override suspend fun getRecentAnalysisRuns(limit: Int): AppResult<List<AnalysisResult>> =
            AppResult.Success(emptyList())

        override suspend fun getGamesForRun(runId: Long): AppResult<List<LottoGame>> =
            AppResult.Success(emptyList())

        override fun observeAnalysisRuns(): Flow<List<AnalysisResult>> = flowOf(emptyList())
    }

    private class FakeLottoRepository(
        private val values: List<Draw>
    ) : LottoRepository {
        override fun observeAllDraws(): Flow<List<Draw>> = flowOf(values)
        override fun observeLatestDraw(): Flow<Draw?> = flowOf(values.maxByOrNull { it.drawNo })
        override suspend fun getDrawByNo(drawNo: Int): AppResult<Draw> =
            values.firstOrNull { it.drawNo == drawNo }?.let(AppResult::Success)
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

    private fun analysisResult(): AnalysisResult {
        val game = LottoGame(
            gameIndex = 1,
            numbers = listOf(1, 2, 3, 4, 5, 6),
            totalScore = 0.5,
            frequencyScore = 0.5,
            consecutiveScore = 0.5,
            parityScore = 0.5,
            oddCount = 3,
            pairCount = 5
        )
        return AnalysisResult(
            latestDrawNo = 10,
            recentN = 10,
            gameCount = 1,
            weightConfig = WeightConfig(gameCount = 1),
            games = listOf(game),
            randomSeed = 1234L,
            dataStartDrawNo = 1,
            dataEndDrawNo = 10
        )
    }

    private fun draws(count: Int): List<Draw> =
        (1..count).map { drawNo ->
            Draw(
                drawNo = drawNo,
                drawDate = "2026-01-01",
                numbers = listOf(1, 2, 3, 4, 5, 6),
                bonus = 7,
                firstPrize = 1_000_000L,
                source = "test",
                fetchedAt = 0L
            )
        }
}
