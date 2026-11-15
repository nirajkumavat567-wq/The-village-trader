package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ClosedTradeEntity
import com.example.data.local.TradeSide
import com.example.ui.TradingUiState
import com.example.ui.theme.LocalVillageColors
import com.example.ui.theme.MonoStyles
import com.example.util.Formatters

private enum class TradeFilter(val label: String) {
    ALL("All Trades"),
    WINS("Wins"),
    LOSSES("Losses"),
    LONG("Long"),
    SHORT("Short")
}

@Composable
fun PerformanceScreen(
    uiState: TradingUiState,
    onShareTradeOnTelegram: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val villageColors = LocalVillageColors.current
    val summary = uiState.performanceSummary
    var activeFilter by remember { mutableStateOf(TradeFilter.ALL) }
    var selectedTrade by remember { mutableStateOf<ClosedTradeEntity?>(null) }

    val filteredTrades = remember(uiState.closedTrades, activeFilter) {
        when (activeFilter) {
            TradeFilter.ALL -> uiState.closedTrades
            TradeFilter.WINS -> uiState.closedTrades.filter { it.netPnl > 0.0 }
            TradeFilter.LOSSES -> uiState.closedTrades.filter { it.netPnl <= 0.0 }
            TradeFilter.LONG -> uiState.closedTrades.filter { it.side == TradeSide.LONG }
            TradeFilter.SHORT -> uiState.closedTrades.filter { it.side == TradeSide.SHORT }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Overall Account Performance Summary Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, villageColors.cardBorder, MaterialTheme.shapes.extraLarge),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = "Performance Summary",
                                tint = villageColors.gold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Performance Summary",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            color = villageColors.elevatedSurface,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${summary.totalTrades} Trades",
                                style = MonoStyles.numberSmall,
                                color = villageColors.gold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryMetricBox(
                            label = "Win Rate",
                            value = String.format("%.1f%%", summary.winRatePct),
                            subtext = "${summary.winningTrades}W / ${summary.losingTrades}L",
                            valueColor = if (summary.winRatePct >= 50.0) villageColors.profit else villageColors.gold,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryMetricBox(
                            label = "Realized Net P&L",
                            value = Formatters.formatUsd(summary.netPnlUsd, showPlus = true),
                            subtext = "PF ${String.format("%.2f", summary.profitFactor)}",
                            valueColor = if (summary.netPnlUsd >= 0.0) villageColors.profit else villageColors.loss,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryMetricBox(
                            label = "Best Trade",
                            value = Formatters.formatUsd(summary.bestTradePnl, showPlus = true),
                            subtext = "Avg Win ${Formatters.formatUsd(summary.avgWinUsd, showPlus = true)}",
                            valueColor = villageColors.profit,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryMetricBox(
                            label = "Worst Trade",
                            value = Formatters.formatUsd(summary.worstTradePnl, showPlus = true),
                            subtext = "Avg Loss ${Formatters.formatUsd(summary.avgLossUsd, showPlus = true)}",
                            valueColor = villageColors.loss,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(villageColors.elevatedSurface)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Vol: $${Formatters.formatVolumeCompact(summary.totalVolumeUsd)}",
                            style = MonoStyles.numberSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Fees: ${Formatters.formatUsd(summary.totalFeesUsd)}",
                            style = MonoStyles.numberSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Avg Hold: ${Formatters.formatDuration(summary.avgDurationMillis)}",
                            style = MonoStyles.numberSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2. Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Every Trade Performance Report",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(TradeFilter.entries) { filter ->
                        val selected = filter == activeFilter
                        Surface(
                            modifier = Modifier
                                .testTag("perf_filter_${filter.name.lowercase()}")
                                .clickable { activeFilter = filter },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) villageColors.goldContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (selected) villageColors.gold else villageColors.cardBorder
                            )
                        ) {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) villageColors.gold else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Per-Trade Performance Cards
        if (filteredTrades.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, villageColors.cardBorder, MaterialTheme.shapes.large),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Analytics,
                            contentDescription = "No closed trades yet",
                            tint = villageColors.gold,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Closed Trades Yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Once you close a Buy or Sell position, a full performance summary with net P&L, ROE%, fees, and execution insight appears here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredTrades, key = { it.id }) { trade ->
                ClosedTradePerformanceCard(
                    trade = trade,
                    onClick = { selectedTrade = trade }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }

    selectedTrade?.let { trade ->
        TradeDetailBottomSheet(
            trade = trade,
            onDismiss = { selectedTrade = null },
            onAskNirajTelegram = {
                val msg = buildString {
                    append("Hi Niraj The Trader, please review my Village Traders demo trade:\n")
                    append("Pair: ${trade.symbol} (${trade.side.badge} ${trade.leverage}x)\n")
                    append("Entry: ${Formatters.formatPrice(trade.entryPrice)} -> Exit: ${Formatters.formatPrice(trade.exitPrice)}\n")
                    append("Net P&L: ${Formatters.formatUsd(trade.netPnl, showPlus = true)} (${Formatters.formatPercent(trade.roePercent)})\n")
                    append("Outcome: ${trade.closeReason.label} • ${trade.insightTag}")
                }
                selectedTrade = null
                onShareTradeOnTelegram(msg)
            }
        )
    }
}

@Composable
private fun SummaryMetricBox(
    label: String,
    value: String,
    subtext: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    val villageColors = LocalVillageColors.current
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = villageColors.elevatedSurface
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MonoStyles.numberMedium.copy(fontWeight = FontWeight.Bold),
                color = valueColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                style = MonoStyles.numberSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ClosedTradePerformanceCard(
    trade: ClosedTradeEntity,
    onClick: () -> Unit
) {
    val villageColors = LocalVillageColors.current
    val isWin = trade.netPnl >= 0.0
    val pnlColor = if (isWin) villageColors.profit else villageColors.loss
    val sideColor = if (trade.side == TradeSide.LONG) villageColors.profit else villageColors.loss
    val sideBg = if (trade.side == TradeSide.LONG) villageColors.profitContainer else villageColors.lossContainer

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("closed_trade_${trade.id}")
            .clickable(onClick = onClick)
            .border(1.dp, villageColors.cardBorder, MaterialTheme.shapes.large),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = sideBg, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = "${trade.side.badge} ${trade.leverage}x",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = sideColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = trade.symbol,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Formatters.formatUsd(trade.netPnl, showPlus = true),
                        style = MonoStyles.numberMedium.copy(fontWeight = FontWeight.Bold),
                        color = pnlColor
                    )
                    Text(
                        text = Formatters.formatPercent(trade.roePercent),
                        style = MonoStyles.numberSmall,
                        color = pnlColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TradeMiniCol("Entry", Formatters.formatPrice(trade.entryPrice))
                TradeMiniCol("Exit", Formatters.formatPrice(trade.exitPrice))
                TradeMiniCol("Margin", Formatters.formatUsd(trade.marginUsed))
                TradeMiniCol("Fees", Formatters.formatUsd(trade.feesPaid))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = villageColors.elevatedSurface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = trade.insightTag,
                        style = MaterialTheme.typography.labelSmall,
                        color = villageColors.gold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = "Duration",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${Formatters.formatDuration(trade.durationMillis)} • ${trade.closeReason.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TradeMiniCol(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MonoStyles.numberSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TradeDetailBottomSheet(
    trade: ClosedTradeEntity,
    onDismiss: () -> Unit,
    onAskNirajTelegram: () -> Unit
) {
    val villageColors = LocalVillageColors.current
    val isWin = trade.netPnl >= 0.0
    val pnlColor = if (isWin) villageColors.profit else villageColors.loss
    val priceChangePct = if (trade.entryPrice > 0.0) {
        val raw = ((trade.exitPrice - trade.entryPrice) / trade.entryPrice) * 100.0
        if (trade.side == TradeSide.LONG) raw else -raw
    } else 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${trade.symbol} Trade Summary",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${trade.side.label} • ${trade.leverage}x • ${trade.closeReason.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = Formatters.formatUsd(trade.netPnl, showPlus = true),
                    style = MonoStyles.priceLarge,
                    color = pnlColor
                )
            }

            Surface(
                color = villageColors.goldContainer,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Performance Takeaway: ${trade.insightTag}",
                    style = MaterialTheme.typography.labelLarge,
                    color = villageColors.gold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            HorizontalDivider(color = villageColors.cardBorder)

            DetailRow("Entry Price", Formatters.formatPrice(trade.entryPrice))
            DetailRow("Exit Price", Formatters.formatPrice(trade.exitPrice))
            DetailRow("Favorable Price Move", Formatters.formatPercent(priceChangePct))
            DetailRow("Contract Quantity", Formatters.formatQty(trade.quantity))
            DetailRow("Margin Allocated", Formatters.formatUsd(trade.marginUsed))
            DetailRow("Gross P&L", Formatters.formatUsd(trade.grossPnl, showPlus = true))
            DetailRow("Demo Fees (Entry + Exit)", Formatters.formatUsd(trade.feesPaid))
            DetailRow("Net P&L & ROE", "${Formatters.formatUsd(trade.netPnl, showPlus = true)} (${Formatters.formatPercent(trade.roePercent)})")
            DetailRow("Holding Duration", Formatters.formatDuration(trade.durationMillis))
            DetailRow("Closed At", Formatters.formatTimestamp(trade.closedAtMillis))

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = onAskNirajTelegram,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("trade_detail_ask_niraj_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Ask Niraj on Telegram",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Review This Trade with Niraj on Telegram")
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MonoStyles.numberSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
