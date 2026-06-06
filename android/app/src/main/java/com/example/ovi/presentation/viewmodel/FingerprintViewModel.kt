package com.example.ovi.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ovi.data.api.LockService
import com.example.ovi.data.dto.device.FingerprintDto
import com.example.ovi.data.dto.device.FingerprintRenameRequest
import com.example.ovi.data.websocket.WebSocketManager
import com.example.ovi.data.websocket.WsEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class EnrollState {
    object Idle : EnrollState()
    data class InProgress(val fingerId: Int, val step: Int, val message: String) : EnrollState()
    data class Success(val fingerId: Int) : EnrollState()
    data class Failure(val reason: String) : EnrollState()
}

@HiltViewModel
class FingerprintViewModel @Inject constructor(
    private val lockService: LockService,
    private val webSocketManager: WebSocketManager
) : ViewModel() {

    private val _fingerprints = MutableStateFlow<List<FingerprintDto>>(emptyList())
    val fingerprints: StateFlow<List<FingerprintDto>> = _fingerprints.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _enrollState = MutableStateFlow<EnrollState>(EnrollState.Idle)
    val enrollState: StateFlow<EnrollState> = _enrollState.asStateFlow()

    private val _enrollDone = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val enrollDone: SharedFlow<Unit> = _enrollDone.asSharedFlow()

    private var wsJob: Job? = null
    private var currentDeviceId: String? = null

    fun load(deviceId: String) {
        currentDeviceId = deviceId
        startWatchingEvents(deviceId)
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val r = lockService.getFingerprints(deviceId)
                if (r.isSuccessful) _fingerprints.value = r.body() ?: emptyList()
                else _error.value = "Failed to load fingerprints"
            } catch (e: Exception) {
                _error.value = "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startEnroll(deviceId: String) {
        viewModelScope.launch {
            _enrollState.value = EnrollState.InProgress(0, 0, "Connecting to device…")
            _error.value = null
            try {
                val r = lockService.enrollFingerprint(deviceId)
                if (r.isSuccessful && r.body() != null) {
                    val fingerId = r.body()!!.fingerId
                    _enrollState.value = EnrollState.InProgress(fingerId, 1, "Place your finger on the scanner")
                } else {
                    val msg = when (r.code()) {
                        503 -> "Device is offline"
                        409 -> "All fingerprint slots are full"
                        else -> "Failed to start enrollment"
                    }
                    _enrollState.value = EnrollState.Failure(msg)
                }
            } catch (e: Exception) {
                _enrollState.value = EnrollState.Failure("Network error")
            }
        }
    }

    fun cancelEnroll() {
        _enrollState.value = EnrollState.Idle
    }

    fun rename(deviceId: String, fingerId: Int, name: String) {
        viewModelScope.launch {
            try {
                val r = lockService.renameFingerprint(deviceId, fingerId, FingerprintRenameRequest(name))
                if (r.isSuccessful && r.body() != null) {
                    _fingerprints.value = _fingerprints.value.map {
                        if (it.fingerId == fingerId) r.body()!! else it
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun delete(deviceId: String, fingerId: Int) {
        viewModelScope.launch {
            try {
                val r = lockService.deleteFingerprint(deviceId, fingerId)
                if (r.isSuccessful) {
                    _fingerprints.value = _fingerprints.value.filter { it.fingerId != fingerId }
                }
            } catch (_: Exception) {
                _error.value = "Delete failed"
            }
        }
    }

    private fun startWatchingEvents(deviceId: String) {
        if (wsJob?.isActive == true) return
        wsJob = viewModelScope.launch {
            webSocketManager.events.collect { event ->
                if (event !is WsEvent.DeviceEvent || event.deviceUuid != deviceId) return@collect
                when (event.eventType) {
                    "finger_progress" -> {
                        val step = (event.eventData["step"] as? Number)?.toInt() ?: 0
                        val msg = event.eventData["msg"] as? String ?: ""
                        val fid = (event.eventData["finger_id"] as? Number)?.toInt() ?: 0
                        val currentState = _enrollState.value
                        val activeFid = if (currentState is EnrollState.InProgress) currentState.fingerId else fid
                        val displayMsg = when (step) {
                            1 -> "Place your finger on the scanner"
                            2 -> "Lift your finger"
                            3 -> "Place the same finger again"
                            else -> msg
                        }
                        _enrollState.value = EnrollState.InProgress(activeFid, step, displayMsg)
                    }
                    "finger_enrolled" -> {
                        val success = event.eventData["success"] as? Boolean ?: false
                        val fid = (event.eventData["finger_id"] as? Number)?.toInt() ?: 0
                        if (success) {
                            _enrollState.value = EnrollState.Success(fid)
                            _enrollDone.tryEmit(Unit)
                            load(deviceId)
                        } else {
                            _enrollState.value = EnrollState.Failure("Enrollment failed. Please try again.")
                        }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        wsJob?.cancel()
    }
}
