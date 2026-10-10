package com.kitchenguard360.app.data.model

sealed class BluetoothUiState {
    data object BluetoothDisabled : BluetoothUiState()
    data object NoPermission : BluetoothUiState()
    data object NoPairedDevice : BluetoothUiState()
    data object Connecting : BluetoothUiState()
    data class Connected(val deviceName: String) : BluetoothUiState()
    data object Disconnected : BluetoothUiState()
    data object NoRecentData : BluetoothUiState()
    data object SensorFault : BluetoothUiState()
    data object Normal : BluetoothUiState()
    data object Warning : BluetoothUiState()
    data object Danger : BluetoothUiState()
    data object Warmup : BluetoothUiState()
}
