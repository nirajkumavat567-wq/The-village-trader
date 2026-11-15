package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.OpenPositionEntity
import com.example.data.local.TradeSide
import com.example.data.remote.MarketTicker
import com.example.ui.TradingUiState
import com.example.ui.components.PnlPerformanceChartCard
import com.example.ui.theme.LocalVillageColors
import com.example.ui.theme.MonoStyles
import com.example.util.Formatters

@Composable
fun DashboardScreen(
    uiState: TradingUiState,
    onNavigateToTrade: (String?) -> Unit,
    onNavigateToPerformance: () -> Unit,
    onOpenWelcome: () -> Unit,
    onDirectMessageTelegram: () -> Unit,
    onClosePosition: (OpenPositionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val villageColors = LocalVillageColors.current
    val isProfit = uiState.totalAccountPnl >= 0.0
    val pnlColor = if (isProfit) villageColors.profit else villageColors.loss
    val pnlBg = if (isProfit) villageColors.profitContainer else villageColors.lossContainer

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Hero $500 Demo Portfolio Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, villageColors.cardBorder, MaterialTheme.shapes.extraLarge),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface,
                                    pnlBg.copy(alpha = 0.45f)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.AccountBalanceWallet,
                                    contentDescription = "Demo Account Equity",
                                    tint = villageColors.gold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "DEMO ACCOUNT EQUITY",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = villageColors.goldContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "$500 DEMO FUND",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = villageColors.gold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = Formatters.formatUsd(uiState.totalEquity),
                                style = MonoStyles.balanceLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                color = pnlBg,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isProfit) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                                        contentDescription = "Account P&L trend",
                                        tint = pnlColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${Formatters.formatUsd(uiState.totalAccountPnl, showPlus = true)} (${Formatters.formatPercent(uiState.totalAccountRoePct)})",
                                        style = MonoStyles.numberSmall.copy(fontWeight = FontWeight.Bold),
                                        color = pnlColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            PortfolioStatItem(
                                label = "Available",
                                value = Formatters.formatUsd(uiState.account.availableBalance)
                            )
                            PortfolioStatItem(
                                label = "In Margin",
                                value = Formatters.formatUsd(uiState.usedMargin)
                            )
                            PortfolioStatItem(
                                label = "Unrealized",
                                value = Formatters.formatUsd(uiState.totalUnrealizedPnl, showPlus = true),
                                valueColor = if (uiState.totalUnrealizedPnl >= 0) villageColors.profit else villageColors.loss
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onNavigateToTrade(null) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("dashboard_trade_now_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = "Trade now",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Buy / Sell")
                            }

                            OutlinedButton(
                                onClick = onNavigateToPerformance,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("dashboard_performance_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, villageColors.cardBorder)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Analytics,
                                    contentDescription = "Performance Summary",
                                    tint = villageColors.gold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Summary",
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Niraj The Trader Welcome Strip
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_welcome_card")
                    .clickable(onClick = onOpenWelcome)
                    .border(1.dp, villageColors.gold.copy(alpha = 0.35f), MaterialTheme.shapes.large),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_niraj_avatar),
                        contentDescription = "Niraj The Trader",
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, villageColors.gold, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Welcome from Niraj The Trader",
                            style = MaterialTheme.typography.titleSmall,
                            color = villageColors.gold
                        )
                        Text(
                            text = "Trade Delta Exchange live markets with $500 demo funds. Tap to read note.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        modifier = Modifier
                            .testTag("dashboard_telegram_chip")
                            .clickable(onClick = onDirectMessageTelegram),
                        shape = RoundedCornerShape(10.dp),
                        color = villageColors.elevatedSurface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "DM on Telegram",
                                tint = villageColors.liveAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "TG DM",
                                style = MaterialTheme.typography.labelMedium,
                                color = villageColors.liveAccent
                            )
                        }
                    }
                }
            }
        }

        // 3. Profit & Loss Chart Card
        item {
            PnlPerformanceChartCard(
                equityCurve = uiState.equityCurve,
                initialBalance = uiState.account.initialBalance,
                totalEquity = uiState.totalEquity,
                netPnl = uiState.totalAccountPnl
            )
        }

        // 4. Active Open Positions
        if (uiState.openPositions.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Open Positions (${uiState.openPositions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Live Mark P&L",
                        style = MaterialTheme.typography.labelMedium,
                        color = villageColors.liveAccent
                    )
                }
            }

            items(uiState.openPositions, key = { it.id }) { pos ->
                val currentPrice = uiState.tickers[pos.symbol]?.lastPrice ?: pos.entryPrice
                OpenPositionRowCard(
                    position = pos,
                    currentPrice = currentPrice,
                    onTapSymbol = { onNavigateToTrade(pos.symbol) },
                    onClose = { onClosePosition(pos) }
                )
            }
        }

        // 5. Live Delta Exchange Markets
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Delta Exchange Live Markets",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Tap any pair to view chart & Buy/Sell",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (uiState.tickers.isEmpty()) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }

        if (uiState.featuredTickers.isNotEmpty()) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.featuredTickers.take(4), key = { "top_${it.symbol}" }) { ticker ->
                        FeaturedMarketCard(
                            ticker = ticker,
                            onClick = { onNavigateToTrade(ticker.symbol) }
                        )
                    }
                }
            }

            items(uiState.featuredTickers, key = { it.symbol }) { ticker ->
                MarketTickerRow(
                    ticker = ticker,
                    onClick = { onNavigateToTrade(ticker.symbol) }
                )
            }
        } else {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = uiState.networkError ?: "Connecting to Delta Exchange live ticker feed...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun PortfolioStatItem(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MonoStyles.numberMedium,
            color = valueColor
        )
    }
}

@Composable
private fun FeaturedMarketCard(
    ticker: MarketTicker,
    onClick: () -> Unit
) {
    val villageColors = LocalVillageColors.current
    val isUp = ticker.change24hPct >= 0.0
    val changeColor = if (isUp) villageColors.profit else villageColors.loss

    Surface(
        modifier = Modifier
            .width(156.dp)
            .testTag("featured_ticker_${ticker.symbol}")
            .clickable(onClick = onClick)
            .border(1.dp, villageColors.cardBorder, MaterialTheme.shapes.medium),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ticker.symbol,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = Formatters.formatPercent(ticker.change24hPct),
                    style = MonoStyles.numberSmall,
                    color = changeColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = Formatters.formatPrice(ticker.lastPrice),
                style = MonoStyles.numberMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Vol ${Formatters.formatVolumeCompact(ticker.volume24h)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MarketTickerRow(
    ticker: MarketTicker,
    onClick: () -> Unit
) {
    val villageColors = LocalVillageColors.current
    val isUp = ticker.change24hPct >= 0.0
    val changeColor = if (isUp) villageColors.profit else villageColors.loss
    val changeBg = if (isUp) villageColors.profitContainer else villageColors.lossContainer

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("market_row_${ticker.symbol}")
            .clickable(onClick = onClick)
            .border(1.dp, villageColors.cardBorder, MaterialTheme.shapes.medium),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ticker.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "H: ${Formatters.formatRawPrice(ticker.high24h)}  L: ${Formatters.formatRawPrice(ticker.low24h)}",
                    style = MonoStyles.numberSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Formatters.formatPrice(ticker.lastPrice),
                    style = MonoStyles.numberMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    color = changeBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = Formatters.formatPercent(ticker.change24hPct),
                        style = MonoStyles.numberSmall,
                        color = changeColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Trade ${ticker.symbol}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun OpenPositionRowCard(
    position: OpenPositionEntity,
    currentPrice: Double,
    onTapSymbol: () -> Unit,
    onClose: () -> Unit
) {
    val villageColors = LocalVillageColors.current
    val unrealized = position.unrealizedPnl(currentPrice)
    val roe = position.roePercent(currentPrice)
    val isProfit = unrealized >= 0.0
    val pnlColor = if (isProfit) villageColors.profit else villageColors.loss
    val sideColor = if (position.side == TradeSide.LONG) villageColors.profit else villageColors.loss
    val sideBg = if (position.side == TradeSide.LONG) villageColors.profitContainer else villageColors.lossContainer

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onTapSymbol)
                ) {
                    Surface(color = sideBg, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = "${position.side.badge} ${position.leverage}x",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = sideColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = position.symbol,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Formatters.formatUsd(unrealized, showPlus = true),
                        style = MonoStyles.numberMedium.copy(fontWeight = FontWeight.Bold),
                        color = pnlColor
                    )
                    Text(
                        text = Formatters.formatPercent(roe),
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
                PositionMetaColumn("Entry", Formatters.formatPrice(position.entryPrice))
                PositionMetaColumn("Mark", Formatters.formatPrice(currentPrice))
                PositionMetaColumn("Margin", Formatters.formatUsd(position.marginUsed))
                PositionMetaColumn("Liq", Formatters.formatPrice(position.liquidationPrice), villageColors.gold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tpText = position.takeProfitPrice?.let { "TP ${Formatters.formatRawPrice(it)}" } ?: "No TP"
                val slText = position.stopLossPrice?.let { "SL ${Formatters.formatRawPrice(it)}" } ?: "No SL"
                Text(
                    text = "$tpText  •  $slText",
                    style = MonoStyles.numberSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onClose,
                    modifier = Modifier.testTag("close_position_${position.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = villageColors.elevatedSurface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close position",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Close", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun PositionMetaColumn(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MonoStyles.numberSmall,
            color = valueColor
        )
    }
}
