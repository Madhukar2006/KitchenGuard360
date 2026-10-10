package com.kitchenguard360.app.data.repository

import android.bluetooth.BluetoothDevice
import com.kitchenguard360.app.bluetooth.BluetoothManager
import com.kitchenguard360.app.bluetooth.BtDeviceItem
import com.kitchenguard360.app.data.model.ConnectionMode
import com.kitchenguard360.app.data.model.ConnectionStatus
import com.kitchenguard360.app.data.model.SafetyAlert
import com.kitchenguard360.app.data.model.SafetyLevel
import com.kitchenguard360.app.data.model.SafetyThresholds
import com.kitchenguard360.app.data.model.SensorData
import com.kitchenguard360.app.data.model.SensorHistoryEntry
import com.kitchenguard360.app.data.model.WifiConnectMode
import com.kitchenguard360.app.data.preferences.AppPreferences
import com.kitchenguard360.app.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

interface KitchenSafetyRepository {
    val sensorData: StateFlow<SensorData>
    val safetyLevel: StateFlow<SafetyLevel>
    val alerts: StateFlow<List<SafetyAlert>>
    val history: StateFlow<List<SensorHistoryEntry>>
    val thresholds: StateFlow<SafetyThresholds>
    val esp32Ip: StateFlow<String>
    val isConnected: StateFlow<Boolean>
    val connectionStatus: StateFlow<ConnectionStatus>
    val connectionMode: StateFlow<ConnectionMode>
    val wifiConnectMode: StateFlow<WifiConnectMode>
    val connectionError: StateFlow<String?>
    val notificationsEnabled: StateFlow<Boolean>
    val btPairedDevices: StateFlow<List<BtDeviceItem>>
    val btConnectedDeviceName: StateFlow<String?>

    fun refreshData()
    fun setEsp32Ip(ip: String)
    fun setConnectionMode(mode: ConnectionMode)
    fun setWifiConnectMode(mode: WifiConnectMode)
    fun connectManualIp(ip: String)
    fun autoDiscoverWifi()
    fun updateThresholds(newThresholds: SafetyThresholds)
    fun setNotificationsEnabled(enabled: Boolean)
    fun clearAlerts()
    fun clearHistory()
    fun testConnection()
    fun refreshBluetoothDevices()
    fun connectBluetoothDevice(device: BluetoothDevice)
    fun disconnectBluetooth()
}

class KitchenSafetyRepositoryImpl(
    private val preferences: AppPreferences = AppPreferences(),
    private val bluetoothManager: BluetoothManager = BluetoothManager(),
    private val notificationHelper: NotificationHelper = NotificationHelper()
) : KitchenSafetyRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val timeFormatter = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
    private val fullDateTimeFormatter = SimpleDateFormat("MMM dd, yyyy - hh:mm:ss a", Locale.getDefault())

    private val _thresholds = MutableStateFlow(preferences.thresholds)
    override val thresholds: StateFlow<SafetyThresholds> = _thresholds.asStateFlow()

    private val _esp32Ip = MutableStateFlow(preferences.esp32Ip)
    override val esp32Ip: StateFlow<String> = _esp32Ip.asStateFlow()

    private val _connectionMode = MutableStateFlow(preferences.connectionMode)
    override val connectionMode: StateFlow<ConnectionMode> = _connectionMode.asStateFlow()

    private val _wifiConnectMode = MutableStateFlow(WifiConnectMode.AUTO)
    override val wifiConnectMode: StateFlow<WifiConnectMode> = _wifiConnectMode.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    override val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    override val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(preferences.notificationsEnabled)
    override val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _sensorData = MutableStateFlow(createInitialData())
    override val sensorData: StateFlow<SensorData> = _sensorData.asStateFlow()

    private val _safetyLevel = MutableStateFlow(SafetyLevel.SAFE)
    override val safetyLevel: StateFlow<SafetyLevel> = _safetyLevel.asStateFlow()

    private val _alerts = MutableStateFlow<List<SafetyAlert>>(createInitialAlerts())
    override val alerts: StateFlow<List<SafetyAlert>> = _alerts.asStateFlow()

    private val _history = MutableStateFlow<List<SensorHistoryEntry>>(createInitialHistory())
    override val history: StateFlow<List<SensorHistoryEntry>> = _history.asStateFlow()

    override val btPairedDevices: StateFlow<List<BtDeviceItem>> = bluetoothManager.pairedDevices
    override val btConnectedDeviceName: StateFlow<String?> = bluetoothManager.connectedDeviceName

    private var previousSafetyLevel: SafetyLevel? = null
    private var previousThreatSummary: String = ""

    init {
        // Bluetooth stream listener
        repositoryScope.launch {
            bluetoothManager.latestJsonData.collect { jsonStr ->
                if (_connectionMode.value == ConnectionMode.BLUETOOTH && !jsonStr.isNullOrEmpty()) {
                    try {
                        val json = JSONObject(jsonStr)
                        val parsed = parseEsp32SensorJson(json)
                        if (parsed != null) {
                            _isConnected.value = true
                            _connectionStatus.value = ConnectionStatus.CONNECTED
                            _connectionError.value = null
                            updateSensorDataWithCalculatedSafety(parsed)
                        } else {
                            _connectionError.value = "Malformed JSON payload from Bluetooth device."
                        }
                    } catch (e: Exception) {
                        _connectionError.value = "Bluetooth JSON parse error: ${e.localizedMessage}"
                    }
                }
            }
        }

        repositoryScope.launch {
            bluetoothManager.connectionStatus.collect { btStatus ->
                if (_connectionMode.value == ConnectionMode.BLUETOOTH) {
                    _connectionStatus.value = btStatus
                    _isConnected.value = (btStatus == ConnectionStatus.CONNECTED)
                    if (btStatus == ConnectionStatus.DISCONNECTED) {
                        _sensorData.value = _sensorData.value.copy(isConnected = false, connectionStatus = ConnectionStatus.DISCONNECTED)
                    }
                }
            }
        }

        // Background polling loop for Wi-Fi HTTP API
        repositoryScope.launch {
            while (true) {
                if (_connectionMode.value == ConnectionMode.WIFI) {
                    fetchFromEsp32Hardware()
                }
                delay(2000)
            }
        }
    }

    override fun refreshData() {
        repositoryScope.launch {
            if (_connectionMode.value == ConnectionMode.WIFI) {
                fetchFromEsp32Hardware()
            } else {
                bluetoothManager.refreshPairedDevices()
            }
        }
    }

    override fun setEsp32Ip(ip: String) {
        val cleanIp = ip.trim()
        _esp32Ip.value = cleanIp
        preferences.esp32Ip = cleanIp
    }

    override fun setConnectionMode(mode: ConnectionMode) {
        _connectionMode.value = mode
        preferences.connectionMode = mode
        _connectionError.value = null
        if (mode == ConnectionMode.WIFI) {
            bluetoothManager.disconnect()
            repositoryScope.launch {
                fetchFromEsp32Hardware()
            }
        } else {
            _isConnected.value = false
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
            _sensorData.value = _sensorData.value.copy(isConnected = false, connectionStatus = ConnectionStatus.DISCONNECTED)
            bluetoothManager.refreshPairedDevices()
        }
    }

    override fun setWifiConnectMode(mode: WifiConnectMode) {
        _wifiConnectMode.value = mode
        _connectionError.value = null
    }

    override fun connectManualIp(ip: String) {
        val cleanIp = ip.trim()
        if (!isValidIpv4(cleanIp)) {
            _connectionError.value = "Invalid IPv4 format. Expected format e.g. 10.224.137.26"
            return
        }

        _esp32Ip.value = cleanIp
        preferences.esp32Ip = cleanIp
        _wifiConnectMode.value = WifiConnectMode.MANUAL_IP
        _connectionError.value = null

        repositoryScope.launch {
            fetchFromEsp32Hardware()
        }
    }

    override fun autoDiscoverWifi() {
        _wifiConnectMode.value = WifiConnectMode.AUTO
        _connectionError.value = null
        repositoryScope.launch {
            fetchFromEsp32Hardware()
        }
    }

    override fun updateThresholds(newThresholds: SafetyThresholds) {
        _thresholds.value = newThresholds
        preferences.thresholds = newThresholds
        reevaluateSafetyStatus(_sensorData.value)
    }

    override fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        preferences.notificationsEnabled = enabled
    }

    override fun clearAlerts() {
        _alerts.value = emptyList()
    }

    override fun clearHistory() {
        _history.value = emptyList()
    }

    override fun testConnection() {
        repositoryScope.launch {
            if (_connectionMode.value == ConnectionMode.WIFI) {
                fetchFromEsp32Hardware()
            } else {
                bluetoothManager.refreshPairedDevices()
            }
        }
    }

    override fun refreshBluetoothDevices() {
        bluetoothManager.refreshPairedDevices()
    }

    override fun connectBluetoothDevice(device: BluetoothDevice) {
        bluetoothManager.connectToDevice(device)
    }

    override fun disconnectBluetooth() {
        bluetoothManager.disconnect()
    }

    private fun updateSensorDataWithCalculatedSafety(data: SensorData) {
        val t = _thresholds.value

        val isSensorFault = data.temperature < -40f || data.temperature > 125f ||
                data.humidity < 0f || data.humidity > 100f

        val isDanger = !isSensorFault && (
                data.flameDetected ||
                        data.gasPpm >= t.gasDangerPpm ||
                        data.smokePpm >= t.smokeDangerPpm ||
                        data.temperature >= t.tempDangerC
                )

        val isWarning = !isSensorFault && !isDanger && (
                data.gasPpm >= t.gasWarningPpm ||
                        data.smokePpm >= t.smokeWarningPpm ||
                        data.temperature >= t.tempWarningC
                )

        val newSafetyLevel = when {
            isSensorFault -> SafetyLevel.SENSOR_FAULT
            isDanger -> SafetyLevel.DANGER
            isWarning -> SafetyLevel.WARNING
            else -> SafetyLevel.SAFE
        }

        val greenLedState = newSafetyLevel == SafetyLevel.SAFE
        val yellowLedState = newSafetyLevel == SafetyLevel.WARNING || newSafetyLevel == SafetyLevel.SENSOR_FAULT
        val redLedState = newSafetyLevel == SafetyLevel.DANGER
        val buzzerState = newSafetyLevel == SafetyLevel.DANGER

        val statusEnum = if (isSensorFault) ConnectionStatus.SENSOR_FAULT else ConnectionStatus.CONNECTED

        val finalData = data.copy(
            greenLed = greenLedState,
            yellowLed = yellowLedState,
            redLed = redLedState,
            buzzer = buzzerState,
            temperature = roundToOneDecimal(data.temperature),
            humidity = roundToOneDecimal(data.humidity),
            connectionStatus = statusEnum,
            connectionMode = _connectionMode.value,
            lastUpdatedTime = timeFormatter.format(Date())
        )

        _sensorData.value = finalData
        _safetyLevel.value = newSafetyLevel
        _connectionStatus.value = statusEnum

        val threatSummary = "Level:${newSafetyLevel.name}|Flame:${data.flameDetected}|Gas:${data.gasPpm >= t.gasWarningPpm}|Smoke:${data.smokePpm >= t.smokeWarningPpm}|Temp:${data.temperature >= t.tempWarningC}"

        if (previousSafetyLevel != newSafetyLevel || threatSummary != previousThreatSummary) {
            onSafetyLevelChanged(previousSafetyLevel, newSafetyLevel, finalData)
            previousSafetyLevel = newSafetyLevel
            previousThreatSummary = threatSummary
        }

        if (data.isConnected) {
            repositoryScope.launch {
                sendActuatorCommands(greenLedState, yellowLedState, redLedState, buzzerState)
            }
        }
    }

    private fun reevaluateSafetyStatus(data: SensorData) {
        updateSensorDataWithCalculatedSafety(data)
    }

    private fun onSafetyLevelChanged(oldLevel: SafetyLevel?, newLevel: SafetyLevel, data: SensorData) {
        val timestamp = fullDateTimeFormatter.format(Date())
        val t = _thresholds.value

        if (newLevel != SafetyLevel.SAFE && _notificationsEnabled.value) {
            val (title, message, sensorType, action) = when {
                newLevel == SafetyLevel.SENSOR_FAULT -> Quad(
                    "SENSOR FAULT DETECTED",
                    "A sensor reported invalid or out-of-range readings (${data.sensorFaultDetails ?: "Check wiring"}).",
                    "Hardware Diagnostic",
                    "Inspect ESP32 sensor wiring and connections."
                )
                data.flameDetected -> Quad(
                    "FLAME DETECTED!",
                    "Infrared flame sensor triggered! Potential fire outbreak in kitchen area.",
                    "Flame Sensor",
                    "Extinguish heat sources immediately and evacuate if necessary."
                )
                data.gasPpm >= t.gasDangerPpm -> Quad(
                    "CRITICAL LPG GAS LEAK",
                    "MQ-6 Sensor recorded dangerous gas level of ${data.gasPpm} PPM (Limit: ${t.gasDangerPpm} PPM).",
                    "MQ-6 LPG Gas",
                    "Turn off main gas cylinder valve immediately! Open windows and do not turn on electrical switches."
                )
                data.smokePpm >= t.smokeDangerPpm -> Quad(
                    "DANGEROUS SMOKE CONCENTRATION",
                    "MQ-2 Sensor recorded smoke level of ${data.smokePpm} PPM (Limit: ${t.smokeDangerPpm} PPM).",
                    "MQ-2 Smoke",
                    "Check for burning food or smoldering wiring. Ventilate kitchen."
                )
                data.temperature >= t.tempDangerC -> Quad(
                    "EXTREME KITCHEN OVERHEATING",
                    "DHT11 Sensor recorded temperature of ${String.format(Locale.US, "%.1f", data.temperature)}°C.",
                    "DHT11 Temp",
                    "Turn off stoves and ovens. Ensure adequate airflow."
                )
                data.gasPpm >= t.gasWarningPpm -> Quad(
                    "LPG Gas Level Elevated",
                    "MQ-6 Gas level at ${data.gasPpm} PPM is approaching threshold (${t.gasWarningPpm} PPM).",
                    "MQ-6 LPG Gas",
                    "Check gas stove knobs and inspect hoses."
                )
                data.smokePpm >= t.smokeWarningPpm -> Quad(
                    "Smoke Detected",
                    "MQ-2 Smoke level at ${data.smokePpm} PPM (${t.smokeWarningPpm} PPM).",
                    "MQ-2 Smoke",
                    "Check stove/oven cooking progress."
                )
                data.temperature >= t.tempWarningC -> Quad(
                    "Kitchen Temperature High",
                    "Temperature elevated to ${String.format(Locale.US, "%.1f", data.temperature)}°C.",
                    "DHT11 Temp",
                    "Enable exhaust fan or open kitchen window."
                )
                else -> Quad(
                    "Safety Warning Triggered",
                    "Sensor parameters require attention.",
                    "Safety Monitor",
                    "Inspect kitchen environment."
                )
            }

            val newAlert = SafetyAlert(
                id = UUID.randomUUID().toString(),
                title = title,
                message = message,
                severity = newLevel,
                timestamp = timestamp,
                sensorType = sensorType,
                recommendedAction = action
            )

            _alerts.value = listOf(newAlert) + _alerts.value

            notificationHelper.showSafetyAlertNotification(title, message, newLevel)
        }

        val historySummary = when (newLevel) {
            SafetyLevel.SAFE -> "Returned to Normal Safety Parameters."
            SafetyLevel.WARNING -> "Warning triggered: ${data.gasPpm} PPM Gas, ${data.smokePpm} PPM Smoke, ${String.format(Locale.US, "%.1f", data.temperature)}°C."
            SafetyLevel.DANGER -> "DANGER EVENT: ${if (data.flameDetected) "FLAME DETECTED! " else ""}${data.gasPpm} PPM Gas, ${data.smokePpm} PPM Smoke, ${String.format(Locale.US, "%.1f", data.temperature)}°C."
            SafetyLevel.SENSOR_FAULT -> "SENSOR FAULT: Diagnostic error recorded."
            SafetyLevel.OFFLINE -> "OFFLINE: Hardware disconnected."
        }

        val historyEntry = SensorHistoryEntry(
            id = UUID.randomUUID().toString(),
            timestamp = timestamp,
            safetyLevel = newLevel,
            gasPpm = data.gasPpm,
            smokePpm = data.smokePpm,
            temperature = data.temperature,
            humidity = data.humidity,
            flameDetected = data.flameDetected,
            summary = historySummary
        )

        _history.value = listOf(historyEntry) + _history.value
    }

    private suspend fun fetchFromEsp32Hardware() = withContext(Dispatchers.IO) {
        _connectionStatus.value = ConnectionStatus.CONNECTING

        val candidates = if (_wifiConnectMode.value == WifiConnectMode.MANUAL_IP) {
            listOf(_esp32Ip.value.trim())
        } else {
            listOf(
                _esp32Ip.value.trim(),
                "10.224.137.26",
                "192.168.4.1",
                "192.168.1.120",
                "192.168.1.100",
                "192.168.1.1",
                "192.168.0.100",
                "kitchenguard.local"
            ).filter { it.isNotEmpty() }.distinct()
        }

        var successData: SensorData? = null
        var foundIp: String? = null
        var lastErrorMsg: String? = null

        for (ip in candidates) {
            if (ip.isEmpty()) continue

            val endpoints = listOf(
                "http://$ip/data",
                "http://$ip/api/sensors",
                "http://$ip/sensors",
                "http://$ip/"
            )

            for (endpointUrl in endpoints) {
                try {
                    val url = URL(endpointUrl)
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 1800
                        readTimeout = 1800
                        requestMethod = "GET"
                        setRequestProperty("Accept", "application/json")
                    }

                    val responseCode = conn.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val reader = BufferedReader(InputStreamReader(conn.inputStream))
                        val sb = StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            sb.append(line)
                        }
                        reader.close()
                        conn.disconnect()

                        val rawBody = sb.toString().trim()
                        if (rawBody.startsWith("{")) {
                            val json = JSONObject(rawBody)
                            val parsed = parseEsp32SensorJson(json)
                            if (parsed != null) {
                                successData = parsed
                                foundIp = ip
                                break
                            } else {
                                lastErrorMsg = "Malformed JSON: Missing required sensor fields."
                            }
                        } else {
                            lastErrorMsg = "Unsupported non-JSON endpoint response."
                        }
                    } else {
                        conn.disconnect()
                        lastErrorMsg = "HTTP $responseCode returned from $endpointUrl."
                    }
                } catch (e: java.net.SocketTimeoutException) {
                    lastErrorMsg = "Connection timeout reaching $ip."
                } catch (e: java.net.ConnectException) {
                    lastErrorMsg = "Device unreachable or connection refused at $ip."
                } catch (e: Exception) {
                    lastErrorMsg = "Network error: ${e.localizedMessage}"
                }
            }
            if (successData != null) break
        }

        if (successData != null && foundIp != null) {
            _esp32Ip.value = foundIp
            preferences.esp32Ip = foundIp
            _isConnected.value = true
            _connectionStatus.value = ConnectionStatus.CONNECTED
            _connectionError.value = null
            val realData = successData
            withContext(Dispatchers.Default) {
                updateSensorDataWithCalculatedSafety(realData)
            }
        } else {
            _isConnected.value = false
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
            _connectionError.value = lastErrorMsg ?: "Unable to connect to ESP32."
            _sensorData.value = _sensorData.value.copy(isConnected = false, connectionStatus = ConnectionStatus.DISCONNECTED)
        }
    }

    private fun parseEsp32SensorJson(json: JSONObject): SensorData? {
        fun findIntKey(vararg keys: String): Int? {
            for (k in keys) {
                if (json.has(k) && !json.isNull(k)) {
                    val v = json.optInt(k, -1)
                    if (v >= 0) return v
                }
            }
            return null
        }

        fun findDoubleKey(vararg keys: String): Double? {
            for (k in keys) {
                if (json.has(k) && !json.isNull(k)) {
                    val v = json.optDouble(k, Double.NaN)
                    if (!v.isNaN()) return v
                }
            }
            return null
        }

        fun findBooleanKey(vararg keys: String): Boolean? {
            for (k in keys) {
                if (json.has(k) && !json.isNull(k)) {
                    return try {
                        json.getBoolean(k)
                    } catch (_: Exception) {
                        json.optInt(k, 0) != 0
                    }
                }
            }
            return null
        }

        val gas = findIntKey("lpg", "gas_ppm", "gasPpm", "gas", "mq6") ?: return null
        val smoke = findIntKey("smoke", "smoke_ppm", "smokePpm", "mq2") ?: return null
        val rawTemp = findDoubleKey("temperature", "temp_c", "temp", "temperatureC") ?: return null
        val rawHum = findDoubleKey("humidity", "hum", "humidity_pct") ?: return null

        val flameVal = findIntKey("flame")
        val flameBool = findBooleanKey("flame_detected", "flameDetected")
        val isFlamePresent = when {
            flameBool != null -> flameBool
            flameVal != null -> flameVal > 2000 || flameVal == 1
            else -> false
        }

        return SensorData(
            gasPpm = gas,
            smokePpm = smoke,
            temperature = roundToOneDecimal(rawTemp.toFloat()),
            humidity = roundToOneDecimal(rawHum.toFloat()),
            flameDetected = isFlamePresent,
            flameRaw = flameVal ?: 0,
            isConnected = true,
            connectionStatus = ConnectionStatus.CONNECTED,
            connectionMode = _connectionMode.value
        )
    }

    private suspend fun sendActuatorCommands(
        greenLed: Boolean,
        yellowLed: Boolean,
        redLed: Boolean,
        buzzer: Boolean
    ) {
        if (_connectionMode.value == ConnectionMode.BLUETOOTH) {
            val payload = JSONObject().apply {
                put("green_led", greenLed)
                put("yellow_led", yellowLed)
                put("red_led", redLed)
                put("buzzer", buzzer)
            }.toString()
            bluetoothManager.sendActuatorCommand(payload)
        } else {
            sendActuatorCommandsToEsp32Http(greenLed, yellowLed, redLed, buzzer)
        }
    }

    private suspend fun sendActuatorCommandsToEsp32Http(
        greenLed: Boolean,
        yellowLed: Boolean,
        redLed: Boolean,
        buzzer: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val ip = _esp32Ip.value.trim()
        if (ip.isEmpty()) return@withContext false

        val endpoints = listOf(
            "http://$ip/api/actuators",
            "http://$ip/actuators"
        )

        val payload = JSONObject().apply {
            put("green_led", greenLed)
            put("yellow_led", yellowLed)
            put("red_led", redLed)
            put("buzzer", buzzer)
            put("greenLed", greenLed)
            put("yellowLed", yellowLed)
            put("redLed", redLed)
        }.toString()

        var postSuccess = false

        for (endpointUrl in endpoints) {
            try {
                val url = URL(endpointUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 2000
                    readTimeout = 2000
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                }

                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(payload)
                writer.flush()
                writer.close()

                val code = conn.responseCode
                conn.disconnect()
                if (code == HttpURLConnection.HTTP_OK || code == HttpURLConnection.HTTP_NO_CONTENT || code == HttpURLConnection.HTTP_CREATED) {
                    postSuccess = true
                    break
                }
            } catch (_: Exception) {
            }
        }
        return@withContext postSuccess
    }

    private fun isValidIpv4(ip: String): Boolean {
        val regex = Regex("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.){3}(25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)$")
        return regex.matches(ip)
    }

    private fun createInitialData() = SensorData(
        gasPpm = 0,
        smokePpm = 0,
        temperature = 0.0f,
        humidity = 0.0f,
        flameDetected = false,
        greenLed = false,
        yellowLed = false,
        redLed = false,
        buzzer = false,
        isConnected = false,
        connectionStatus = ConnectionStatus.DISCONNECTED,
        connectionMode = _connectionMode.value,
        lastUpdatedTime = timeFormatter.format(Date())
    )

    private fun createInitialAlerts(): List<SafetyAlert> = emptyList()

    private fun createInitialHistory(): List<SensorHistoryEntry> = emptyList()

    private fun roundToOneDecimal(value: Float): Float {
        return (value * 10f).roundToInt() / 10f
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
