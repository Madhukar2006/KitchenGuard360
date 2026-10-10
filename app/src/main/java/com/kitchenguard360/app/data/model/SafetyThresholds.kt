package com.kitchenguard360.app.data.model

data class SafetyThresholds(
    val gasWarningPpm: Int = 300,
    val gasDangerPpm: Int = 600,
    val smokeWarningPpm: Int = 200,
    val smokeDangerPpm: Int = 450,
    val tempWarningC: Float = 40.5f,
    val tempDangerC: Float = 50.5f
)
