package com.example.lottoinsight.core.model

import com.example.lottoinsight.core.common.Constants

data class WeightConfig(
    val frequencyWeight: Int = 40,
    val consecutiveWeight: Int = 30,
    val parityWeight: Int = 30,
    val usePrizeIndex: Boolean = true,
    val recentN: Int = Constants.DEFAULT_RECENT_N,
    val gameCount: Int = Constants.DEFAULT_GAME_COUNT,
    val candidateCount: Int = Constants.DEFAULT_CANDIDATE_COUNT
) {
    val normalizedFrequency: Double
    val normalizedConsecutive: Double
    val normalizedParity: Double

    init {
        val (fRatio, cRatio, pRatio) = calculateNormalizedRatios(
            frequencyWeight,
            consecutiveWeight,
            parityWeight
        )
        normalizedFrequency = fRatio
        normalizedConsecutive = cRatio
        normalizedParity = pRatio
    }

    companion object {
        private fun calculateNormalizedRatios(
            freq: Int,
            consec: Int,
            parity: Int
        ): Triple<Double, Double, Double> {
            val safeFreq = maxOf(0, freq)
            val safeConsec = maxOf(0, consec)
            val safeParity = maxOf(0, parity)
            val total = (safeFreq + safeConsec + safeParity).toDouble()

            if (total <= 0.0) {
                val uniform = 1.0 / 3.0
                return Triple(uniform, uniform, uniform)
            }

            return Triple(
                safeFreq / total,
                safeConsec / total,
                safeParity / total
            )
        }
    }
}
