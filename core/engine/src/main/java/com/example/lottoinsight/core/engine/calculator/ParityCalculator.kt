package com.example.lottoinsight.core.engine.calculator

import com.example.lottoinsight.core.common.Constants

object ParityCalculator {

    fun calculateParityScores(): Map<Int, Double> {
        val scores = mutableMapOf<Int, Double>()
        for (num in Constants.LOTTO_MIN_NUMBER..Constants.LOTTO_MAX_NUMBER) {
            scores[num] = if (num % 2 != 0) 0.55 else 0.45
        }
        return scores
    }

    fun isBalancedParity(numbers: List<Int>): Boolean {
        if (numbers.size != Constants.LOTTO_PICK_COUNT) return false
        val oddCount = numbers.count { it % 2 != 0 }
        return oddCount in 2..4
    }
}
