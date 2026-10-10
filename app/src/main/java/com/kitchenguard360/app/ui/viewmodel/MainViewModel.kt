package com.kitchenguard360.app.ui.viewmodel

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kitchenguard360.app.bluetooth.BtDeviceItem
import com.kitchenguard360.app.data.model.ConnectionMode
import com.kitchenguard360.app.data.model.ConnectionStatus
import com.kitchenguard360.app.data.model.SafetyAlert
import com.kitchenguard360.app.data.model.SafetyLevel
import com.kitchenguard360.app.data.model.SafetyThresholds
import com.kitchenguard360.app.data.model.SensorData
import com.kitchenguard360.app.data.model.SensorHistoryEntry
import com.kitchenguard360.app.data.model.WifiConnectMode
import com.kitchenguard360.app.data.repository.KitchenSafetyRepository
import com.kitchenguard360.app.data.repository.KitchenSafetyRepositoryImpl
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(
    private val repository: KitchenSafetyRepository = KitchenSafetyRepositoryImpl()
) : ViewModel() {

    val sensorData: StateFlow<SensorData> = repository.sensorData
    val safetyLevel: StateFlow<SafetyLevel> = repository.safetyLevel
    val alerts: StateFlow<List<SafetyAlert>> = repository.alerts
    val history: StateFlow<List<SensorHistoryEntry>> = repository.history
    val thresholds: StateFlow<SafetyThresholds> = repository.thresholds
    val esp32Ip: StateFlow<String> = repository.esp32Ip
    val isConnected: StateFlow<Boolean> = repository.isConnected
    val connectionStatus: StateFlow<ConnectionStatus> = repository.connectionStatus
    val connectionMode: StateFlow<ConnectionMode> = repository.connectionMode
    val wifiConnectMode: StateFlow<WifiConnectMode> = repository.wifiConnectMode
    val connectionError: StateFlow<String?> = repository.connectionError
    val notificationsEnabled: StateFlow<Boolean> = repository.notificationsEnabled
    val btPairedDevices: StateFlow<List<BtDeviceItem>> = repository.btPairedDevices
    val btConnectedDeviceName: StateFlow<String?> = repository.btConnectedDeviceName

    fun refreshData() {
        repository.refreshData()
    }

    fun updateEsp32Ip(ip: String) {
        repository.setEsp32Ip(ip)
    }

    fun setConnectionMode(mode: ConnectionMode) {
        repository.setConnectionMode(mode)
    }

    fun setWifiConnectMode(mode: WifiConnectMode) {
        repository.setWifiConnectMode(mode)
    }

    fun connectManualIp(ip: String) {
        repository.connectManualIp(ip)
    }

    fun autoDiscoverWifi() {
        repository.autoDiscoverWifi()
    }

    fun updateThresholds(newThresholds: SafetyThresholds) {
        repository.updateThresholds(newThresholds)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        repository.setNotificationsEnabled(enabled)
    }

    fun clearAlerts() {
        repository.clearAlerts()
    }

    fun clearHistory() {
        repository.clearHistory()
    }

    fun testConnection() {
        repository.testConnection()
    }

    fun refreshBluetoothDevices() {
        repository.refreshBluetoothDevices()
    }

    fun connectBluetoothDevice(device: BluetoothDevice) {
        repository.connectBluetoothDevice(device)
    }

    fun disconnectBluetooth() {
        repository.disconnectBluetooth()
    }

    companion object {
        fun provideFactory(repository: KitchenSafetyRepository = KitchenSafetyRepositoryImpl()): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(repository) as T
                }
            }
    }
}
