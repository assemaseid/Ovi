package com.example.ovi.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ovi.data.local.dao.LockDao
import com.example.ovi.data.mapper.toLockDomain
import com.example.ovi.domain.model.DeviceItem
import com.example.ovi.domain.model.EventType
import com.example.ovi.domain.model.LockEvent
import com.example.ovi.domain.model.UnlockMethod
import com.example.ovi.domain.ble.BleManager
import com.example.ovi.domain.repository.EventRepository
import com.example.ovi.domain.repository.LockRepository
import com.example.ovi.data.websocket.WebSocketManager
import com.example.ovi.data.websocket.WsEvent
import com.example.ovi.util.LockNotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.ovi.data.dto.BleDeviceInfo
import com.example.ovi.util.BleConstants
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

sealed class LockOperationState {
    object Idle : LockOperationState()
    object Loading : LockOperationState()
    data class Success(val message: String) : LockOperationState()
    data class Error(val message: String) : LockOperationState()
}

@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val lockRepository: LockRepository,
    private val lockDao: LockDao,
    private val eventRepository: EventRepository,
    private val bleManager: BleManager,
    private val wsManager: WebSocketManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    init {
        wsManager.connect()
        viewModelScope.launch { lockRepository.syncDevicesFromServer() }
        viewModelScope.launch {
            bleManager.isServicesReady.collect { ready ->
                if (ready) lockRepository.getDeviceInfo()
            }
        }
        viewModelScope.launch { collectWsEvents() }
    }

    private suspend fun collectWsEvents() {
        wsManager.events.collect { event ->
            if (event !is WsEvent.DeviceEvent) return@collect
            val entity = lockDao.getLockById(event.deviceUuid) ?: return@collect
            when (event.eventType) {
                "unlock_success" -> {
                    lockDao.updateLock(entity.copy(isLocked = false, lastSynced = System.currentTimeMillis()))
                    // Only log and notify if BLE not connected — remote unlock via MQTT.
                    // If BLE is connected, toggleLock() already logged the event.
                    if (bleManager.connectedDeviceAddress.value == null) {
                        eventRepository.addEvent(LockEvent(
                            id = "", lockId = event.deviceUuid,
                            timestamp = System.currentTimeMillis(),
                            type = EventType.UNLOCK, success = true, method = UnlockMethod.REMOTE
                        ))
                        LockNotificationHelper.show(context, entity.name, "Unlocked remotely")
                    }
                }
                "lock_success" -> {
                    lockDao.updateLock(entity.copy(isLocked = true, lastSynced = System.currentTimeMillis()))
                    if (bleManager.connectedDeviceAddress.value == null) {
                        eventRepository.addEvent(LockEvent(
                            id = "", lockId = event.deviceUuid,
                            timestamp = System.currentTimeMillis(),
                            type = EventType.LOCK, success = true, method = UnlockMethod.REMOTE
                        ))
                    }
                }
                "tamper_detected" -> {
                    eventRepository.addEvent(LockEvent(
                        id = "", lockId = event.deviceUuid,
                        timestamp = System.currentTimeMillis(),
                        type = EventType.TAMPER_DETECTED, success = true, method = UnlockMethod.MANUAL
                    ))
                    LockNotificationHelper.show(context, entity.name, "Tamper detected!")
                }
                "low_battery", "battery_low" -> {
                    eventRepository.addEvent(LockEvent(
                        id = "", lockId = event.deviceUuid,
                        timestamp = System.currentTimeMillis(),
                        type = EventType.LOW_BATTERY, success = true, method = UnlockMethod.MANUAL
                    ))
                    LockNotificationHelper.show(context, entity.name, "Battery low")
                }
            }
        }
    }

    val devices: StateFlow<List<DeviceItem>> = lockDao.getAllLocks()
        .map { entities ->
            entities.map { entity ->
                val lock = entity.toLockDomain()
                DeviceItem(
                    id = lock.id,
                    name = lock.name,
                    battery_level = lock.batteryLevel,
                    locked = lock.isLocked,
                    lastSeen = formatTimestamp(lock.lastSynced)
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _operationState = MutableStateFlow<LockOperationState>(LockOperationState.Idle)
    val operationState: StateFlow<LockOperationState> = _operationState.asStateFlow()

    fun toggleLock(lockId: String) {
        viewModelScope.launch {
            val lock = devices.value.find { it.id == lockId } ?: return@launch
            _operationState.value = LockOperationState.Loading
            
            if (bleManager.connectedDeviceAddress.value == null) {
                val success = if (lock.locked) {
                    lockRepository.remoteUnlock(lockId)
                } else {
                    lockRepository.remoteLock(lockId)
                }
                if (success) {
                    val msg = if (lock.locked) "Unlock command sent, waiting..." else "Lock command sent, waiting..."
                    _operationState.value = LockOperationState.Success(msg)
                } else {
                    _operationState.value = LockOperationState.Error("No internet connection or session expired")
                }
                delay(3_000)
                _operationState.value = LockOperationState.Idle
                return@launch
            }

            val success = if (lock.locked) {
                lockRepository.unlock(lockId)
            } else {
                lockRepository.lock(lockId)
            }

            val eventType = if (lock.locked) EventType.UNLOCK else EventType.LOCK
            _operationState.value = if (success) {
                eventRepository.addEvent(
                    LockEvent(
                        id = "",
                        lockId = lockId,
                        timestamp = System.currentTimeMillis(),
                        type = eventType,
                        success = true,
                        method = UnlockMethod.BLUETOOTH
                    )
                )
                val msg = if (lock.locked) "Unlocked successfully" else "Locked"
                LockNotificationHelper.show(context, lock.name, msg)
                LockOperationState.Success(msg)
            } else {
                LockOperationState.Error(if (lock.locked) "Unlock failed" else "Lock failed")
            }

            delay(2_000)
            _operationState.value = LockOperationState.Idle
        }
    }

    fun deleteDevice(lockId: String) {
        viewModelScope.launch {
            lockRepository.deleteDevice(lockId)
        }
    }

    fun refreshBattery(lockId: String) {
        viewModelScope.launch { doRefreshBattery(lockId) }
    }

    private suspend fun doRefreshBattery(lockId: String) {
        if (bleManager.connectedDeviceAddress.value == null) return
        val cmd = """{"cmd":"get_info","req_id":"${UUID.randomUUID()}"}"""
        val notification = coroutineScope {
            val notifJob = async(start = CoroutineStart.UNDISPATCHED) {
                withTimeoutOrNull(10_000L) {
                    bleManager.notifications.first { (uuid, value) ->
                        uuid == BleConstants.CHAR_STATUS_NOTIFY && value.contains("info_response")
                    }
                }
            }
            val sent = bleManager.sendMessage(cmd)
            if (sent) notifJob.await() else { notifJob.cancel(); null }
        } ?: return
        val battery = try {
            Gson().fromJson(notification.second, BleDeviceInfo::class.java).data.battery_level
        } catch (e: Exception) {
            return
        }
        lockDao.getLockById(lockId)?.let { entity ->
            lockDao.updateLock(entity.copy(batteryLevel = battery))
        }
    }

    private fun formatTimestamp(millis: Long): String {
        if (millis == 0L) return "—"
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
    }
}