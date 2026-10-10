package com.kitchenguard360.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomNavDestination(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    DASHBOARD("dashboard", "Dashboard", Icons.Default.Dashboard),
    SENSORS("sensors", "Sensors", Icons.Default.Sensors),
    CONNECTIVITY("connectivity", "Connect", Icons.Default.BluetoothSearching),
    ALERTS("alerts", "Alerts", Icons.Default.Notifications),
    SETTINGS("settings", "Settings", Icons.Default.Settings)
}
