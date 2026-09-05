package com.example.lottoinsight.core.engine

import com.example.lottoinsight.core.common.AppError
import com.example.lottoinsight.core.common.AppResult
import com.example.lottoinsight.core.common.Constants
import com.example.lottoinsight.core.engine.calculator.ConsecutiveCalculator
import com.example.lottoinsight.core.engine.calculator.FrequencyCalculator
import com.example.lottoinsight.core.engine.calculator.ParityCalculator
import com.example.lottoinsight.core.engine.calculator.PrizeIndexCalculator
import com.example.lottoinsight.core.engine.generator.NumberGenerator
import com.example.lottoinsight.core.model.AnalysisResult
import com.example.lottoinsight.core.model.Draw
import com.example.lottoinsight.core.model.HistoricalPrizeIndex
import com.example.lottoinsight.core.model.WeightConfig

interface AnalysisEngine {
    fun analyzeAndGenerate(draws: List<Draw>, config: WeightConfig): AppResult<AnalysisResult>
    fun calculatePrizeIndexes(draws: List<Draw>): AppResult<List<HistoricalPrizeIndex>>
}

class AnalysisEngineImpl(
    private val generator: NumberGenerator = NumberGenerator()
) : AnalysisEngine {

    override fun analyzeAndGenerate(draws: List<Draw>, config: WeightConfig): AppResult<AnalysisResult> {
        if (draws.size < Constants.MIN_REQUIRED_DRAWS_FOR_ANALYSIS) {
            return AppResult.Error(
                AppError.InsufficientData("Requires at least ${Constants.MIN_REQUIRED_DRAWS_FOR_ANALYSIS} draws for analysis, got ${draws.size}")
            )
        }

        val targetDraws = draws.take(config.recentN)
        if (targetDraws.isEmpty()) {
            return AppResult.Error(AppError.InsufficientData("No draws available within recentN=${config.recentN} filter"))
        }

        val combinedScores = calculateCombinedScores(targetDraws, config)
        val games = generator.generateGames(combinedScores, config.gameCount)
        val latestDrawNo = targetDraws.firstOrNull()?.drawNo ?: 0

        val result = AnalysisResult(
            runId = 0,
            createdAt = System.currentTimeMillis(),
            latestDrawNo = latestDrawNo,
            config = config,
            recommendedGames = games,
            algorithmVersion = "1.0.0"
        )

        return AppResult.Success(result)
    }

    private fun calculateCombinedScores(targetDraws: List<Draw>, config: WeightConfig): Map<Int, Double> {
        val freqScores = FrequencyCalculator.calculateNormalizedScores(targetDraws)
        val consecutiveScores = ConsecutiveCalculator.calculateConsecutiveScores(targetDraws)
        val parityScores = ParityCalculator.calculateParityScores()
        val prizeIndexes = PrizeIndexCalculator.calculateHistoricalPrizeIndexes(targetDraws)
        val prizeIndexMap = prizeIndexes.associate { it.number to it.normalizedWeight }

        val normalizedWeights = config.calculateNormalizedRatios()
        val wFreq = normalizedWeights["frequency"] ?: 0.33
        val wConsec = normalizedWeights["consecutive"] ?: 0.33
        val wParity = normalizedWeights["parity"] ?: 0.34

        return (Constants.LOTTO_MIN_NUMBER..Constants.LOTTO_MAX_NUMBER).associateWith { num ->
            val baseScore = (freqScores[num] ?: 0.5) * wFreq +
                    (consecutiveScores[num] ?: 0.5) * wConsec +
                    (parityScores[num] ?: 0.5) * wParity

            if (config.usePrizeIndex) {
                baseScore * 0.7 + (prizeIndexMap[num] ?: 0.5) * 0.3
            } else {
                baseScore
            }
        }
    }

    override fun calculatePrizeIndexes(draws: List<Draw>): AppResult<List<HistoricalPrizeIndex>> {
        if (draws.isEmpty()) {
            return AppResult.Error(AppError.InsufficientData("Cannot calculate prize indexes from empty draws list"))
        }
        val indexes = PrizeIndexCalculator.calculateHistoricalPrizeIndexes(draws)
        return AppResult.Success(indexes)
    }
}
