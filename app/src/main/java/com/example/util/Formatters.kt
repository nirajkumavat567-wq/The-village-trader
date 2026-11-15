package com.example.util

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object Formatters {
    private val usdFormat = DecimalFormat("#,##0.00")
    private val priceHighFormat = DecimalFormat("#,##0.00")
    private val priceMidFormat = DecimalFormat("#,##0.000")
    private val priceLowFormat = DecimalFormat("#,##0.00000")
    private val qtyFormat = DecimalFormat("#,##0.####")
    private val timeFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    private val shortTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun formatUsd(amount: Double, showPlus: Boolean = false): String {
        val absStr = usdFormat.format(abs(amount))
        return when {
            amount < -0.0001 -> "-$$absStr"
            showPlus && amount > 0.0001 -> "+$$absStr"
            else -> "$$absStr"
        }
    }

    fun formatPrice(price: Double): String {
        if (price <= 0.0) return "$0.00"
        return when {
            price >= 100.0 -> "$" + priceHighFormat.format(price)
            price >= 1.0 -> "$" + priceMidFormat.format(price)
            else -> "$" + priceLowFormat.format(price)
        }
    }

    fun formatRawPrice(price: Double): String {
        if (price <= 0.0) return "0.00"
        return when {
            price >= 100.0 -> priceHighFormat.format(price)
            price >= 1.0 -> priceMidFormat.format(price)
            else -> priceLowFormat.format(price)
        }
    }

    fun formatPercent(pct: Double, showPlus: Boolean = true): String {
        val formatted = String.format(Locale.US, "%.2f", abs(pct))
        return when {
            pct < -0.001 -> "-$formatted%"
            showPlus && pct > 0.001 -> "+$formatted%"
            else -> "$formatted%"
        }
    }

    fun formatQty(qty: Double): String = qtyFormat.format(qty)

    fun formatTimestamp(millis: Long): String = timeFormat.format(Date(millis))

    fun formatCandleTime(seconds: Long): String = shortTimeFormat.format(Date(seconds * 1000L))

    fun formatDuration(durationMillis: Long): String {
        val totalSeconds = (durationMillis / 1000L).coerceAtLeast(1L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    fun formatVolumeCompact(value: Double): String {
        return when {
            value >= 1_000_000_000 -> String.format(Locale.US, "%.2fB", value / 1_000_000_000.0)
            value >= 1_000_000 -> String.format(Locale.US, "%.2fM", value / 1_000_000.0)
            value >= 1_000 -> String.format(Locale.US, "%.1fK", value / 1_000.0)
            else -> String.format(Locale.US, "%.0f", value)
        }
    }
}
