package com.example.lottoinsight.feature.ui.analysis

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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

    private fun assertStatusChipLayoutAtWidth(
        expectedTag: String,
        absentTag: String,
        fontScale: Float = 1f,
        assertNoSemanticOverlap: Boolean = false
    ) {
        val labels = listOf(
            "최신 1234회",
            "분석 100회",
            "EV 사용",
            "알고리즘 test-v1"
        )
        setStatusChips(fontScale = fontScale)

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

    private fun setStatusChips(
        fontScale: Float,
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
                        latestDrawNo = 1234,
                        recentN = 100,
                        usePrizeIndex = true,
                        algorithmVersion = algorithmVersion
                    )
                }
            }
        }
    }
}
