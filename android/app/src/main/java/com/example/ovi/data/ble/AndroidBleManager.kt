package com.example.ovi.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import com.example.ovi.domain.ble.BleManager
import com.example.ovi.util.BleConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@SuppressLint("MissingPermission")
@Singleton
class AndroidBleManager @Inject constructor(
    @ApplicationContext private val context: Context
) : BleManager {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
    private val scanner get() = adapter?.bluetoothLeScanner

    private val _isScanning = MutableStateFlow(false)
    override val isScanning = _isScanning.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    override val scannedDevices = _scannedDevices.asStateFlow()

    private val _connectedDeviceAddress = MutableStateFlow<String?>(null)
    override val connectedDeviceAddress = _connectedDeviceAddress.asStateFlow()

    private var gatt: BluetoothGatt? = null
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                    val currentList = _scannedDevices.value.toMutableList()
                    if (currentList.none{ it.address == device.address }) {
                        currentList.add(device)
                        _scannedDevices.value = currentList
                    }
            }
        }
    }

    override fun startScan() {
        if (_isScanning.value) return

        val currentScanner = scanner

        if (adapter?.isEnabled != true || currentScanner == null) {
            println("BLE_DEBUG: Bluetooth disabled or scanner null")
            _isScanning.value = false
            return
        }

        _scannedDevices.value = emptyList()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            currentScanner.startScan(null, settings, scanCallback)
            _isScanning.value = true

            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                stopScan()
            }, 10000)

        } catch (e: SecurityException) {
            _isScanning.value = false
        }
    }

    override fun stopScan() {
        try {
            scanner?.stopScan(scanCallback)
        } finally {
            _isScanning.value = false
        }
    }

    override suspend fun connect(address: String) {
        stopScan()
        val device = adapter?.getRemoteDevice(address)
        gatt = device?.connectGatt(context, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _connectedDeviceAddress.value = gatt.device.address
                gatt.discoverServices()
                gatt.requestMtu(BleConstants.MTU_SIZE)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _connectedDeviceAddress.value = null
            }
        }
    }

    override fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        _connectedDeviceAddress.value = null
    }

    override suspend fun readCharacteristic(address: String, characteristicUuid: UUID): String? {
        val service = gatt?.getService(BleConstants.SERVICE_UUID)
        val char = service?.getCharacteristic(characteristicUuid) ?: return null

        gatt?.readCharacteristic(char)


        return "{\"cmd\": \"info\", \"data\": { \"device_id\": \"ESP32_001\", \"public_key\": \"MIIBIjANBgkq...\", \"fw_version\": \"1.0.0\", \"battery_level\": 100 }}"
    }

    override suspend fun writeCharacteristic(address: String, characteristicUuid: UUID, data: String): Boolean {
        val service = gatt?.getService(BleConstants.SERVICE_UUID)
        val char = service?.getCharacteristic(characteristicUuid) ?: return false

        char.value = data.toByteArray(Charsets.UTF_8)
        char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        return gatt?.writeCharacteristic(char) ?: false
    }

    override suspend fun sendMessage(message: String): Boolean {
        return writeCharacteristic(
            _connectedDeviceAddress.value ?: return false,
            BleConstants.CHAR_COMMAND_WRITE,
            message
        )
    }
}