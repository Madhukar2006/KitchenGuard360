package com.kitchenguard360.app.data.model

data class SensorHistoryEntry(
    val id: String,
    val timestamp: String,
    val safetyLevel: SafetyLevel,
    val gasPpm: Int,
    val smokePpm: Int,
    val temperature: Float,
    val humidity: Float,
    val flameDetected: Boolean,
    val summary: String
)
