package com.example.lottoinsight.core.engine.calculator

import com.example.lottoinsight.core.common.Constants
import com.example.lottoinsight.core.model.Draw
import com.example.lottoinsight.core.model.HistoricalPrizeIndex

object PrizeIndexCalculator {

    /**
     * Prize-index policy:
     * - appearanceCount/appearanceRate remain descriptive over every stored draw.
     * - EV excludes draws whose firstPrize is null or non-positive from both the
     *   prize-eligible denominator and the prize sample for a number.
     * This prevents missing prize metadata from depressing a number's EV.
     */
    fun calculateHistoricalPrizeIndexes(draws: List<Draw>): List<HistoricalPrizeIndex> {
        if (draws.isEmpty()) return emptyList()

        val allAppearanceCount = (Constants.LOTTO_MIN_NUMBER..Constants.LOTTO_MAX_NUMBER)
            .associateWith { 0 }
            .toMutableMap()
        val prizeValues = (Constants.LOTTO_MIN_NUMBER..Constants.LOTTO_MAX_NUMBER)
            .associateWith { mutableListOf<Double>() }

        draws.forEach { draw ->
            draw.numbers.forEach { number ->
                if (number in Constants.LOTTO_MIN_NUMBER..Constants.LOTTO_MAX_NUMBER) {
                    allAppearanceCount[number] = (allAppearanceCount[number] ?: 0) + 1
                }
            }
        }

        val prizeEligibleDraws = draws.filter { (it.firstPrize ?: 0L) > 0L }
        prizeEligibleDraws.forEach { draw ->
            val prize = requireNotNull(draw.firstPrize).toDouble()
            draw.numbers.forEach { number ->
                if (number in Constants.LOTTO_MIN_NUMBER..Constants.LOTTO_MAX_NUMBER) {
                    prizeValues[number]?.add(prize)
                }
            }
        }

        val totalDrawsCount = draws.size.toDouble().coerceAtLeast(1.0)
        val prizeEligibleCount = prizeEligibleDraws.size.toDouble()
        val rawIndexes = (Constants.LOTTO_MIN_NUMBER..Constants.LOTTO_MAX_NUMBER).associateWith { number ->
            val samples = prizeValues[number].orEmpty()
            val averagePrize = if (samples.isEmpty()) 0.0 else samples.average()
            val eligibleAppearanceRate = if (prizeEligibleCount <= 0.0) {
                0.0
            } else {
                samples.size / prizeEligibleCount
            }
            eligibleAppearanceRate * averagePrize
        }

        val maxEV = rawIndexes.values.maxOrNull() ?: 0.0
        val minEV = rawIndexes.values.minOrNull() ?: 0.0
        val range = maxEV - minEV
        val ranking = rawIndexes.entries
            .sortedWith(compareByDescending<Map.Entry<Int, Double>> { it.value }.thenBy { it.key })
            .mapIndexed { index, entry -> entry.key to index + 1 }
            .toMap()

        return (Constants.LOTTO_MIN_NUMBER..Constants.LOTTO_MAX_NUMBER).map { num ->
            val ev = rawIndexes[num] ?: 0.0
            val normalized = if (range <= 0.0) 0.0 else ((ev - minEV) / range).coerceIn(0.0, 1.0)
            val appearanceCount = allAppearanceCount[num] ?: 0
            val samples = prizeValues[num].orEmpty()
            val averagePrize = if (samples.isEmpty()) 0.0 else samples.average()

            HistoricalPrizeIndex(
                number = num,
                appearanceCount = appearanceCount,
                appearanceRate = appearanceCount / totalDrawsCount,
                prizeSampleCount = samples.size,
                averageFirstPrize = averagePrize,
                rawIndex = ev,
                normalizedScore = normalized,
                rank = ranking[num] ?: Constants.LOTTO_MAX_NUMBER
            )
        }
    }
}
