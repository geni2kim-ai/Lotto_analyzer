package com.example.lottoinsight.feature.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ResponsiveActionContainerTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun largeFontScaleStacksActionsVertically() {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 1f, fontScale = 2f)
            ) {
                ResponsiveActionContainer(
                    primary = { modifier ->
                        Button(onClick = {}, modifier = modifier) {
                            Text("primary")
                        }
                    },
                    secondary = { modifier ->
                        Button(onClick = {}, modifier = modifier) {
                            Text("secondary")
                        }
                    },
                    modifier = Modifier.width(420.dp)
                )
            }
        }

        composeRule.onNodeWithTag(RESPONSIVE_ACTIONS_COLUMN_TAG)
            .assertExists()
        composeRule.onNodeWithTag(RESPONSIVE_ACTIONS_ROW_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun regularFontAndWideWidthChooseHorizontalDecision() {
        assertFalse(
            shouldStackActions(
                availableWidth = 420.dp,
                fontScale = 1f
            )
        )
    }

    @Test
    @Config(sdk = [34], qualifiers = "w359dp-h800dp")
    fun width359dpStacksActionsVertically() {
        assertResponsiveLayoutTagAtWidth(
            expectedTag = RESPONSIVE_ACTIONS_COLUMN_TAG,
            absentTag = RESPONSIVE_ACTIONS_ROW_TAG
        )
        assertTrue(
            shouldStackActions(
                availableWidth = 359.dp,
                fontScale = 1f
            )
        )
    }

    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h800dp")
    fun width360dpKeepsActionsHorizontal() {
        assertResponsiveLayoutTagAtWidth(
            expectedTag = RESPONSIVE_ACTIONS_ROW_TAG,
            absentTag = RESPONSIVE_ACTIONS_COLUMN_TAG
        )
        assertFalse(
            shouldStackActions(
                availableWidth = 360.dp,
                fontScale = 1f
            )
        )
    }

    @Test
    @Config(sdk = [34], qualifiers = "w361dp-h800dp")
    fun width361dpKeepsActionsHorizontal() {
        assertResponsiveLayoutTagAtWidth(
            expectedTag = RESPONSIVE_ACTIONS_ROW_TAG,
            absentTag = RESPONSIVE_ACTIONS_COLUMN_TAG
        )
        assertFalse(
            shouldStackActions(
                availableWidth = 361.dp,
                fontScale = 1f
            )
        )
    }

    // Boundary precision contract: Robolectric wNNNdp qualifiers are assumed to map
    // exactly to the same integer dp value observed by BoxWithConstraints.maxWidth.
    // Tag assertions exercise that integrated layout path. The direct shouldStackActions
    // assertions above anchor the threshold values, but because they call the production
    // decision function itself they do not independently prove the qualifier mapping.
    private fun assertResponsiveLayoutTagAtWidth(
        expectedTag: String,
        absentTag: String
    ) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = density.density,
                    fontScale = 1f
                )
            ) {
                ResponsiveActionContainer(
                    primary = { modifier ->
                        Button(onClick = {}, modifier = modifier) {
                            Text("primary")
                        }
                    },
                    secondary = { modifier ->
                        Button(onClick = {}, modifier = modifier) {
                            Text("secondary")
                        }
                    }
                )
            }
        }

        composeRule.onNodeWithTag(expectedTag)
            .assertExists()
        composeRule.onNodeWithTag(absentTag)
            .assertDoesNotExist()
    }

}
