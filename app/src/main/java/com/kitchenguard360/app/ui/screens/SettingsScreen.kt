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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitchenguard360.app.data.model.SafetyThresholds
import com.kitchenguard360.app.ui.theme.SafeGreen
import java.util.Locale

@Composable
fun SettingsScreen(
    isConnected: Boolean,
    thresholds: SafetyThresholds,
    notificationsEnabled: Boolean,
    onUpdateThresholds: (SafetyThresholds) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onTestConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    var gasWarning by remember(thresholds.gasWarningPpm) { mutableFloatStateOf(thresholds.gasWarningPpm.toFloat()) }
    var gasDanger by remember(thresholds.gasDangerPpm) { mutableFloatStateOf(thresholds.gasDangerPpm.toFloat()) }

    var smokeWarning by remember(thresholds.smokeWarningPpm) { mutableFloatStateOf(thresholds.smokeWarningPpm.toFloat()) }
    var smokeDanger by remember(thresholds.smokeDangerPpm) { mutableFloatStateOf(thresholds.smokeDangerPpm.toFloat()) }

    var tempWarning by remember(thresholds.tempWarningC) { mutableFloatStateOf(thresholds.tempWarningC) }
    var tempDanger by remember(thresholds.tempDangerC) { mutableFloatStateOf(thresholds.tempDangerC) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "App & Device Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Hardware diagnostics, notifications & safety parameters",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. ESP32 Hardware Status Quick Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Router,
                        contentDescription = "ESP32 Target",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "ESP32 Target Hardware",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isConnected) SafeGreen else MaterialTheme.colorScheme.error)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isConnected) "Connected" else "Disconnected",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Hardware Target: Active",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onTestConnection,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Test Connection")
                    }
                }
            }
        }

        // 2. Safety Alerts & Push Notifications
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Safety Alerts & Push Notifications",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Notify immediately on warning or danger events",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = onToggleNotifications
                    )
                }
            }
        }

        // 3. Safety Sensor Thresholds Sliders
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = "Thresholds",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Safety Sensor Thresholds",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // LPG Gas Threshold
                Text(
                    text = "LPG Gas Warning: ${gasWarning.toInt()} PPM | Danger: ${gasDanger.toInt()} PPM",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Slider(
                    value = gasDanger,
                    onValueChange = {
                        gasDanger = it
                        gasWarning = (it * 0.5f).coerceAtLeast(150f)
                        onUpdateThresholds(
                            thresholds.copy(
                                gasWarningPpm = gasWarning.toInt(),
                                gasDangerPpm = gasDanger.toInt()
                            )
                        )
                    },
                    valueRange = 300f..1000f
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Smoke Threshold
                Text(
                    text = "Smoke Warning: ${smokeWarning.toInt()} PPM | Danger: ${smokeDanger.toInt()} PPM",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Slider(
                    value = smokeDanger,
                    onValueChange = {
                        smokeDanger = it
                        smokeWarning = (it * 0.45f).coerceAtLeast(100f)
                        onUpdateThresholds(
                            thresholds.copy(
                                smokeWarningPpm = smokeWarning.toInt(),
                                smokeDangerPpm = smokeDanger.toInt()
                            )
                        )
                    },
                    valueRange = 250f..800f
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Temperature Threshold
                Text(
                    text = "Temp Warning: ${String.format(Locale.US, "%.1f", tempWarning)}°C | Danger: ${String.format(Locale.US, "%.1f", tempDanger)}°C",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Slider(
                    value = tempDanger,
                    onValueChange = {
                        tempDanger = it
                        tempWarning = (it - 10f).coerceAtLeast(30f)
                        onUpdateThresholds(
                            thresholds.copy(
                                tempWarningC = tempWarning,
                                tempDangerC = tempDanger
                            )
                        )
                    },
                    valueRange = 40f..80f
                )
            }
        }

        // 4. About KitchenGuard360 & Hardware Specifications Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "About",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "About KitchenGuard360",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Version 1.0.0 (Production Edition)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Hardware Integration Specs:\n" +
                            "• Microcontroller: ESP32 NodeMCU Wi-Fi & Bluetooth\n" +
                            "• Gas Sensor: MQ-6 LPG Gas Sensor\n" +
                            "• Smoke Sensor: MQ-2 Smoke & Gas Sensor\n" +
                            "• Climate Sensor: DHT11 Temp & Humidity\n" +
                            "• Flame Sensor: Infrared Flame Detector\n" +
                            "• Actuators: Tri-color Status LEDs (Green, Yellow, Red) & Active 5V Buzzer",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
