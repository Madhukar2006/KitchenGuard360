package com.kitchenguard360.app.data.model

data class KitchenStatus(
    val device: String?,
    val state: String?,
    val mq6Raw: Int?,
    val mq2Raw: Int?,
    val flameDetected: Boolean?,
    val temperatureC: Double?,
    val humidityPercent: Double?,
    val mq6Warning: Int?,
    val mq6Danger: Int?,
    val mq2Warning: Int?,
    val mq2Danger: Int?,
    val temperatureWarning: Double?,
    val temperatureDanger: Double?,
    val uptimeSeconds: Long?,
    val receivedAt: Long = System.currentTimeMillis()
)

object KitchenStatusParser {
    fun parseJson(rawString: String?): KitchenStatus? {
        if (rawString.isNullOrBlank()) return null
        val trimmed = rawString.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return null

        return try {
            val device = extractString(trimmed, "device")
            if (device == null || !device.equals("KitchenGuard360", ignoreCase = true)) {
                return null
            }

            val state = extractString(trimmed, "state")?.uppercase() ?: "NORMAL"
            val mq6Raw = extractInt(trimmed, "mq6_raw") ?: extractInt(trimmed, "lpg")
            val mq2Raw = extractInt(trimmed, "mq2_raw") ?: extractInt(trimmed, "smoke")
            val flameDetected = extractBoolean(trimmed, "flame_detected") ?: extractBoolean(trimmed, "flame")
            val temperatureC = extractDouble(trimmed, "temperature_c") ?: extractDouble(trimmed, "temperature")
            val humidityPercent = extractDouble(trimmed, "humidity_percent") ?: extractDouble(trimmed, "humidity")
            val mq6Warning = extractInt(trimmed, "mq6_warning")
            val mq6Danger = extractInt(trimmed, "mq6_danger")
            val mq2Warning = extractInt(trimmed, "mq2_warning")
            val mq2Danger = extractInt(trimmed, "mq2_danger")
            val temperatureWarning = extractDouble(trimmed, "temperature_warning")
            val temperatureDanger = extractDouble(trimmed, "temperature_danger")
            val uptimeSeconds = extractLong(trimmed, "uptime_seconds")

            KitchenStatus(
                device = device,
                state = state,
                mq6Raw = mq6Raw,
                mq2Raw = mq2Raw,
                flameDetected = flameDetected,
                temperatureC = temperatureC,
                humidityPercent = humidityPercent,
                mq6Warning = mq6Warning,
                mq6Danger = mq6Danger,
                mq2Warning = mq2Warning,
                mq2Danger = mq2Danger,
                temperatureWarning = temperatureWarning,
                temperatureDanger = temperatureDanger,
                uptimeSeconds = uptimeSeconds,
                receivedAt = System.currentTimeMillis()
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun extractString(json: String, key: String): String? {
        val pattern = Regex(""""$key"\s*:\s*"([^"]*)"""")
        val match = pattern.find(json) ?: return null
        val value = match.groupValues[1]
        return if (value.equals("null", ignoreCase = true)) null else value
    }

    private fun extractInt(json: String, key: String): Int? {
        val pattern = Regex(""""$key"\s*:\s*(-?\d+)""")
        val match = pattern.find(json) ?: return null
        return match.groupValues[1].toIntOrNull()
    }

    private fun extractDouble(json: String, key: String): Double? {
        val pattern = Regex(""""$key"\s*:\s*(-?\d+(?:\.\d+)?)""")
        val match = pattern.find(json) ?: return null
        return match.groupValues[1].toDoubleOrNull()
    }

    private fun extractLong(json: String, key: String): Long? {
        val pattern = Regex(""""$key"\s*:\s*(-?\d+)""")
        val match = pattern.find(json) ?: return null
        return match.groupValues[1].toLongOrNull()
    }

    private fun extractBoolean(json: String, key: String): Boolean? {
        val boolPattern = Regex(""""$key"\s*:\s*(true|false)""", RegexOption.IGNORE_CASE)
        val boolMatch = boolPattern.find(json)
        if (boolMatch != null) {
            return boolMatch.groupValues[1].lowercase() == "true"
        }
        val intPattern = Regex(""""$key"\s*:\s*(\d+)""")
        val intMatch = intPattern.find(json)
        if (intMatch != null) {
            val num = intMatch.groupValues[1].toIntOrNull()
            if (num != null) return num > 2000 || num == 1
        }
        return null
    }
}
