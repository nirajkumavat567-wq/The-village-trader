package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class DeltaRegion(
    val displayName: String,
    val restBaseUrl: String,
    val wsUrl: String
) {
    INDIA(
        displayName = "Delta India",
        restBaseUrl = "https://api.india.delta.exchange/",
        wsUrl = "wss://socket.india.delta.exchange"
    ),
    GLOBAL(
        displayName = "Delta Global",
        restBaseUrl = "https://api.delta.exchange/",
        wsUrl = "wss://socket.delta.exchange"
    )
}

enum class ConnectionStatus(val label: String) {
    CONNECTING("CONNECTING"),
    LIVE_WS("LIVE WS"),
    LIVE_REST("LIVE REST"),
    RECONNECTING("RECONNECTING"),
    OFFLINE("OFFLINE")
}

@JsonClass(generateAdapter = true)
data class DeltaTickersResponse(
    val success: Boolean = false,
    val result: List<DeltaTickerDto>? = null
)

@JsonClass(generateAdapter = true)
data class DeltaTickerDto(
    @Json(name = "product_id") val productId: Long? = null,
    val symbol: String? = null,
    val description: String? = null,
    @Json(name = "contract_type") val contractType: String? = null,
    @Json(name = "spot_price") val spotPrice: String? = null,
    @Json(name = "mark_price") val markPrice: String? = null,
    val close: Double? = null,
    val open: Double? = null,
    val high: Double? = null,
    val low: Double? = null,
    val volume: Double? = null,
    val turnover: Double? = null,
    @Json(name = "turnover_symbol") val turnoverSymbol: String? = null,
    val oi: String? = null,
    @Json(name = "funding_rate") val fundingRate: String? = null,
    val quotes: DeltaQuotesDto? = null,
    val timestamp: Long? = null
)

@JsonClass(generateAdapter = true)
data class DeltaQuotesDto(
    @Json(name = "best_bid") val bestBid: String? = null,
    @Json(name = "best_ask") val bestAsk: String? = null
)

@JsonClass(generateAdapter = true)
data class DeltaCandlesResponse(
    val success: Boolean = false,
    val result: List<DeltaCandleDto>? = null
)

@JsonClass(generateAdapter = true)
data class DeltaCandleDto(
    val time: Long = 0L,
    val open: Double = 0.0,
    val high: Double = 0.0,
    val low: Double = 0.0,
    val close: Double = 0.0,
    val volume: Double = 0.0
)

data class MarketTicker(
    val symbol: String,
    val description: String,
    val contractType: String,
    val lastPrice: Double,
    val markPrice: Double,
    val open24h: Double,
    val high24h: Double,
    val low24h: Double,
    val volume24h: Double,
    val turnoverUsd: Double,
    val fundingRatePct: Double,
    val bestBid: Double,
    val bestAsk: Double,
    val updatedAtMillis: Long
) {
    val change24hPct: Double
        get() = if (open24h > 0.0) ((lastPrice - open24h) / open24h) * 100.0 else 0.0
}

data class CandlePoint(
    val timeSeconds: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)
