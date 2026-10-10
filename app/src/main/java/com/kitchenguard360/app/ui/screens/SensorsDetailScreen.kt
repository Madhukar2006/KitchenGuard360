package com.kitchenguard360.app.ui.screens

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
import androidx.compose.material.icons.filled.Co2
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitchenguard360.app.data.model.ConnectionMode
import com.kitchenguard360.app.data.model.ConnectionStatus
import com.kitchenguard360.app.data.model.SafetyLevel
import com.kitchenguard360.app.data.model.SafetyThresholds
import com.kitchenguard360.app.data.model.SensorData
import com.kitchenguard360.app.ui.components.SensorCard
import com.kitchenguard360.app.ui.components.StatusBadge
import com.kitchenguard360.app.ui.theme.DangerRed
import com.kitchenguard360.app.ui.theme.SafeGreen
import com.kitchenguard360.app.ui.theme.WarningAmber
import java.util.Locale

@Composable
fun SensorsDetailScreen(
    sensorData: SensorData,
    thresholds: SafetyThresholds,
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
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Live Sensor Telemetry",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Real-time hardware readings from ESP32",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = when (sensorData.connectionStatus) {
                    ConnectionStatus.CONNECTED -> SafeGreen.copy(alpha = 0.15f)
                    ConnectionStatus.CONNECTING -> WarningAmber.copy(alpha = 0.15f)
                    else -> DangerRed.copy(alpha = 0.15f)
                }
            ) {
                Text(
                    text = sensorData.connectionStatus.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (sensorData.connectionStatus) {
                        ConnectionStatus.CONNECTED -> SafeGreen
                        ConnectionStatus.CONNECTING -> WarningAmber
                        else -> DangerRed
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // Connection Summary Pill
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = "Sensors",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mode: ${sensorData.connectionMode.displayName}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "Updated: ${sensorData.lastUpdatedTime}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }

        // 1. LPG Gas Sensor (MQ-6)
        val gasLevel = when {
            sensorData.gasPpm >= thresholds.gasDangerPpm -> SafetyLevel.DANGER
            sensorData.gasPpm >= thresholds.gasWarningPpm -> SafetyLevel.WARNING
            else -> SafetyLevel.SAFE
        }
        SensorCard(
            title = "MQ-6 LPG Gas Sensor",
            subtitle = "Liquefied Petroleum Gas Concentration",
            valueText = "${sensorData.gasPpm}",
            unit = "PPM",
            icon = Icons.Default.Co2,
            statusLevel = gasLevel,
            progressFraction = sensorData.gasPpm.toFloat() / (thresholds.gasDangerPpm * 1.3f),
            thresholdInfo = "Warning Threshold: >${thresholds.gasWarningPpm} PPM | Danger Threshold: >${thresholds.gasDangerPpm} PPM"
        )

        // 2. Smoke Sensor (MQ-2)
        val smokeLevel = when {
            sensorData.smokePpm >= thresholds.smokeDangerPpm -> SafetyLevel.DANGER
            sensorData.smokePpm >= thresholds.smokeWarningPpm -> SafetyLevel.WARNING
            else -> SafetyLevel.SAFE
        }
        SensorCard(
            title = "MQ-2 Smoke & Gas Sensor",
            subtitle = "Smoke Particles & Combustible Gases",
            valueText = "${sensorData.smokePpm}",
            unit = "PPM",
            icon = Icons.Default.Air,
            statusLevel = smokeLevel,
            progressFraction = sensorData.smokePpm.toFloat() / (thresholds.smokeDangerPpm * 1.3f),
            thresholdInfo = "Warning Threshold: >${thresholds.smokeWarningPpm} PPM | Danger Threshold: >${thresholds.smokeDangerPpm} PPM"
        )

        // 3. Temperature Sensor (DHT11)
        val tempLevel = when {
            sensorData.temperature >= thresholds.tempDangerC -> SafetyLevel.DANGER
            sensorData.temperature >= thresholds.tempWarningC -> SafetyLevel.WARNING
            else -> SafetyLevel.SAFE
        }
        SensorCard(
            title = "DHT11 Temperature Sensor",
            subtitle = "Kitchen Ambient Air Temperature",
            valueText = String.format(Locale.US, "%.1f", sensorData.temperature),
            unit = "°C",
            icon = Icons.Default.DeviceThermostat,
            statusLevel = tempLevel,
            progressFraction = sensorData.temperature / (thresholds.tempDangerC * 1.3f),
            thresholdInfo = "Warning Threshold: >${thresholds.tempWarningC}°C | Danger Threshold: >${thresholds.tempDangerC}°C"
        )

        // 4. Humidity Sensor (DHT11)
        SensorCard(
            title = "DHT11 Humidity Sensor",
            subtitle = "Relative Air Moisture",
            valueText = String.format(Locale.US, "%.1f", sensorData.humidity),
            unit = "% RH",
            icon = Icons.Default.WaterDrop,
            statusLevel = SafetyLevel.SAFE,
            progressFraction = sensorData.humidity / 100f,
            thresholdInfo = "Optimal Indoor Humidity Range: 30% - 70% RH"
        )

        // 5. Infrared Flame Sensor
        val flameLevel = if (sensorData.flameDetected) SafetyLevel.DANGER else SafetyLevel.SAFE
        SensorCard(
            title = "Infrared Flame Sensor",
            subtitle = if (sensorData.flameRaw > 0) "Raw ADC: ${sensorData.flameRaw} | IR Fire Optics" else "Direct Optical Fire Detector",
            valueText = if (sensorData.flameDetected) "FLAME DETECTED!" else "NO FLAME",
            unit = "",
            icon = Icons.Default.LocalFireDepartment,
            statusLevel = flameLevel,
            progressFraction = if (sensorData.flameDetected) 1.0f else 0.0f,
            thresholdInfo = "Instant Optical Trigger on Open Flame"
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
