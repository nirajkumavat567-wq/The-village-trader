package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.remote.ConnectionStatus
import com.example.data.remote.DeltaRegion
import com.example.ui.theme.LocalVillageColors

object TelegramLauncher {
    fun openDirectMessage(context: Context, handle: String, prefilledMessage: String) {
        val cleanHandle = handle.trim().removePrefix("@").ifBlank { "nirajthetrader" }
        val encodedText = Uri.encode(prefilledMessage)
        val tgUri = Uri.parse("https://t.me/$cleanHandle?text=$encodedText")
        val intent = Intent(Intent.ACTION_VIEW, tgUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(
                context,
                "Install Telegram or open t.me/$cleanHandle in your browser",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

@Composable
fun VillageTopBar(
    connectionStatus: ConnectionStatus,
    region: DeltaRegion,
    onToggleRegion: () -> Unit,
    onSupportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val villageColors = LocalVillageColors.current
    val pulseTransition = rememberInfiniteTransition(label = "live_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val statusColor = when (connectionStatus) {
        ConnectionStatus.LIVE_WS -> villageColors.profit
        ConnectionStatus.LIVE_REST -> villageColors.liveAccent
        ConnectionStatus.CONNECTING, ConnectionStatus.RECONNECTING -> villageColors.gold
        ConnectionStatus.OFFLINE -> villageColors.loss
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_village_traders_logo),
                    contentDescription = "The Village Traders logo",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, villageColors.cardBorder, RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .testTag("region_switch_pill")
                            .clickable(onClick = onToggleRegion)
                            .padding(vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .alpha(if (connectionStatus == ConnectionStatus.LIVE_WS) pulseAlpha else 1f)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${connectionStatus.label} • ${region.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Prominent 1-Tap Support Button
            FilledTonalButton(
                onClick = onSupportClick,
                modifier = Modifier.testTag("top_support_button"),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = villageColors.goldContainer,
                    contentColor = villageColors.gold
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SupportAgent,
                    contentDescription = "Open Support",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.action_support),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun NirajWelcomeDialog(
    onStartTrading: () -> Unit,
    onMessageTelegram: () -> Unit
) {
    val villageColors = LocalVillageColors.current

    Dialog(
        onDismissRequest = onStartTrading,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .border(1.dp, villageColors.gold.copy(alpha = 0.4f), MaterialTheme.shapes.extraLarge),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_village_hero),
                        contentDescription = "The Village Traders Welcome Banner",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(132.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                                    )
                                )
                            )
                    )
                    IconButton(
                        onClick = onStartTrading,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .testTag("welcome_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close welcome message",
                            tint = Color.White
                        )
                    }
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_niraj_avatar),
                            contentDescription = "Niraj The Trader avatar",
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .border(2.dp, villageColors.gold, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Niraj The Trader",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = "Verified Mentor",
                                    tint = villageColors.gold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Founder • The Village Traders",
                                style = MaterialTheme.typography.labelMedium,
                                color = villageColors.gold
                            )
                        }
                    }
                }

                Column(modifier = Modifier.padding(20.dp)) {
                    Surface(
                        color = villageColors.profitContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "$500.00 USD Demo Fund Credited",
                            style = MaterialTheme.typography.labelLarge,
                            color = villageColors.profit,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.welcome_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.welcome_message_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onStartTrading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("welcome_start_trading_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.welcome_cta),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onMessageTelegram,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("welcome_telegram_dm_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Message on Telegram",
                            tint = villageColors.liveAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Direct Message Niraj on Telegram",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportQuickBottomSheet(
    telegramHandle: String,
    region: DeltaRegion,
    onDismiss: () -> Unit,
    onOpenCenterTab: () -> Unit,
    onDirectMessageTelegram: (String) -> Unit,
    onSwitchRegion: (DeltaRegion) -> Unit
) {
    val villageColors = LocalVillageColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.HeadsetMic,
                    contentDescription = "Trader Support",
                    tint = villageColors.gold,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.support_desk_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "The Village Traders • Direct help from Niraj",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onDirectMessageTelegram(
                        "Hi Niraj The Trader! I need help with my $500 Village Traders demo account."
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sheet_telegram_dm_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Telegram DM",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Direct Message @$telegramHandle on Telegram")
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    onDismiss()
                    onOpenCenterTab()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sheet_open_center_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Open Trader Center & Custom Support Form")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Delta Exchange Live Feed Endpoint",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DeltaRegion.entries.forEach { option ->
                    val selected = option == region
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sheet_region_${option.name.lowercase()}")
                            .clickable { onSwitchRegion(option) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) villageColors.goldContainer else villageColors.elevatedSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selected) villageColors.gold else villageColors.cardBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Public,
                                contentDescription = option.displayName,
                                tint = if (selected) villageColors.gold else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = option.displayName,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) villageColors.gold else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
