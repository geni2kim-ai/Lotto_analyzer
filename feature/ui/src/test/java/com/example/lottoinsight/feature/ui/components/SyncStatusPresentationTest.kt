package com.example.lottoinsight.feature.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncStatusPresentationTest {

    @Test
    fun failurePresentationIncludesFailedCountAndUsesFailureTone() {
        val presentation = resolveSyncStatusPresentation(
            isSyncing = false,
            message = "부분 완료",
            isFailed = true,
            failedDrawCount = 3
        )

        assertEquals("동기화 확인 필요", presentation.title)
        assertEquals(
            "동기화 확인 필요. 부분 완료. 실패 회차 3건",
            presentation.announcement
        )
        assertEquals(SyncStatusTone.FAILURE, presentation.tone)
    }

    @Test
    fun activePresentationUsesSyncingTitleWithoutFailureCount() {
        val presentation = resolveSyncStatusPresentation(
            isSyncing = true,
            message = "동기화 중",
            isFailed = false,
            failedDrawCount = 0
        )

        assertEquals("데이터 동기화 중", presentation.title)
        assertEquals("데이터 동기화 중. 동기화 중", presentation.announcement)
        assertEquals(SyncStatusTone.ACTIVE, presentation.tone)
    }
}
