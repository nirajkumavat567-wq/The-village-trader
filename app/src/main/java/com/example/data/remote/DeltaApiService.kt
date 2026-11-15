package com.example.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface DeltaApiService {

    @GET("v2/tickers")
    suspend fun getTickers(): DeltaTickersResponse

    @GET("v2/history/candles")
    suspend fun getCandles(
        @Query("resolution") resolution: String,
        @Query("symbol") symbol: String,
        @Query("start") startSeconds: Long,
        @Query("end") endSeconds: Long
    ): DeltaCandlesResponse
}
