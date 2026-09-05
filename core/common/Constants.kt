package com.example.lottoinsight.core.common

object Constants {
    const val LOTTO_MIN_NUMBER = 1
    const val LOTTO_MAX_NUMBER = 45
    const val LOTTO_PICK_COUNT = 6

    const val DEFAULT_CANDIDATE_COUNT = 5000
    const val DEFAULT_RECENT_N = 100
    const val DEFAULT_GAME_COUNT = 5

    const val ALGORITHM_VERSION = "lotto-analysis-1.0"
    const val PAYLOAD_VERSION = 1

    val ALL_NUMBERS: List<Int> = (LOTTO_MIN_NUMBER..LOTTO_MAX_NUMBER).toList()

    fun getBallColorHex(number: Int): String {
        return when (number) {
            in 1..10 -> "#F2B134"
            in 11..20 -> "#3B82F6"
            in 21..30 -> "#EF4444"
            in 31..40 -> "#6B7280"
            in 41..45 -> "#22A06B"
            else -> "#8792A7"
        }
    }
}
