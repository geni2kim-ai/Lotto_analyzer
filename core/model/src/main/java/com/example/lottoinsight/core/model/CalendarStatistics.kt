package com.example.lottoinsight.core.model

data class CalendarGroupCount(
    val categoryCount: Int,
    val totalDraws: Int,
    val avgWinners: Double,
    val avgPrize: Double
)

data class CalendarStatistics(
    val totalRounds: Int,
    val monthRangeCorrWithPrize: Double,
    val dayRangeCorrWithPrize: Double,
    val evenOddCorrWithPrize: Double,
    val consec2CorrWithPrize: Double,
    val consec3CorrWithPrize: Double,
    val monthRangeGroups: List<CalendarGroupCount>,
    val evenOddGroups: List<CalendarGroupCount>,
    val consecGroups: List<CalendarGroupCount>
)
