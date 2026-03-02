package com.example.ovi.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import com.example.ovi.domain.ble.BleManager
import com.example.ovi.util.BleConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
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
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDevice>> = _scannedDevices.asStateFlow()

    private val _connectedDeviceAddress = MutableStateFlow<String?>(null)
    override val connectedDeviceAddress: StateFlow<String?> = _connectedDeviceAddress.asStateFlow()
    
    private val _notifications = MutableSharedFlow<Pair<UUID, String>>(extraBufferCapacity = 32)
    override val notifications: SharedFlow<Pair<UUID, String>> = _notifications.asSharedFlow()
    
    private val _deviceRssi = MutableStateFlow<Map<String, Int>>(emptyMap())
    override val deviceRssi: StateFlow<Map<String, Int>> = _deviceRssi.asStateFlow()

    private var gatt: BluetoothGatt? = null
    
    private val readMutex = Mutex()
    private val writeMutex = Mutex()
    private var pendingRead: CompletableDeferred<String?>? = null
    private var pendingWrite: CompletableDeferred<Boolean>? = null
    
    private var reconnectAttempts = 0
    private var isManualDisconnect = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private var connectionTimeoutRunnable: Runnable? = null
    

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result ?: return
            val device = result.device ?: return
            val current = _scannedDevices.value.toMutableList()
            if (current.none { it.address == device.address }) {
                current.add(device)
                _scannedDevices.value = current
            }
            // Track RSSI for pre-unlock range check (issue #12)
            _deviceRssi.value = _deviceRssi.value.toMutableMap().also { it[device.address] = result.rssi }
        }
    }

    override fun startScan() {
        if (_isScanning.value) return
        val currentScanner = scanner ?: return
        if (adapter?.isEnabled != true) { _isScanning.value = false; return }

        _scannedDevices.value = emptyList()
        
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            currentScanner.startScan(listOf(filter), settings, scanCallback)
            _isScanning.value = true
            // Issue #9: 30-second scan window (spec: BLE connection timeout = 30s)
            mainHandler.postDelayed({ stopScan() }, BleConstants.BLE_CONNECTION_TIMEOUT_MS)
        } catch (e: SecurityException) {
            _isScanning.value = false
        }
    }

    override fun stopScan() {
        try { scanner?.stopScan(scanCallback) } finally { _isScanning.value = false }
    }

    override fun getRssi(address: String): Int? = _deviceRssi.value[address]
    

    override suspend fun connect(address: String) {
        stopScan()
        isManualDisconnect = false
        reconnectAttempts = 0

        val device = adapter?.getRemoteDevice(address) ?: return
        gatt = device.connectGatt(context, false, gattCallback)

        // Issue #9: 30-second hard timeout on the connection attempt
        connectionTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        val timeout = Runnable {
            if (_connectedDeviceAddress.value == null) {
                disconnect()
            }
        }
        connectionTimeoutRunnable = timeout
        mainHandler.postDelayed(timeout, BleConstants.BLE_CONNECTION_TIMEOUT_MS)
    }

    override fun disconnect() {
        isManualDisconnect = true
        connectionTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        _connectedDeviceAddress.value = null
        reconnectAttempts = 0
    }
    

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectionTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
                    reconnectAttempts = 0
                    _connectedDeviceAddress.value = gatt.device.address
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectedDeviceAddress.value = null
                    // Issue #10: auto-reconnect up to MAX_RECONNECT_ATTEMPTS on unexpected disconnect
                    if (!isManualDisconnect && reconnectAttempts < BleConstants.MAX_RECONNECT_ATTEMPTS) {
                        reconnectAttempts++
                        val delay = 1000L * reconnectAttempts
                        mainHandler.postDelayed({ gatt.connect() }, delay)
                    } else {
                        gatt.close()
                        this@AndroidBleManager.gatt = null
                        reconnectAttempts = 0
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return
            gatt.requestMtu(BleConstants.MTU_SIZE)
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            val service = gatt.getService(BleConstants.SERVICE_UUID) ?: return
            val notifyChar = service.getCharacteristic(BleConstants.CHAR_STATUS_NOTIFY) ?: return
            gatt.setCharacteristicNotification(notifyChar, true)
            val cccd = notifyChar.getDescriptor(BleConstants.CCCD_UUID) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                @Suppress("DEPRECATION")
                cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(cccd)
            }
        }
        
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            val result = if (status == BluetoothGatt.GATT_SUCCESS) value.toString(Charsets.UTF_8) else null
            pendingRead?.complete(result)
            pendingRead = null
        }
        
        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            val value = if (status == BluetoothGatt.GATT_SUCCESS) {
                characteristic.value?.toString(Charsets.UTF_8)
            } else null
            pendingRead?.complete(value)
            pendingRead = null
        }
        
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            pendingWrite?.complete(status == BluetoothGatt.GATT_SUCCESS)
            pendingWrite = null
        }
        
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            _notifications.tryEmit(characteristic.uuid to value.toString(Charsets.UTF_8))
        }
        
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val value = characteristic.value?.toString(Charsets.UTF_8) ?: return
            _notifications.tryEmit(characteristic.uuid to value)
        }
    }
    
    override suspend fun readCharacteristic(address: String, characteristicUuid: UUID): String? {
        return readMutex.withLock {
            val service = gatt?.getService(BleConstants.SERVICE_UUID) ?: return null
            val char = service.getCharacteristic(characteristicUuid) ?: return null
            val deferred = CompletableDeferred<String?>()
            pendingRead = deferred
            val initiated = gatt?.readCharacteristic(char) == true
            if (!initiated) { pendingRead = null; return null }
            withTimeoutOrNull(BleConstants.BLE_OPERATION_TIMEOUT_MS) { deferred.await() }
        }
    }

    override suspend fun writeCharacteristic(
        address: String,
        characteristicUuid: UUID,
        data: String
    ): Boolean {
        return writeMutex.withLock {
            val service = gatt?.getService(BleConstants.SERVICE_UUID) ?: return false
            val char = service.getCharacteristic(characteristicUuid) ?: return false
            val bytes = data.toByteArray(Charsets.UTF_8)
            val deferred = CompletableDeferred<Boolean>()
            pendingWrite = deferred
            val initiated = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt?.writeCharacteristic(char, bytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothGatt.GATT_SUCCESS
            } else {
                @Suppress("DEPRECATION")
                char.value = bytes
                @Suppress("DEPRECATION")
                char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                @Suppress("DEPRECATION")
                gatt?.writeCharacteristic(char) == true
            }
            if (!initiated) { pendingWrite = null; return false }
            withTimeoutOrNull(BleConstants.BLE_OPERATION_TIMEOUT_MS) { deferred.await() } ?: false
        }
    }

    override suspend fun sendMessage(message: String): Boolean {
        return writeCharacteristic(
            _connectedDeviceAddress.value ?: return false,
            BleConstants.CHAR_COMMAND_WRITE,
            message
        )
    }
}