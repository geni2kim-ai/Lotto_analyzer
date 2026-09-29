package com.example.lottoinsight.feature.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SyncStatusCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun summaryIsMergedLiveRegionWhileActionsRemainIndependentClickTargets() {
        composeRule.setContent {
            MaterialTheme {
                SyncStatusCard(
                    isSyncing = false,
                    completed = 4,
                    total = 7,
                    message = "부분 완료: 총 7건 중 4건 성공, 3건 실패",
                    isFailed = true,
                    failedDrawNos = (1..7).toList(),
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
            "동기화 확인 필요. 부분 완료: 총 7건 중 4건 성공, 3건 실패. 실패 회차 7건"
        )

        composeRule.onNode(liveRegion and expectedState)
            .assertExists()
            .assertTextContains("동기화 확인 필요")

        composeRule.onNode(liveRegion and hasClickAction())
            .assertDoesNotExist()

        composeRule.onNodeWithText("실패한 7개 회차 다시 시도")
            .assertExists()
            .assertHasClickAction()

        composeRule.onNodeWithText("실패 목록 보기")
            .assertExists()
            .assertHasClickAction()
    }
}
