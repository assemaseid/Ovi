package com.example.ovi.domain.ble

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

interface BleManager {
    val isScanning: StateFlow<Boolean>
    val scannedDevices: StateFlow<List<BluetoothDevice>>
    val connectedDeviceAddress: StateFlow<String?>

    /** Emits (characteristicUuid → raw value string) for every incoming BLE notification. */
    val notifications: SharedFlow<Pair<UUID, String>>

    /** Last observed RSSI per device MAC address, populated during scanning. */
    val deviceRssi: StateFlow<Map<String, Int>>

    fun startScan()
    fun stopScan()
    suspend fun connect(address: String)
    fun disconnect()
    suspend fun sendMessage(message: String): Boolean

    suspend fun readCharacteristic(address: String, characteristicUuid: UUID): String?
    suspend fun writeCharacteristic(address: String, characteristicUuid: UUID, data: String): Boolean

    /** Returns the last known RSSI for a scanned device, or null if not seen. */
    fun getRssi(address: String): Int?
}