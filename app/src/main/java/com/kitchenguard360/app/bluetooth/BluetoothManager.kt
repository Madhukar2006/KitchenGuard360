package com.kitchenguard360.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager as AndroidBluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.kitchenguard360.app.KitchenGuardApplication
import com.kitchenguard360.app.data.model.ConnectionStatus
import com.kitchenguard360.app.data.model.KitchenStatus
import com.kitchenguard360.app.data.model.KitchenStatusParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.UUID

data class BtDeviceItem(
    val name: String,
    val address: String,
    val device: BluetoothDevice,
    val isKitchenGuard: Boolean = name.equals("KitchenGuard360", ignoreCase = true)
)

class BluetoothManager(
    private val context: Context = KitchenGuardApplication.instance
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val androidBtManager: AndroidBluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? AndroidBluetoothManager

    private val adapter: BluetoothAdapter? = androidBtManager?.adapter

    private val _pairedDevices = MutableStateFlow<List<BtDeviceItem>>(emptyList())
    val pairedDevices: StateFlow<List<BtDeviceItem>> = _pairedDevices.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _latestJsonData = MutableStateFlow<String?>(null)
    val latestJsonData: StateFlow<String?> = _latestJsonData.asStateFlow()

    private val _latestKitchenStatus = MutableStateFlow<KitchenStatus?>(null)
    val latestKitchenStatus: StateFlow<KitchenStatus?> = _latestKitchenStatus.asStateFlow()

    private var socket: BluetoothSocket? = null
    private var isConnected = false
    private var lastReceivedTimestamp: Long = 0
    private var staleCheckJob: Job? = null
    private var lastConnectedDevice: BluetoothDevice? = null

    init {
        refreshPairedDevices()
        startStaleDataCheck()
    }

    fun getBluetoothAdapter(): BluetoothAdapter? = adapter

    fun isBluetoothEnabled(): Boolean = adapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun getPairedDevicesList(): List<BluetoothDevice> {
        if (adapter == null || !adapter.isEnabled) return emptyList()
        return try {
            adapter.bondedDevices?.toList() ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if (adapter == null || !adapter.isEnabled) return
        try {
            if (adapter.isDiscovering) adapter.cancelDiscovery()
            adapter.startDiscovery()
        } catch (_: SecurityException) {}
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        if (adapter == null) return
        try {
            if (adapter.isDiscovering) adapter.cancelDiscovery()
        } catch (_: SecurityException) {}
    }

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        if (adapter == null || !adapter.isEnabled) {
            _pairedDevices.value = emptyList()
            return
        }

        try {
            val bonded = adapter.bondedDevices
            val items = bonded?.map { device ->
                val deviceName = device.name ?: "Unknown Device"
                BtDeviceItem(
                    name = deviceName,
                    address = device.address,
                    device = device,
                    isKitchenGuard = deviceName.equals("KitchenGuard360", ignoreCase = true)
                )
            }?.sortedByDescending { it.isKitchenGuard } ?: emptyList()

            _pairedDevices.value = items
        } catch (_: SecurityException) {
            _pairedDevices.value = emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        disconnectInternal()
        lastConnectedDevice = device

        scope.launch {
            _connectionStatus.value = ConnectionStatus.CONNECTING
            _connectedDeviceName.value = device.name ?: device.address

            try {
                stopDiscovery()

                val sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                val clientSocket = device.createRfcommSocketToServiceRecord(sppUuid)

                clientSocket.connect()
                socket = clientSocket
                isConnected = true
                _connectionStatus.value = ConnectionStatus.CONNECTED

                readStream(clientSocket)

            } catch (_: Exception) {
                isConnected = false
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
                _connectedDeviceName.value = null
            }
        }
    }

    fun sendActuatorCommand(jsonPayload: String): Boolean {
        val currentSocket = socket ?: return false
        if (!isConnected) return false

        return try {
            val writer = OutputStreamWriter(currentSocket.outputStream, StandardCharsets.UTF_8)
            writer.write(jsonPayload + "\n")
            writer.flush()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun disconnect() {
        lastConnectedDevice = null
        disconnectInternal()
    }

    private fun disconnectInternal() {
        isConnected = false
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        _connectedDeviceName.value = null
    }

    fun observeConnectionState(): StateFlow<ConnectionStatus> = connectionStatus

    fun observeSensorStatus(): StateFlow<KitchenStatus?> = latestKitchenStatus

    private suspend fun readStream(btSocket: BluetoothSocket) = withContext(Dispatchers.IO) {
        try {
            val reader = BufferedReader(InputStreamReader(btSocket.inputStream, StandardCharsets.UTF_8))
            while (isConnected && btSocket.isConnected) {
                val line = reader.readLine() ?: break
                val trimmed = line.trim()
                if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                    val parsed = KitchenStatusParser.parseJson(trimmed)
                    if (parsed != null) {
                        lastReceivedTimestamp = System.currentTimeMillis()
                        _latestJsonData.value = trimmed
                        _latestKitchenStatus.value = parsed
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            disconnectInternal()
            // Auto reconnect if device is saved
            val deviceToReconnect = lastConnectedDevice
            if (deviceToReconnect != null) {
                delay(3000)
                connectToDevice(deviceToReconnect)
            }
        }
    }

    private fun startStaleDataCheck() {
        staleCheckJob?.cancel()
        staleCheckJob = scope.launch {
            while (true) {
                delay(2000)
                val now = System.currentTimeMillis()
                if (isConnected && lastReceivedTimestamp > 0 && (now - lastReceivedTimestamp) > 8000) {
                    // Stale data timeout (no packets for 8s)
                    _latestKitchenStatus.value = null
                }
            }
        }
    }
}
