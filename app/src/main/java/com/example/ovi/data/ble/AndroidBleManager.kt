package com.example.ovi.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Message
import com.example.ovi.domain.ble.BleManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@SuppressLint("MissingPermission")
@Singleton
class AndroidBleManager @Inject constructor(
    @ApplicationContext private val context: Context
) : BleManager {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
    private val scanner = adapter?.bluetoothLeScanner

    private val _isScanning = MutableStateFlow(false)
    override val isScanning = _isScanning.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    override val scannedDevices = _scannedDevices.asStateFlow()

    private val _connectedDeviceAddress = MutableStateFlow<String?>(null)
    override val connectedDeviceAddress = _connectedDeviceAddress.asStateFlow()
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                if (!device.name.isNullOrBlank()) {
                    val currentList = _scannedDevices.value.toMutableList()
                    if (currentList.none{ it.address == device.address }) {
                        currentList.add(device)
                        _scannedDevices.value = currentList
                    }
                }
            }
        }
    }

    override fun startScan() {
        if (adapter?.isEnabled == true && !_isScanning.value) {
            _scannedDevices.value = emptyList()
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            scanner?.startScan(null, settings,scanCallback)
            _isScanning.value = true
        }
    }

    override fun stopScan() {
        scanner?.stopScan(scanCallback)
        _isScanning.value = false
    }

    override fun connect(address: String) {
        _connectedDeviceAddress.value = address
        stopScan()
    }

    override fun disconnect() {
        _connectedDeviceAddress.value = null
    }

    override suspend fun sendMessage(message: String): Boolean = true
}