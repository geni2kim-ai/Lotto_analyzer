package com.example.lottoinsight.core.network.model

import com.example.lottoinsight.core.common.AppError
import com.example.lottoinsight.core.common.Constants

object RemoteDrawResponseValidator {

    fun validate(dto: RemoteDrawDto, expectedDrawNo: Int? = null): AppError? {
        val statusError = validateStatus(dto.returnValue)
        if (statusError != null) return statusError

        val drawNo = dto.drwNo ?: return AppError.ParseError("Missing drwNo field in remote payload")
        val drawNoError = validateDrawNo(drawNo)
        if (drawNoError != null) return drawNoError
        if (expectedDrawNo != null && drawNo != expectedDrawNo) {
            return AppError.InvalidDraw(
                expectedDrawNo,
                "Response draw number $drawNo does not match requested draw $expectedDrawNo"
            )
        }

        val numbers = listOfNotNull(dto.drwtNo1, dto.drwtNo2, dto.drwtNo3, dto.drwtNo4, dto.drwtNo5, dto.drwtNo6)
        val numbersError = validateNumbers(drawNo, numbers)
        if (numbersError != null) return numbersError

        val bonus = dto.bnusNo ?: return AppError.ParseError("Missing bnusNo bonus field for draw $drawNo")
        return validateBonus(drawNo, bonus, numbers)
    }

    private fun validateStatus(returnValue: String?): AppError? {
        if (returnValue != "success") {
            return AppError.InvalidDraw(0, "API returned fail or unexpected status: $returnValue")
        }
        return null
    }

    private fun validateDrawNo(drawNo: Int): AppError? {
        if (drawNo <= 0) {
            return AppError.InvalidDraw(drawNo, "drwNo must be positive integer, got $drawNo")
        }
        return null
    }

    private fun validateNumbers(drawNo: Int, numbers: List<Int>): AppError? {
        if (numbers.size != Constants.LOTTO_PICK_COUNT) {
            return AppError.ParseError("Draw $drawNo numbers count must be ${Constants.LOTTO_PICK_COUNT}, got ${numbers.size}")
        }
        if (numbers.distinct().size != Constants.LOTTO_PICK_COUNT) {
            return AppError.InvalidDraw(drawNo, "Draw $drawNo numbers must be unique, got $numbers")
        }
        if (numbers.any { it !in Constants.LOTTO_MIN_NUMBER..Constants.LOTTO_MAX_NUMBER }) {
            return AppError.InvalidDraw(drawNo, "Draw $drawNo numbers must be in 1..45 range, got $numbers")
        }
        return null
    }

    private fun validateBonus(drawNo: Int, bonus: Int, numbers: List<Int>): AppError? {
        if (bonus !in Constants.LOTTO_MIN_NUMBER..Constants.LOTTO_MAX_NUMBER) {
            return AppError.InvalidDraw(drawNo, "Draw $drawNo bonus number must be in 1..45 range, got $bonus")
        }
        if (bonus in numbers) {
            return AppError.InvalidDraw(drawNo, "Draw $drawNo bonus number must not duplicate a winning number")
        }
        return null
    }
}
