package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CloseReason
import com.example.data.local.ClosedTradeEntity
import com.example.data.local.DemoAccountEntity
import com.example.data.local.OpenPositionEntity
import com.example.data.local.TradeSide
import com.example.data.local.UserPreferences
import com.example.data.local.VillageDatabase
import com.example.data.remote.CandlePoint
import com.example.data.remote.ConnectionStatus
import com.example.data.remote.DeltaExchangeClient
import com.example.data.remote.DeltaRegion
import com.example.data.remote.MarketTicker
import com.example.data.repository.TradingRepository
import com.example.util.Formatters
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

data class PerformanceSummary(
    val totalTrades: Int = 0,
    val winningTrades: Int = 0,
    val losingTrades: Int = 0,
    val winRatePct: Double = 0.0,
    val netPnlUsd: Double = 0.0,
    val profitFactor: Double = 0.0,
    val bestTradePnl: Double = 0.0,
    val worstTradePnl: Double = 0.0,
    val avgWinUsd: Double = 0.0,
    val avgLossUsd: Double = 0.0,
    val avgDurationMillis: Long = 0L,
    val totalVolumeUsd: Double = 0.0,
    val totalFeesUsd: Double = 0.0
)

data class EquityCurvePoint(
    val label: String,
    val equityUsd: Double,
    val tradePnlUsd: Double,
    val symbol: String
)

data class TradingUiState(
    val account: DemoAccountEntity = DemoAccountEntity(),
    val tickers: Map<String, MarketTicker> = emptyMap(),
    val openPositions: List<OpenPositionEntity> = emptyList(),
    val closedTrades: List<ClosedTradeEntity> = emptyList(),
    val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTING,
    val region: DeltaRegion = DeltaRegion.INDIA,
    val lastSyncMillis: Long = 0L,
    val networkError: String? = null,
    val selectedSymbol: String = "BTCUSD",
    val selectedTimeframe: String = "15m",
    val candles: List<CandlePoint> = emptyList(),
    val isCandlesLoading: Boolean = false,
    val hasSeenWelcome: Boolean = true,
    val telegramHandle: String = UserPreferences.DEFAULT_TELEGRAM_HANDLE,
    val isPlacingOrder: Boolean = false
) {
    val usedMargin: Double
        get() = openPositions.sumOf { it.marginUsed }

    val totalUnrealizedPnl: Double
        get() = openPositions.sumOf { pos ->
            val currentPrice = tickers[pos.symbol]?.lastPrice ?: pos.entryPrice
            pos.unrealizedPnl(currentPrice)
        }

    val totalEquity: Double
        get() = account.availableBalance + usedMargin + totalUnrealizedPnl

    val totalAccountPnl: Double
        get() = totalEquity - account.initialBalance

    val totalAccountRoePct: Double
        get() = if (account.initialBalance > 0.0) {
            (totalAccountPnl / account.initialBalance) * 100.0
        } else {
            0.0
        }

    val selectedTicker: MarketTicker?
        get() = tickers[selectedSymbol] ?: tickers["BTCUSD"] ?: tickers.values.firstOrNull()

    val featuredTickers: List<MarketTicker>
        get() {
            val all = tickers
            val priority = DeltaExchangeClient.FEATURED_SYMBOLS.mapNotNull { all[it] }
            return if (priority.isNotEmpty()) priority else all.values.sortedByDescending { it.turnoverUsd }.take(12)
        }

    val performanceSummary: PerformanceSummary
        get() {
            if (closedTrades.isEmpty()) return PerformanceSummary()
            val wins = closedTrades.filter { it.netPnl > 0.0 }
            val losses = closedTrades.filter { it.netPnl <= 0.0 }
            val grossWins = wins.sumOf { it.netPnl }
            val grossLosses = abs(losses.sumOf { it.netPnl })
            val pf = when {
                grossLosses > 0.0 -> grossWins / grossLosses
                grossWins > 0.0 -> 99.9
                else -> 0.0
            }
            val totalVol = closedTrades.sumOf { (it.entryPrice * it.quantity) + (it.exitPrice * it.quantity) }
            val totalFees = closedTrades.sumOf { it.feesPaid }
            val avgDur = closedTrades.map { it.durationMillis }.average().toLong()
            return PerformanceSummary(
                totalTrades = closedTrades.size,
                winningTrades = wins.size,
                losingTrades = losses.size,
                winRatePct = (wins.size.toDouble() / closedTrades.size.toDouble()) * 100.0,
                netPnlUsd = closedTrades.sumOf { it.netPnl },
                profitFactor = pf,
                bestTradePnl = closedTrades.maxOf { it.netPnl },
                worstTradePnl = closedTrades.minOf { it.netPnl },
                avgWinUsd = if (wins.isNotEmpty()) grossWins / wins.size else 0.0,
                avgLossUsd = if (losses.isNotEmpty()) losses.sumOf { it.netPnl } / losses.size else 0.0,
                avgDurationMillis = avgDur,
                totalVolumeUsd = totalVol,
                totalFeesUsd = totalFees
            )
        }

    val equityCurve: List<EquityCurvePoint>
        get() {
            val chronological = closedTrades.reversed()
            val points = mutableListOf(
                EquityCurvePoint(
                    label = "Start",
                    equityUsd = account.initialBalance,
                    tradePnlUsd = 0.0,
                    symbol = "FUND"
                )
            )
            var running = account.initialBalance
            chronological.forEachIndexed { idx, trade ->
                running += trade.netPnl
                points.add(
                    EquityCurvePoint(
                        label = "#${idx + 1}",
                        equityUsd = running.coerceAtLeast(0.0),
                        tradePnlUsd = trade.netPnl,
                        symbol = trade.symbol
                    )
                )
            }
            if (openPositions.isNotEmpty()) {
                points.add(
                    EquityCurvePoint(
                        label = "Live",
                        equityUsd = (running + totalUnrealizedPnl).coerceAtLeast(0.0),
                        tradePnlUsd = totalUnrealizedPnl,
                        symbol = "OPEN"
                    )
                )
            }
            return points
        }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val deltaClient = DeltaExchangeClient(viewModelScope)
    private val database = VillageDatabase.getInstance(application)
    private val preferences = UserPreferences(application)
    private val repository = TradingRepository(
        tradingDao = database.tradingDao(),
        deltaClient = deltaClient,
        userPreferences = preferences
    )

    private val _selectedSymbol = MutableStateFlow("BTCUSD")
    private val _selectedTimeframe = MutableStateFlow("15m")
    private val _candles = MutableStateFlow<List<CandlePoint>>(emptyList())
    private val _isCandlesLoading = MutableStateFlow(false)
    private val _isPlacingOrder = MutableStateFlow(false)
    private val _forceShowWelcome = MutableStateFlow(false)

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    val showWelcomeModal: StateFlow<Boolean> = combine(
        repository.hasSeenWelcomeFlow,
        _forceShowWelcome
    ) { seen, forced ->
        !seen || forced
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    private var candleRefreshJob: Job? = null

    val uiState: StateFlow<TradingUiState> = combine(
        repository.accountFlow,
        repository.tickers,
        repository.openPositionsFlow,
        repository.closedTradesFlow,
        repository.connectionStatus,
        repository.region,
        repository.lastSyncMillis,
        repository.lastError,
        _selectedSymbol,
        _selectedTimeframe,
        _candles,
        _isCandlesLoading,
        repository.hasSeenWelcomeFlow,
        repository.telegramHandleFlow,
        _isPlacingOrder
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        TradingUiState(
            account = (values[0] as DemoAccountEntity?) ?: DemoAccountEntity(),
            tickers = values[1] as Map<String, MarketTicker>,
            openPositions = values[2] as List<OpenPositionEntity>,
            closedTrades = values[3] as List<ClosedTradeEntity>,
            connectionStatus = values[4] as ConnectionStatus,
            region = values[5] as DeltaRegion,
            lastSyncMillis = values[6] as Long,
            networkError = values[7] as String?,
            selectedSymbol = values[8] as String,
            selectedTimeframe = values[9] as String,
            candles = values[10] as List<CandlePoint>,
            isCandlesLoading = values[11] as Boolean,
            hasSeenWelcome = values[12] as Boolean,
            telegramHandle = values[13] as String,
            isPlacingOrder = values[14] as Boolean
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = TradingUiState()
    )

    init {
        viewModelScope.launch {
            repository.initialize()
        }
        observeTriggersForTpSl()
        observeCandlesForSelectedPair()
    }

    private fun observeTriggersForTpSl() {
        viewModelScope.launch {
            combine(
                repository.openPositionsFlow,
                repository.tickers
            ) { positions, tickers ->
                positions to tickers
            }.collect { (positions, tickers) ->
                val triggered = repository.evaluateLiveTriggers(positions, tickers)
                for (trade in triggered) {
                    val signPnl = Formatters.formatUsd(trade.netPnl, showPlus = true)
                    _events.tryEmit("${trade.closeReason.label}: ${trade.symbol} ($signPnl)")
                }
            }
        }
    }

    private fun observeCandlesForSelectedPair() {
        viewModelScope.launch {
            combine(_selectedSymbol, _selectedTimeframe) { sym, tf -> sym to tf }
                .distinctUntilChanged()
                .collect { (sym, tf) ->
                    candleRefreshJob?.cancel()
                    candleRefreshJob = viewModelScope.launch {
                        loadCandlesOnce(sym, tf, showSpinner = true)
                        while (isActive) {
                            delay(15_000L)
                            loadCandlesOnce(sym, tf, showSpinner = false)
                        }
                    }
                }
        }
    }

    private suspend fun loadCandlesOnce(symbol: String, resolution: String, showSpinner: Boolean) {
        if (showSpinner) _isCandlesLoading.value = true
        repository.fetchCandles(symbol, resolution)
            .onSuccess { points ->
                if (points.isNotEmpty()) {
                    _candles.value = points
                }
            }
        if (showSpinner) _isCandlesLoading.value = false
    }

    fun selectSymbol(symbol: String) {
        _selectedSymbol.value = symbol.uppercase()
    }

    fun selectTimeframe(timeframe: String) {
        _selectedTimeframe.value = timeframe
    }

    fun switchRegion(region: DeltaRegion) {
        viewModelScope.launch {
            repository.switchRegion(region)
            loadCandlesOnce(_selectedSymbol.value, _selectedTimeframe.value, showSpinner = true)
            _events.tryEmit("Switched live feed to ${region.displayName}")
        }
    }

    fun dismissWelcome() {
        _forceShowWelcome.value = false
        viewModelScope.launch {
            repository.markWelcomeSeen()
        }
    }

    fun openWelcomeMessage() {
        _forceShowWelcome.value = true
    }

    fun placeOrder(
        side: TradeSide,
        marginUsd: Double,
        leverage: Int,
        takeProfitPrice: Double?,
        stopLossPrice: Double?
    ) {
        if (_isPlacingOrder.value) return
        viewModelScope.launch {
            _isPlacingOrder.value = true
            val res = repository.placeOrder(
                symbol = _selectedSymbol.value,
                side = side,
                marginUsd = marginUsd,
                leverage = leverage,
                takeProfitPrice = takeProfitPrice,
                stopLossPrice = stopLossPrice
            )
            _isPlacingOrder.value = false
            res.onSuccess { msg ->
                _events.tryEmit(msg)
            }.onFailure { err ->
                _events.tryEmit(err.message ?: "Unable to place demo order")
            }
        }
    }

    fun closePosition(position: OpenPositionEntity) {
        viewModelScope.launch {
            repository.closePosition(position, reason = CloseReason.MANUAL)
                .onSuccess { trade ->
                    val pnlStr = Formatters.formatUsd(trade.netPnl, showPlus = true)
                    _events.tryEmit("Closed ${trade.symbol} • Net P&L: $pnlStr")
                }
                .onFailure { err ->
                    _events.tryEmit(err.message ?: "Failed to close position")
                }
        }
    }

    fun updateTpSl(positionId: Long, tp: Double?, sl: Double?) {
        viewModelScope.launch {
            repository.updatePositionTpSl(positionId, tp, sl)
                .onSuccess { _events.tryEmit("TP / SL updated") }
                .onFailure { err -> _events.tryEmit(err.message ?: "Invalid TP/SL level") }
        }
    }

    fun resetDemoAccount() {
        viewModelScope.launch {
            repository.resetDemoAccount()
            _events.tryEmit("Demo account reset to $500.00 USD")
        }
    }

    fun updateTelegramHandle(handle: String) {
        viewModelScope.launch {
            repository.updateTelegramHandle(handle)
            _events.tryEmit("Telegram handle updated")
        }
    }

    override fun onCleared() {
        super.onCleared()
        deltaClient.close()
    }
}
