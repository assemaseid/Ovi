package com.example.ovi.presentation.ui.screens.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ovi.domain.model.SmartLock
import com.example.ovi.domain.repository.LockRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeviceItem(
    val id: String,
    val name: String,
    val isOnline: Boolean,
    val isLocked: Boolean
)
class DevicesViewModel(
    private val lockRepository: LockRepository
) : ViewModel() {

    private val _devices = MutableStateFlow<List<SmartLock>>(emptyList())
    val devices: StateFlow<List<SmartLock>> = _devices.asStateFlow()

    init {
        loadDevices()
    }

    private fun loadDevices() {
        viewModelScope.launch {
            _devices.value = lockRepository.getPairedLocks()
        }
    }

    fun toggleLock(lockId: String){
        viewModelScope.launch {
            val device = _devices.value.find { it.id == lockId } ?: return@launch

            val success = if (device.isLocked) {
                lockRepository.unlock(lockId)
            } else {
                lockRepository.lock(lockId)
            }

            if(success) {
                _devices.value = _devices.value.map {
                    if (it.id == lockId) it.copy(isLocked = !it.isLocked) else it
                }
            }
        }
    }

}

