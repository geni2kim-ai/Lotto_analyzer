package com.example.lottoinsight.core.network.api

import com.example.lottoinsight.core.network.model.RemoteDrawDto
import com.example.lottoinsight.core.network.model.RemoteDrawListResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface LottoApiService {
    @Headers(
        "Accept: application/json,text/plain,*/*",
        "Referer: https://www.dhlottery.co.kr/lt645/result",
        "User-Agent: Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36"
    )
    @GET("common.do?method=getLottoNumber")
    suspend fun getDraw(@Query("drwNo") drawNo: Int): Response<RemoteDrawDto>

    @Headers(
        "Accept: application/json,text/plain,*/*",
        "Referer: https://www.dhlottery.co.kr/lt645/result",
        "User-Agent: Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36"
    )
    @GET("lt645/selectPstLt645Info.do")
    suspend fun getDraws(
        @Query("srchLtEpsd") drawQuery: String,
        @Query("_") cacheBuster: Long
    ): Response<RemoteDrawListResponse>

    @Headers(
        "Accept: text/html,application/xhtml+xml,*/*",
        "Referer: https://www.dhlottery.co.kr/lt645/result",
        "User-Agent: Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36"
    )
    @GET("lt645/result")
    suspend fun getResultPage(): Response<String>

    companion object {
        const val BASE_URL = "https://www.dhlottery.co.kr/"
    }
}
