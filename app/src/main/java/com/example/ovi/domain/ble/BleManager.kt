package com.example.ovi.domain.ble

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

interface BleManager {
    val isScanning: StateFlow<Boolean>
    val scannedDevices: StateFlow<List<BluetoothDevice>>
    val connectedDeviceAddress: StateFlow<String?>

    fun startScan()
    fun stopScan()
    suspend fun connect(address: String)
    fun disconnect()
    suspend fun sendMessage(message: String): Boolean

    suspend fun readCharacteristic(address: String, characteristicUuid: UUID): String?
    suspend fun writeCharacteristic(address: String, characteristicUuid: UUID, data: String): Boolean
}