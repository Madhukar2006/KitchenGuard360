package com.kitchenguard360.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Co2
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitchenguard360.app.R
import com.kitchenguard360.app.data.model.ConnectionMode
import com.kitchenguard360.app.data.model.ConnectionStatus
import com.kitchenguard360.app.data.model.SafetyLevel
import com.kitchenguard360.app.data.model.SafetyThresholds
import com.kitchenguard360.app.data.model.SensorData
import com.kitchenguard360.app.ui.components.ActuatorCard
import com.kitchenguard360.app.ui.components.SensorCard
import com.kitchenguard360.app.ui.components.StatusBadge
import com.kitchenguard360.app.ui.theme.DangerRed
import com.kitchenguard360.app.ui.theme.DangerRedContainer
import com.kitchenguard360.app.ui.theme.OnDangerRedContainer
import com.kitchenguard360.app.ui.theme.OnSafeGreenContainer
import com.kitchenguard360.app.ui.theme.OnWarningAmberContainer
import com.kitchenguard360.app.ui.theme.SafeGreen
import com.kitchenguard360.app.ui.theme.SafeGreenContainer
import com.kitchenguard360.app.ui.theme.WarningAmber
import com.kitchenguard360.app.ui.theme.WarningAmberContainer
import java.util.Locale

private data class OverallStatusConfig(
    val bgColor: Color,
    val textColor: Color,
    val titleColor: Color,
    val icon: ImageVector,
    val statusText: String
)

@Composable
fun DashboardScreen(
    sensorData: SensorData,
    safetyLevel: SafetyLevel,
    thresholds: SafetyThresholds,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DashboardHeader(
            connectionStatus = sensorData.connectionStatus,
            connectionMode = sensorData.connectionMode,
            lastUpdated = sensorData.lastUpdatedTime,
            onRefresh = onRefresh
        )

        OverallStatusCard(
            safetyLevel = if (sensorData.isConnected) safetyLevel else SafetyLevel.OFFLINE,
            sensorData = sensorData,
            thresholds = thresholds
        )

        ActuatorCard(
            greenLed = sensorData.greenLed,
            yellowLed = sensorData.yellowLed,
            redLed = sensorData.redLed,
            buzzer = sensorData.buzzer
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Live Sensor Telemetry",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "5 Monitored Signals",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val gasLevel = when {
            !sensorData.isConnected -> SafetyLevel.OFFLINE
            sensorData.gasPpm >= thresholds.gasDangerPpm -> SafetyLevel.DANGER
            sensorData.gasPpm >= thresholds.gasWarningPpm -> SafetyLevel.WARNING
            else -> SafetyLevel.SAFE
        }
        SensorCard(
            title = "MQ-6 LPG Gas Sensor",
            subtitle = "Liquefied Petroleum Gas Level",
            valueText = if (sensorData.isConnected) "${sensorData.gasPpm}" else "--",
            unit = if (sensorData.isConnected) "PPM" else "",
            icon = Icons.Default.Co2,
            statusLevel = gasLevel,
            progressFraction = if (sensorData.isConnected) sensorData.gasPpm.toFloat() / (thresholds.gasDangerPpm * 1.3f) else 0f,
            thresholdInfo = "Warning: >${thresholds.gasWarningPpm} PPM | Danger: >${thresholds.gasDangerPpm} PPM"
        )

        val smokeLevel = when {
            !sensorData.isConnected -> SafetyLevel.OFFLINE
            sensorData.smokePpm >= thresholds.smokeDangerPpm -> SafetyLevel.DANGER
            sensorData.smokePpm >= thresholds.smokeWarningPpm -> SafetyLevel.WARNING
            else -> SafetyLevel.SAFE
        }
        SensorCard(
            title = "MQ-2 Smoke Sensor",
            subtitle = "Smoke & Combustion Particles",
            valueText = if (sensorData.isConnected) "${sensorData.smokePpm}" else "--",
            unit = if (sensorData.isConnected) "PPM" else "",
            icon = Icons.Default.Air,
            statusLevel = smokeLevel,
            progressFraction = if (sensorData.isConnected) sensorData.smokePpm.toFloat() / (thresholds.smokeDangerPpm * 1.3f) else 0f,
            thresholdInfo = "Warning: >${thresholds.smokeWarningPpm} PPM | Danger: >${thresholds.smokeDangerPpm} PPM"
        )

        val tempLevel = when {
            !sensorData.isConnected -> SafetyLevel.OFFLINE
            sensorData.temperature >= thresholds.tempDangerC -> SafetyLevel.DANGER
            sensorData.temperature >= thresholds.tempWarningC -> SafetyLevel.WARNING
            else -> SafetyLevel.SAFE
        }
        SensorCard(
            title = "DHT11 Temperature Sensor",
            subtitle = "Kitchen Ambient Heat",
            valueText = if (sensorData.isConnected) String.format(Locale.US, "%.1f", sensorData.temperature) else "--",
            unit = if (sensorData.isConnected) "°C" else "",
            icon = Icons.Default.DeviceThermostat,
            statusLevel = tempLevel,
            progressFraction = if (sensorData.isConnected) sensorData.temperature / (thresholds.tempDangerC * 1.3f) else 0f,
            thresholdInfo = "Warning: >${thresholds.tempWarningC}°C | Danger: >${thresholds.tempDangerC}°C"
        )

        SensorCard(
            title = "DHT11 Humidity Sensor",
            subtitle = "Relative Air Moisture",
            valueText = if (sensorData.isConnected) String.format(Locale.US, "%.1f", sensorData.humidity) else "--",
            unit = if (sensorData.isConnected) "% RH" else "",
            icon = Icons.Default.WaterDrop,
            statusLevel = if (sensorData.isConnected) SafetyLevel.SAFE else SafetyLevel.OFFLINE,
            progressFraction = if (sensorData.isConnected) sensorData.humidity / 100f else 0f,
            thresholdInfo = "Normal Range: 30% - 70% RH"
        )

        val flameLevel = when {
            !sensorData.isConnected -> SafetyLevel.OFFLINE
            sensorData.flameDetected -> SafetyLevel.DANGER
            else -> SafetyLevel.SAFE
        }
        SensorCard(
            title = "Infrared Flame Sensor",
            subtitle = "Direct Fire Optical Detection",
            valueText = when {
                !sensorData.isConnected -> "OFFLINE"
                sensorData.flameDetected -> "FLAME DETECTED!"
                else -> "NO FLAME"
            },
            unit = "",
            icon = Icons.Default.LocalFireDepartment,
            statusLevel = flameLevel,
            progressFraction = if (sensorData.isConnected && sensorData.flameDetected) 1.0f else 0.0f,
            thresholdInfo = "Instant IR Trigger on Open Flame"
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DashboardHeader(
    connectionStatus: ConnectionStatus,
    connectionMode: ConnectionMode,
    lastUpdated: String,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_logo),
                        contentDescription = "KitchenGuard360 Logo",
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "KitchenGuard360",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Smart Kitchen Safety Monitor",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reconnect / Refresh",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = when (connectionStatus) {
                        ConnectionStatus.CONNECTED -> SafeGreen.copy(alpha = 0.15f)
                        ConnectionStatus.CONNECTING -> WarningAmber.copy(alpha = 0.15f)
                        else -> DangerRed.copy(alpha = 0.15f)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Router,
                            contentDescription = "ESP32 Mode",
                            tint = when (connectionStatus) {
                                ConnectionStatus.CONNECTED -> SafeGreen
                                ConnectionStatus.CONNECTING -> WarningAmber
                                else -> DangerRed
                            },
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (connectionStatus) {
                                ConnectionStatus.CONNECTED -> "ESP32 Connected (${connectionMode.displayName})"
                                ConnectionStatus.CONNECTING -> "Connecting to ESP32..."
                                ConnectionStatus.SENSOR_FAULT -> "ESP32 Sensor Fault"
                                ConnectionStatus.DISCONNECTED -> "ESP32 Offline"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when (connectionStatus) {
                                ConnectionStatus.CONNECTED -> SafeGreen
                                ConnectionStatus.CONNECTING -> WarningAmber
                                ConnectionStatus.SENSOR_FAULT -> WarningAmber
                                ConnectionStatus.DISCONNECTED -> DangerRed
                            }
                        )
                    }
                }

                Text(
                    text = "Updated $lastUpdated",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OverallStatusCard(
    safetyLevel: SafetyLevel,
    sensorData: SensorData,
    thresholds: SafetyThresholds
) {
    val config = when {
        !sensorData.isConnected || safetyLevel == SafetyLevel.OFFLINE -> OverallStatusConfig(
            DangerRedContainer,
            OnDangerRedContainer,
            DangerRed,
            Icons.Default.WifiOff,
            "ESP32 OFFLINE / DISCONNECTED"
        )
        safetyLevel == SafetyLevel.SAFE -> OverallStatusConfig(
            SafeGreenContainer,
            OnSafeGreenContainer,
            SafeGreen,
            Icons.Default.CheckCircle,
            "ALL SYSTEMS SAFE"
        )
        safetyLevel == SafetyLevel.WARNING -> OverallStatusConfig(
            WarningAmberContainer,
            OnWarningAmberContainer,
            WarningAmber,
            Icons.Default.Warning,
            "SAFETY WARNING ACTIVE"
        )
        safetyLevel == SafetyLevel.DANGER -> OverallStatusConfig(
            DangerRedContainer,
            OnDangerRedContainer,
            DangerRed,
            Icons.Default.Error,
            "CRITICAL HAZARD DETECTED!"
        )
        else -> OverallStatusConfig(
            WarningAmberContainer,
            OnWarningAmberContainer,
            WarningAmber,
            Icons.Default.Warning,
            "HARDWARE SENSOR FAULT"
        )
    }

    val actionNote = when {
        !sensorData.isConnected || safetyLevel == SafetyLevel.OFFLINE -> "ESP32 is disconnected or unreachable. Connect phone to the ESP32 Wi-Fi/Hotspot or Bluetooth."
        sensorData.flameDetected -> "IR Flame sensor triggered open flame! Active Buzzer & Red LED ACTIVE!"
        sensorData.gasPpm >= thresholds.gasDangerPpm -> "Dangerous LPG gas concentration! Turn off main gas valve immediately!"
        sensorData.smokePpm >= thresholds.smokeDangerPpm -> "Heavy smoke detected! Turn on kitchen exhaust ventilation!"
        sensorData.temperature >= thresholds.tempDangerC -> "Kitchen temperature extreme! Turn off stove and ovens!"
        sensorData.gasPpm >= thresholds.gasWarningPpm -> "LPG Gas level approaching threshold."
        sensorData.smokePpm >= thresholds.smokeWarningPpm -> "Smoke level elevated."
        sensorData.temperature >= thresholds.tempWarningC -> "Kitchen temperature elevated."
        else -> "All 5 sensors (MQ-6, MQ-2, DHT11 Temp, DHT11 Humidity, IR Flame) operating within safe parameters."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = config.bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(config.titleColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = config.icon,
                            contentDescription = safetyLevel.name,
                            tint = config.titleColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Overall Status",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = config.textColor.copy(alpha = 0.8f)
                        )
                        Text(
                            text = config.statusText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = config.textColor
                        )
                    }
                }

                StatusBadge(level = if (sensorData.isConnected) safetyLevel else SafetyLevel.OFFLINE)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = actionNote,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp,
                color = config.textColor
            )
        }
    }
}
