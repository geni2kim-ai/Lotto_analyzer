package com.example.lottoinsight.feature.ui.analysis

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.unit.Density
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AnalysisStatusChipsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(sdk = [34], qualifiers = "w359dp-h800dp")
    fun width359dpUsesFlowWrapLayout() {
        assertStatusChipLayoutAtWidth(
            expectedTag = ANALYSIS_STATUS_CHIPS_WRAP_TAG,
            absentTag = ANALYSIS_STATUS_CHIPS_GRID_TAG
        )
    }

    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h800dp")
    fun width360dpKeepsStatusChipsInTwoColumnGrid() {
        assertStatusChipLayoutAtWidth(
            expectedTag = ANALYSIS_STATUS_CHIPS_GRID_TAG,
            absentTag = ANALYSIS_STATUS_CHIPS_WRAP_TAG
        )
    }

    @Test
    @Config(sdk = [34], qualifiers = "w359dp-h800dp")
    fun width359dpAt2_0FontScaleKeepsAllStatusChipsInWrapLayout() {
        assertStatusChipLayoutAtWidth(
            expectedTag = ANALYSIS_STATUS_CHIPS_WRAP_TAG,
            absentTag = ANALYSIS_STATUS_CHIPS_GRID_TAG,
            fontScale = 2.0f,
            assertNoSemanticOverlap = true
        )
    }

    @Test
    @Config(sdk = [34], qualifiers = "w359dp-h800dp")
    fun longAlgorithmVersionRemainsAvailableInSemantics() {
        val longVersion = "analysis-engine-production-2026-09-29-build-abcdef1234567890"
        setStatusChips(
            fontScale = 2.0f,
            algorithmVersion = longVersion
        )

        composeRule.onNodeWithTag(ANALYSIS_STATUS_CHIPS_WRAP_TAG)
            .assertExists()
        composeRule.onNodeWithText("알고리즘 $longVersion")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    @Config(sdk = [34], qualifiers = "w359dp-h800dp")
    fun localizationStressAt359dpAnd2_0FontScaleKeepsFlowItemsVisibleAndSeparate() {
        assertStatusChipLayoutAtWidth(
            expectedTag = ANALYSIS_STATUS_CHIPS_WRAP_TAG,
            absentTag = ANALYSIS_STATUS_CHIPS_GRID_TAG,
            fontScale = 2.0f,
            assertNoSemanticOverlap = true,
            latestDrawNo = Int.MAX_VALUE,
            recentN = Int.MAX_VALUE,
            usePrizeIndex = false,
            algorithmVersion = "analysis-engine-production-localized-2026-09-29-build-abcdef1234567890"
        )
    }

    @Test
    @Config(sdk = [34], qualifiers = "w359dp-h800dp")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun u1Captures359dpAt2_0FontScaleFlowRowScreenshot() {
        setStatusChips(
            fontScale = 2.0f,
            latestDrawNo = Int.MAX_VALUE,
            recentN = Int.MAX_VALUE,
            usePrizeIndex = false,
            algorithmVersion = "analysis-engine-production-localized-2026-09-29-build-abcdef1234567890"
        )

        val output = File(
            System.getProperty("user.dir"),
            "feature/ui/build/reports/analysis-status-chips/u1-359dp-2x.png"
        )
        output.parentFile?.mkdirs()

        FileOutputStream(output).use { stream ->
            val compressed = composeRule
                .onNodeWithTag(ANALYSIS_STATUS_CHIPS_WRAP_TAG)
                .captureToImage()
                .asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, stream)
            assertTrue(compressed)
        }
        assertTrue(output.isFile)
        assertTrue(output.length() > 0L)
    }

    @Test
    @Config(sdk = [34], qualifiers = "w359dp-h800dp")
    fun u2FlowRowSemanticOrderMatchesWrappedVisualOrder() {
        val labels = listOf(
            "최신 2147483647회",
            "분석 2147483647회",
            "EV 미사용",
            "알고리즘 analysis-engine-production-localized-2026-09-29-build-abcdef1234567890"
        )
        setStatusChips(
            fontScale = 2.0f,
            latestDrawNo = Int.MAX_VALUE,
            recentN = Int.MAX_VALUE,
            usePrizeIndex = false,
            algorithmVersion = "analysis-engine-production-localized-2026-09-29-build-abcdef1234567890"
        )

        val semanticOrder = collectTextInSemanticsOrder(
            composeRule.onNodeWithTag(
                ANALYSIS_STATUS_CHIPS_WRAP_TAG,
                useUnmergedTree = true
            )
        ).filter { it in labels }

        val visualOrder = labels.sortedWith(
            compareBy<String> { label ->
                composeRule.onNodeWithText(label).fetchSemanticsNode().boundsInRoot.top
            }.thenBy { label ->
                composeRule.onNodeWithText(label).fetchSemanticsNode().boundsInRoot.left
            }
        )

        assertEquals(labels, semanticOrder)
        assertEquals(labels, visualOrder)
    }

    private fun assertStatusChipLayoutAtWidth(
        expectedTag: String,
        absentTag: String,
        fontScale: Float = 1f,
        assertNoSemanticOverlap: Boolean = false,
        latestDrawNo: Int = 1234,
        recentN: Int = 100,
        usePrizeIndex: Boolean = true,
        algorithmVersion: String = "test-v1"
    ) {
        val labels = listOf(
            "최신 ${latestDrawNo}회",
            "분석 ${recentN}회",
            if (usePrizeIndex) "EV 사용" else "EV 미사용",
            "알고리즘 $algorithmVersion"
        )
        setStatusChips(
            fontScale = fontScale,
            latestDrawNo = latestDrawNo,
            recentN = recentN,
            usePrizeIndex = usePrizeIndex,
            algorithmVersion = algorithmVersion
        )

        composeRule.onNodeWithTag(expectedTag)
            .assertExists()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(absentTag)
            .assertDoesNotExist()

        // onNodeWithText uses exact matching by default (substring = false), so
        // trailing/leading whitespace in production labels would fail this assertion.
        labels.forEach { label ->
            composeRule.onNodeWithText(label)
                .assertExists()
                .assertIsDisplayed()
        }

        if (assertNoSemanticOverlap) {
            val wrapBounds = composeRule.onNodeWithTag(expectedTag)
                .fetchSemanticsNode()
                .boundsInRoot
            val labelBounds = labels.map { label ->
                composeRule.onNodeWithText(label)
                    .fetchSemanticsNode()
                    .boundsInRoot
            }

            labelBounds.forEach { bounds ->
                assertTrue(bounds.width > 0f)
                assertTrue(bounds.height > 0f)
                assertTrue(bounds.left >= wrapBounds.left)
                assertTrue(bounds.top >= wrapBounds.top)
                assertTrue(bounds.right <= wrapBounds.right)
                assertTrue(bounds.bottom <= wrapBounds.bottom)
            }
            labelBounds.forEachIndexed { index, bounds ->
                labelBounds.drop(index + 1).forEach { other ->
                    assertFalse(bounds.overlaps(other))
                }
            }
        }
    }

    private fun collectTextInSemanticsOrder(
        interaction: SemanticsNodeInteraction
    ): List<String> {
        fun collect(node: androidx.compose.ui.semantics.SemanticsNode): List<String> {
            val ownText = runCatching {
                node.config[SemanticsProperties.Text]
            }.getOrDefault(emptyList())
                .map { it.text }
            return ownText + node.children.flatMap(::collect)
        }
        return collect(interaction.fetchSemanticsNode())
    }

    private fun setStatusChips(
        fontScale: Float,
        latestDrawNo: Int = 1234,
        recentN: Int = 100,
        usePrizeIndex: Boolean = true,
        algorithmVersion: String = "test-v1"
    ) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = density.density,
                    fontScale = fontScale
                )
            ) {
                MaterialTheme {
                    AnalysisStatusChips(
                        latestDrawNo = latestDrawNo,
                        recentN = recentN,
                        usePrizeIndex = usePrizeIndex,
                        algorithmVersion = algorithmVersion
                    )
                }
            }
        }
    }
}
