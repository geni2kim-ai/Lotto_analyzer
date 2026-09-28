package com.example.lottoinsight.core.engine

import com.example.lottoinsight.core.common.AppResult
import com.example.lottoinsight.core.engine.generator.NumberGenerator
import com.example.lottoinsight.core.model.Draw
import com.example.lottoinsight.core.model.LottoGame
import com.example.lottoinsight.core.model.WeightConfig
import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisEngineTest {

    @Test
    fun usesMostRecentDrawsRegardlessOfInputOrder() {
        val draws = diverseDraws(12).shuffled(Random(7L))

        val result = AnalysisEngineImpl().analyzeAndGenerate(
            draws = draws,
            config = WeightConfig(recentN = 3, gameCount = 1)
        )

        val analysis = (result as AppResult.Success).data
        assertEquals(12, analysis.latestDrawNo)
        assertEquals(10, analysis.dataStartDrawNo)
        assertEquals(12, analysis.dataEndDrawNo)
    }

    @Test
    fun generatedGamesAreUniqueAndReportActualPriorOverlap() {
        val result = AnalysisEngineImpl().analyzeAndGenerate(
            draws = diverseDraws(24),
            config = WeightConfig(
                recentN = 20,
                gameCount = 8,
                candidateCount = 12,
                usePrizeIndex = true
            )
        )

        val games = (result as AppResult.Success).data.games
        assertEquals(8, games.size)
        assertEquals(games.size, games.map { it.numbers }.distinct().size)
        assertTrue(games.all { it.totalScore.isFinite() })

        games.forEachIndexed { index, game ->
            val expectedOverlap = games.take(index)
                .maxOfOrNull { prior ->
                    game.numbers.toSet().intersect(prior.numbers.toSet()).size
                } ?: 0
            assertEquals(expectedOverlap, game.maxOverlap)
        }
    }

    @Test
    fun analysisUsesDeterministicSeedForSameInputsAndConfig() {
        val draws = diverseDraws(24)
        val config = WeightConfig(
            recentN = 20,
            gameCount = 5,
            candidateCount = 250,
            usePrizeIndex = true
        )
        val engine = AnalysisEngineImpl()

        val first = (engine.analyzeAndGenerate(draws, config) as AppResult.Success).data
        val second = (engine.analyzeAndGenerate(draws.shuffled(Random(99L)), config) as AppResult.Success).data
        val changed = (
            engine.analyzeAndGenerate(
                draws,
                config.copy(frequencyWeight = config.frequencyWeight + 1)
            ) as AppResult.Success
        ).data

        assertEquals(first.randomSeed, second.randomSeed)
        assertEquals(first.games, second.games)
        assertTrue(first.randomSeed != changed.randomSeed)
    }

    @Test
    fun oversizedRequestedRecentNMatchesSameAppliedRecentNSeed() {
        val draws = diverseDraws(12)
        val engine = AnalysisEngineImpl()
        val oversizedRequest = WeightConfig(
            recentN = 100,
            gameCount = 3,
            candidateCount = 120,
            usePrizeIndex = true
        )
        val exactAppliedRequest = oversizedRequest.copy(recentN = draws.size)

        val oversized = (
            engine.analyzeAndGenerate(draws, oversizedRequest) as AppResult.Success
        ).data
        val exactApplied = (
            engine.analyzeAndGenerate(draws, exactAppliedRequest) as AppResult.Success
        ).data

        assertEquals(draws.size, oversized.recentN)
        assertEquals(oversized.randomSeed, exactApplied.randomSeed)
        assertEquals(oversized.games, exactApplied.games)
    }

    @Test
    fun fallbackGamesRejectsDuplicatesAndTracksOverlapAgainstEarlierFallbacks() {
        val existingTicket = listOf(1, 2, 3, 4, 5, 6)
        val firstFallback = listOf(1, 7, 13, 19, 25, 31)
        val secondFallback = listOf(1, 7, 14, 20, 26, 32)
        val generator = ScriptedNumberGenerator(
            listOf(
                existingTicket,
                firstFallback,
                firstFallback,
                secondFallback
            )
        )
        val engine = AnalysisEngineImpl(generator)
        val stats = engine.buildStats(diverseDraws(12))
        val config = WeightConfig(
            recentN = 12,
            gameCount = 3,
            candidateCount = 1,
            usePrizeIndex = false
        )
        val alreadySelected = listOf(game(existingTicket, gameIndex = 1))

        val fallback = engine.fallbackGames(
            baseWeights = (1..45).associateWith { 1.0 },
            stats = stats,
            weights = mapOf(
                "frequency" to config.normalizedFrequency,
                "consecutive" to config.normalizedConsecutive,
                "parity" to config.normalizedParity
            ),
            prizeScores = (1..45).associateWith { 0.0 },
            config = config,
            random = Random(1L),
            alreadySelected = alreadySelected
        )

        assertEquals(2, fallback.size)
        assertEquals(listOf(firstFallback, secondFallback), fallback.map { it.numbers })
        assertEquals(1, fallback[0].maxOverlap)
        assertEquals(2, fallback[1].maxOverlap)
    }

    @Test
    fun seededGenerationIsRepeatable() {
        val scores = (1..45).associateWith { it.toDouble() }
        val first = NumberGenerator().generateGames(scores, gameCount = 2, seed = 1234L)
        val second = NumberGenerator().generateGames(scores, gameCount = 2, seed = 1234L)

        assertEquals(first, second)
    }

    private fun diverseDraws(count: Int): List<Draw> =
        (1..count).map { drawNo ->
            val numbers = (0 until 6)
                .map { offset -> ((drawNo * 7 + offset * 5) % 45) + 1 }
                .sorted()
            val bonus = (1..45).first { it !in numbers }
            Draw(
                drawNo = drawNo,
                drawDate = "2026-01-01",
                numbers = numbers,
                bonus = bonus,
                firstPrize = 1_000_000L + drawNo * 10_000L,
                source = "test",
                fetchedAt = 0L
            )
        }

    private fun game(numbers: List<Int>, gameIndex: Int): LottoGame =
        LottoGame(
            gameIndex = gameIndex,
            numbers = numbers,
            totalScore = 0.0,
            frequencyScore = 0.0,
            consecutiveScore = 0.0,
            parityScore = 0.0,
            prizeScore = 0.0,
            oddCount = numbers.count { it % 2 != 0 },
            pairCount = numbers.zipWithNext().count { (left, right) -> right - left == 1 }
        )

    private class ScriptedNumberGenerator(
        tickets: List<List<Int>>
    ) : NumberGenerator() {
        private val remaining = tickets.toMutableList()

        override fun weightedSample(
            scores: Map<Int, Double>,
            count: Int,
            activeRandom: Random
        ): List<Int> = remaining.removeAt(0)
    }
}
