package com.example.lottoinsight.core.network.model

import com.example.lottoinsight.core.common.AppError
import com.example.lottoinsight.core.common.Constants
import com.example.lottoinsight.core.model.Draw
import com.google.gson.Gson

object NewRemoteDrawResponseValidator {

    fun validate(dto: NewRemoteDrawDto, expectedDrawNo: Int? = null): AppError? {
        return LegacyRemoteDrawResponseValidator.validate(
            RemoteDrawDto(
                drwNo = dto.ltEpsd,
                drwtNo1 = dto.tm1WnNo,
                drwtNo2 = dto.tm2WnNo,
                drwtNo3 = dto.tm3WnNo,
                drwtNo4 = dto.tm4WnNo,
                drwtNo5 = dto.tm5WnNo,
                drwtNo6 = dto.tm6WnNo,
                bnusNo = dto.bnsWnNo,
                returnValue = "success"
            ),
            expectedDrawNo
        )
    }
}

fun NewRemoteDrawDto.toDomain(): Draw {
    val drawDate = ltRflYmd?.let { raw ->
        if (raw.length == 8 && raw.all(Char::isDigit)) {
            "${raw.substring(0, 4)}-${raw.substring(4, 6)}-${raw.substring(6, 8)}"
        } else {
            raw
        }
    }.orEmpty()

    return Draw(
        drawNo = ltEpsd ?: 0,
        drawDate = drawDate,
        numbers = listOfNotNull(tm1WnNo, tm2WnNo, tm3WnNo, tm4WnNo, tm5WnNo, tm6WnNo),
        bonus = bnsWnNo ?: 0,
        firstWinners = rnk1WnNope,
        firstPrize = rnk1WnAmt,
        firstTotal = rnk1SumWnAmt,
        secondWinners = rnk2WnNope,
        secondPrize = rnk2WnAmt,
        thirdWinners = rnk3WnNope,
        thirdPrize = rnk3WnAmt,
        salesAmount = rlvtEpsdSumNtslAmt,
        source = "dhlottery_new_api",
        fetchedAt = System.currentTimeMillis(),
        payloadVersion = Constants.PAYLOAD_VERSION,
        rawJson = Gson().toJson(this)
    )
}
