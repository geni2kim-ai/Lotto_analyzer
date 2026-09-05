package com.example.lottoinsight.core.engine

import com.example.lottoinsight.core.model.Draw
import com.example.lottoinsight.core.model.WeightConfig
import com.example.lottoinsight.core.engine.generator.NumberGenerator
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalysisEngineTest {

    @Test
    fun usesMostRecentDrawsRegardlessOfInputOrder() {
        val draws = (1..10).map { drawNo ->
            Draw(
                drawNo = drawNo,
                drawDate = "2026-01-%02d".format(drawNo),
                numbers = listOf(1, 2, 3, 4, 5, 6),
                bonus = 7,
                source = "test",
                fetchedAt = 0L
            )
        }

        val result = AnalysisEngineImpl().analyzeAndGenerate(
            draws = draws,
            config = WeightConfig(recentN = 3, gameCount = 1)
        )

        val analysis = (result as com.example.lottoinsight.core.common.AppResult.Success).data
        assertEquals(10, analysis.latestDrawNo)
        assertEquals(8, analysis.dataStartDrawNo)
        assertEquals(10, analysis.dataEndDrawNo)
    }

    @Test
    fun seededGenerationIsRepeatable() {
        val scores = (1..45).associateWith { it.toDouble() }
        val first = NumberGenerator().generateGames(scores, gameCount = 2, seed = 1234L)
        val second = NumberGenerator().generateGames(scores, gameCount = 2, seed = 1234L)

        assertEquals(first, second)
    }
}
