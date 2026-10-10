package com.kitchenguard360.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitchenguard360.app.data.model.SafetyLevel
import com.kitchenguard360.app.ui.screens.AlertsScreen
import com.kitchenguard360.app.ui.screens.ConnectivityScreen
import com.kitchenguard360.app.ui.screens.DashboardScreen
import com.kitchenguard360.app.ui.screens.SensorsDetailScreen
import com.kitchenguard360.app.ui.screens.SettingsScreen
import com.kitchenguard360.app.ui.theme.DangerRed
import com.kitchenguard360.app.ui.viewmodel.MainViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(factory = MainViewModel.provideFactory())
) {
    var selectedDestination by remember { mutableStateOf(BottomNavDestination.DASHBOARD) }

    val sensorData by viewModel.sensorData.collectAsState()
    val safetyLevel by viewModel.safetyLevel.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val thresholds by viewModel.thresholds.collectAsState()
    val esp32Ip by viewModel.esp32Ip.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val connectionMode by viewModel.connectionMode.collectAsState()
    val wifiConnectMode by viewModel.wifiConnectMode.collectAsState()
    val connectionError by viewModel.connectionError.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val btPairedDevices by viewModel.btPairedDevices.collectAsState()
    val btConnectedDeviceName by viewModel.btConnectedDeviceName.collectAsState()

    val activeAlertCount = alerts.count { it.severity != SafetyLevel.SAFE }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                BottomNavDestination.entries.forEach { destination ->
                    val selected = destination == selectedDestination
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedDestination = destination },
                        icon = {
                            if (destination == BottomNavDestination.ALERTS && activeAlertCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = DangerRed,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        ) {
                                            Text(
                                                text = "$activeAlertCount",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.title
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.title
                                )
                            }
                        },
                        label = {
                            Text(
                                text = destination.title,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedDestination) {
                BottomNavDestination.DASHBOARD -> DashboardScreen(
                    sensorData = sensorData,
                    safetyLevel = safetyLevel,
                    thresholds = thresholds,
                    onRefresh = { viewModel.refreshData() }
                )
                BottomNavDestination.SENSORS -> SensorsDetailScreen(
                    sensorData = sensorData,
                    thresholds = thresholds
                )
                BottomNavDestination.CONNECTIVITY -> ConnectivityScreen(
                    savedEsp32Ip = esp32Ip,
                    connectionStatus = connectionStatus,
                    connectionMode = connectionMode,
                    wifiConnectMode = wifiConnectMode,
                    connectionError = connectionError,
                    btPairedDevices = btPairedDevices,
                    btConnectedDeviceName = btConnectedDeviceName,
                    onSetConnectionMode = { mode -> viewModel.setConnectionMode(mode) },
                    onSetWifiConnectMode = { mode -> viewModel.setWifiConnectMode(mode) },
                    onConnectManualIp = { ip -> viewModel.connectManualIp(ip) },
                    onAutoDiscoverWifi = { viewModel.autoDiscoverWifi() },
                    onRefreshBtDevices = { viewModel.refreshBluetoothDevices() },
                    onConnectBtDevice = { device -> viewModel.connectBluetoothDevice(device) },
                    onDisconnectBt = { viewModel.disconnectBluetooth() }
                )
                BottomNavDestination.ALERTS -> AlertsScreen(
                    alerts = alerts,
                    onClearAlerts = { viewModel.clearAlerts() }
                )
                BottomNavDestination.SETTINGS -> SettingsScreen(
                    isConnected = isConnected,
                    thresholds = thresholds,
                    notificationsEnabled = notificationsEnabled,
                    onUpdateThresholds = { t -> viewModel.updateThresholds(t) },
                    onToggleNotifications = { enabled -> viewModel.setNotificationsEnabled(enabled) },
                    onTestConnection = { viewModel.testConnection() }
                )
            }
        }
    }
}
