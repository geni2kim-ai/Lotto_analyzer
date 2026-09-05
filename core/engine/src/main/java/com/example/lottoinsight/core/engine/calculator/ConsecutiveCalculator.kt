package com.example.lottoinsight.core.engine.calculator

import com.example.lottoinsight.core.common.Constants
import com.example.lottoinsight.core.model.Draw

object ConsecutiveCalculator {

    fun calculateConsecutiveScores(draws: List<Draw>): Map<Int, Double> {
        val consecutiveCounts = (Constants.LOTTO_MIN_NUMBER..Constants.LOTTO_MAX_NUMBER).associateWith { 0 }.toMutableMap()

        draws.forEach { draw ->
            val numbers = draw.numbers.sorted()
            for (i in 0 until numbers.size - 1) {
                if (numbers[i + 1] - numbers[i] == 1) {
                    consecutiveCounts[numbers[i]] = (consecutiveCounts[numbers[i]] ?: 0) + 1
                    consecutiveCounts[numbers[i + 1]] = (consecutiveCounts[numbers[i + 1]] ?: 0) + 1
                }
            }
        }

        val maxCount = consecutiveCounts.values.maxOrNull() ?: 0
        val minCount = consecutiveCounts.values.minOrNull() ?: 0
        val range = (maxCount - minCount).toDouble()

        return consecutiveCounts.mapValues { (_, count) ->
            if (range <= 0.0) 0.5 else (count - minCount) / range
        }
    }
}
