package com.example.lottoinsight.core.engine

import com.example.lottoinsight.core.common.AppResult
import com.example.lottoinsight.core.engine.generator.NumberGenerator
import com.example.lottoinsight.core.model.Draw
import com.example.lottoinsight.core.model.WeightConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisEngineTest {

    @Test
    fun usesMostRecentDrawsRegardlessOfInputOrder() {
        val draws = diverseDraws(12).shuffled(java.util.Random(7L))

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
}
