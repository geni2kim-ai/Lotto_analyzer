package com.example.lottoinsight.core.model

import com.example.lottoinsight.core.common.Constants

data class Draw(
    val drawNo: Int,
    val drawDate: String,
    val numbers: List<Int>,
    val bonus: Int,
    val firstWinners: Long? = null,
    val firstPrize: Long? = null,
    val firstTotal: Long? = null,
    val secondWinners: Long? = null,
    val secondPrize: Long? = null,
    val thirdWinners: Long? = null,
    val thirdPrize: Long? = null,
    val salesAmount: Long? = null,
    val source: String,
    val fetchedAt: Long,
    val payloadVersion: Int = Constants.PAYLOAD_VERSION,
    val rawJson: String? = null
) {
    init {
        require(drawNo > 0) { "drawNo must be greater than 0, got $drawNo" }
        require(numbers.size == Constants.LOTTO_PICK_COUNT) {
            "Draw must contain exactly ${Constants.LOTTO_PICK_COUNT} numbers, got ${numbers.size}"
        }
        require(numbers.distinct().size == Constants.LOTTO_PICK_COUNT) {
            "Draw numbers must be unique, got duplicates in $numbers"
        }
        val isValidRange = numbers.all { it in Constants.LOTTO_MIN_NUMBER..Constants.LOTTO_MAX_NUMBER }
        require(isValidRange) {
            "Numbers must be in ${Constants.LOTTO_MIN_NUMBER}..${Constants.LOTTO_MAX_NUMBER} range, got $numbers"
        }
        require(bonus in Constants.LOTTO_MIN_NUMBER..Constants.LOTTO_MAX_NUMBER) {
            "Bonus must be in ${Constants.LOTTO_MIN_NUMBER}..${Constants.LOTTO_MAX_NUMBER} range, got $bonus"
        }
    }
}
