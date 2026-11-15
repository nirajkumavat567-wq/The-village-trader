package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.OpenPositionEntity
import com.example.data.remote.CandlePoint
import com.example.ui.EquityCurvePoint
import com.example.ui.theme.LocalVillageColors
import com.example.ui.theme.MonoStyles
import com.example.util.Formatters
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun PnlPerformanceChartCard(
    equityCurve: List<EquityCurvePoint>,
    initialBalance: Double,
    totalEquity: Double,
    netPnl: Double,
    modifier: Modifier = Modifier
) {
    val villageColors = LocalVillageColors.current
    var chartMode by remember { mutableIntStateOf(0) } // 0 = Equity Curve, 1 = Trade P&L Bars
    val isOverallProfit = netPnl >= 0.0
    val accentColor = if (isOverallProfit) villageColors.profit else villageColors.loss

    Surface(
        modifier = modifier
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
                Column {
                    Text(
                        text = "Profit & Loss Chart",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = Formatters.formatUsd(netPnl, showPlus = true),
                            style = MonoStyles.numberMedium,
                            color = accentColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "from ${Formatters.formatUsd(initialBalance)} demo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Segmented chart type toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(villageColors.elevatedSurface)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ChartModeChip(
                        selected = chartMode == 0,
                        label = "Curve",
                        icon = Icons.AutoMirrored.Filled.ShowChart,
                        onClick = { chartMode = 0 },
                        tag = "pnl_chart_mode_curve"
                    )
                    ChartModeChip(
                        selected = chartMode == 1,
                        label = "Trades",
                        icon = Icons.Filled.BarChart,
                        onClick = { chartMode = 1 },
                        tag = "pnl_chart_mode_bars"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (equityCurve.size <= 1) {
                // Friendly empty state before first trade
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(168.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(villageColors.elevatedSurface.copy(alpha = 0.6f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Timeline,
                            contentDescription = "Profit and loss chart placeholder",
                            tint = villageColors.gold,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your $500.00 Demo Curve Starts Here",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Place a Buy or Sell trade on Delta Exchange live markets to plot your P&L performance.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (chartMode == 0) {
                EquityCurveCanvas(
                    points = equityCurve,
                    baseline = initialBalance,
                    lineColor = accentColor,
                    gridColor = villageColors.cardBorder,
                    goldColor = villageColors.gold
                )
            } else {
                val tradeOnly = equityCurve.drop(1)
                PnlBarsCanvas(
                    points = tradeOnly,
                    profitColor = villageColors.profit,
                    lossColor = villageColors.loss,
                    gridColor = villageColors.cardBorder
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(villageColors.gold)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Base: ${Formatters.formatUsd(initialBalance)}",
                        style = MonoStyles.numberSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Equity: ${Formatters.formatUsd(totalEquity)}",
                    style = MonoStyles.numberSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ChartModeChip(
    selected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    tag: String
) {
    val bg = if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .testTag(tag)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = fg,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = fg
        )
    }
}

@Composable
private fun EquityCurveCanvas(
    points: List<EquityCurvePoint>,
    baseline: Double,
    lineColor: Color,
    gridColor: Color,
    goldColor: Color
) {
    val minVal = min(baseline, points.minOf { it.equityUsd })
    val maxVal = max(baseline, points.maxOf { it.equityUsd })
    val range = (maxVal - minVal).let { if (it < 5.0) 10.0 else it * 1.18 }
    val paddedMin = ((minVal + maxVal) / 2.0) - (range / 2.0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(172.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val leftPad = 8.dp.toPx()
            val rightPad = 8.dp.toPx()
            val topPad = 12.dp.toPx()
            val bottomPad = 12.dp.toPx()
            val chartW = (w - leftPad - rightPad).coerceAtLeast(1f)
            val chartH = (h - topPad - bottomPad).coerceAtLeast(1f)

            fun yFor(value: Double): Float {
                val ratio = ((value - paddedMin) / range).toFloat().coerceIn(0f, 1f)
                return topPad + chartH * (1f - ratio)
            }

            // Horizontal guide lines
            for (i in 0..3) {
                val y = topPad + (chartH / 3f) * i
                drawLine(
                    color = gridColor.copy(alpha = 0.45f),
                    start = Offset(leftPad, y),
                    end = Offset(w - rightPad, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Baseline $500 dashed reference
            val baseY = yFor(baseline)
            drawLine(
                color = goldColor.copy(alpha = 0.7f),
                start = Offset(leftPad, baseY),
                end = Offset(w - rightPad, baseY),
                strokeWidth = 1.3.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
            )

            val coords = points.mapIndexed { idx, pt ->
                val x = if (points.size == 1) {
                    leftPad + chartW / 2f
                } else {
                    leftPad + (idx.toFloat() / (points.size - 1).toFloat()) * chartW
                }
                Offset(x, yFor(pt.equityUsd))
            }

            val linePath = Path()
            val fillPath = Path()
            coords.forEachIndexed { i, offset ->
                if (i == 0) {
                    linePath.moveTo(offset.x, offset.y)
                    fillPath.moveTo(offset.x, h - bottomPad)
                    fillPath.lineTo(offset.x, offset.y)
                } else {
                    val prev = coords[i - 1]
                    val midX = (prev.x + offset.x) / 2f
                    linePath.cubicTo(midX, prev.y, midX, offset.y, offset.x, offset.y)
                    fillPath.cubicTo(midX, prev.y, midX, offset.y, offset.x, offset.y)
                }
            }
            fillPath.lineTo(coords.last().x, h - bottomPad)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.34f),
                        lineColor.copy(alpha = 0.02f)
                    ),
                    startY = topPad,
                    endY = h - bottomPad
                )
            )

            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(
                    width = 2.6.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            coords.forEachIndexed { idx, pt ->
                val radius = if (idx == coords.lastIndex) 5.dp.toPx() else 3.dp.toPx()
                drawCircle(
                    color = lineColor,
                    radius = radius,
                    center = pt
                )
                if (idx == coords.lastIndex) {
                    drawCircle(
                        color = lineColor.copy(alpha = 0.25f),
                        radius = 10.dp.toPx(),
                        center = pt
                    )
                }
            }
        }
    }
}

@Composable
private fun PnlBarsCanvas(
    points: List<EquityCurvePoint>,
    profitColor: Color,
    lossColor: Color,
    gridColor: Color
) {
    val maxAbs = points.maxOfOrNull { abs(it.tradePnlUsd) }?.coerceAtLeast(1.0) ?: 10.0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(172.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val padH = 12.dp.toPx()
            val padW = 8.dp.toPx()
            val usableH = (h - padH * 2f).coerceAtLeast(1f)
            val zeroY = padH + usableH / 2f

            drawLine(
                color = gridColor.copy(alpha = 0.7f),
                start = Offset(padW, zeroY),
                end = Offset(w - padW, zeroY),
                strokeWidth = 1.2.dp.toPx()
            )

            val visible = points.takeLast(18)
            val slotW = (w - padW * 2f) / visible.size.coerceAtLeast(1)
            val barW = (slotW * 0.58f).coerceIn(6.dp.toPx(), 26.dp.toPx())

            visible.forEachIndexed { idx, pt ->
                val centerX = padW + slotW * idx + slotW / 2f
                val ratio = (abs(pt.tradePnlUsd) / maxAbs).toFloat().coerceIn(0.06f, 1f)
                val barHeight = (usableH / 2f) * ratio * 0.9f
                val color = if (pt.tradePnlUsd >= 0.0) profitColor else lossColor
                val topY = if (pt.tradePnlUsd >= 0.0) zeroY - barHeight else zeroY

                drawRoundRect(
                    color = color,
                    topLeft = Offset(centerX - barW / 2f, topY),
                    size = Size(barW, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun LiveCandlestickChart(
    candles: List<CandlePoint>,
    livePrice: Double,
    openPositionForSymbol: OpenPositionEntity?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val villageColors = LocalVillageColors.current
    val surfaceBg = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(228.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(villageColors.elevatedSurface.copy(alpha = 0.55f))
            .border(1.dp, villageColors.cardBorder, MaterialTheme.shapes.medium)
    ) {
        if (candles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (isLoading) "Loading Delta Exchange candles..." else "Syncing live candle feed...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val visibleCandles = candles.takeLast(45)
            val allLows = visibleCandles.minOf { it.low }
            val allHighs = visibleCandles.maxOf { it.high }
            val minPrice = min(allLows, livePrice.takeIf { it > 0.0 } ?: allLows)
            val maxPrice = max(allHighs, livePrice.takeIf { it > 0.0 } ?: allHighs)
            val span = (maxPrice - minPrice).let { if (it <= 0.0) maxPrice * 0.01 else it * 1.12 }
            val mid = (maxPrice + minPrice) / 2.0
            val chartMin = mid - span / 2.0

            Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 10.dp)) {
                val w = size.width
                val h = size.height
                val chartW = (w - 64.dp.toPx()).coerceAtLeast(1f)

                fun yFor(p: Double): Float {
                    val ratio = ((p - chartMin) / span).toFloat().coerceIn(0f, 1f)
                    return h * (1f - ratio)
                }

                // Grid lines
                for (i in 0..3) {
                    val y = (h / 3f) * i
                    drawLine(
                        color = villageColors.cardBorder.copy(alpha = 0.45f),
                        start = Offset(0f, y),
                        end = Offset(chartW, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val slotW = chartW / visibleCandles.size.coerceAtLeast(1)
                val bodyW = (slotW * 0.62f).coerceIn(2.dp.toPx(), 10.dp.toPx())

                visibleCandles.forEachIndexed { idx, c ->
                    val cx = slotW * idx + slotW / 2f
                    val isBull = c.close >= c.open
                    val color = if (isBull) villageColors.profit else villageColors.loss

                    val highY = yFor(c.high)
                    val lowY = yFor(c.low)
                    val openY = yFor(c.open)
                    val closeY = yFor(c.close)

                    // Wick
                    drawLine(
                        color = color,
                        start = Offset(cx, highY),
                        end = Offset(cx, lowY),
                        strokeWidth = 1.4.dp.toPx()
                    )

                    // Body
                    val top = min(openY, closeY)
                    val bodyH = max(abs(closeY - openY), 2.dp.toPx())
                    drawRect(
                        color = color,
                        topLeft = Offset(cx - bodyW / 2f, top),
                        size = Size(bodyW, bodyH)
                    )
                }

                // Entry / TP / SL overlays for active position
                openPositionForSymbol?.let { pos ->
                    val entryY = yFor(pos.entryPrice)
                    drawLine(
                        color = villageColors.gold,
                        start = Offset(0f, entryY),
                        end = Offset(chartW, entryY),
                        strokeWidth = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )
                    pos.takeProfitPrice?.let { tp ->
                        val tpY = yFor(tp)
                        drawLine(
                            color = villageColors.profit,
                            start = Offset(0f, tpY),
                            end = Offset(chartW, tpY),
                            strokeWidth = 1.2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    }
                    pos.stopLossPrice?.let { sl ->
                        val slY = yFor(sl)
                        drawLine(
                            color = villageColors.loss,
                            start = Offset(0f, slY),
                            end = Offset(chartW, slY),
                            strokeWidth = 1.2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    }
                }

                // Live current price dashed line
                if (livePrice > 0.0) {
                    val liveY = yFor(livePrice)
                    drawLine(
                        color = villageColors.liveAccent,
                        start = Offset(0f, liveY),
                        end = Offset(w, liveY),
                        strokeWidth = 1.3.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                    )
                }
            }

            // Right-side price scale labels
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = Formatters.formatRawPrice(maxPrice),
                    style = MonoStyles.numberSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                if (livePrice > 0.0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(villageColors.liveAccent)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = Formatters.formatRawPrice(livePrice),
                            style = MonoStyles.numberSmall.copy(fontWeight = FontWeight.Bold),
                            color = surfaceBg
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = Formatters.formatRawPrice(minPrice),
                    style = MonoStyles.numberSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
