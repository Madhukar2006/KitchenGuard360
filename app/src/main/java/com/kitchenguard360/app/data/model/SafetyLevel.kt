package com.kitchenguard360.app.data.model

enum class SafetyLevel(val label: String, val description: String) {
    SAFE(
        label = "SAFE",
        description = "All kitchen safety parameters are normal. No active threats detected."
    ),
    WARNING(
        label = "WARNING",
        description = "Sensor levels approaching danger thresholds. Caution advised."
    ),
    DANGER(
        label = "DANGER",
        description = "Hazardous condition detected! Flame, gas leak, or high smoke level!"
    ),
    SENSOR_FAULT(
        label = "SENSOR FAULT",
        description = "Hardware fault detected or sensor reading out of range. Check ESP32 wiring."
    ),
    OFFLINE(
        label = "OFFLINE",
        description = "ESP32 hardware is disconnected or unreachable. No live sensor data available."
    )
}
