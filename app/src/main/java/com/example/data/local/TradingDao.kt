package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TradingDao {

    @Query("SELECT * FROM demo_account WHERE id = 1")
    fun observeAccount(): Flow<DemoAccountEntity?>

    @Query("SELECT * FROM demo_account WHERE id = 1")
    suspend fun getAccountOnce(): DemoAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccount(account: DemoAccountEntity)

    @Query("SELECT * FROM open_positions ORDER BY openedAtMillis DESC")
    fun observeOpenPositions(): Flow<List<OpenPositionEntity>>

    @Query("SELECT * FROM open_positions WHERE id = :positionId")
    suspend fun getOpenPositionById(positionId: Long): OpenPositionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosition(position: OpenPositionEntity): Long

    @Update
    suspend fun updatePosition(position: OpenPositionEntity)

    @Query("DELETE FROM open_positions WHERE id = :positionId")
    suspend fun deletePositionById(positionId: Long)

    @Query("DELETE FROM open_positions")
    suspend fun clearAllPositions()

    @Query("SELECT * FROM closed_trades ORDER BY closedAtMillis DESC")
    fun observeClosedTrades(): Flow<List<ClosedTradeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClosedTrade(trade: ClosedTradeEntity): Long

    @Query("DELETE FROM closed_trades")
    suspend fun clearAllClosedTrades()

    @Transaction
    suspend fun ensureDemoAccountSeeded() {
        if (getAccountOnce() == null) {
            upsertAccount(
                DemoAccountEntity(
                    id = 1,
                    initialBalance = 500.0,
                    availableBalance = 500.0,
                    totalTrades = 0,
                    winningTrades = 0,
                    totalRealizedPnl = 0.0,
                    totalFeesPaid = 0.0,
                    createdAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    @Transaction
    suspend fun executeOpenPosition(
        position: OpenPositionEntity,
        totalDebit: Double
    ): Result<Long> {
        val account = getAccountOnce() ?: DemoAccountEntity()
        if (account.availableBalance + 1e-6 < totalDebit) {
            return Result.failure(IllegalStateException("Insufficient demo margin balance"))
        }
        val newAvailable = (account.availableBalance - totalDebit).coerceAtLeast(0.0)
        upsertAccount(
            account.copy(
                availableBalance = newAvailable,
                totalFeesPaid = account.totalFeesPaid + position.entryFee
            )
        )
        val newId = insertPosition(position)
        return Result.success(newId)
    }

    @Transaction
    suspend fun executeClosePosition(
        positionId: Long,
        exitPrice: Double,
        exitFee: Double,
        grossPnl: Double,
        netPnl: Double,
        roePercent: Double,
        closeReason: CloseReason,
        insightTag: String,
        closedAtMillis: Long
    ): ClosedTradeEntity? {
        val position = getOpenPositionById(positionId) ?: return null
        val account = getAccountOnce() ?: DemoAccountEntity()

        val creditBack = (position.marginUsed + grossPnl - exitFee).coerceAtLeast(0.0)
        val isWin = netPnl > 0.0

        upsertAccount(
            account.copy(
                availableBalance = account.availableBalance + creditBack,
                totalTrades = account.totalTrades + 1,
                winningTrades = account.winningTrades + if (isWin) 1 else 0,
                totalRealizedPnl = account.totalRealizedPnl + netPnl,
                totalFeesPaid = account.totalFeesPaid + exitFee
            )
        )

        val closedTrade = ClosedTradeEntity(
            symbol = position.symbol,
            side = position.side,
            entryPrice = position.entryPrice,
            exitPrice = exitPrice,
            quantity = position.quantity,
            marginUsed = position.marginUsed,
            leverage = position.leverage,
            grossPnl = grossPnl,
            feesPaid = position.entryFee + exitFee,
            netPnl = netPnl,
            roePercent = roePercent,
            closeReason = closeReason,
            insightTag = insightTag,
            openedAtMillis = position.openedAtMillis,
            closedAtMillis = closedAtMillis
        )
        val tradeId = insertClosedTrade(closedTrade)
        deletePositionById(positionId)
        return closedTrade.copy(id = tradeId)
    }

    @Transaction
    suspend fun resetDemoAccount() {
        clearAllPositions()
        clearAllClosedTrades()
        upsertAccount(
            DemoAccountEntity(
                id = 1,
                initialBalance = 500.0,
                availableBalance = 500.0,
                totalTrades = 0,
                winningTrades = 0,
                totalRealizedPnl = 0.0,
                totalFeesPaid = 0.0,
                createdAtMillis = System.currentTimeMillis()
            )
        )
    }
}
