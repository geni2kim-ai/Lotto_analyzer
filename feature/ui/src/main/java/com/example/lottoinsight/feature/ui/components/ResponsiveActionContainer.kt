package com.example.lottoinsight.feature.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal const val RESPONSIVE_ACTIONS_ROW_TAG = "responsive-actions-row"
internal const val RESPONSIVE_ACTIONS_COLUMN_TAG = "responsive-actions-column"

/**
 * Responsive layout for a clear primary/secondary action pair.
 *
 * Use this primitive when two actions compete for horizontal space and their
 * hierarchy is explicit. Do not wrap a single action merely for consistency;
 * a native full-width button is simpler and avoids unnecessary layout policy.
 */
@Composable
internal fun ResponsiveActionContainer(
    primary: @Composable (Modifier) -> Unit,
    secondary: (@Composable (Modifier) -> Unit)? = null,
    modifier: Modifier = Modifier,
    stackBelowWidth: Dp = 360.dp,
    stackAtFontScale: Float = 1.6f
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val fontScale = LocalDensity.current.fontScale
        val stackVertically = shouldStackActions(
            availableWidth = maxWidth,
            fontScale = fontScale,
            stackBelowWidth = stackBelowWidth,
            stackAtFontScale = stackAtFontScale
        )

        if (stackVertically) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(RESPONSIVE_ACTIONS_COLUMN_TAG),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                primary(Modifier.fillMaxWidth())
                secondary?.invoke(Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(RESPONSIVE_ACTIONS_ROW_TAG),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                primary(Modifier.weight(1f))
                secondary?.invoke(Modifier)
            }
        }
    }
}


internal fun shouldStackActions(
    availableWidth: Dp,
    fontScale: Float,
    stackBelowWidth: Dp = 360.dp,
    stackAtFontScale: Float = 1.6f
): Boolean =
    availableWidth < stackBelowWidth || fontScale >= stackAtFontScale
