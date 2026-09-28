package com.example.lottoinsight.core.network.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteDrawResponseValidatorTest {

    @Test
    fun legacyValidatorRejectsResponseForDifferentRequestedDraw() {
        val error = LegacyRemoteDrawResponseValidator.validate(validDto(drwNo = 42), expectedDrawNo = 41)

        assertNotNull(error)
    }

    @Test
    fun legacyValidatorRejectsBonusNumberThatDuplicatesWinningNumber() {
        val error = LegacyRemoteDrawResponseValidator.validate(
            validDto(bnusNo = 6),
            expectedDrawNo = 42
        )

        assertNotNull(error)
    }

    @Test
    fun legacyValidatorAcceptsValidResponseForRequestedDraw() {
        val error = LegacyRemoteDrawResponseValidator.validate(validDto(), expectedDrawNo = 42)

        assertNull(error)
    }

    @Test
    fun newValidatorAcceptsAndMapsOfficialApiResponse() {
        val dto = NewRemoteDrawDto(
            ltEpsd = 42,
            tm1WnNo = 1,
            tm2WnNo = 2,
            tm3WnNo = 3,
            tm4WnNo = 4,
            tm5WnNo = 5,
            tm6WnNo = 6,
            bnsWnNo = 7,
            ltRflYmd = "20260809"
        )

        assertNull(NewRemoteDrawResponseValidator.validate(dto, expectedDrawNo = 42))
        assertEquals("2026-08-09", dto.toDomain().drawDate)
        assertEquals(listOf(1, 2, 3, 4, 5, 6), dto.toDomain().numbers)
    }

    private fun validDto(
        drwNo: Int = 42,
        bnusNo: Int = 7
    ) = RemoteDrawDto(
        drwNo = drwNo,
        drwtNo1 = 1,
        drwtNo2 = 2,
        drwtNo3 = 3,
        drwtNo4 = 4,
        drwtNo5 = 5,
        drwtNo6 = 6,
        bnusNo = bnusNo,
        returnValue = "success"
    )
}
