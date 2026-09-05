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
        maxAttemptsPerGame: Int = 100
    ): List<LottoGame> {
        val games = mutableListOf<LottoGame>()
        for (gameIdx in 0 until gameCount) {
            var attempts = 0
            var pickedNumbers: List<Int> = emptyList()

            while (attempts < maxAttemptsPerGame) {
                attempts++
                val candidate = sampleSixNumbers(scores)
                if (isValidGameCombination(candidate)) {
                    pickedNumbers = candidate.sorted()
                    break
                }
            }

            if (pickedNumbers.isEmpty()) {
                pickedNumbers = sampleSixNumbers(scores).sorted()
            }

            val gameScore = pickedNumbers.sumOf { scores[it] ?: 0.5 } / Constants.LOTTO_PICK_COUNT
            games.add(
                LottoGame(
                    gameIndex = gameIdx + 1,
                    numbers = pickedNumbers,
                    score = gameScore
                )
            )
        }
        return games
    }

    private fun sampleSixNumbers(scores: Map<Int, Double>): List<Int> {
        val candidates = (Constants.LOTTO_MIN_NUMBER..Constants.LOTTO_MAX_NUMBER).toMutableList()
        val picked = mutableSetOf<Int>()

        while (picked.size < Constants.LOTTO_PICK_COUNT && candidates.isNotEmpty()) {
            val selected = selectWeightedCandidate(candidates, scores)
            candidates.remove(selected)
            picked.add(selected)
        }
        return picked.toList()
    }

    private fun selectWeightedCandidate(candidates: List<Int>, scores: Map<Int, Double>): Int {
        val totalWeight = candidates.sumOf { scores[it] ?: 0.5 }
        if (totalWeight <= 0.0) {
            val randomIndex = random.nextInt(candidates.size)
            return candidates[randomIndex]
        }
        val r = random.nextDouble() * totalWeight
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
