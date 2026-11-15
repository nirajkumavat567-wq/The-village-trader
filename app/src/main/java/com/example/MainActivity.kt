package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CandlestickChart
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.remote.DeltaRegion
import com.example.ui.MainViewModel
import com.example.ui.components.NirajWelcomeDialog
import com.example.ui.components.SupportQuickBottomSheet
import com.example.ui.components.TelegramLauncher
import com.example.ui.components.VillageTopBar
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PerformanceScreen
import com.example.ui.screens.TradeScreen
import com.example.ui.screens.TraderCenterScreen
import com.example.ui.theme.LocalVillageColors
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.VillageTradersTheme

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_TRADE = "trade"
const val ROUTE_PERFORMANCE = "performance"
const val ROUTE_CENTER = "center"

private data class NavTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val APP_TABS = listOf(
    NavTab(
        route = ROUTE_DASHBOARD,
        labelRes = R.string.nav_dashboard,
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        testTag = "nav_tab_dashboard"
    ),
    NavTab(
        route = ROUTE_TRADE,
        labelRes = R.string.nav_trade,
        selectedIcon = Icons.Filled.CandlestickChart,
        unselectedIcon = Icons.Outlined.CandlestickChart,
        testTag = "nav_tab_trade"
    ),
    NavTab(
        route = ROUTE_PERFORMANCE,
        labelRes = R.string.nav_performance,
        selectedIcon = Icons.Filled.Analytics,
        unselectedIcon = Icons.Outlined.Analytics,
        testTag = "nav_tab_performance"
    ),
    NavTab(
        route = ROUTE_CENTER,
        labelRes = R.string.nav_center,
        selectedIcon = Icons.Filled.SupportAgent,
        unselectedIcon = Icons.Outlined.SupportAgent,
        testTag = "nav_tab_center"
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(ObsidianBg.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(ObsidianBg.toArgb())
        )
        setContent {
            VillageTradersTheme(darkTheme = true) {
                VillageTradersApp()
            }
        }
    }
}

@Composable
fun VillageTradersApp(
    viewModel: MainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showWelcome by viewModel.showWelcomeModal.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showSupportSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val villageColors = LocalVillageColors.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 680.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.navigationBars,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                VillageTopBar(
                    connectionStatus = uiState.connectionStatus,
                    region = uiState.region,
                    onToggleRegion = {
                        val next = if (uiState.region == DeltaRegion.INDIA) {
                            DeltaRegion.GLOBAL
                        } else {
                            DeltaRegion.INDIA
                        }
                        viewModel.switchRegion(next)
                    },
                    onSupportClick = { showSupportSheet = true },
                    modifier = Modifier.statusBarsPadding()
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        APP_TABS.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            val label = stringResource(tab.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag(tab.testTag),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = villageColors.gold,
                                    selectedTextColor = villageColors.gold,
                                    indicatorColor = villageColors.goldContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        APP_TABS.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            val label = stringResource(tab.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag(tab.testTag),
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = villageColors.gold,
                                    selectedTextColor = villageColors.gold,
                                    indicatorColor = villageColors.goldContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp)
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = ROUTE_DASHBOARD,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable(ROUTE_DASHBOARD) {
                                DashboardScreen(
                                    uiState = uiState,
                                    onNavigateToTrade = { symbol ->
                                        symbol?.let { viewModel.selectSymbol(it) }
                                        navigateToTab(ROUTE_TRADE)
                                    },
                                    onNavigateToPerformance = { navigateToTab(ROUTE_PERFORMANCE) },
                                    onOpenWelcome = { viewModel.openWelcomeMessage() },
                                    onDirectMessageTelegram = {
                                        TelegramLauncher.openDirectMessage(
                                            context = context,
                                            handle = uiState.telegramHandle,
                                            prefilledMessage = "Namaste Niraj The Trader! I just started on The Village Traders $500 demo account."
                                        )
                                    },
                                    onClosePosition = { pos -> viewModel.closePosition(pos) }
                                )
                            }

                            composable(ROUTE_TRADE) {
                                TradeScreen(
                                    uiState = uiState,
                                    onSelectSymbol = { viewModel.selectSymbol(it) },
                                    onSelectTimeframe = { viewModel.selectTimeframe(it) },
                                    onPlaceOrder = { side, margin, lev, tp, sl ->
                                        viewModel.placeOrder(side, margin, lev, tp, sl)
                                    },
                                    onClosePosition = { viewModel.closePosition(it) },
                                    onUpdateTpSl = { id, tp, sl -> viewModel.updateTpSl(id, tp, sl) }
                                )
                            }

                            composable(ROUTE_PERFORMANCE) {
                                PerformanceScreen(
                                    uiState = uiState,
                                    onShareTradeOnTelegram = { msg ->
                                        TelegramLauncher.openDirectMessage(
                                            context = context,
                                            handle = uiState.telegramHandle,
                                            prefilledMessage = msg
                                        )
                                    }
                                )
                            }

                            composable(ROUTE_CENTER) {
                                TraderCenterScreen(
                                    uiState = uiState,
                                    onDirectMessageTelegram = { msg ->
                                        TelegramLauncher.openDirectMessage(
                                            context = context,
                                            handle = uiState.telegramHandle,
                                            prefilledMessage = msg
                                        )
                                    },
                                    onOpenWelcome = { viewModel.openWelcomeMessage() },
                                    onUpdateTelegramHandle = { viewModel.updateTelegramHandle(it) },
                                    onSwitchRegion = { viewModel.switchRegion(it) },
                                    onResetDemoAccount = { viewModel.resetDemoAccount() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWelcome) {
        NirajWelcomeDialog(
            onStartTrading = { viewModel.dismissWelcome() },
            onMessageTelegram = {
                viewModel.dismissWelcome()
                TelegramLauncher.openDirectMessage(
                    context = context,
                    handle = uiState.telegramHandle,
                    prefilledMessage = "Namaste Niraj The Trader! Thank you for welcoming me to The Village Traders."
                )
            }
        )
    }

    if (showSupportSheet) {
        SupportQuickBottomSheet(
            telegramHandle = uiState.telegramHandle,
            region = uiState.region,
            onDismiss = { showSupportSheet = false },
            onOpenCenterTab = { navigateToTab(ROUTE_CENTER) },
            onDirectMessageTelegram = { msg ->
                TelegramLauncher.openDirectMessage(
                    context = context,
                    handle = uiState.telegramHandle,
                    prefilledMessage = msg
                )
            },
            onSwitchRegion = { viewModel.switchRegion(it) }
        )
    }
}
