package com.example.lottoinsight.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.lottoinsight.core.common.Constants
import com.example.lottoinsight.core.model.Draw

@Entity(tableName = "draws")
data class DrawEntity(
    @PrimaryKey
    val drawNo: Int,
    val drawDate: String,
    val n1: Int,
    val n2: Int,
    val n3: Int,
    val n4: Int,
    val n5: Int,
    val n6: Int,
    val bonus: Int,
    val firstWinners: Long?,
    val firstPrize: Long?,
    val firstTotal: Long?,
    val secondWinners: Long?,
    val secondPrize: Long?,
    val thirdWinners: Long?,
    val thirdPrize: Long?,
    val salesAmount: Long?,
    val source: String,
    val fetchedAt: Long,
    val payloadVersion: Int,
    val rawJson: String?
) {
    fun toDomain(): Draw {
        return Draw(
            drawNo = drawNo,
            drawDate = drawDate,
            numbers = listOf(n1, n2, n3, n4, n5, n6),
            bonus = bonus,
            firstWinners = firstWinners,
            firstPrize = firstPrize,
            firstTotal = firstTotal,
            secondWinners = secondWinners,
            secondPrize = secondPrize,
            thirdWinners = thirdWinners,
            thirdPrize = thirdPrize,
            salesAmount = salesAmount,
            source = source,
            fetchedAt = fetchedAt,
            payloadVersion = payloadVersion,
            rawJson = rawJson
        )
    }

    companion object {
        fun fromDomain(draw: Draw): DrawEntity {
            require(draw.numbers.size == Constants.LOTTO_PICK_COUNT) {
                "Draw domain object must contain exactly ${Constants.LOTTO_PICK_COUNT} numbers for DrawEntity mapping"
            }
            val sorted = draw.numbers.sorted()
            return DrawEntity(
                drawNo = draw.drawNo,
                drawDate = draw.drawDate,
                n1 = sorted[0],
                n2 = sorted[1],
                n3 = sorted[2],
                n4 = sorted[3],
                n5 = sorted[4],
                n6 = sorted[5],
                bonus = draw.bonus,
                firstWinners = draw.firstWinners,
                firstPrize = draw.firstPrize,
                firstTotal = draw.firstTotal,
                secondWinners = draw.secondWinners,
                secondPrize = draw.secondPrize,
                thirdWinners = draw.thirdWinners,
                thirdPrize = draw.thirdPrize,
                salesAmount = draw.salesAmount,
                source = draw.source,
                fetchedAt = draw.fetchedAt,
                payloadVersion = draw.payloadVersion,
                rawJson = draw.rawJson
            )
        }
    }
}
