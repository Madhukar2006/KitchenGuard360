package com.kitchenguard360.app.data.model

data class SafetyAlert(
    val id: String,
    val title: String,
    val message: String,
    val severity: SafetyLevel,
    val timestamp: String,
    val sensorType: String,
    val recommendedAction: String
)
