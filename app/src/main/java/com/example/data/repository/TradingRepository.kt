package com.example.data.repository

import com.example.data.local.CloseReason
import com.example.data.local.ClosedTradeEntity
import com.example.data.local.DemoAccountEntity
import com.example.data.local.OpenPositionEntity
import com.example.data.local.TradeSide
import com.example.data.local.TradingDao
import com.example.data.local.UserPreferences
import com.example.data.remote.CandlePoint
import com.example.data.remote.ConnectionStatus
import com.example.data.remote.DeltaExchangeClient
import com.example.data.remote.DeltaRegion
import com.example.data.remote.MarketTicker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlin.math.abs

class TradingRepository(
    private val tradingDao: TradingDao,
    private val deltaClient: DeltaExchangeClient,
    private val userPreferences: UserPreferences
) {
    companion object {
        private const val TAKER_FEE_RATE = 0.0005 // 0.05% realistic futures taker fee
    }

    val tickers: StateFlow<Map<String, MarketTicker>> = deltaClient.tickers
    val connectionStatus: StateFlow<ConnectionStatus> = deltaClient.connectionStatus
    val region: StateFlow<DeltaRegion> = deltaClient.region
    val lastSyncMillis: StateFlow<Long> = deltaClient.lastSyncMillis
    val lastError: StateFlow<String?> = deltaClient.lastError

    val accountFlow: Flow<DemoAccountEntity?> = tradingDao.observeAccount()
    val openPositionsFlow: Flow<List<OpenPositionEntity>> = tradingDao.observeOpenPositions()
    val closedTradesFlow: Flow<List<ClosedTradeEntity>> = tradingDao.observeClosedTrades()

    val hasSeenWelcomeFlow: Flow<Boolean> = userPreferences.hasSeenWelcome
    val preferredRegionFlow: Flow<DeltaRegion> = userPreferences.selectedRegion
    val telegramHandleFlow: Flow<String> = userPreferences.telegramHandle

    suspend fun initialize() {
        tradingDao.ensureDemoAccountSeeded()
        val savedRegion = userPreferences.selectedRegion.first()
        deltaClient.start(savedRegion)
    }

    suspend fun switchRegion(newRegion: DeltaRegion) {
        userPreferences.setSelectedRegion(newRegion)
        deltaClient.switchRegion(newRegion)
    }

    suspend fun markWelcomeSeen() {
        userPreferences.setWelcomeSeen(true)
    }

    suspend fun updateTelegramHandle(handle: String) {
        userPreferences.setTelegramHandle(handle)
    }

    suspend fun fetchCandles(symbol: String, resolution: String): Result<List<CandlePoint>> {
        return deltaClient.fetchCandles(symbol, resolution)
    }

    suspend fun placeOrder(
        symbol: String,
        side: TradeSide,
        marginUsd: Double,
        leverage: Int,
        takeProfitPrice: Double?,
        stopLossPrice: Double?
    ): Result<String> {
        tradingDao.ensureDemoAccountSeeded()
        val ticker = tickers.value[symbol]
            ?: return Result.failure(IllegalStateException("Waiting for live Delta Exchange price for $symbol"))

        val entryPrice = if (side == TradeSide.LONG) {
            if (ticker.bestAsk > 0.0) ticker.bestAsk else ticker.lastPrice
        } else {
            if (ticker.bestBid > 0.0) ticker.bestBid else ticker.lastPrice
        }

        if (entryPrice <= 0.0) {
            return Result.failure(IllegalStateException("Invalid live price for $symbol"))
        }
        if (marginUsd < 5.0) {
            return Result.failure(IllegalArgumentException("Minimum demo order margin is $5.00 USD"))
        }

        if (takeProfitPrice != null) {
            if (side == TradeSide.LONG && takeProfitPrice <= entryPrice) {
                return Result.failure(IllegalArgumentException("Take Profit for LONG must be above entry price"))
            }
            if (side == TradeSide.SHORT && takeProfitPrice >= entryPrice) {
                return Result.failure(IllegalArgumentException("Take Profit for SHORT must be below entry price"))
            }
        }
        if (stopLossPrice != null) {
            if (side == TradeSide.LONG && stopLossPrice >= entryPrice) {
                return Result.failure(IllegalArgumentException("Stop Loss for LONG must be below entry price"))
            }
            if (side == TradeSide.SHORT && stopLossPrice <= entryPrice) {
                return Result.failure(IllegalArgumentException("Stop Loss for SHORT must be above entry price"))
            }
        }

        val notionalValue = marginUsd * leverage
        val quantity = notionalValue / entryPrice
        val entryFee = notionalValue * TAKER_FEE_RATE
        val totalDebit = marginUsd + entryFee

        // Calculate realistic isolated-margin liquidation price (~90% margin buffer)
        val liqOffsetPct = 0.90 / leverage.toDouble()
        val liquidationPrice = if (side == TradeSide.LONG) {
            (entryPrice * (1.0 - liqOffsetPct)).coerceAtLeast(0.0)
        } else {
            entryPrice * (1.0 + liqOffsetPct)
        }

        val position = OpenPositionEntity(
            symbol = symbol,
            side = side,
            entryPrice = entryPrice,
            quantity = quantity,
            marginUsed = marginUsd,
            leverage = leverage,
            entryFee = entryFee,
            liquidationPrice = liquidationPrice,
            takeProfitPrice = takeProfitPrice,
            stopLossPrice = stopLossPrice,
            openedAtMillis = System.currentTimeMillis()
        )

        val result = tradingDao.executeOpenPosition(position, totalDebit)
        return result.map {
            "${side.badge} $symbol opened @ $${String.format("%.2f", entryPrice)} (${leverage}x)"
        }
    }

    suspend fun closePosition(
        position: OpenPositionEntity,
        exitPriceOverride: Double? = null,
        reason: CloseReason = CloseReason.MANUAL
    ): Result<ClosedTradeEntity> {
        val ticker = tickers.value[position.symbol]
        val exitPrice = exitPriceOverride ?: ticker?.let {
            if (position.side == TradeSide.LONG) {
                if (it.bestBid > 0.0) it.bestBid else it.lastPrice
            } else {
                if (it.bestAsk > 0.0) it.bestAsk else it.lastPrice
            }
        } ?: position.entryPrice

        val grossPnl = position.unrealizedPnl(exitPrice)
        val exitNotional = exitPrice * position.quantity
        val exitFee = exitNotional * TAKER_FEE_RATE
        val netPnl = grossPnl - position.entryFee - exitFee
        val roePct = if (position.marginUsed > 0.0) (netPnl / position.marginUsed) * 100.0 else 0.0
        val now = System.currentTimeMillis()
        val durationMillis = (now - position.openedAtMillis).coerceAtLeast(0L)

        val insightTag = generateInsightTag(
            netPnl = netPnl,
            roePct = roePct,
            leverage = position.leverage,
            durationMillis = durationMillis,
            reason = reason
        )

        val closed = tradingDao.executeClosePosition(
            positionId = position.id,
            exitPrice = exitPrice,
            exitFee = exitFee,
            grossPnl = grossPnl,
            netPnl = netPnl,
            roePercent = roePct,
            closeReason = reason,
            insightTag = insightTag,
            closedAtMillis = now
        )
        return if (closed != null) {
            Result.success(closed)
        } else {
            Result.failure(IllegalStateException("Position already closed"))
        }
    }

    suspend fun updatePositionTpSl(
        positionId: Long,
        takeProfitPrice: Double?,
        stopLossPrice: Double?
    ): Result<Unit> {
        val pos = tradingDao.getOpenPositionById(positionId)
            ?: return Result.failure(IllegalStateException("Position not found"))
        if (takeProfitPrice != null) {
            if (pos.side == TradeSide.LONG && takeProfitPrice <= pos.entryPrice) {
                return Result.failure(IllegalArgumentException("Take Profit for LONG must exceed entry price"))
            }
            if (pos.side == TradeSide.SHORT && takeProfitPrice >= pos.entryPrice) {
                return Result.failure(IllegalArgumentException("Take Profit for SHORT must be below entry price"))
            }
        }
        if (stopLossPrice != null) {
            if (pos.side == TradeSide.LONG && stopLossPrice >= pos.entryPrice) {
                return Result.failure(IllegalArgumentException("Stop Loss for LONG must be below entry price"))
            }
            if (pos.side == TradeSide.SHORT && stopLossPrice <= pos.entryPrice) {
                return Result.failure(IllegalArgumentException("Stop Loss for SHORT must exceed entry price"))
            }
        }
        tradingDao.updatePosition(
            pos.copy(
                takeProfitPrice = takeProfitPrice,
                stopLossPrice = stopLossPrice
            )
        )
        return Result.success(Unit)
    }

    suspend fun evaluateLiveTriggers(
        openPositions: List<OpenPositionEntity>,
        currentTickers: Map<String, MarketTicker>
    ): List<ClosedTradeEntity> {
        if (openPositions.isEmpty() || currentTickers.isEmpty()) return emptyList()
        val triggeredTrades = mutableListOf<ClosedTradeEntity>()

        for (pos in openPositions) {
            val ticker = currentTickers[pos.symbol] ?: continue
            val price = ticker.lastPrice
            if (price <= 0.0) continue

            val reason: CloseReason? = when (pos.side) {
                TradeSide.LONG -> when {
                    price <= pos.liquidationPrice -> CloseReason.LIQUIDATED
                    pos.stopLossPrice != null && price <= pos.stopLossPrice -> CloseReason.STOP_LOSS
                    pos.takeProfitPrice != null && price >= pos.takeProfitPrice -> CloseReason.TAKE_PROFIT
                    else -> null
                }
                TradeSide.SHORT -> when {
                    price >= pos.liquidationPrice -> CloseReason.LIQUIDATED
                    pos.stopLossPrice != null && price >= pos.stopLossPrice -> CloseReason.STOP_LOSS
                    pos.takeProfitPrice != null && price <= pos.takeProfitPrice -> CloseReason.TAKE_PROFIT
                    else -> null
                }
            }

            if (reason != null) {
                val triggerPrice = when (reason) {
                    CloseReason.TAKE_PROFIT -> pos.takeProfitPrice ?: price
                    CloseReason.STOP_LOSS -> pos.stopLossPrice ?: price
                    CloseReason.LIQUIDATED -> pos.liquidationPrice
                    CloseReason.MANUAL -> price
                }
                closePosition(pos, exitPriceOverride = triggerPrice, reason = reason)
                    .onSuccess { triggeredTrades.add(it) }
            }
        }
        return triggeredTrades
    }

    suspend fun resetDemoAccount() {
        tradingDao.resetDemoAccount()
    }

    private fun generateInsightTag(
        netPnl: Double,
        roePct: Double,
        leverage: Int,
        durationMillis: Long,
        reason: CloseReason
    ): String {
        return when (reason) {
            CloseReason.TAKE_PROFIT -> "Target Hit • Disciplined Exit"
            CloseReason.STOP_LOSS -> "Stop Loss Protected Capital"
            CloseReason.LIQUIDATED -> "Liquidated • Lower Leverage Advised"
            CloseReason.MANUAL -> when {
                netPnl > 0.0 && roePct >= 25.0 -> "High-Conviction Winner"
                netPnl > 0.0 && durationMillis < 5 * 60_000L -> "Quick Momentum Scalp"
                netPnl > 0.0 -> "Clean Profit Execution"
                abs(roePct) <= 5.0 -> "Tight Risk Cut"
                leverage >= 10 && netPnl < 0.0 -> "High-Leverage Drawdown"
                else -> "Review Entry Timing"
            }
        }
    }
}
