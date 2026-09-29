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
}
