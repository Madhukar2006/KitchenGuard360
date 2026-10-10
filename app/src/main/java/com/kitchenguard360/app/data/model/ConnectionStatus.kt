package com.kitchenguard360.app.data.model

enum class ConnectionStatus(val label: String) {
    DISCONNECTED("Disconnected"),
    CONNECTING("Connecting..."),
    CONNECTED("Connected"),
    SENSOR_FAULT("Sensor Fault")
}
