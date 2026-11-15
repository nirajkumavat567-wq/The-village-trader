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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.remote.DeltaRegion
import com.example.ui.TradingUiState
import com.example.ui.theme.LocalVillageColors
import com.example.ui.theme.MonoStyles
import com.example.util.Formatters

@Composable
fun TraderCenterScreen(
    uiState: TradingUiState,
    onDirectMessageTelegram: (String) -> Unit,
    onOpenWelcome: () -> Unit,
    onUpdateTelegramHandle: (String) -> Unit,
    onSwitchRegion: (DeltaRegion) -> Unit,
    onResetDemoAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val villageColors = LocalVillageColors.current
    val topics = listOf(
        "Trade Review",
        "Strategy Doubt",
        "$500 Demo Help",
        "Live Feed Support"
    )
    var selectedTopic by remember { mutableStateOf(topics.first()) }
    var customMessage by remember {
        mutableStateOf("Hi Niraj The Trader! I'm trading on The Village Traders demo account and want guidance on my trades.")
    }
    var showHandleDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Niraj The Trader Welcome & Mentor Hero Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, villageColors.gold.copy(alpha = 0.45f), MaterialTheme.shapes.extraLarge),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_village_hero),
                            contentDescription = "The Village Traders Center Banner",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.surface
                                        )
                                    )
                                )
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_niraj_avatar),
                                contentDescription = "Niraj The Trader avatar",
                                modifier = Modifier
                                    .size(56.dp)
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
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Filled.Verified,
                                        contentDescription = "Verified Mentor",
                                        tint = villageColors.gold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "The Village Traders • Mentor & Support Center",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = villageColors.gold
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.welcome_message_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(
                            onClick = onOpenWelcome,
                            modifier = Modifier.testTag("center_replay_welcome_button")
                        ) {
                            Text("View Welcome Card")
                        }
                    }
                }
            }
        }

        // 2. Direct Message Niraj on Telegram Hub
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.telegram_dm_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Handle: @${uiState.telegramHandle}",
                                style = MonoStyles.numberSmall,
                                color = villageColors.liveAccent
                            )
                        }
                        IconButton(
                            onClick = { showHandleDialog = true },
                            modifier = Modifier.testTag("edit_telegram_handle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit Telegram username",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.telegram_dm_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(topics) { topic ->
                            val selected = topic == selectedTopic
                            Surface(
                                modifier = Modifier
                                    .testTag("tg_topic_${topic.lowercase().replace(" ", "_")}")
                                    .clickable { selectedTopic = topic },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) villageColors.goldContainer else villageColors.elevatedSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) villageColors.gold else villageColors.cardBorder
                                )
                            ) {
                                Text(
                                    text = topic,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) villageColors.gold else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = customMessage,
                        onValueChange = { customMessage = it },
                        label = { Text("Your Message to Niraj The Trader") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("telegram_message_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val finalText = buildString {
                                append("[$selectedTopic] ")
                                append(customMessage.trim())
                                append("\n\nDemo Equity: ${Formatters.formatUsd(uiState.totalEquity)} ")
                                append("| Trades: ${uiState.account.totalTrades}")
                            }
                            onDirectMessageTelegram(finalText)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("center_telegram_dm_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Direct Message on Telegram",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.telegram_dm_button),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // 3. Dedicated Support Button & Quick Help Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, villageColors.cardBorder, MaterialTheme.shapes.large),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.HeadsetMic,
                            contentDescription = "Support Center",
                            tint = villageColors.gold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Trader Support Desk",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "Need immediate technical help with Delta Exchange live feeds, order execution, or your $500 demo fund?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            onDirectMessageTelegram(
                                "[URGENT SUPPORT] Hi Niraj The Trader, I need support on The Village Traders app (${uiState.region.displayName}, Equity: ${Formatters.formatUsd(uiState.totalEquity)})."
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("center_support_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = villageColors.gold,
                            contentColor = Color(0xFF281A00)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SupportAgent,
                            contentDescription = "Contact Support",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Instant Support via Telegram",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // 4. Delta Exchange Server & $500 Demo Fund Reset
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, villageColors.cardBorder, MaterialTheme.shapes.large),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Delta Exchange Connection & Demo Reset",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DeltaRegion.entries.forEach { reg ->
                            val selected = reg == uiState.region
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("center_region_${reg.name.lowercase()}")
                                    .clickable { onSwitchRegion(reg) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) villageColors.goldContainer else villageColors.elevatedSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) villageColors.gold else villageColors.cardBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Public,
                                        contentDescription = reg.displayName,
                                        tint = if (selected) villageColors.gold else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = reg.displayName,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) villageColors.gold else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("center_reset_demo_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, villageColors.loss.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Reset $500 Demo Fund",
                            tint = villageColors.loss,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.reset_account_title),
                            color = villageColors.loss
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }

    if (showHandleDialog) {
        var handleDraft by remember { mutableStateOf(uiState.telegramHandle) }
        AlertDialog(
            onDismissRequest = { showHandleDialog = false },
            title = { Text("Niraj Telegram Username") },
            text = {
                OutlinedTextField(
                    value = handleDraft,
                    onValueChange = { handleDraft = it },
                    label = { Text("Telegram @username") },
                    prefix = { Text("@") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateTelegramHandle(handleDraft)
                    showHandleDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHandleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.reset_account_title)) },
            text = { Text(stringResource(R.string.reset_account_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        onResetDemoAccount()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = villageColors.loss)
                ) {
                    Text("Reset to $500", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
