package com.example.lottoinsight.core.engine.calculator

import com.example.lottoinsight.core.common.Constants
import com.example.lottoinsight.core.model.Draw
import com.example.lottoinsight.core.model.HistoricalPrizeIndex

object PrizeIndexCalculator {

    fun calculateHistoricalPrizeIndexes(draws: List<Draw>): List<HistoricalPrizeIndex> {
        val totalFirstPrize = (Constants.LOTTO_MIN_NUMBER..Constants.LOTTO_MAX_NUMBER).associateWith { 0.0 }.toMutableMap()
        val appearanceCount = (Constants.LOTTO_MIN_NUMBER..Constants.LOTTO_MAX_NUMBER).associateWith { 0 }.toMutableMap()

        draws.forEach { draw ->
            val prize = draw.firstPrize?.toDouble() ?: 0.0
            draw.numbers.forEach { number ->
                if (number in Constants.LOTTO_MIN_NUMBER..Constants.LOTTO_MAX_NUMBER) {
                    totalFirstPrize[number] = (totalFirstPrize[number] ?: 0.0) + prize
                    appearanceCount[number] = (appearanceCount[number] ?: 0) + 1
                }
            }
        }

        val totalDrawsCount = draws.size.toDouble().coerceAtLeast(1.0)
        val rawIndexes = (Constants.LOTTO_MIN_NUMBER..Constants.LOTTO_MAX_NUMBER).map { num ->
            val totalPrize = totalFirstPrize[num] ?: 0.0
            val count = appearanceCount[num] ?: 0
            val expectedValue = if (totalDrawsCount > 0) totalPrize / totalDrawsCount else 0.0
            num to expectedValue
        }.toMap()

        val maxEV = rawIndexes.values.maxOrNull() ?: 1.0
        val minEV = rawIndexes.values.minOrNull() ?: 0.0
        val range = (maxEV - minEV).coerceAtLeast(1.0)

        return (Constants.LOTTO_MIN_NUMBER..Constants.LOTTO_MAX_NUMBER).map { num ->
            val ev = rawIndexes[num] ?: 0.0
            val normalized = ((ev - minEV) / range).coerceIn(0.0, 1.0)
            HistoricalPrizeIndex(
                number = num,
                prizeIndex = ev,
                appearanceCount = appearanceCount[num] ?: 0,
                normalizedWeight = normalized
            )
        }
    }
}
