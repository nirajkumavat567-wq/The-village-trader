package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class TradingTypeConverters {
    @TypeConverter
    fun fromTradeSide(side: TradeSide): String = side.name

    @TypeConverter
    fun toTradeSide(value: String): TradeSide = TradeSide.valueOf(value)

    @TypeConverter
    fun fromCloseReason(reason: CloseReason): String = reason.name

    @TypeConverter
    fun toCloseReason(value: String): CloseReason = CloseReason.valueOf(value)
}

@Database(
    entities = [
        DemoAccountEntity::class,
        OpenPositionEntity::class,
        ClosedTradeEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(TradingTypeConverters::class)
abstract class VillageDatabase : RoomDatabase() {
    abstract fun tradingDao(): TradingDao

    companion object {
        @Volatile
        private var INSTANCE: VillageDatabase? = null

        fun getInstance(context: Context): VillageDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VillageDatabase::class.java,
                    "village_traders_demo.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
