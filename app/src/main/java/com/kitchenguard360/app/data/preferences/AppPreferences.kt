package com.kitchenguard360.app.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.kitchenguard360.app.KitchenGuardApplication
import com.kitchenguard360.app.data.model.ConnectionMode
import com.kitchenguard360.app.data.model.SafetyThresholds

class AppPreferences(context: Context = KitchenGuardApplication.instance) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("kitchen_guard_settings", Context.MODE_PRIVATE)

    var esp32Ip: String
        get() = prefs.getString(KEY_ESP32_IP, "10.224.137.26") ?: "10.224.137.26"
        set(value) = prefs.edit().putString(KEY_ESP32_IP, value.trim()).apply()

    var connectionMode: ConnectionMode
        get() {
            val modeStr = prefs.getString(KEY_CONNECTION_MODE, ConnectionMode.WIFI.name)
            return try {
                ConnectionMode.valueOf(modeStr ?: ConnectionMode.WIFI.name)
            } catch (_: Exception) {
                ConnectionMode.WIFI
            }
        }
        set(value) = prefs.edit().putString(KEY_CONNECTION_MODE, value.name).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

    var thresholds: SafetyThresholds
        get() = SafetyThresholds(
            gasWarningPpm = prefs.getInt(KEY_GAS_WARN, 300),
            gasDangerPpm = prefs.getInt(KEY_GAS_DANGER, 600),
            smokeWarningPpm = prefs.getInt(KEY_SMOKE_WARN, 200),
            smokeDangerPpm = prefs.getInt(KEY_SMOKE_DANGER, 450),
            tempWarningC = prefs.getFloat(KEY_TEMP_WARN, 40.5f),
            tempDangerC = prefs.getFloat(KEY_TEMP_DANGER, 50.5f)
        )
        set(value) {
            prefs.edit()
                .putInt(KEY_GAS_WARN, value.gasWarningPpm)
                .putInt(KEY_GAS_DANGER, value.gasDangerPpm)
                .putInt(KEY_SMOKE_WARN, value.smokeWarningPpm)
                .putInt(KEY_SMOKE_DANGER, value.smokeDangerPpm)
                .putFloat(KEY_TEMP_WARN, value.tempWarningC)
                .putFloat(KEY_TEMP_DANGER, value.tempDangerC)
                .apply()
        }

    companion object {
        private const val KEY_ESP32_IP = "esp32_ip"
        private const val KEY_CONNECTION_MODE = "connection_mode"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_GAS_WARN = "gas_warn"
        private const val KEY_GAS_DANGER = "gas_danger"
        private const val KEY_SMOKE_WARN = "smoke_warn"
        private const val KEY_SMOKE_DANGER = "smoke_danger"
        private const val KEY_TEMP_WARN = "temp_warn"
        private const val KEY_TEMP_DANGER = "temp_danger"
    }
}
