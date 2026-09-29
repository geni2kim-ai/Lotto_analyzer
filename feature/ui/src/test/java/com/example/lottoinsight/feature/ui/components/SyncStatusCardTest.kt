package com.example.lottoinsight.feature.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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
}
