package com.example.lottoinsight.core.engine.calculator

import com.example.lottoinsight.core.common.Constants
import com.example.lottoinsight.core.model.Draw
import com.example.lottoinsight.core.model.HistoricalPrizeIndex

object PrizeIndexCalculator {

    fun calculateHistoricalPrizeIndexes(draws: List<Draw>): List<HistoricalPrizeIndex> {
        if (draws.isEmpty()) return emptyList()

        val prizeValues = (Constants.LOTTO_MIN_NUMBER..Constants.LOTTO_MAX_NUMBER)
            .associateWith { mutableListOf<Double>() }
        val appearanceCount = (Constants.LOTTO_MIN_NUMBER..Constants.LOTTO_MAX_NUMBER).associateWith { 0 }.toMutableMap()

        draws.forEach { draw ->
            val prize = draw.firstPrize?.toDouble() ?: 0.0
            draw.numbers.forEach { number ->
                if (number in Constants.LOTTO_MIN_NUMBER..Constants.LOTTO_MAX_NUMBER) {
                    appearanceCount[number] = (appearanceCount[number] ?: 0) + 1
                    if (prize > 0.0) {
                        prizeValues[number]?.add(prize)
                    }
                }
            }
        }

        val totalDrawsCount = draws.size.toDouble().coerceAtLeast(1.0)
        val rawIndexes = (Constants.LOTTO_MIN_NUMBER..Constants.LOTTO_MAX_NUMBER).associateWith { number ->
            val values = prizeValues[number].orEmpty()
            val averagePrize = values.average().takeIf { values.isNotEmpty() } ?: 0.0
            val appearanceRate = (appearanceCount[number] ?: 0) / totalDrawsCount
            appearanceRate * averagePrize
        }

        val maxEV = rawIndexes.values.maxOrNull() ?: 1.0
        val minEV = rawIndexes.values.minOrNull() ?: 0.0
        val range = maxEV - minEV
        val ranking = rawIndexes.entries
            .sortedByDescending { it.value }
            .mapIndexed { index, entry -> entry.key to index + 1 }
            .toMap()

        return (Constants.LOTTO_MIN_NUMBER..Constants.LOTTO_MAX_NUMBER).map { num ->
            val ev = rawIndexes[num] ?: 0.0
            val normalized = if (range <= 0.0) 0.0 else ((ev - minEV) / range).coerceIn(0.0, 1.0)
            val appCnt = appearanceCount[num] ?: 0
            val samples = prizeValues[num].orEmpty()
            val averagePrize = samples.average().takeIf { samples.isNotEmpty() } ?: 0.0

            HistoricalPrizeIndex(
                number = num,
                appearanceCount = appCnt,
                appearanceRate = appCnt / totalDrawsCount,
                prizeSampleCount = samples.size,
                averageFirstPrize = averagePrize,
                rawIndex = ev,
                normalizedScore = normalized,
                rank = ranking[num] ?: Constants.LOTTO_MAX_NUMBER
            )
        }
    }
}
