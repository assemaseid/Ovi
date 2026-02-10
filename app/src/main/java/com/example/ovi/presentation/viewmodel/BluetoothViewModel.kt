package com.example.ovi.presentation.viewmodel

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ovi.data.remote.dto.*
import com.example.ovi.data.api.LockService
import com.example.ovi.data.local.SessionManager
import com.example.ovi.data.remote.dto.BleDeviceInfo
import com.example.ovi.domain.ble.BleManager
import com.example.ovi.domain.model.SmartLock
import com.example.ovi.domain.repository.LockRepository
import com.example.ovi.util.BleConstants
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val bleManager: BleManager,
    private val lockService: LockService,
    private val lockRepository: LockRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val isScanning = bleManager.isScanning
    val scannedDevices = bleManager.scannedDevices
    val connectedAddress = bleManager.connectedDeviceAddress

    fun startBleScan() = bleManager.startScan()
    fun stopBleScan() = bleManager.stopScan()

    fun pairAndConnect(device: BluetoothDevice) {
        viewModelScope.launch {
            try{
                bleManager.connect(device.address)

                val rawInfo = bleManager.readCharacteristic(device.address, BleConstants.CHAR_INFO_READ) ?: return@launch
                val info = Gson().fromJson(rawInfo, BleDeviceInfo::class.java)

                val registrationRequest = DeviceRegistrationRequest(
                    device = DeviceInfo(
                        device_id = info.data.device_id,
                        public_key = info.data.public_key,
                        type = "smart_lock_v2"
                    ),
                    owner_info = OwnerInfo(
                        user_id = sessionManager.getUserId() ?: "",
                        location = "Home"
                    )
                )

                val response = lockService.registerDevice(registrationRequest)

                if (response.isSuccessful) {
                    val body = response.body() ?: return@launch

                    bleManager.writeCharacteristic(
                        device.address,
                        BleConstants.CHAR_COMMAND_WRITE,
                        body.server_public_key
                    )

                    val newLock = SmartLock(
                        id = body.device_uuid,
                        hardwareId = info.data.device_id,
                        ownerUuid = sessionManager.getUserId() ?: "",
                        name = device.name ?: "Smart Lock",
                        publicKey = info.data.public_key,
                        batteryLevel = info.data.battery_level,
                        isLocked = true,
                        firmwareVersion = info.data.fw_version,
                        lastSynced = System.currentTimeMillis()
                    )

                    lockRepository.addLock(newLock)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}