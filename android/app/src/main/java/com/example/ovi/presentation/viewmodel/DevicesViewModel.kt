package com.example.ovi.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ovi.data.local.dao.LockDao
import com.example.ovi.data.local.entity.LockEntity
import com.example.ovi.data.mapper.toLockDomain
import com.example.ovi.domain.model.DeviceItem
import com.example.ovi.domain.model.EventType
import com.example.ovi.domain.model.LockEvent
import com.example.ovi.domain.model.UnlockMethod
import com.example.ovi.domain.ble.BleManager
import com.example.ovi.domain.repository.EventRepository
import com.example.ovi.domain.repository.LockRepository
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
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    val connectedAddress = bleManager.connectedDeviceAddress

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
                val entity = lockDao.getLockById(lockId)
                if (entity != null) {
                    lockDao.updateLock(entity.copy(isLocked = !entity.isLocked, lastSynced = System.currentTimeMillis()))
                    val msg = if (lock.locked) "Unlocked (no BLE)" else "Locked (no BLE)"
                    LockNotificationHelper.show(context, lock.name, msg)
                    _operationState.value = LockOperationState.Success(msg)
                } else {
                    _operationState.value = LockOperationState.Error("Lock not found")
                }
                delay(2_000)
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

    /** fake locks for testing */
    fun seedTestData() {
        viewModelScope.launch {
            lockDao.insertLock(
                LockEntity(
                    deviceId = "test-lock-01",
                    hardwareId = "HW001",
                    ownerUuid = "test-user",
                    name = "Front Door",
                    publicKey = "",
                    batteryLevel = 87,
                    isLocked = true,
                    firmwareVersion = "1.0.0",
                    lastSynced = System.currentTimeMillis()
                )
            )
            lockDao.insertLock(
                LockEntity(
                    deviceId = "test-lock-02",
                    hardwareId = "HW002",
                    ownerUuid = "test-user",
                    name = "Garage",
                    publicKey = "",
                    batteryLevel = 42,
                    isLocked = false,
                    firmwareVersion = "1.0.0",
                    lastSynced = System.currentTimeMillis() - 3_600_000
                )
            )
        }
    }

    
     //For UI notification testing
    fun toggleLockLocal(lockId: String) {
        viewModelScope.launch {
            val device = devices.value.find { it.id == lockId } ?: return@launch
            _operationState.value = LockOperationState.Loading
            val entity = lockDao.getLockById(lockId) ?: return@launch
            val willBeUnlocked = entity.isLocked
            lockDao.updateLock(
                entity.copy(isLocked = !entity.isLocked, lastSynced = System.currentTimeMillis())
            )
            val msg = if (willBeUnlocked) "Unlocked (local test)" else "Locked (local test)"
            LockNotificationHelper.show(context, device.name, msg)
            _operationState.value = LockOperationState.Success(msg)
            delay(2_000)
            _operationState.value = LockOperationState.Idle
        }
    }

    private fun formatTimestamp(millis: Long): String {
        if (millis == 0L) return "—"
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
    }
}