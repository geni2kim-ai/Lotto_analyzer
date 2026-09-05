package com.example.lottoinsight.feature.ui.analysis

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.example.lottoinsight.core.model.AnalysisResult
import com.example.lottoinsight.core.model.LottoGame
import java.util.Locale

internal object AnalysisImageRenderer {
    private const val IMAGE_WIDTH = 1080
    private const val HORIZONTAL_PADDING = 56f
    private const val HEADER_HEIGHT = 190f
    private const val CARD_HEIGHT = 158f
    private const val CARD_GAP = 18f

    fun render(result: AnalysisResult): Bitmap {
        val imageHeight = (
            HEADER_HEIGHT +
                result.games.size * CARD_HEIGHT +
                (result.games.size - 1).coerceAtLeast(0) * CARD_GAP +
                56f
            ).toInt()
        val bitmap = Bitmap.createBitmap(
            IMAGE_WIDTH,
            imageHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(247, 243, 250))

        val titlePaint = textPaint(Color.rgb(44, 36, 52), 48f, bold = true)
        val subtitlePaint = textPaint(Color.rgb(92, 83, 101), 28f)

        canvas.drawText("추천 번호 결과", HORIZONTAL_PADDING, 72f, titlePaint)
        canvas.drawText("최근 ${result.recentN}회 · ${result.games.size}개 게임", HORIZONTAL_PADDING, 122f, subtitlePaint)
        canvas.drawText("최근 회차 ${result.latestDrawNo}", HORIZONTAL_PADDING, 162f, subtitlePaint)

        result.games.forEachIndexed { index, game ->
            drawGameCard(canvas, game, HEADER_HEIGHT + index * (CARD_HEIGHT + CARD_GAP))
        }
        return bitmap
    }

    private fun drawGameCard(canvas: Canvas, game: LottoGame, top: Float) {
        val cardLeft = HORIZONTAL_PADDING
        val cardRight = IMAGE_WIDTH - HORIZONTAL_PADDING
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            setShadowLayer(8f, 0f, 3f, 0x22000000)
        }
        canvas.drawRoundRect(RectF(cardLeft, top, cardRight, top + CARD_HEIGHT), 24f, 24f, cardPaint)
        cardPaint.clearShadowLayer()

        val gamePaint = textPaint(Color.rgb(44, 36, 52), 30f, bold = true)
        canvas.drawText("GAME ${game.gameIndex}", cardLeft + 28f, top + 48f, gamePaint)

        game.sortedNumbers.forEachIndexed { index, number ->
            drawNumberBall(canvas, number, cardLeft + 205f + index * 105f, top + 45f)
        }

        val detailPaint = textPaint(Color.rgb(92, 83, 101), 24f)
        canvas.drawText(
            "점수 ${percent(game.totalScore)} · 홀수 ${game.oddCount}개 · " +
                "연속 ${game.pairCount}개 · 중복 ${game.maxOverlap}개",
            cardLeft + 28f,
            top + 118f,
            detailPaint
        )
    }

    private fun drawNumberBall(canvas: Canvas, number: Int, centerX: Float, centerY: Float) {
        val ballPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorFor(number) }
        canvas.drawCircle(centerX, centerY, 32f, ballPaint)
        val numberPaint = textPaint(
            if (number in 1..10) Color.BLACK else Color.WHITE,
            27f,
            bold = true
        ).apply { textAlign = Paint.Align.CENTER }
        canvas.drawText(number.toString(), centerX, centerY - (numberPaint.ascent() + numberPaint.descent()) / 2f, numberPaint)
    }

    private fun textPaint(color: Int, textSize: Float, bold: Boolean = false): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.textSize = textSize
            typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

    private fun colorFor(number: Int): Int = when (number) {
        in 1..10 -> Color.rgb(255, 196, 55)
        in 11..20 -> Color.rgb(72, 132, 221)
        in 21..30 -> Color.rgb(239, 101, 91)
        in 31..40 -> Color.rgb(125, 125, 125)
        else -> Color.rgb(92, 184, 92)
    }

    private fun percent(value: Double): String =
        String.format(Locale.US, "%.1f%%", value * 100.0)
}
