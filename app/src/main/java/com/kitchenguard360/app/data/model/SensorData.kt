package com.kitchenguard360.app.data.model

data class SensorData(
    val gasPpm: Int = 0,               // MQ-6 LPG Gas level
    val smokePpm: Int = 0,              // MQ-2 Smoke level
    val temperature: Float = 0.0f,      // DHT11 Temperature in °C
    val humidity: Float = 0.0f,         // DHT11 Humidity in %
    val flameDetected: Boolean = false, // Flame sensor (true = flame present)
    val flameRaw: Int = 0,               // Flame raw ADC value (e.g. 2500)
    val greenLed: Boolean = false,       // Green LED status
    val yellowLed: Boolean = false,      // Yellow LED status
    val redLed: Boolean = false,         // Red LED status
    val buzzer: Boolean = false,         // Buzzer status
    val isConnected: Boolean = false,    // Simple connection flag
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val connectionMode: ConnectionMode = ConnectionMode.WIFI,
    val lastUpdatedTime: String = "Not updated",
    val sensorFaultDetails: String? = null
)
