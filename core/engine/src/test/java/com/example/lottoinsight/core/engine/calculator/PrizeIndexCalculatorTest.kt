package com.example.lottoinsight.core.engine.calculator

import com.example.lottoinsight.core.model.Draw
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrizeIndexCalculatorTest {

    @Test
    fun computesEvFromPrizeEligibleDrawsAndIgnoresMissingPrizeMetadata() {
        val draws = listOf(
            draw(1, listOf(1, 2, 3, 4, 5, 6), bonus = 7, firstPrize = 1_000L),
            draw(2, listOf(1, 7, 8, 9, 10, 11), bonus = 2, firstPrize = null),
            draw(3, listOf(7, 8, 9, 10, 11, 12), bonus = 1, firstPrize = 2_000L)
        )

        val result = PrizeIndexCalculator.calculateHistoricalPrizeIndexes(draws)
        val one = result.single { it.number == 1 }
        val seven = result.single { it.number == 7 }

        assertEquals(2, one.appearanceCount)
        assertEquals(2.0 / 3.0, one.appearanceRate, 1e-9)
        assertEquals(1, one.prizeSampleCount)
        assertEquals(1_000.0, one.averageFirstPrize, 1e-9)
        assertEquals(500.0, one.rawIndex, 1e-9)

        assertEquals(1, seven.prizeSampleCount)
        assertEquals(2_000.0, seven.averageFirstPrize, 1e-9)
        assertEquals(1_000.0, seven.rawIndex, 1e-9)
        assertTrue(seven.rank < one.rank)
    }

    @Test
    fun normalizedScoresStayWithinUnitRangeAndRanksCoverAllNumbers() {
        val draws = (1..12).map { drawNo ->
            val numbers = (0 until 6)
                .map { offset -> ((drawNo * 5 + offset * 7) % 45) + 1 }
                .distinct()
                .sorted()
            val bonus = (1..45).first { it !in numbers }
            draw(
                drawNo = drawNo,
                numbers = numbers,
                bonus = bonus,
                firstPrize = 1_000L + drawNo * 100L
            )
        }

        val result = PrizeIndexCalculator.calculateHistoricalPrizeIndexes(draws)

        assertEquals(45, result.size)
        assertTrue(result.all { it.normalizedScore in 0.0..1.0 })
        assertEquals((1..45).toSet(), result.map { it.rank }.toSet())
    }

    private fun draw(
        drawNo: Int,
        numbers: List<Int>,
        bonus: Int,
        firstPrize: Long?
    ) = Draw(
        drawNo = drawNo,
        drawDate = "2026-01-01",
        numbers = numbers,
        bonus = bonus,
        firstPrize = firstPrize,
        source = "test",
        fetchedAt = 0L
    )
}
