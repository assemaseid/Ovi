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
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
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

    private val _isServicesReady = MutableStateFlow(false)
    override val isServicesReady: StateFlow<Boolean> = _isServicesReady.asStateFlow()

    private var gatt: BluetoothGatt? = null
    
    private val readMutex = Mutex()
    private val writeMutex = Mutex()
    private var pendingRead: CompletableDeferred<String?>? = null
    private var pendingWrite: CompletableDeferred<Boolean>? = null
    
    private var reconnectAttempts = 0
    private var isManualDisconnect = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private var connectionTimeoutRunnable: Runnable? = null

    private val bondStateReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != BluetoothDevice.ACTION_BOND_STATE_CHANGED) return
            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            } ?: return
            if (device.address != gatt?.device?.address) return
            when (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)) {
                BluetoothDevice.BOND_BONDED -> gatt?.discoverServices()
                BluetoothDevice.BOND_NONE -> {
                    _connectedDeviceAddress.value = null
                    gatt?.close()
                    gatt = null
                }
            }
        }
    }

    init {
        val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(bondStateReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(bondStateReceiver, filter)
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result ?: return
            val device = result.device ?: return
            val current = _scannedDevices.value.toMutableList()
            if (current.none { it.address == device.address }) {
                current.add(device)
                _scannedDevices.value = current
            }
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
        _isServicesReady.value = false

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
        _isServicesReady.value = false
        reconnectAttempts = 0
    }
    

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.d("BLE", "onConnectionStateChange status=$status newState=$newState (2=connected,0=disconnected)")
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectionTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
                    reconnectAttempts = 0
                    _connectedDeviceAddress.value = gatt.device.address
                    val bondState = gatt.device.bondState
                    Log.d("BLE", "Connected, bondState=$bondState (12=bonded)")
                    // ESP32 has no BLE pairing configured — skip bonding, discover immediately
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.w("BLE", "Disconnected, status=$status isManualDisconnect=$isManualDisconnect reconnectAttempts=$reconnectAttempts")
                    _connectedDeviceAddress.value = null
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
            if (gatt != this@AndroidBleManager.gatt) {
                Log.w("BLE", "onServicesDiscovered: stale GATT callback, ignoring")
                return
            }
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e("BLE", "onServicesDiscovered failed, status=$status")
                return
            }
            Log.d("BLE", "Services discovered: ${gatt.services.map { it.uuid }}")
            gatt.services.forEach { svc ->
                Log.d("BLE", "  Service ${svc.uuid} chars: ${svc.characteristics.map { it.uuid }}")
            }
            gatt.requestMtu(BleConstants.MTU_SIZE)
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.d("BLE", "MTU changed to $mtu, status=$status, gattMatch=${gatt == this@AndroidBleManager.gatt}")
            if (gatt != this@AndroidBleManager.gatt) {
                Log.w("BLE", "onMtuChanged: stale GATT callback, ignoring")
                return
            }
            val service = gatt.getService(BleConstants.SERVICE_UUID) ?: run {
                Log.e("BLE", "Service ${BleConstants.SERVICE_UUID} not found after MTU change")
                return
            }
            val notifyChar = service.getCharacteristic(BleConstants.CHAR_STATUS_NOTIFY)
            if (notifyChar == null) {
                Log.e("BLE", "CHAR_STATUS_NOTIFY (${BleConstants.CHAR_STATUS_NOTIFY}) not found in service")
                return
            }
            gatt.setCharacteristicNotification(notifyChar, true)
            val cccd = notifyChar.getDescriptor(BleConstants.CCCD_UUID)
            if (cccd == null) {
                Log.e("BLE", "CCCD descriptor not found on notify char. Descriptors: ${notifyChar.descriptors.map { it.uuid }}")
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val result = gatt.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                Log.d("BLE", "writeDescriptor (CCCD) API33+ result=$result (0=success)")
            } else {
                @Suppress("DEPRECATION")
                cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                val result = gatt.writeDescriptor(cccd)
                Log.d("BLE", "writeDescriptor (CCCD) legacy result=$result")
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            Log.d("BLE", "onDescriptorWrite uuid=${descriptor.uuid} status=$status")
            if (descriptor.uuid == BleConstants.CCCD_UUID) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.e("BLE", "CCCD write failed, status=$status")
                }
                _isServicesReady.value = status == BluetoothGatt.GATT_SUCCESS
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
            Log.d("BLE", "onCharacteristicWrite uuid=${characteristic.uuid} status=$status (0=success)")
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
            val gattRef = gatt ?: run { Log.e("BLE", "readCharacteristic: gatt is null"); return null }
            // Search across all services — the characteristic may not be in the primary service
            val char = gattRef.services.firstNotNullOfOrNull { it.getCharacteristic(characteristicUuid) }
            if (char == null) {
                Log.e("BLE", "readCharacteristic: $characteristicUuid not found in any service")
                return null
            }
            val deferred = CompletableDeferred<String?>()
            pendingRead = deferred
            val initiated = gattRef.readCharacteristic(char) == true
            if (!initiated) {
                Log.e("BLE", "readCharacteristic: gatt.readCharacteristic() returned false (GATT busy or disconnected)")
                pendingRead = null
                return null
            }
            val result = withTimeoutOrNull(BleConstants.BLE_OPERATION_TIMEOUT_MS) { deferred.await() }
            if (result == null) Log.e("BLE", "readCharacteristic: timed out waiting for callback")
            result
        }
    }

    override suspend fun writeCharacteristic(
        address: String,
        characteristicUuid: UUID,
        data: String
    ): Boolean {
        return writeMutex.withLock {
            val gattRef = gatt ?: run { Log.e("BLE", "writeCharacteristic: gatt is null"); return false }
            val service = gattRef.getService(BleConstants.SERVICE_UUID)
            if (service == null) {
                Log.e("BLE", "writeCharacteristic: service ${BleConstants.SERVICE_UUID} not found")
                return false
            }
            val char = service.getCharacteristic(characteristicUuid)
            if (char == null) {
                Log.e("BLE", "writeCharacteristic: characteristic $characteristicUuid not found. Available: ${service.characteristics.map { it.uuid }}")
                return false
            }
            Log.d("BLE", "writeCharacteristic: writing ${data.length} bytes to $characteristicUuid, properties=${char.properties}")
            val bytes = data.toByteArray(Charsets.UTF_8)
            val deferred = CompletableDeferred<Boolean>()
            pendingWrite = deferred
            val initiated = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val result = gattRef.writeCharacteristic(char, bytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                Log.d("BLE", "writeCharacteristic: API33+ result=$result (0=success)")
                result == BluetoothGatt.GATT_SUCCESS
            } else {
                @Suppress("DEPRECATION")
                char.value = bytes
                @Suppress("DEPRECATION")
                char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                @Suppress("DEPRECATION")
                val result = gattRef.writeCharacteristic(char)
                Log.d("BLE", "writeCharacteristic: legacy result=$result")
                result == true
            }
            if (!initiated) {
                Log.e("BLE", "writeCharacteristic: write not initiated")
                pendingWrite = null
                return false
            }
            val result = withTimeoutOrNull(BleConstants.BLE_OPERATION_TIMEOUT_MS) { deferred.await() }
            if (result == null) Log.e("BLE", "writeCharacteristic: timed out waiting for onCharacteristicWrite callback")
            result ?: false
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