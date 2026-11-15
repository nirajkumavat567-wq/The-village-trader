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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.OpenPositionEntity
import com.example.data.local.TradeSide
import com.example.data.remote.MarketTicker
import com.example.ui.TradingUiState
import com.example.ui.components.LiveCandlestickChart
import com.example.ui.theme.LocalVillageColors
import com.example.ui.theme.MonoStyles
import com.example.util.Formatters

@Composable
fun TradeScreen(
    uiState: TradingUiState,
    onSelectSymbol: (String) -> Unit,
    onSelectTimeframe: (String) -> Unit,
    onPlaceOrder: (TradeSide, Double, Int, Double?, Double?) -> Unit,
    onClosePosition: (OpenPositionEntity) -> Unit,
    onUpdateTpSl: (Long, Double?, Double?) -> Unit,
    modifier: Modifier = Modifier
) {
    val villageColors = LocalVillageColors.current
    val ticker = uiState.selectedTicker
    val livePrice = ticker?.lastPrice ?: 0.0

    var selectedSide by remember { mutableStateOf(TradeSide.LONG) }
    var marginInput by remember { mutableStateOf("50") }
    var leverage by remember { mutableIntStateOf(5) }
    var tpInput by remember { mutableStateOf("") }
    var slInput by remember { mutableStateOf("") }
    var showPairPicker by remember { mutableStateOf(false) }
    var editingPosition by remember { mutableStateOf<OpenPositionEntity?>(null) }

    val marginUsd = marginInput.toDoubleOrNull() ?: 0.0
    val notionalUsd = marginUsd * leverage
    val estQty = if (livePrice > 0.0) notionalUsd / livePrice else 0.0
    val estFee = notionalUsd * 0.0005
    val totalNeeded = marginUsd + estFee
    val canAfford = marginUsd >= 5.0 && totalNeeded <= uiState.account.availableBalance + 1e-6 && livePrice > 0.0

    val activePosForSymbol = uiState.openPositions.firstOrNull { it.symbol == uiState.selectedSymbol }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Symbol Header & Live Price Strip
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
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
                        Surface(
                            modifier = Modifier
                                .testTag("trade_pair_selector")
                                .clickable { showPairPicker = true },
                            shape = RoundedCornerShape(10.dp),
                            color = villageColors.elevatedSurface,
                            border = BorderStroke(1.dp, villageColors.cardBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = uiState.selectedSymbol,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = "Select trading pair",
                                    tint = villageColors.gold
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (livePrice > 0.0) Formatters.formatPrice(livePrice) else "Loading...",
                                style = MonoStyles.priceLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            ticker?.let {
                                val isUp = it.change24hPct >= 0.0
                                Text(
                                    text = "24h ${Formatters.formatPercent(it.change24hPct)}",
                                    style = MonoStyles.numberSmall,
                                    color = if (isUp) villageColors.profit else villageColors.loss
                                )
                            }
                        }
                    }

                    ticker?.let {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MiniStat("Mark", Formatters.formatRawPrice(it.markPrice))
                            MiniStat("24h High", Formatters.formatRawPrice(it.high24h))
                            MiniStat("24h Low", Formatters.formatRawPrice(it.low24h))
                            MiniStat("Funding", "${String.format("%.4f", it.fundingRatePct)}%")
                        }
                    }
                }
            }
        }

        // 2. Timeframe Chips + Live Delta Exchange Candlestick Chart
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val timeframes = listOf("1m", "5m", "15m", "1h", "4h")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(timeframes) { tf ->
                        val selected = tf == uiState.selectedTimeframe
                        Surface(
                            modifier = Modifier
                                .testTag("timeframe_$tf")
                                .clickable { onSelectTimeframe(tf) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) villageColors.goldContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (selected) villageColors.gold else villageColors.cardBorder
                            )
                        ) {
                            Text(
                                text = tf,
                                style = MonoStyles.numberSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) villageColors.gold else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                LiveCandlestickChart(
                    candles = uiState.candles,
                    livePrice = livePrice,
                    openPositionForSymbol = activePosForSymbol,
                    isLoading = uiState.isCandlesLoading
                )
            }
        }

        // 3. Buy / Sell Demo Execution Ticket
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, villageColors.cardBorder, MaterialTheme.shapes.large),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Demo Order Ticket",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Avail: ${Formatters.formatUsd(uiState.account.availableBalance)}",
                            style = MonoStyles.numberSmall,
                            color = villageColors.gold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // BUY / SELL Segmented Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(villageColors.elevatedSurface)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TradeSideButton(
                            label = "BUY / LONG",
                            selected = selectedSide == TradeSide.LONG,
                            activeColor = villageColors.profit,
                            onClick = { selectedSide = TradeSide.LONG },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("side_buy_button")
                        )
                        TradeSideButton(
                            label = "SELL / SHORT",
                            selected = selectedSide == TradeSide.SHORT,
                            activeColor = villageColors.loss,
                            onClick = { selectedSide = TradeSide.SHORT },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("side_sell_button")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Margin USD Input
                    OutlinedTextField(
                        value = marginInput,
                        onValueChange = { raw ->
                            if (raw.isEmpty() || raw.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                marginInput = raw
                            }
                        },
                        label = { Text("Order Margin (USD)") },
                        prefix = { Text("$") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("order_margin_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick % allocation chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10, 25, 50, 95).forEach { pct ->
                            val label = if (pct == 95) "MAX" else "$pct%"
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("margin_pct_$pct")
                                    .clickable {
                                        val maxPossible = (uiState.account.availableBalance / (1.0 + leverage * 0.0005))
                                        val allocated = (maxPossible * (pct / 100.0)).coerceAtLeast(0.0)
                                        marginInput = String.format("%.2f", allocated)
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = villageColors.elevatedSurface,
                                border = BorderStroke(1.dp, villageColors.cardBorder)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MonoStyles.numberSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Leverage Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Leverage",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${leverage}x Isolated",
                            style = MonoStyles.numberMedium,
                            color = villageColors.gold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 2, 5, 10, 20).forEach { lev ->
                            val selected = leverage == lev
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("leverage_${lev}x")
                                    .clickable { leverage = lev },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) villageColors.goldContainer else villageColors.elevatedSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) villageColors.gold else villageColors.cardBorder
                                )
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${lev}x",
                                        style = MonoStyles.numberSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (selected) villageColors.gold else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Optional TP / SL
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = tpInput,
                            onValueChange = { raw ->
                                if (raw.isEmpty() || raw.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    tpInput = raw
                                }
                            },
                            label = { Text("Take Profit (Opt)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("order_tp_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = slInput,
                            onValueChange = { raw ->
                                if (raw.isEmpty() || raw.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    slInput = raw
                                }
                            },
                            label = { Text("Stop Loss (Opt)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("order_sl_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Order Preview Summary
                    Surface(
                        color = villageColors.elevatedSurface.copy(alpha = 0.7f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PreviewRow("Position Value (${leverage}x)", Formatters.formatUsd(notionalUsd))
                            PreviewRow("Est. Contract Size", "${Formatters.formatQty(estQty)} ${uiState.selectedSymbol.removeSuffix("USD")}")
                            PreviewRow("Demo Fee (0.05%)", Formatters.formatUsd(estFee))
                            PreviewRow("Total Required", Formatters.formatUsd(totalNeeded))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val submitColor = if (selectedSide == TradeSide.LONG) villageColors.profit else villageColors.loss
                    val submitTextColor = if (selectedSide == TradeSide.LONG) Color(0xFF002114) else Color.White

                    Button(
                        onClick = {
                            onPlaceOrder(
                                selectedSide,
                                marginUsd,
                                leverage,
                                tpInput.toDoubleOrNull(),
                                slInput.toDoubleOrNull()
                            )
                        },
                        enabled = canAfford && !uiState.isPlacingOrder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_order_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = submitColor,
                            contentColor = submitTextColor
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = if (selectedSide == TradeSide.LONG) {
                                "BUY / LONG ${uiState.selectedSymbol}"
                            } else {
                                "SELL / SHORT ${uiState.selectedSymbol}"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    if (!canAfford && marginUsd > 0.0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (marginUsd < 5.0) {
                                "Minimum margin per demo order is $5.00 USD"
                            } else {
                                "Insufficient demo margin balance (Avail: ${Formatters.formatUsd(uiState.account.availableBalance)})"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = villageColors.loss
                        )
                    }
                }
            }
        }

        // 4. Active Open Positions
        item {
            Text(
                text = "Active Positions (${uiState.openPositions.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (uiState.openPositions.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "No open positions right now. Place a Buy or Sell order above to start trading live Delta Exchange prices.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(uiState.openPositions, key = { "trade_pos_${it.id}" }) { pos ->
                val currentPrice = uiState.tickers[pos.symbol]?.lastPrice ?: pos.entryPrice
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OpenPositionRowCard(
                        position = pos,
                        currentPrice = currentPrice,
                        onTapSymbol = { onSelectSymbol(pos.symbol) },
                        onClose = { onClosePosition(pos) }
                    )
                    OutlinedButton(
                        onClick = { editingPosition = pos },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_tpsl_${pos.id}"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, villageColors.cardBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit TP/SL",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit TP / SL for ${pos.symbol}")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }

    if (showPairPicker) {
        PairPickerBottomSheet(
            tickers = uiState.featuredTickers,
            allTickers = uiState.tickers.values.sortedByDescending { it.turnoverUsd },
            selectedSymbol = uiState.selectedSymbol,
            onSelect = { symbol ->
                onSelectSymbol(symbol)
                showPairPicker = false
            },
            onDismiss = { showPairPicker = false }
        )
    }

    editingPosition?.let { pos ->
        EditTpSlDialog(
            position = pos,
            onDismiss = { editingPosition = null },
            onSave = { tp, sl ->
                onUpdateTpSl(pos.id, tp, sl)
                editingPosition = null
            }
        )
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
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

@Composable
private fun TradeSideButton(
    label: String,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) activeColor else Color.Transparent
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (selected) Color(0xFF090E17) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PreviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
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
private fun PairPickerBottomSheet(
    tickers: List<MarketTicker>,
    allTickers: List<MarketTicker>,
    selectedSymbol: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val source = if (query.isBlank()) {
        (tickers + allTickers).distinctBy { it.symbol }.take(30)
    } else {
        allTickers.filter {
            it.symbol.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
        }.take(30)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Delta Exchange Pair",
                    style = MaterialTheme.typography.titleLarge
                )
                TextButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close pair picker")
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search pairs") },
                placeholder = { Text("Search BTCUSD, ETHUSD, SOLUSD...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.height(340.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(source, key = { "pick_${it.symbol}" }) { item ->
                    val isCurrent = item.symbol == selectedSymbol
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(item.symbol) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCurrent) {
                            LocalVillageColors.current.goldContainer
                        } else {
                            LocalVillageColors.current.elevatedSurface
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = item.symbol,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = item.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = Formatters.formatPrice(item.lastPrice),
                                    style = MonoStyles.numberMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = Formatters.formatPercent(item.change24hPct),
                                    style = MonoStyles.numberSmall,
                                    color = if (item.change24hPct >= 0.0) {
                                        LocalVillageColors.current.profit
                                    } else {
                                        LocalVillageColors.current.loss
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditTpSlDialog(
    position: OpenPositionEntity,
    onDismiss: () -> Unit,
    onSave: (Double?, Double?) -> Unit
) {
    var tpText by remember { mutableStateOf(position.takeProfitPrice?.toString() ?: "") }
    var slText by remember { mutableStateOf(position.stopLossPrice?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit TP / SL (${position.symbol})") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Entry: ${Formatters.formatPrice(position.entryPrice)} (${position.side.badge})",
                    style = MonoStyles.numberSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = tpText,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) tpText = it },
                    label = { Text("Take Profit Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = slText,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) slText = it },
                    label = { Text("Stop Loss Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(tpText.toDoubleOrNull(), slText.toDoubleOrNull()) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
