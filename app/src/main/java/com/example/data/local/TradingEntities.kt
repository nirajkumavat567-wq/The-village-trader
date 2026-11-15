package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TradeSide(val label: String, val badge: String) {
    LONG("BUY / LONG", "LONG"),
    SHORT("SELL / SHORT", "SHORT")
}

enum class CloseReason(val label: String) {
    MANUAL("Manual Close"),
    TAKE_PROFIT("Take Profit Hit"),
    STOP_LOSS("Stop Loss Hit"),
    LIQUIDATED("Liquidated")
}

@Entity(tableName = "demo_account")
data class DemoAccountEntity(
    @PrimaryKey val id: Int = 1,
    val initialBalance: Double = 500.0,
    val availableBalance: Double = 500.0,
    val totalTrades: Int = 0,
    val winningTrades: Int = 0,
    val totalRealizedPnl: Double = 0.0,
    val totalFeesPaid: Double = 0.0,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "open_positions")
data class OpenPositionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val symbol: String,
    val side: TradeSide,
    val entryPrice: Double,
    val quantity: Double,
    val marginUsed: Double,
    val leverage: Int,
    val entryFee: Double,
    val liquidationPrice: Double,
    val takeProfitPrice: Double? = null,
    val stopLossPrice: Double? = null,
    val openedAtMillis: Long = System.currentTimeMillis()
) {
    fun unrealizedPnl(currentPrice: Double): Double {
        if (currentPrice <= 0.0 || entryPrice <= 0.0) return 0.0
        val diff = if (side == TradeSide.LONG) {
            currentPrice - entryPrice
        } else {
            entryPrice - currentPrice
        }
        return diff * quantity
    }

    fun roePercent(currentPrice: Double): Double {
        if (marginUsed <= 0.0) return 0.0
        return (unrealizedPnl(currentPrice) / marginUsed) * 100.0
    }
}

@Entity(tableName = "closed_trades")
data class ClosedTradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val symbol: String,
    val side: TradeSide,
    val entryPrice: Double,
    val exitPrice: Double,
    val quantity: Double,
    val marginUsed: Double,
    val leverage: Int,
    val grossPnl: Double,
    val feesPaid: Double,
    val netPnl: Double,
    val roePercent: Double,
    val closeReason: CloseReason,
    val insightTag: String,
    val openedAtMillis: Long,
    val closedAtMillis: Long
) {
    val durationMillis: Long
        get() = (closedAtMillis - openedAtMillis).coerceAtLeast(0L)
}
