package com.example.lottoinsight.core.model

import org.junit.Test

class DrawTest {

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBonusNumberThatDuplicatesWinningNumber() {
        Draw(
            drawNo = 42,
            drawDate = "2026-01-01",
            numbers = listOf(1, 2, 3, 4, 5, 6),
            bonus = 6,
            source = "test",
            fetchedAt = 0L
        )
    }
}
