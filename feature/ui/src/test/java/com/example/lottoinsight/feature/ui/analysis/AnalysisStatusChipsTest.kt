package com.example.lottoinsight.feature.ui.analysis

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
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
    fun width359dpWrapsStatusChipsToSingleColumn() {
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
            fontScale = 2.0f
        )
    }

    private fun assertStatusChipLayoutAtWidth(
        expectedTag: String,
        absentTag: String,
        fontScale: Float = 1f
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
                        algorithmVersion = "test-v1"
                    )
                }
            }
        }

        composeRule.onNodeWithTag(expectedTag)
            .assertExists()
        composeRule.onNodeWithTag(absentTag)
            .assertDoesNotExist()

        // onNodeWithText uses exact matching by default (substring = false), so
        // trailing/leading whitespace in production labels would fail this assertion.
        listOf(
            "최신 1234회",
            "분석 100회",
            "EV 사용",
            "알고리즘 test-v1"
        ).forEach { label ->
            composeRule.onNodeWithText(label)
                .assertExists()
        }
    }
}
