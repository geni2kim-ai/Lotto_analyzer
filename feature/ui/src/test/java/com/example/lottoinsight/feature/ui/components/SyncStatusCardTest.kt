package com.example.lottoinsight.feature.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
// Keep Robolectric qualifiers normalized: no leading/trailing whitespace.
@Config(
    sdk = [34],
    qualifiers = "w600dp-h800dp"
)
class SyncStatusCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun summaryIsMergedLiveRegionWhileActionsRemainIndependentClickTargets() {
        val message = "partial-sync-test-message"
        val failedDrawNos = (1..7).toList()
        val presentation = resolveSyncStatusPresentation(
            isSyncing = false,
            message = message,
            isFailed = true,
            failedDrawCount = failedDrawNos.size
        )

        composeRule.setContent {
            MaterialTheme {
                SyncStatusCard(
                    isSyncing = false,
                    completed = 4,
                    total = 7,
                    message = message,
                    isFailed = true,
                    failedDrawNos = failedDrawNos,
                    onRetry = {},
                    onShowFailedDetails = {}
                )
            }
        }

        val liveRegion = SemanticsMatcher.expectValue(
            SemanticsProperties.LiveRegion,
            LiveRegionMode.Polite
        )
        val expectedState = SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription,
            presentation.announcement
        )

        composeRule.onNode(liveRegion and expectedState)
            .assertExists()

        composeRule.onNode(liveRegion and hasClickAction())
            .assertDoesNotExist()

        composeRule.onNodeWithTag(SYNC_RETRY_ACTION_TAG)
            .assertExists()
            .assertHasClickAction()

        composeRule.onNodeWithTag(SYNC_FAILURE_DETAILS_ACTION_TAG)
            .assertExists()
            .assertHasClickAction()
    }

    @Test
    fun fontScale1_0KeepsActionsInRow() {
        assertSyncActionLayoutAtFontScale(
            fontScale = 1.0f,
            expectedTag = RESPONSIVE_ACTIONS_ROW_TAG,
            absentTag = RESPONSIVE_ACTIONS_COLUMN_TAG
        )
    }

    @Test
    fun fontScale1_3KeepsActionsInRow() {
        assertSyncActionLayoutAtFontScale(
            fontScale = 1.3f,
            expectedTag = RESPONSIVE_ACTIONS_ROW_TAG,
            absentTag = RESPONSIVE_ACTIONS_COLUMN_TAG
        )
    }

    @Test
    fun fontScale1_6StacksActionsInColumn() {
        assertSyncActionLayoutAtFontScale(
            fontScale = 1.6f,
            expectedTag = RESPONSIVE_ACTIONS_COLUMN_TAG,
            absentTag = RESPONSIVE_ACTIONS_ROW_TAG
        )
    }

    @Test
    fun fontScale2_0StacksActionsInColumn() {
        assertSyncActionLayoutAtFontScale(
            fontScale = 2.0f,
            expectedTag = RESPONSIVE_ACTIONS_COLUMN_TAG,
            absentTag = RESPONSIVE_ACTIONS_ROW_TAG
        )
    }

    private fun assertSyncActionLayoutAtFontScale(
        fontScale: Float,
        expectedTag: String,
        absentTag: String
    ) {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 1f, fontScale = fontScale)
            ) {
                MaterialTheme {
                    SyncStatusCard(
                        isSyncing = false,
                        completed = 4,
                        total = 7,
                        message = "matrix-test",
                        isFailed = true,
                        failedDrawNos = (1..7).toList(),
                        onRetry = {},
                        onShowFailedDetails = {}
                    )
                }
            }
        }

        composeRule.onNodeWithTag(expectedTag)
            .assertExists()
        composeRule.onNodeWithTag(absentTag)
            .assertDoesNotExist()
    }

}
