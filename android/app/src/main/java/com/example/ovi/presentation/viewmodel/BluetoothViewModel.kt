package com.example.ovi.presentation.viewmodel

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ovi.data.api.LockService
import com.example.ovi.data.dto.ble.BleDeviceInfo
import com.example.ovi.data.dto.device.DeviceInfo
import com.example.ovi.data.dto.device.DeviceRegistrationRequest
import com.example.ovi.data.dto.device.DeviceRegistrationResponse
import com.example.ovi.data.dto.device.OwnerInfo
import com.example.ovi.data.local.SessionManager
import com.example.ovi.domain.ble.BleManager
import com.example.ovi.domain.model.SmartLock
import com.example.ovi.domain.repository.LockRepository
import com.example.ovi.util.BleConstants
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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

    fun pairAndConnect(device: BluetoothDevice, wifiSsid: String, wifiPassword: String) {
        viewModelScope.launch {
            val result = withTimeoutOrNull(60_000L) {
                runOnboarding(device, wifiSsid, wifiPassword)
            }
            if (result == null) {
                _onboardingState.value = OnboardingState.Error("Onboarding timed out (60s)")
            }
        }
    }

    private suspend fun runOnboarding(device: BluetoothDevice, wifiSsid: String, wifiPassword: String) {
        try {
            _onboardingState.value = OnboardingState.Connecting
            android.util.Log.d("ONBOARD", "1. connecting to ${device.address}")
            bleManager.connect(device.address)

            val connected = withTimeoutOrNull(BleConstants.BLE_CONNECTION_TIMEOUT_MS) {
                bleManager.connectedDeviceAddress.first { it != null }
            }
            if (connected == null) {
                android.util.Log.e("ONBOARD", "2. connection timed out")
                _onboardingState.value = OnboardingState.Error("Connection timed out")
                return
            }
            android.util.Log.d("ONBOARD", "2. connected ok")

            val servicesReady = withTimeoutOrNull(10_000L) {
                bleManager.isServicesReady.first { it }
            }
            if (servicesReady == null) {
                android.util.Log.e("ONBOARD", "3. service discovery timed out")
                _onboardingState.value = OnboardingState.Error("Service discovery timed out")
                return
            }
            android.util.Log.d("ONBOARD", "3. services ready")

            delay(500L)

            _onboardingState.value = OnboardingState.ReadingInfo
            android.util.Log.d("ONBOARD", "4. sending get_info")
            val infoReqId = UUID.randomUUID().toString()
            val getInfoCmd = """{"cmd":"get_info","req_id":"$infoReqId"}"""

            // Subscribe BEFORE writing — if we write first the notification may arrive before
            // the collector is active and be silently dropped by SharedFlow.
            var cmdSent = false
            val infoNotification = coroutineScope {
                val notifJob = async(start = CoroutineStart.UNDISPATCHED) {
                    withTimeoutOrNull(10_000L) {
                        bleManager.notifications.first { (uuid, value) ->
                            uuid == BleConstants.CHAR_STATUS_NOTIFY && value.contains("info_response")
                        }
                    }
                }
                cmdSent = bleManager.writeCharacteristic(device.address, BleConstants.CHAR_COMMAND_WRITE, getInfoCmd)
                if (cmdSent) notifJob.await() else { notifJob.cancel(); null }
            }
            if (!cmdSent) {
                android.util.Log.e("ONBOARD", "4. failed to send get_info")
                _onboardingState.value = OnboardingState.Error("Failed to send get_info command")
                return
            }
            if (infoNotification == null) {
                android.util.Log.e("ONBOARD", "4. no info_response (timeout)")
                _onboardingState.value = OnboardingState.Error("No info response from device (timeout)")
                return
            }
            android.util.Log.d("ONBOARD", "4. got info_response ok")
            val rawInfo = infoNotification.second
            android.util.Log.d("BLE", "Info response: $rawInfo")

            val info = try {
                Gson().fromJson(rawInfo, BleDeviceInfo::class.java)
            } catch (e: Exception) {
                android.util.Log.e("BLE", "JSON parse failed: ${e.message}, raw=$rawInfo")
                _onboardingState.value = OnboardingState.Error("Invalid device info format")
                return
            }

            val deviceName = try { device.name ?: "Smart Lock" } catch (_: SecurityException) { "Smart Lock" }
            var lockId = UUID.randomUUID().toString()

            _onboardingState.value = OnboardingState.Registering
            var registrationBody: DeviceRegistrationResponse? = null
            var alreadyRegistered = false
            try {
                val registrationRequest = DeviceRegistrationRequest(
                    device = DeviceInfo(
                        hardware_id = info.data.device_id,
                        public_key = info.data.public_key,
                        type = "smart_lock_v2"
                    ),
                    owner_info = OwnerInfo(
                        user_uuid = sessionManager.getUserId().toString(),
                        location = "Home"
                    )
                )

                val response = lockService.registerDevice(registrationRequest)
                android.util.Log.d("REGISTER", "response code: ${response.code()}")
                if (response.isSuccessful) {
                    val body = response.body()
                    android.util.Log.d("REGISTER", "body: $body")
                    if (body != null) {
                        lockId = body.device_uuid
                        registrationBody = body
                    }
                } else if (response.code() == 409) {
                    android.util.Log.d("REGISTER", "409: already registered, fetching existing device")
                    val devicesResponse = lockService.getDevices()
                    if (devicesResponse.isSuccessful) {
                        val existing = devicesResponse.body()
                            ?.firstOrNull { it.hardwareId == info.data.device_id }
                        if (existing != null) {
                            android.util.Log.d("REGISTER", "found: ${existing.deviceUuid}")
                            lockId = existing.deviceUuid
                            alreadyRegistered = true
                        }
                    }
                } else {
                    android.util.Log.e("REGISTER", "error: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("REGISTER", "exception: ${e.message}", e)
            }

            if (registrationBody == null && !alreadyRegistered) {
                _onboardingState.value = OnboardingState.Error("Server registration failed. Check internet connection and try again.")
                return
            }

            _onboardingState.value = OnboardingState.Configuring

            // BLE may have dropped during server registration — reconnect if needed
            if (bleManager.connectedDeviceAddress.value == null) {
                android.util.Log.d("ONBOARD", "5. BLE dropped — disconnecting old gatt and reconnecting")
                bleManager.disconnect()  // stop any auto-reconnect first
                delay(500L)
                bleManager.connect(device.address)
                val reconnected = withTimeoutOrNull(BleConstants.BLE_CONNECTION_TIMEOUT_MS) {
                    bleManager.connectedDeviceAddress.first { it != null }
                }
                if (reconnected == null) {
                    android.util.Log.e("ONBOARD", "5. reconnect timed out")
                    _onboardingState.value = OnboardingState.Error("BLE disconnected after registration, could not reconnect")
                    return
                }
                android.util.Log.d("ONBOARD", "5. reconnected ok, waiting for services")
                val servicesReady2 = withTimeoutOrNull(15_000L) {
                    bleManager.isServicesReady.first { it }
                }
                if (servicesReady2 == null) {
                    android.util.Log.e("ONBOARD", "5. services not ready after reconnect")
                    _onboardingState.value = OnboardingState.Error("BLE reconnected but services not ready")
                    return
                }
                delay(500L)
                android.util.Log.d("ONBOARD", "5. reconnect complete, continuing with config")
            } else {
                android.util.Log.d("ONBOARD", "5. BLE still connected, no reconnect needed")
            }

            val regBody = registrationBody
            android.util.Log.d("ONBOARD", "regBody=${regBody != null}, connectedAddr=${bleManager.connectedDeviceAddress.value}")
            if (regBody != null) {
                val configPacket = JSONObject().apply {
                    put("cmd", "config")
                    put("device_uuid", regBody.device_uuid)
                    put("server_public_key", regBody.server_public_key)
                    put("device_secret", regBody.device_secret)
                    put("wifi_ssid", wifiSsid)
                    put("wifi_password", wifiPassword)
                    put("mqtt_broker", regBody.mqtt_config.broker)
                    put("mqtt_port", regBody.mqtt_config.port)
                    put("mqtt_client_id", regBody.mqtt_config.client_id)
                }.toString()

                android.util.Log.d("ONBOARD", "sending config packet (${configPacket.length} bytes)")

                var writeOk = false
                val notified = coroutineScope {
                    val notifJob = async(start = CoroutineStart.UNDISPATCHED) {
                        withTimeoutOrNull(10_000L) {
                            bleManager.notifications.first { (uuid, value) ->
                                uuid == BleConstants.CHAR_STATUS_NOTIFY && value.contains("configured")
                            }
                        }
                    }
                    writeOk = bleManager.writeCharacteristic(
                        device.address,
                        BleConstants.CHAR_COMMAND_WRITE,
                        configPacket
                    )
                    android.util.Log.d("ONBOARD", "writeCharacteristic result: $writeOk")
                    if (writeOk) notifJob.await() else { notifJob.cancel(); null }
                }
                android.util.Log.d("ONBOARD", "notified=${notified != null}")
                if (!writeOk) {
                    _onboardingState.value = OnboardingState.Error("Failed to write config to device")
                    return
                }
                if (notified == null) {
                    _onboardingState.value = OnboardingState.Error("Lock did not confirm configuration")
                    return
                }
            } else {
                android.util.Log.d("ONBOARD", "skipping config (alreadyRegistered=$alreadyRegistered)")
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
                lastSynced = System.currentTimeMillis(),
                deviceSecret = regBody?.device_secret.orEmpty(),
                rotationHours = regBody?.config?.rotation_hours ?: 24
            )
            lockRepository.addLock(newLock)
            _onboardingState.value = OnboardingState.Success

        } catch (e: Exception) {
            e.printStackTrace()
            _onboardingState.value = OnboardingState.Error("Unexpected error: ${e.message}")
        } finally {
            if (_onboardingState.value !is OnboardingState.Success) {
                bleManager.disconnect()
            }
        }
    }

    fun resetOnboardingState() {
        _onboardingState.value = OnboardingState.Idle
    }
}