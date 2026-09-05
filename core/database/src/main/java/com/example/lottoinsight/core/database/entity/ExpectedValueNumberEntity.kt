package com.example.lottoinsight.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.example.lottoinsight.core.model.HistoricalPrizeIndex

@Entity(
    tableName = "expected_value_numbers",
    primaryKeys = ["runId", "number"],
    foreignKeys = [
        ForeignKey(
            entity = ExpectedValueRunEntity::class,
            parentColumns = ["id"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["runId"])]
)
data class ExpectedValueNumberEntity(
    val runId: Long,
    val number: Int,
    val appearanceCount: Int,
    val appearanceRate: Double,
    val prizeSampleCount: Int,
    val averageFirstPrize: Double,
    val rawIndex: Double,
    val normalizedScore: Double,
    val rank: Int
) {
    fun toDomain(): HistoricalPrizeIndex {
        return HistoricalPrizeIndex(
            runId = runId,
            number = number,
            appearanceCount = appearanceCount,
            appearanceRate = appearanceRate,
            prizeSampleCount = prizeSampleCount,
            averageFirstPrize = averageFirstPrize,
            rawIndex = rawIndex,
            normalizedScore = normalizedScore,
            rank = rank
        )
    }

    companion object {
        fun fromDomain(runId: Long, item: HistoricalPrizeIndex): ExpectedValueNumberEntity {
            return ExpectedValueNumberEntity(
                runId = runId,
                number = item.number,
                appearanceCount = item.appearanceCount,
                appearanceRate = item.appearanceRate,
                prizeSampleCount = item.prizeSampleCount,
                averageFirstPrize = item.averageFirstPrize,
                rawIndex = item.rawIndex,
                normalizedScore = item.normalizedScore,
                rank = item.rank
            )
        }
    }
}
