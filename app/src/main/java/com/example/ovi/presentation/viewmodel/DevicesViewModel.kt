package com.example.ovi.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ovi.domain.ble.BleManager
import com.example.ovi.domain.model.EventType
import com.example.ovi.domain.model.LockEvent
import com.example.ovi.domain.model.SmartLock
import com.example.ovi.domain.model.UnlockMethod
import com.example.ovi.domain.repository.AuthRepository
import com.example.ovi.domain.repository.EventRepository
import com.example.ovi.domain.repository.LockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val lockRepository: LockRepository,
    private val eventRepository: EventRepository,
    private val bleManager: BleManager
): ViewModel(){

    private val _devices = MutableStateFlow<List<SmartLock>>(emptyList())
    val devices: StateFlow<List<SmartLock>> = _devices.asStateFlow()

    init {
        loadDevices()
    }

    private fun loadDevices(){
        viewModelScope.launch {
            _devices.value = lockRepository.getPairedLocks()
        }
    }

    fun toggleLock(lockId: String){
        viewModelScope.launch {
            val lock = _devices.value.find { it.id == lockId } ?: return@launch

            val success = if (lock.isLocked) {
                lockRepository.unlock(lockId)
            } else {
                lockRepository.lock(lockId)
            }

            eventRepository.addEvent(
                LockEvent(
                    id = "",
                    lockId = lockId,
                    timestamp = System.currentTimeMillis(),
                    type = if (lock.isLocked) EventType.UNLOCK else EventType.LOCK,
                    success = success,
                    method = UnlockMethod.MANUAL
                )
            )

            if (success) {
                _devices.value = devices.value.map {
                    if (it.id == lockId) it.copy(isLocked = !it.isLocked) else it
                }
            }
        }
    }
}