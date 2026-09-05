package com.example.lottoinsight.core.engine.generator

import com.example.lottoinsight.core.common.Constants
import com.example.lottoinsight.core.model.LottoGame
import java.util.Random

class NumberGenerator(
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
                val candidate = sampleSixNumbers(scores, activeRandom)
                if (isValidGameCombination(candidate)) {
                    pickedNumbers = candidate.sorted()
                    break
                }
            }

            if (pickedNumbers.isEmpty()) {
                pickedNumbers = sampleSixNumbers(scores, activeRandom).sorted()
            }

            val gameScore = pickedNumbers.sumOf { scores[it] ?: 0.5 } / Constants.LOTTO_PICK_COUNT
            val oddCnt = pickedNumbers.count { it % 2 != 0 }
            var pairCnt = 0
            val sorted = pickedNumbers.sorted()
            for (i in 0 until sorted.size - 1) {
                if (sorted[i + 1] - sorted[i] == 1) pairCnt++
            }

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

    private fun sampleSixNumbers(scores: Map<Int, Double>, activeRandom: Random): List<Int> {
        val candidates = (Constants.LOTTO_MIN_NUMBER..Constants.LOTTO_MAX_NUMBER).toMutableList()
        val picked = mutableSetOf<Int>()

        while (picked.size < Constants.LOTTO_PICK_COUNT && candidates.isNotEmpty()) {
            val selected = selectWeightedCandidate(candidates, scores, activeRandom)
            candidates.remove(selected)
            picked.add(selected)
        }
        return picked.toList()
    }

    private fun selectWeightedCandidate(
        candidates: List<Int>,
        scores: Map<Int, Double>,
        activeRandom: Random
    ): Int {
        val totalWeight = candidates.sumOf { scores[it] ?: 0.5 }
        if (totalWeight <= 0.0) {
            val randomIndex = activeRandom.nextInt(candidates.size)
            return candidates[randomIndex]
        }
        val r = activeRandom.nextDouble() * totalWeight
        var cumulative = 0.0
        for (cand in candidates) {
            cumulative += scores[cand] ?: 0.5
            if (r <= cumulative) {
                return cand
            }
        }
        return candidates.last()
    }

    private fun isValidGameCombination(numbers: List<Int>): Boolean {
        if (numbers.size != Constants.LOTTO_PICK_COUNT) return false
        if (numbers.distinct().size != Constants.LOTTO_PICK_COUNT) return false
        val sorted = numbers.sorted()
        var consecutiveCount = 0
        for (i in 0 until sorted.size - 1) {
            if (sorted[i + 1] - sorted[i] == 1) {
                consecutiveCount++
            }
        }
        return consecutiveCount <= 3
    }
}
