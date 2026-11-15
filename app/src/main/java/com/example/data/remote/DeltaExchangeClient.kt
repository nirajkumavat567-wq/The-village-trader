package com.example.data.remote

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class DeltaExchangeClient(
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "DeltaExchangeClient"
        val FEATURED_SYMBOLS = listOf(
            "BTCUSD", "ETHUSD", "SOLUSD", "XRPUSD",
            "BNBUSD", "DOGEUSD", "ADAUSD", "AVAXUSD",
            "LINKUSD", "SUIUSD", "LTCUSD", "DOTUSD"
        )
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val _region = MutableStateFlow(DeltaRegion.INDIA)
    val region: StateFlow<DeltaRegion> = _region.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.CONNECTING)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _tickers = MutableStateFlow<Map<String, MarketTicker>>(emptyMap())
    val tickers: StateFlow<Map<String, MarketTicker>> = _tickers.asStateFlow()

    private val _lastSyncMillis = MutableStateFlow(0L)
    val lastSyncMillis: StateFlow<Long> = _lastSyncMillis.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private var activeWebSocket: WebSocket? = null
    private var pollingJob: Job? = null
    private var wsReconnectJob: Job? = null
    private var activeServiceRegion: DeltaRegion = DeltaRegion.INDIA
    private var apiService: DeltaApiService = createService(DeltaRegion.INDIA)

    private fun createService(targetRegion: DeltaRegion): DeltaApiService {
        return Retrofit.Builder()
            .baseUrl(targetRegion.restBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(DeltaApiService::class.java)
    }

    fun start(initialRegion: DeltaRegion = DeltaRegion.INDIA) {
        _region.value = initialRegion
        activeServiceRegion = initialRegion
        apiService = createService(initialRegion)
        restartFeeds()
    }

    fun switchRegion(newRegion: DeltaRegion) {
        if (_region.value == newRegion && activeWebSocket != null) return
        _region.value = newRegion
        activeServiceRegion = newRegion
        apiService = createService(newRegion)
        restartFeeds()
    }

    private fun restartFeeds() {
        pollingJob?.cancel()
        wsReconnectJob?.cancel()
        activeWebSocket?.close(1000, "Restarting feed")
        activeWebSocket = null
        _connectionStatus.value = ConnectionStatus.CONNECTING

        // Start REST snapshot + periodic refresh (ensures full market list + fallback resilience)
        pollingJob = scope.launch(Dispatchers.IO) {
            fetchRestSnapshot(allowRegionFallback = true)
            connectWebSocket()
            while (isActive) {
                delay(4000L)
                fetchRestSnapshot(allowRegionFallback = false)
            }
        }
    }

    private suspend fun fetchRestSnapshot(allowRegionFallback: Boolean) {
        try {
            val response = apiService.getTickers()
            val list = response.result.orEmpty()
            if (list.isNotEmpty()) {
                val now = System.currentTimeMillis()
                val mapped = buildMap {
                    for (dto in list) {
                        val sym = dto.symbol?.trim()?.uppercase() ?: continue
                        val cType = dto.contractType.orEmpty()
                        // Prioritize perpetual futures & clean USD pairs
                        if (cType == "perpetual_futures" || sym in FEATURED_SYMBOLS) {
                            val last = dto.close ?: dto.markPrice?.toDoubleOrNull() ?: continue
                            if (last <= 0.0) continue
                            val mark = dto.markPrice?.toDoubleOrNull() ?: last
                            val open = dto.open ?: last
                            val high = dto.high ?: last
                            val low = dto.low ?: last
                            val vol = dto.volume ?: 0.0
                            val turnover = dto.turnover ?: 0.0
                            val funding = (dto.fundingRate?.toDoubleOrNull() ?: 0.0)
                            val bid = dto.quotes?.bestBid?.toDoubleOrNull() ?: last
                            val ask = dto.quotes?.bestAsk?.toDoubleOrNull() ?: last
                            put(
                                sym,
                                MarketTicker(
                                    symbol = sym,
                                    description = dto.description ?: sym,
                                    contractType = cType.ifBlank { "perpetual_futures" },
                                    lastPrice = last,
                                    markPrice = mark,
                                    open24h = open,
                                    high24h = high,
                                    low24h = low,
                                    volume24h = vol,
                                    turnoverUsd = turnover,
                                    fundingRatePct = funding,
                                    bestBid = bid,
                                    bestAsk = ask,
                                    updatedAtMillis = now
                                )
                            )
                        }
                    }
                }
                if (mapped.isNotEmpty()) {
                    _tickers.update { existing ->
                        val merged = existing.toMutableMap()
                        merged.putAll(mapped)
                        merged
                    }
                    _lastSyncMillis.value = now
                    _lastError.value = null
                    if (_connectionStatus.value != ConnectionStatus.LIVE_WS) {
                        _connectionStatus.value = ConnectionStatus.LIVE_REST
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "REST ticker fetch failed on ${activeServiceRegion.displayName}: ${e.message}")
            if (allowRegionFallback) {
                val fallback = if (activeServiceRegion == DeltaRegion.INDIA) {
                    DeltaRegion.GLOBAL
                } else {
                    DeltaRegion.INDIA
                }
                try {
                    val fallbackService = createService(fallback)
                    val fbResponse = fallbackService.getTickers()
                    if (!fbResponse.result.isNullOrEmpty()) {
                        activeServiceRegion = fallback
                        apiService = fallbackService
                        _region.value = fallback
                        fetchRestSnapshot(allowRegionFallback = false)
                        return
                    }
                } catch (fbEx: Exception) {
                    Log.w(TAG, "Fallback REST fetch also failed: ${fbEx.message}")
                }
            }
            if (_tickers.value.isEmpty()) {
                _connectionStatus.value = ConnectionStatus.OFFLINE
            } else if (_connectionStatus.value != ConnectionStatus.LIVE_WS) {
                _connectionStatus.value = ConnectionStatus.RECONNECTING
            }
            _lastError.value = "Sync issue with Delta Exchange. Retrying..."
        }
    }

    private fun connectWebSocket() {
        val currentRegion = activeServiceRegion
        val request = Request.Builder().url(currentRegion.wsUrl).build()
        activeWebSocket = okHttpClient.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "WebSocket opened: ${currentRegion.wsUrl}")
                    _connectionStatus.value = ConnectionStatus.LIVE_WS
                    _lastError.value = null

                    val symbolsToSubscribe = (_tickers.value.keys + FEATURED_SYMBOLS)
                        .distinct()
                        .take(30)
                    val subPayload = JSONObject().apply {
                        put("type", "subscribe")
                        put(
                            "payload",
                            JSONObject().apply {
                                put(
                                    "channels",
                                    JSONArray().apply {
                                        put(
                                            JSONObject().apply {
                                                put("name", "v2/ticker")
                                                put("symbols", JSONArray(symbolsToSubscribe))
                                            }
                                        )
                                        put(
                                            JSONObject().apply {
                                                put("name", "v2/tickers")
                                                put("symbols", JSONArray(symbolsToSubscribe))
                                            }
                                        )
                                    }
                                )
                            }
                        )
                    }
                    webSocket.send(subPayload.toString())
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleWebSocketMessage(text)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.w(TAG, "WebSocket failure: ${t.message}")
                    if (_tickers.value.isNotEmpty()) {
                        _connectionStatus.value = ConnectionStatus.LIVE_REST
                    } else {
                        _connectionStatus.value = ConnectionStatus.RECONNECTING
                    }
                    scheduleWsReconnect()
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WebSocket closed: $code $reason")
                    if (code != 1000) {
                        scheduleWsReconnect()
                    }
                }
            }
        )
    }

    private fun scheduleWsReconnect() {
        wsReconnectJob?.cancel()
        wsReconnectJob = scope.launch(Dispatchers.IO) {
            delay(5000L)
            if (isActive) {
                connectWebSocket()
            }
        }
    }

    private fun handleWebSocketMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type", "")
            if (type.contains("ticker") || json.has("symbol")) {
                val symbol = json.optString("symbol", "").trim().uppercase()
                if (symbol.isBlank()) return
                val close = json.optDouble("close", Double.NaN)
                val mark = json.optString("mark_price", "").toDoubleOrNull()
                    ?: json.optDouble("mark_price", Double.NaN)
                val price = when {
                    !close.isNaN() && close > 0.0 -> close
                    !mark.isNaN() && mark > 0.0 -> mark
                    else -> return
                }
                val now = System.currentTimeMillis()
                _tickers.update { map ->
                    val prev = map[symbol]
                    val open = json.optDouble("open", prev?.open24h ?: price).let {
                        if (it.isNaN() || it <= 0.0) price else it
                    }
                    val high = json.optDouble("high", prev?.high24h ?: price).let {
                        if (it.isNaN() || it <= 0.0) price else it
                    }
                    val low = json.optDouble("low", prev?.low24h ?: price).let {
                        if (it.isNaN() || it <= 0.0) price else it
                    }
                    val vol = json.optDouble("volume", prev?.volume24h ?: 0.0).let {
                        if (it.isNaN()) 0.0 else it
                    }
                    val quotes = json.optJSONObject("quotes")
                    val bid = quotes?.optString("best_bid", "")?.toDoubleOrNull()
                        ?: prev?.bestBid ?: price
                    val ask = quotes?.optString("best_ask", "")?.toDoubleOrNull()
                        ?: prev?.bestAsk ?: price
                    val funding = json.optString("funding_rate", "").toDoubleOrNull()
                        ?: prev?.fundingRatePct ?: 0.0

                    val updated = MarketTicker(
                        symbol = symbol,
                        description = prev?.description ?: json.optString("description", symbol),
                        contractType = prev?.contractType ?: "perpetual_futures",
                        lastPrice = price,
                        markPrice = if (!mark.isNaN() && mark > 0.0) mark else price,
                        open24h = open,
                        high24h = high,
                        low24h = low,
                        volume24h = vol,
                        turnoverUsd = prev?.turnoverUsd ?: 0.0,
                        fundingRatePct = funding,
                        bestBid = bid,
                        bestAsk = ask,
                        updatedAtMillis = now
                    )
                    map + (symbol to updated)
                }
                _lastSyncMillis.value = now
                _connectionStatus.value = ConnectionStatus.LIVE_WS
            }
        } catch (_: Exception) {
            // Ignore heartbeat / subscription ack frames that aren't ticker payloads
        }
    }

    suspend fun fetchCandles(
        symbol: String,
        resolution: String
    ): Result<List<CandlePoint>> {
        return try {
            val endSeconds = System.currentTimeMillis() / 1000L
            val intervalSeconds = when (resolution) {
                "1m" -> 60L
                "5m" -> 300L
                "15m" -> 900L
                "1h" -> 3600L
                "4h" -> 14400L
                "1d" -> 86400L
                else -> 900L
            }
            val startSeconds = endSeconds - (intervalSeconds * 80L)
            val resp = apiService.getCandles(
                resolution = resolution,
                symbol = symbol,
                startSeconds = startSeconds,
                endSeconds = endSeconds
            )
            val points = resp.result.orEmpty()
                .map { dto ->
                    CandlePoint(
                        timeSeconds = dto.time,
                        open = dto.open,
                        high = dto.high,
                        low = dto.low,
                        close = dto.close,
                        volume = dto.volume
                    )
                }
                .sortedBy { it.timeSeconds }
            Result.success(points)
        } catch (e: Exception) {
            Log.w(TAG, "Candle fetch failed for $symbol ($resolution): ${e.message}")
            Result.failure(e)
        }
    }

    fun close() {
        pollingJob?.cancel()
        wsReconnectJob?.cancel()
        activeWebSocket?.close(1000, "App closed")
        activeWebSocket = null
    }
}
