package com.example.lottoinsight.core.network.api

import com.example.lottoinsight.core.network.model.RemoteDrawDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface LottoApiService {
    @GET("common.do?method=getLottoNumber")
    suspend fun getDraw(@Query("drwNo") drawNo: Int): Response<RemoteDrawDto>

    companion object {
        const val BASE_URL = "https://www.dhlottery.co.kr/"
    }
}
