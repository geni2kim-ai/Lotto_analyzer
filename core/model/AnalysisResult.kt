package com.example.lottoinsight.core.model

import com.example.lottoinsight.core.common.Constants

data class AnalysisResult(
    val runId: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val latestDrawNo: Int,
    val recentN: Int,
    val gameCount: Int,
    val weightConfig: WeightConfig,
    val games: List<LottoGame>,
    val randomSeed: Long,
    val algorithmVersion: String = Constants.ALGORITHM_VERSION,
    val dataStartDrawNo: Int = 1,
    val dataEndDrawNo: Int = latestDrawNo
)
