package com.example.ovi.presentation.viewmodel

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ovi.data.api.LockService
import com.example.ovi.data.dto.ble.BleDeviceInfo
import com.example.ovi.data.dto.grant.GuestJoinBody
import com.example.ovi.data.dto.grant.GuestRequestBody
import com.example.ovi.domain.ble.BleManager
import com.example.ovi.domain.repository.LockRepository
import com.example.ovi.util.BleConstants
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import javax.inject.Inject

sealed class GuestJoinState {
    object Idle : GuestJoinState()
    object RequestingAccess : GuestJoinState()
    data class WaitingForPin(val deviceUuid: String) : GuestJoinState()
    object Joining : GuestJoinState()
    object Success : GuestJoinState()
    data class Error(val message: String) : GuestJoinState()
}

@HiltViewModel
class GuestJoinViewModel @Inject constructor(
    private val bleManager: BleManager,
    private val lockService: LockService,
    private val lockRepository: LockRepository,
) : ViewModel() {

    val isScanning = bleManager.isScanning
    val scannedDevices = bleManager.scannedDevices
    val connectedAddress = bleManager.connectedDeviceAddress

    private val _state = MutableStateFlow<GuestJoinState>(GuestJoinState.Idle)
    val state: StateFlow<GuestJoinState> = _state.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    private var pendingDeviceUuid: String? = null

    fun startBleScan() = bleManager.startScan()
    fun stopBleScan() = bleManager.stopScan()

    fun requestAccess(device: BluetoothDevice) {
        viewModelScope.launch {
            _state.value = GuestJoinState.RequestingAccess
            try {
                bleManager.connect(device.address)
                val connected = withTimeoutOrNull(BleConstants.BLE_CONNECTION_TIMEOUT_MS) {
                    bleManager.connectedDeviceAddress.first { it != null }
                }
                if (connected == null) {
                    _state.value = GuestJoinState.Error("Connection timed out")
                    return@launch
                }
                val servicesReady = withTimeoutOrNull(10_000L) {
                    bleManager.isServicesReady.first { it }
                }
                if (servicesReady == null) {
                    bleManager.disconnect()
                    _state.value = GuestJoinState.Error("Service discovery timed out")
                    return@launch
                }
                delay(500L)

                val infoCmd = """{"cmd":"get_info","req_id":"${UUID.randomUUID()}","timestamp":${System.currentTimeMillis() / 1000}}"""
                var cmdSent = false
                val infoResponse = coroutineScope {
                    val notifJob = async(start = CoroutineStart.UNDISPATCHED) {
                        withTimeoutOrNull(10_000L) {
                            bleManager.notifications.first { (uuid, value) ->
                                uuid == BleConstants.CHAR_STATUS_NOTIFY && value.contains("info_response")
                            }
                        }
                    }
                    cmdSent = bleManager.writeCharacteristic(device.address, BleConstants.CHAR_COMMAND_WRITE, infoCmd)
                    if (cmdSent) notifJob.await() else { notifJob.cancel(); null }
                }

                bleManager.disconnect()

                if (!cmdSent || infoResponse == null) {
                    _state.value = GuestJoinState.Error("Failed to read device info")
                    return@launch
                }

                val info = Gson().fromJson(infoResponse.second, BleDeviceInfo::class.java)
                val hardwareId = info.data.device_id

                val response = lockService.requestGuestAccess(GuestRequestBody(hardwareId))
                if (!response.isSuccessful) {
                    _state.value = GuestJoinState.Error("Device not registered or server error")
                    return@launch
                }

                pendingDeviceUuid = response.body()!!.deviceUuid
                _state.value = GuestJoinState.WaitingForPin(pendingDeviceUuid!!)

            } catch (e: Exception) {
                bleManager.disconnect()
                _state.value = GuestJoinState.Error("Error: ${e.message}")
            }
        }
    }

    fun submitPin(pin: String) {
        val deviceUuid = pendingDeviceUuid ?: return
        viewModelScope.launch {
            _state.value = GuestJoinState.Joining
            try {
                val response = lockService.joinAsGuest(deviceUuid, GuestJoinBody(pin))
                if (response.isSuccessful) {
                    lockRepository.syncDevicesFromServer()
                    _state.value = GuestJoinState.Success
                } else {
                    _errorMessage.tryEmit("Invalid or expired PIN")
                    _state.value = GuestJoinState.WaitingForPin(deviceUuid)
                }
            } catch (e: Exception) {
                _errorMessage.tryEmit("Network error: ${e.message}")
                _state.value = GuestJoinState.WaitingForPin(deviceUuid)
            }
        }
    }

    fun resetState() {
        _state.value = GuestJoinState.Idle
        pendingDeviceUuid = null
    }
}
