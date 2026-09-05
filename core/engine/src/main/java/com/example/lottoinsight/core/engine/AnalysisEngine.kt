package com.example.lottoinsight.core.engine

import com.example.lottoinsight.core.common.AppError
import com.example.lottoinsight.core.common.AppResult
import com.example.lottoinsight.core.common.Constants
import com.example.lottoinsight.core.engine.calculator.PrizeIndexCalculator
import com.example.lottoinsight.core.model.AnalysisResult
import com.example.lottoinsight.core.model.Draw
import com.example.lottoinsight.core.model.HistoricalPrizeIndex
import com.example.lottoinsight.core.model.LottoGame
import com.example.lottoinsight.core.model.WeightConfig
import java.util.Random
import kotlin.math.abs

interface AnalysisEngine {
    fun analyzeAndGenerate(draws: List<Draw>, config: WeightConfig): AppResult<AnalysisResult>
    fun calculatePrizeIndexes(draws: List<Draw>): AppResult<List<HistoricalPrizeIndex>>
}

/**
 * Kotlin implementation of the Windows app's analysis pipeline.
 *
 * The important detail is that the individual feature distributions are
 * calculated from the selected historical draws, then candidate tickets are
 * ranked and diversified. This keeps Android results comparable to the
 * reference desktop app instead of only sampling six numbers from one scalar
 * score per number.
 */
class AnalysisEngineImpl : AnalysisEngine {

    override fun analyzeAndGenerate(draws: List<Draw>, config: WeightConfig): AppResult<AnalysisResult> {
        if (draws.size < Constants.MIN_REQUIRED_DRAWS_FOR_ANALYSIS) {
            return AppResult.Error(AppError.InsufficientData)
        }
        if (config.recentN <= 0 || config.gameCount <= 0 || config.candidateCount <= 0) {
            return AppResult.Error(AppError.InsufficientData)
        }

        val effectiveRecentN = minOf(config.recentN, draws.size)
        val effectiveConfig = config.copy(recentN = effectiveRecentN)
        val targetDraws = draws.sortedByDescending { it.drawNo }.take(effectiveRecentN)
        if (targetDraws.isEmpty()) {
            return AppResult.Error(AppError.InsufficientData)
        }

        val stats = buildStats(targetDraws)
        val prizeIndexes = PrizeIndexCalculator.calculateHistoricalPrizeIndexes(targetDraws)
        val prizeIndexMap = prizeIndexes.associate { it.number to it.normalizedScore }
        val randomSeed = Random().nextLong()
        val games = generateGames(
            stats = stats,
            config = effectiveConfig,
            prizeScores = prizeIndexMap,
            seed = randomSeed
        )

        val latestDrawNo = targetDraws.maxOf { it.drawNo }
        return AppResult.Success(
            AnalysisResult(
                runId = 0,
                createdAt = System.currentTimeMillis(),
                latestDrawNo = latestDrawNo,
                recentN = effectiveRecentN,
                gameCount = effectiveConfig.gameCount,
                weightConfig = effectiveConfig,
                games = games,
                randomSeed = randomSeed,
                algorithmVersion = Constants.ALGORITHM_VERSION,
                dataStartDrawNo = targetDraws.minOf { it.drawNo },
                dataEndDrawNo = latestDrawNo
            )
        )
    }

    override fun calculatePrizeIndexes(draws: List<Draw>): AppResult<List<HistoricalPrizeIndex>> {
        if (draws.isEmpty()) {
            return AppResult.Error(AppError.InsufficientData)
        }
        return AppResult.Success(PrizeIndexCalculator.calculateHistoricalPrizeIndexes(draws))
    }

    private data class AnalysisStats(
        val frequency: Map<Int, Double>,
        val consecutiveInvolvement: Map<Int, Double>,
        val consecutivePairPattern: Map<Int, Double>,
        val parityPattern: Map<Int, Double>,
        val oddShare: Double
    )

    private fun buildStats(draws: List<Draw>): AnalysisStats {
        val frequency = numberCounter()
        val consecutiveInvolvement = numberCounter()
        val pairPattern = mutableMapOf<Int, Int>()
        val parityPattern = mutableMapOf<Int, Int>()
        var totalOdd = 0

        draws.forEach { draw ->
            val numbers = draw.numbers.sorted()
            numbers.forEach { number -> frequency[number] = (frequency[number] ?: 0) + 1 }

            val oddCount = numbers.count { it % 2 != 0 }
            totalOdd += oddCount
            parityPattern[oddCount] = (parityPattern[oddCount] ?: 0) + 1

            var pairCount = 0
            numbers.zipWithNext().forEach { (left, right) ->
                if (right - left == 1) {
                    consecutiveInvolvement[left] = (consecutiveInvolvement[left] ?: 0) + 1
                    consecutiveInvolvement[right] = (consecutiveInvolvement[right] ?: 0) + 1
                    pairCount++
                }
            }
            pairPattern[pairCount] = (pairPattern[pairCount] ?: 0) + 1
        }

        val maxPairCount = pairPattern.keys.maxOrNull() ?: 0
        val pairValues = (0..maxOf(5, maxPairCount)).associateWith { pairPattern[it] ?: 0 }
        val parityValues = (0..Constants.LOTTO_PICK_COUNT).associateWith { parityPattern[it] ?: 0 }

        return AnalysisStats(
            frequency = normalize(frequency.mapValues { it.value.toDouble() }),
            consecutiveInvolvement = normalize(consecutiveInvolvement.mapValues { it.value.toDouble() }),
            consecutivePairPattern = normalize(pairValues.mapValues { it.value.toDouble() }),
            parityPattern = normalize(parityValues.mapValues { it.value.toDouble() }),
            oddShare = totalOdd.toDouble() / (draws.size * Constants.LOTTO_PICK_COUNT)
        )
    }

    private fun generateGames(
        stats: AnalysisStats,
        config: WeightConfig,
        prizeScores: Map<Int, Double>,
        seed: Long
    ): List<LottoGame> {
        val random = Random(seed)
        val weights = mapOf(
            "frequency" to config.normalizedFrequency,
            "consecutive" to config.normalizedConsecutive,
            "parity" to config.normalizedParity
        )
        val parityNumberFactor = normalize(
            Constants.ALL_NUMBERS.associateWith { number ->
                if (number % 2 != 0) stats.oddShare else 1.0 - stats.oddShare
            }
        )
        val baseWeights = Constants.ALL_NUMBERS.associateWith { number ->
            var score = 0.05 +
                    weights.getValue("frequency") * stats.frequency.getValue(number) +
                    weights.getValue("consecutive") * stats.consecutiveInvolvement.getValue(number) +
                    weights.getValue("parity") * parityNumberFactor.getValue(number)
            if (config.usePrizeIndex) {
                score *= 1.0 + 0.05 * prizeScores.getValue(number)
            }
            score.coerceAtLeast(1e-6)
        }

        val unique = linkedMapOf<List<Int>, LottoGame>()
        val attempts = maxOf(config.candidateCount, config.gameCount * 400)
        repeat(attempts) {
            val ticket = weightedSample(baseWeights, Constants.LOTTO_PICK_COUNT, random)
            if (ticket !in unique) {
                unique[ticket] = scoreTicket(ticket, stats, weights, prizeScores, config.usePrizeIndex)
            }
        }

        val remaining = unique.values.sortedByDescending { it.totalScore }.toMutableList()
        val selected = mutableListOf<LottoGame>()
        while (remaining.isNotEmpty() && selected.size < config.gameCount) {
            var bestIndex = 0
            var bestAdjusted = Double.NEGATIVE_INFINITY
            remaining.take(1500).forEachIndexed { index, candidate ->
                val overlap = if (selected.isEmpty()) 0 else selected.maxOf { chosen ->
                    candidate.numbers.toSet().intersect(chosen.numbers.toSet()).size
                }
                val adjusted = candidate.totalScore - maxOf(0, overlap - 2) * 0.07
                if (adjusted > bestAdjusted) {
                    bestAdjusted = adjusted
                    bestIndex = index
                }
            }
            val chosen = remaining.removeAt(bestIndex)
            val maxOverlap = if (selected.isEmpty()) 0 else selected.maxOf { prior ->
                chosen.numbers.toSet().intersect(prior.numbers.toSet()).size
            }
            selected += chosen.copy(
                gameIndex = selected.size + 1,
                maxOverlap = maxOverlap
            )
        }

        if (selected.size < config.gameCount) {
            return selected + fallbackGames(baseWeights, stats, weights, prizeScores, config, random, selected.size)
        }
        return selected
    }

    private fun fallbackGames(
        baseWeights: Map<Int, Double>,
        stats: AnalysisStats,
        weights: Map<String, Double>,
        prizeScores: Map<Int, Double>,
        config: WeightConfig,
        random: Random,
        alreadySelected: Int
    ): List<LottoGame> {
        val fallback = mutableListOf<LottoGame>()
        while (alreadySelected + fallback.size < config.gameCount) {
            val ticket = weightedSample(baseWeights, Constants.LOTTO_PICK_COUNT, random)
            val candidate = scoreTicket(ticket, stats, weights, prizeScores, config.usePrizeIndex)
            fallback += candidate.copy(gameIndex = alreadySelected + fallback.size + 1)
        }
        return fallback
    }

    private fun scoreTicket(
        numbers: List<Int>,
        stats: AnalysisStats,
        weights: Map<String, Double>,
        prizeScores: Map<Int, Double>,
        usePrizeIndex: Boolean
    ): LottoGame {
        val ordered = numbers.sorted()
        val frequencyScore = ordered.map { stats.frequency.getValue(it) }.average()
        val pairCount = ordered.zipWithNext().count { (left, right) -> right - left == 1 }
        val perNumberConsecutive = ordered.map { stats.consecutiveInvolvement.getValue(it) }.average()
        val pairPatternScore = stats.consecutivePairPattern[pairCount] ?: 0.0
        val consecutiveScore = 0.6 * perNumberConsecutive + 0.4 * pairPatternScore
        val oddCount = ordered.count { it % 2 != 0 }
        val parityScore = stats.parityPattern[oddCount] ?: 0.0
        val prizeScore = if (prizeScores.isEmpty()) 0.0 else ordered.map { prizeScores.getValue(it) }.average()
        val coreScore = weights.getValue("frequency") * frequencyScore +
                weights.getValue("consecutive") * consecutiveScore +
                weights.getValue("parity") * parityScore
        val totalScore = if (usePrizeIndex) 0.95 * coreScore + 0.05 * prizeScore else coreScore

        return LottoGame(
            gameIndex = 0,
            numbers = ordered,
            totalScore = totalScore,
            frequencyScore = frequencyScore,
            consecutiveScore = consecutiveScore,
            parityScore = parityScore,
            prizeScore = prizeScore,
            oddCount = oddCount,
            pairCount = pairCount
        )
    }

    private fun weightedSample(weights: Map<Int, Double>, count: Int, random: Random): List<Int> {
        val items = weights.keys.toMutableList()
        val itemWeights = items.map { maxOf(1e-9, weights.getValue(it)) }.toMutableList()
        val selected = mutableListOf<Int>()
        repeat(minOf(count, items.size)) {
            val total = itemWeights.sum()
            val pick = random.nextDouble() * total
            var cumulative = 0.0
            var selectedIndex = items.lastIndex
            itemWeights.forEachIndexed { index, weight ->
                cumulative += weight
                if (pick <= cumulative && selectedIndex == items.lastIndex) {
                    selectedIndex = index
                }
            }
            selected += items.removeAt(selectedIndex)
            itemWeights.removeAt(selectedIndex)
        }
        return selected.sorted()
    }

    private fun numberCounter(): MutableMap<Int, Int> =
        Constants.ALL_NUMBERS.associateWith { 0 }.toMutableMap()

    private fun normalize(values: Map<Int, Double>): Map<Int, Double> {
        if (values.isEmpty()) return emptyMap()
        val low = values.values.minOrNull() ?: 0.0
        val high = values.values.maxOrNull() ?: 0.0
        if (abs(low - high) < 1e-12) return values.mapValues { 0.5 }
        return values.mapValues { (_, value) -> (value - low) / (high - low) }
    }
}
