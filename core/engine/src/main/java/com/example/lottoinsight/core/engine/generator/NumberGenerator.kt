package com.example.lottoinsight.core.engine.generator

import com.example.lottoinsight.core.common.Constants
import com.example.lottoinsight.core.model.LottoGame
import java.util.Random

open class NumberGenerator(
    private val random: Random = Random()
) {

    fun generateGames(
        scores: Map<Int, Double>,
        gameCount: Int = 5,
        maxAttemptsPerGame: Int = 100,
        seed: Long? = null
    ): List<LottoGame> {
        require(gameCount > 0) { "gameCount must be positive" }
        require(maxAttemptsPerGame > 0) { "maxAttemptsPerGame must be positive" }

        val activeRandom = seed?.let(::Random) ?: random
        val games = mutableListOf<LottoGame>()
        for (gameIdx in 0 until gameCount) {
            var attempts = 0
            var pickedNumbers: List<Int> = emptyList()

            while (attempts < maxAttemptsPerGame) {
                attempts++
                val candidate = weightedSample(scores, Constants.LOTTO_PICK_COUNT, activeRandom)
                if (isValidGameCombination(candidate)) {
                    pickedNumbers = candidate
                    break
                }
            }

            if (pickedNumbers.isEmpty()) {
                pickedNumbers = weightedSample(scores, Constants.LOTTO_PICK_COUNT, activeRandom)
            }

            val gameScore = pickedNumbers.sumOf { scores[it] ?: DEFAULT_WEIGHT } / Constants.LOTTO_PICK_COUNT
            val oddCnt = pickedNumbers.count { it % 2 != 0 }
            val pairCnt = pickedNumbers.zipWithNext().count { (left, right) -> right - left == 1 }

            games.add(
                LottoGame(
                    gameIndex = gameIdx + 1,
                    numbers = pickedNumbers,
                    totalScore = gameScore,
                    frequencyScore = gameScore,
                    consecutiveScore = 0.5,
                    parityScore = 0.5,
                    prizeScore = 0.5,
                    oddCount = oddCnt,
                    pairCount = pairCnt
                )
            )
        }
        return games
    }

    internal open fun weightedSample(
        scores: Map<Int, Double>,
        count: Int,
        activeRandom: Random
    ): List<Int> {
        require(count >= 0) { "count must not be negative" }
        val candidates = Constants.ALL_NUMBERS.toMutableList()
        val selected = mutableListOf<Int>()

        repeat(minOf(count, candidates.size)) {
            val weights = candidates.map { candidate ->
                (scores[candidate] ?: DEFAULT_WEIGHT).coerceAtLeast(0.0)
            }
            val totalWeight = weights.sum()

            val selectedIndex = if (totalWeight <= 0.0) {
                activeRandom.nextInt(candidates.size)
            } else {
                val threshold = activeRandom.nextDouble() * totalWeight
                var cumulative = 0.0
                var matchIndex = candidates.lastIndex
                for (index in candidates.indices) {
                    cumulative += weights[index]
                    if (threshold <= cumulative) {
                        matchIndex = index
                        break
                    }
                }
                matchIndex
            }

            selected += candidates.removeAt(selectedIndex)
        }

        return selected.sorted()
    }

    private fun isValidGameCombination(numbers: List<Int>): Boolean {
        if (numbers.size != Constants.LOTTO_PICK_COUNT) return false
        if (numbers.distinct().size != Constants.LOTTO_PICK_COUNT) return false
        val consecutiveCount = numbers.zipWithNext().count { (left, right) -> right - left == 1 }
        return consecutiveCount <= 3
    }

    private companion object {
        const val DEFAULT_WEIGHT = 0.5
    }
}
