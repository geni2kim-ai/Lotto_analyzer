package com.example.lottoinsight.core.network.model

import com.example.lottoinsight.core.common.Constants
import com.example.lottoinsight.core.model.Draw
import com.google.gson.annotations.SerializedName

data class RemoteDrawDto(
    @SerializedName("drwNo")
    val drwNo: Int? = null,
    @SerializedName("drwNoDate")
    val drwNoDate: String? = null,
    @SerializedName("drwtNo1")
    val drwtNo1: Int? = null,
    @SerializedName("drwtNo2")
    val drwtNo2: Int? = null,
    @SerializedName("drwtNo3")
    val drwtNo3: Int? = null,
    @SerializedName("drwtNo4")
    val drwtNo4: Int? = null,
    @SerializedName("drwtNo5")
    val drwtNo5: Int? = null,
    @SerializedName("drwtNo6")
    val drwtNo6: Int? = null,
    @SerializedName("bnusNo")
    val bnusNo: Int? = null,
    @SerializedName("firstWinamnt")
    val firstWinamnt: Long? = null,
    @SerializedName("firstPrzwnerCo")
    val firstPrzwnerCo: Long? = null,
    @SerializedName("firstAccumMn")
    val firstAccumMn: Long? = null,
    @SerializedName("secondWinamnt")
    val secondWinamnt: Long? = null,
    @SerializedName("secondPrzwnerCo")
    val secondPrzwnerCo: Long? = null,
    @SerializedName("thirdWinamnt")
    val thirdWinamnt: Long? = null,
    @SerializedName("thirdPrzwnerCo")
    val thirdPrzwnerCo: Long? = null,
    @SerializedName("totSellMn")
    val totSellMn: Long? = null,
    @SerializedName("returnValue")
    val returnValue: String? = null
) {
    fun toDomain(rawJsonString: String? = null): Draw {
        val numbers = listOfNotNull(drwtNo1, drwtNo2, drwtNo3, drwtNo4, drwtNo5, drwtNo6)
        return Draw(
            drawNo = drwNo ?: 0,
            drawDate = drwNoDate ?: "",
            numbers = numbers,
            bonus = bnusNo ?: 0,
            firstWinners = firstPrzwnerCo,
            firstPrize = firstWinamnt,
            firstTotal = firstAccumMn,
            secondWinners = secondPrzwnerCo,
            secondPrize = secondWinamnt,
            thirdWinners = thirdPrzwnerCo,
            thirdPrize = thirdWinamnt,
            salesAmount = totSellMn,
            source = "dhlottery_api",
            fetchedAt = System.currentTimeMillis(),
            payloadVersion = Constants.PAYLOAD_VERSION,
            rawJson = rawJsonString
        )
    }
}
