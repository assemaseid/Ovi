package com.example.ovi.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ovi.data.api.LockService
import com.example.ovi.data.dto.device.PinScheduleRequest
import com.example.ovi.data.dto.device.PinScheduleResponse
import com.example.ovi.data.websocket.WebSocketManager
import com.example.ovi.data.websocket.WsEvent
import com.example.ovi.domain.repository.LockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigInteger
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

@HiltViewModel
class AutoPinViewModel @Inject constructor(
    private val lockRepository: LockRepository,
    private val lockService: LockService,
    private val webSocketManager: WebSocketManager
) : ViewModel() {

    private val _currentPin = MutableStateFlow<String?>(null)
    val currentPin: StateFlow<String?> = _currentPin.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _schedule = MutableStateFlow<PinScheduleResponse?>(null)
    val schedule: StateFlow<PinScheduleResponse?> = _schedule.asStateFlow()

    private val _saveSuccess = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saveSuccess: SharedFlow<Unit> = _saveSuccess.asSharedFlow()

    private var pinWatchJob: Job? = null
    private var watchedLockId: String? = null

    private fun startWatchingPin(deviceId: String) {
        if (watchedLockId == deviceId) return
        watchedLockId = deviceId
        pinWatchJob?.cancel()
        pinWatchJob = viewModelScope.launch {
            webSocketManager.events.collect { event ->
                if (event is WsEvent.PinRotated && event.deviceUuid == deviceId) {
                    _currentPin.value = event.newPin
                    val current = _schedule.value
                    if (current != null) {
                        _schedule.value = current.copy(
                            current_pin = event.newPin,
                            next_rotation_at = event.nextRotationAt ?: current.next_rotation_at
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pinWatchJob?.cancel()
    }

    fun loadSchedule(deviceId: String) {
        startWatchingPin(deviceId)
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = lockService.getPinSchedule(deviceId)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    _schedule.value = body
                    _currentPin.value = body.current_pin
                } else {
                    loadLocalPin(deviceId)
                }
            } catch (e: Exception) {
                loadLocalPin(deviceId)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshPin(deviceId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val rotateResponse = lockService.rotatePinOnServer(deviceId)
                if (rotateResponse.isSuccessful && rotateResponse.body() != null) {
                    _currentPin.value = rotateResponse.body()!!.message
                    // Also refresh schedule state
                    val scheduleResponse = lockService.getPinSchedule(deviceId)
                    if (scheduleResponse.isSuccessful && scheduleResponse.body() != null) {
                        _schedule.value = scheduleResponse.body()
                    }
                } else {
                    loadLocalPin(deviceId)
                }
            } catch (e: Exception) {
                _error.value = "Refresh failed"
                loadLocalPin(deviceId)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveSchedule(deviceId: String, enabled: Boolean, intervalHours: Int, nextRotationAt: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = lockService.updatePinSchedule(
                    deviceId,
                    PinScheduleRequest(enabled, intervalHours, nextRotationAt)
                )
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    _schedule.value = body
                    _currentPin.value = body.current_pin
                    _saveSuccess.tryEmit(Unit)
                } else {
                    _error.value = "Failed to save schedule"
                }
            } catch (e: Exception) {
                _error.value = "Network error. Check your connection."
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadLocalPin(deviceId: String) {
        val lock = lockRepository.getLockById(deviceId) ?: run {
            _error.value = "Device not found"
            return
        }
        if (lock.deviceSecret.isEmpty()) {
            _error.value = "Device secret not available. Re-pair the lock to enable Auto PIN."
            return
        }
        _currentPin.value = computePin(lock.deviceSecret, lock.rotationHours)
    }

    fun getNextRotationMillis(rotationHours: Int): Long {
        val rotationSeconds = rotationHours * 3600L
        val currentSlot = System.currentTimeMillis() / 1000L / rotationSeconds
        return (currentSlot + 1) * rotationSeconds * 1000L
    }

    private fun computePin(deviceSecret: String, rotationHours: Int): String {
        val rotationSeconds = rotationHours * 3600L
        val timeSlot = System.currentTimeMillis() / 1000L / rotationSeconds
        val keyBytes = deviceSecret.toByteArray(Charsets.UTF_8)
        val messageBytes = timeSlot.toString().toByteArray(Charsets.UTF_8)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(keyBytes, "HmacSHA256"))
        val hashBytes = mac.doFinal(messageBytes)
        val pinNum = BigInteger(1, hashBytes).mod(BigInteger.valueOf(1_000_000L)).toLong()
        return String.format("%06d", pinNum)
    }
}
