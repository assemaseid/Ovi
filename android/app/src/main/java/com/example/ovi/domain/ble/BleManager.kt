package com.example.ovi.domain.ble

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

interface BleManager {
    val isScanning: StateFlow<Boolean>
    val scannedDevices: StateFlow<List<BluetoothDevice>>
    val connectedDeviceAddress: StateFlow<String?>

    val notifications: SharedFlow<Pair<UUID, String>>

    val deviceRssi: StateFlow<Map<String, Int>>

    val isServicesReady: StateFlow<Boolean>

    fun startScan()
    fun stopScan()
    suspend fun connect(address: String)
    fun disconnect()
    suspend fun sendMessage(message: String): Boolean

    suspend fun readCharacteristic(address: String, characteristicUuid: UUID): String?
    suspend fun writeCharacteristic(address: String, characteristicUuid: UUID, data: String): Boolean

    fun getRssi(address: String): Int?
}