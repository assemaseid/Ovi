package com.example.ovi.presentation.viewmodel

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ovi.data.api.LockService
import com.example.ovi.data.dto.DeviceInfo
import com.example.ovi.data.dto.DeviceRegistrationRequest
import com.example.ovi.data.dto.OwnerInfo
import com.example.ovi.data.dto.BleDeviceInfo
import com.example.ovi.data.local.SessionManager
import com.example.ovi.domain.ble.BleManager
import com.example.ovi.domain.model.SmartLock
import com.example.ovi.domain.repository.LockRepository
import com.example.ovi.util.BleConstants
import com.example.ovi.util.CryptoUtils
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

sealed class OnboardingState {
    object Idle : OnboardingState()
    object Connecting : OnboardingState()
    object ReadingInfo : OnboardingState()
    object Registering : OnboardingState()
    object Configuring : OnboardingState()
    object Success : OnboardingState()
    data class Error(val message: String) : OnboardingState()
}

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

    private val _onboardingState = MutableStateFlow<OnboardingState>(OnboardingState.Idle)
    val onboardingState: StateFlow<OnboardingState> = _onboardingState.asStateFlow()

    fun startBleScan() = bleManager.startScan()
    fun stopBleScan() = bleManager.stopScan()

    fun pairAndConnect(device: BluetoothDevice) {
        viewModelScope.launch {
            val result = withTimeoutOrNull(60_000L) {
                runOnboarding(device)
            }
            if (result == null) {
                _onboardingState.value = OnboardingState.Error("Onboarding timed out (60s)")
            }
        }
    }

    private suspend fun runOnboarding(device: BluetoothDevice) {
        try {
            _onboardingState.value = OnboardingState.Connecting
            bleManager.connect(device.address)
            
            val connected = withTimeoutOrNull(BleConstants.BLE_CONNECTION_TIMEOUT_MS) {
                bleManager.connectedDeviceAddress.first { it != null }
            }
            if (connected == null) {
                _onboardingState.value = OnboardingState.Error("Connection timed out")
                return
            }

            val servicesReady = withTimeoutOrNull(10_000L) {
                bleManager.isServicesReady.first { it }
            }
            if (servicesReady == null) {
                _onboardingState.value = OnboardingState.Error("Service discovery timed out")
                return
            }

            _onboardingState.value = OnboardingState.ReadingInfo
            val rawInfo = bleManager.readCharacteristic(device.address, BleConstants.CHAR_INFO_READ)
            if (rawInfo == null) {
                _onboardingState.value = OnboardingState.Error("Failed to read device info")
                return
            }

            val info = try {
                Gson().fromJson(rawInfo, BleDeviceInfo::class.java)
            } catch (e: Exception) {
                _onboardingState.value = OnboardingState.Error("Invalid device info format")
                return
            }

            val deviceName = try { device.name ?: "Smart Lock" } catch (_: SecurityException) { "Smart Lock" }
            var lockId = UUID.randomUUID().toString()
            
            _onboardingState.value = OnboardingState.Registering
            var serverPublicKey: String? = null
            try {
                val registrationRequest = DeviceRegistrationRequest(
                    device = DeviceInfo(
                        device_id = info.data.device_id,
                        public_key = info.data.public_key,
                        type = "smart_lock_v2"
                    ),
                    owner_info = OwnerInfo(
                        user_id = sessionManager.getUserId().toString(),
                        location = "Home"
                    )
                )

                val response = lockService.registerDevice(registrationRequest)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        lockId = body.device_uuid
                        serverPublicKey = body.server_public_key
                    }
                }
            } catch (e: Exception) {
                // Backend unavailable for now — device will still be saved locally with a local ID
            }
            
            val sessionKey = CryptoUtils.generateSessionKey()
            
            _onboardingState.value = OnboardingState.Configuring
            if (serverPublicKey != null) {
                val configPacket = JSONObject().apply {
                    put("server_public_key", serverPublicKey)
                    put("session_key", android.util.Base64.encodeToString(sessionKey, android.util.Base64.NO_WRAP))
                }.toString()

                val writeOk = bleManager.writeCharacteristic(
                    device.address,
                    BleConstants.CHAR_COMMAND_WRITE,
                    configPacket
                )
                if (!writeOk) {
                    _onboardingState.value = OnboardingState.Error("Failed to write config to device")
                    return
                }
                
                val notified = withTimeoutOrNull(10_000L) {
                    bleManager.notifications.first { (uuid, value) ->
                        uuid == BleConstants.CHAR_STATUS_NOTIFY && value.contains("configured")
                    }
                }
                if (notified == null) {
                    _onboardingState.value = OnboardingState.Error("Lock did not confirm configuration")
                    return
                }
                
                sessionManager.saveSessionKey(lockId, sessionKey)
            }
            
            val newLock = SmartLock(
                id = lockId,
                hardwareId = info.data.device_id,
                ownerUuid = sessionManager.getUserId().toString(),
                name = deviceName,
                publicKey = info.data.public_key,
                batteryLevel = info.data.battery_level,
                isLocked = true,
                firmwareVersion = info.data.fw_version,
                lastSynced = System.currentTimeMillis()
            )
            lockRepository.addLock(newLock)
            _onboardingState.value = OnboardingState.Success

        } catch (e: Exception) {
            e.printStackTrace()
            _onboardingState.value = OnboardingState.Error("Unexpected error: ${e.message}")
        }
    }

    fun resetOnboardingState() {
        _onboardingState.value = OnboardingState.Idle
    }
}