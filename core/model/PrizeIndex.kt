package com.example.lottoinsight.core.model

data class HistoricalPrizeIndex(
    val runId: Long = 0,
    val number: Int,
    val appearanceCount: Int,
    val appearanceRate: Double,
    val prizeSampleCount: Int,
    val averageFirstPrize: Double,
    val rawIndex: Double,
    val normalizedScore: Double,
    val rank: Int = 0
)

data class PrizeIndexRun(
    val id: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val latestDrawNo: Int,
    val recentN: Int,
    val algorithmVersion: String,
    val items: List<HistoricalPrizeIndex> = emptyList()
)
