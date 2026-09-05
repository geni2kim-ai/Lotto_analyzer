package com.example.lottoinsight.core.engine.calculator

import com.example.lottoinsight.core.common.Constants
import com.example.lottoinsight.core.model.Draw

object FrequencyCalculator {

    fun calculateFrequencies(draws: List<Draw>): Map<Int, Int> {
        val frequencyMap = (Constants.LOTTO_MIN_NUMBER..Constants.LOTTO_MAX_NUMBER).associateWith { 0 }.toMutableMap()
        draws.forEach { draw ->
            draw.numbers.forEach { number ->
                if (number in Constants.LOTTO_MIN_NUMBER..Constants.LOTTO_MAX_NUMBER) {
                    frequencyMap[number] = (frequencyMap[number] ?: 0) + 1
                }
            }
        }
        return frequencyMap
    }

    fun calculateNormalizedScores(draws: List<Draw>): Map<Int, Double> {
        val defaultScores = (Constants.LOTTO_MIN_NUMBER..Constants.LOTTO_MAX_NUMBER).associateWith { 0.5 }
        if (draws.isEmpty()) {
            return defaultScores
        }

        val frequencies = calculateFrequencies(draws)
        val maxFreq = frequencies.values.maxOrNull() ?: 0
        val minFreq = frequencies.values.minOrNull() ?: 0
        val range = (maxFreq - minFreq).toDouble()

        return frequencies.mapValues { (_, count) ->
            if (range <= 0.0) 0.5 else (count - minFreq) / range
        }
    }
}
