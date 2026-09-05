package com.example.lottoinsight.core.model

import com.example.lottoinsight.core.common.Constants

data class LottoGame(
    val gameIndex: Int,
    val numbers: List<Int>,
    val totalScore: Double,
    val frequencyScore: Double,
    val consecutiveScore: Double,
    val parityScore: Double,
    val prizeScore: Double = 0.0,
    val oddCount: Int,
    val pairCount: Int,
    val maxOverlap: Int = 0
) {
    val sortedNumbers: List<Int> = numbers.sorted()

    init {
        require(numbers.size == Constants.LOTTO_PICK_COUNT) { "Game must have exactly 6 numbers" }
        require(numbers.distinct().size == Constants.LOTTO_PICK_COUNT) { "Game numbers must be unique" }
    }
}
