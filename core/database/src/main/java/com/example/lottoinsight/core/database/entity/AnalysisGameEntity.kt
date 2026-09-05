package com.example.lottoinsight.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.lottoinsight.core.common.Constants
import com.example.lottoinsight.core.model.LottoGame

@Entity(
    tableName = "analysis_games",
    foreignKeys = [
        ForeignKey(
            entity = AnalysisRunEntity::class,
            parentColumns = ["id"],
            childColumns = ["analysisRunId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["analysisRunId"])]
)
data class AnalysisGameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val analysisRunId: Long,
    val gameIndex: Int,
    val n1: Int,
    val n2: Int,
    val n3: Int,
    val n4: Int,
    val n5: Int,
    val n6: Int,
    val totalScore: Double,
    val frequencyScore: Double,
    val consecutiveScore: Double,
    val parityScore: Double,
    val prizeScore: Double,
    val oddCount: Int,
    val pairCount: Int,
    val maxOverlap: Int
) {
    fun toDomain(): LottoGame {
        return LottoGame(
            gameIndex = gameIndex,
            numbers = listOf(n1, n2, n3, n4, n5, n6),
            totalScore = totalScore,
            frequencyScore = frequencyScore,
            consecutiveScore = consecutiveScore,
            parityScore = parityScore,
            prizeScore = prizeScore,
            oddCount = oddCount,
            pairCount = pairCount,
            maxOverlap = maxOverlap
        )
    }

    companion object {
        fun fromDomain(analysisRunId: Long, game: LottoGame): AnalysisGameEntity {
            require(game.numbers.size == Constants.LOTTO_PICK_COUNT) {
                "LottoGame object must contain exactly ${Constants.LOTTO_PICK_COUNT} numbers for AnalysisGameEntity mapping"
            }
            val sorted = game.numbers.sorted()
            return AnalysisGameEntity(
                analysisRunId = analysisRunId,
                gameIndex = game.gameIndex,
                n1 = sorted[0],
                n2 = sorted[1],
                n3 = sorted[2],
                n4 = sorted[3],
                n5 = sorted[4],
                n6 = sorted[5],
                totalScore = game.totalScore,
                frequencyScore = game.frequencyScore,
                consecutiveScore = game.consecutiveScore,
                parityScore = game.parityScore,
                prizeScore = game.prizeScore,
                oddCount = game.oddCount,
                pairCount = game.pairCount,
                maxOverlap = game.maxOverlap
            )
        }
    }
}
