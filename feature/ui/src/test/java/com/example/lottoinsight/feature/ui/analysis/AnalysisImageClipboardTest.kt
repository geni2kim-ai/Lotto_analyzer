package com.example.lottoinsight.feature.ui.analysis

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.example.lottoinsight.core.model.AnalysisResult
import com.example.lottoinsight.core.model.LottoGame
import com.example.lottoinsight.core.model.WeightConfig
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class AnalysisImageClipboardTest {
    private lateinit var context: Context
    private lateinit var imageFile: File

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        imageFile = File(
            context.cacheDir,
            "lotto_analysis_result/lotto_analysis_result.png"
        )
        imageFile.delete()
    }

    @After
    fun tearDown() {
        imageFile.delete()
        imageFile.parentFile?.delete()
    }

    @Test
    fun rendererCreatesStablePngDimensions() {
        val bitmap = AnalysisImageRenderer.render(analysisResultFixture())
        try {
            assertEquals(1080, bitmap.width)
            assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
            assertTrue(bitmap.height > 190)
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun rendererIncludesTwoGamesSixBallsAndFullCardHeight() {
        val bitmap = AnalysisImageRenderer.render(twoGameAnalysisResultFixture())

        try {
            assertEquals(1080, bitmap.width)
            assertEquals(580, bitmap.height)

            val background = Color.rgb(247, 243, 250)
            assertNotEquals(background, bitmap.getPixel(120, 250))
            assertNotEquals(Color.WHITE, bitmap.getPixel(120, 360))
            assertNotEquals(background, bitmap.getPixel(120, 426))
            assertNotEquals(Color.WHITE, bitmap.getPixel(120, 536))

            val ballXs = intArrayOf(261, 366, 471, 576, 681, 786)
            listOf(235, 411).forEach { centerY ->
                ballXs.forEach { centerX ->
                    assertNotEquals(background, bitmap.getPixel(centerX - 24, centerY))
                }
            }
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun copyWritesPngAndPublishesFileProviderUriToClipboard() {
        assertTrue(copyAnalysisResultAsImage(context, analysisResultFixture()))
        assertTrue(imageFile.isFile)

        val pngHeader = ByteArray(8)
        imageFile.inputStream().use { input ->
            assertEquals(8, input.read(pngHeader))
        }
        assertArrayEquals(
            byteArrayOf(
                0x89.toByte(), 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
            ),
            pngHeader
        )

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        assertNotNull(clip)
        assertEquals(1, clip!!.itemCount)
        val uri = clip.getItemAt(0).uri
        assertNotNull(uri)
        assertEquals(context.packageName + ".fileprovider", uri.authority)
        assertNotNull(context.contentResolver.getType(uri))
        context.contentResolver.openInputStream(uri).use { input ->
            assertNotNull(input)
            assertEquals(0x89, input!!.read())
        }
    }

    private fun analysisResultFixture() = AnalysisResult(
        latestDrawNo = 100,
        recentN = 3,
        gameCount = 1,
        weightConfig = WeightConfig(recentN = 3, gameCount = 1),
        games = listOf(
            LottoGame(
                gameIndex = 1,
                numbers = listOf(1, 2, 3, 10, 20, 30),
                totalScore = 0.75,
                frequencyScore = 0.80,
                consecutiveScore = 0.60,
                parityScore = 0.70,
                oddCount = 2,
                pairCount = 2
            )
        ),
        randomSeed = 1234L
    )

    private fun twoGameAnalysisResultFixture() = AnalysisResult(
        latestDrawNo = 100,
        recentN = 3,
        gameCount = 2,
        weightConfig = WeightConfig(recentN = 3, gameCount = 2),
        games = listOf(
            LottoGame(
                gameIndex = 1,
                numbers = listOf(1, 15, 25, 35, 45, 50),
                totalScore = 0.75,
                frequencyScore = 0.80,
                consecutiveScore = 0.60,
                parityScore = 0.70,
                oddCount = 3,
                pairCount = 2
            ),
            LottoGame(
                gameIndex = 2,
                numbers = listOf(2, 16, 26, 36, 46, 51),
                totalScore = 0.65,
                frequencyScore = 0.70,
                consecutiveScore = 0.55,
                parityScore = 0.60,
                oddCount = 2,
                pairCount = 1
            )
        ),
        randomSeed = 1234L
    )
}
