package com.example.lottoinsight.core.network.datasource

import com.example.lottoinsight.core.common.AppResult
import com.example.lottoinsight.core.network.api.LottoApiService
import com.example.lottoinsight.core.network.model.NewRemoteDrawDto
import com.example.lottoinsight.core.network.model.RemoteDrawDto
import com.example.lottoinsight.core.network.model.RemoteDrawListData
import com.example.lottoinsight.core.network.model.RemoteDrawListResponse
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class LottoRemoteDataSourceImplTest {

    @Test
    fun fetchDrawRangeReturnsSuccessfulDrawsAndFailedDrawNumbers() = runBlocking {
        val service = FakeLottoApiService(failingDrawNos = setOf(2))
        val dataSource = LottoRemoteDataSourceImpl(service)
        val progress = mutableListOf<Pair<Int, Int>>()

        val result = dataSource.fetchDrawRange(1, 3) { completed, total ->
            progress += completed to total
        }

        assertTrue(result is AppResult.Success)
        val report = (result as AppResult.Success).data
        assertEquals(listOf(1, 3), report.successful.map { it.drawNo })
        assertEquals(listOf(2), report.failedDrawNos)
        assertEquals(3, report.attemptedCount)
        assertTrue(report.isPartialSuccess)
        assertEquals(3, progress.size)
        assertEquals(setOf(1, 2, 3), progress.map { it.first }.toSet())
        assertTrue(progress.all { it.second == 3 })
    }

    @Test
    fun officialApiLatestDrawAllowsGapGreaterThanTwoHundred() = runBlocking {
        val service = FakeLottoApiService(
            allDraws = listOf(newDto(450))
        )
        val dataSource = LottoRemoteDataSourceImpl(service)

        val result = dataSource.fetchLatestDrawNo(existingMaxDrawNo = 100)

        assertTrue(result is AppResult.Success)
        assertEquals(450, (result as AppResult.Success).data)
        assertEquals(0, service.resultPageCalls)
    }

    private class FakeLottoApiService(
        private val failingDrawNos: Set<Int> = emptySet(),
        private val allDraws: List<NewRemoteDrawDto> = emptyList()
    ) : LottoApiService {
        var resultPageCalls: Int = 0
            private set

        override suspend fun getDraw(drawNo: Int): Response<RemoteDrawDto> {
            if (drawNo in failingDrawNos) {
                throw IOException("simulated legacy failure for $drawNo")
            }
            return Response.success(
                RemoteDrawDto(
                    drwNo = drawNo,
                    drwtNo1 = 1,
                    drwtNo2 = 2,
                    drwtNo3 = 3,
                    drwtNo4 = 4,
                    drwtNo5 = 5,
                    drwtNo6 = 6,
                    bnusNo = 7,
                    returnValue = "success"
                )
            )
        }

        override suspend fun getDraws(
            drawQuery: String,
            cacheBuster: Long
        ): Response<RemoteDrawListResponse> {
            if (drawQuery == "all") {
                return Response.success(
                    RemoteDrawListResponse(
                        data = RemoteDrawListData(list = allDraws)
                    )
                )
            }

            val drawNo = drawQuery.toInt()
            if (drawNo in failingDrawNos) {
                throw IOException("simulated new-api failure for $drawNo")
            }
            return Response.success(
                RemoteDrawListResponse(
                    data = RemoteDrawListData(list = listOf(newDto(drawNo)))
                )
            )
        }

        override suspend fun getResultPage(): Response<String> {
            resultPageCalls++
            return Response.success("<html></html>")
        }
    }

    companion object {
        private fun newDto(drawNo: Int): NewRemoteDrawDto {
            val start = ((drawNo - 1) % 38) + 1
            val numbers = (start until start + 6).toList()
            val bonus = (1..45).first { it !in numbers }
            return NewRemoteDrawDto(
                ltEpsd = drawNo,
                tm1WnNo = numbers[0],
                tm2WnNo = numbers[1],
                tm3WnNo = numbers[2],
                tm4WnNo = numbers[3],
                tm5WnNo = numbers[4],
                tm6WnNo = numbers[5],
                bnsWnNo = bonus,
                ltRflYmd = "20260928",
                rnk1WnAmt = 1_000_000L
            )
        }
    }
}
