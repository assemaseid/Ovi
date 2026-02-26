package com.example.ovi.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.example.ovi.domain.model.DeviceItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import kotlin.collections.map

class DevicesViewModel: ViewModel(){

    private val _devices = MutableStateFlow(
        listOf(
            DeviceItem("lock-1", "Smart Lock #1", 64, false, "20:30"),
            DeviceItem("lock-2", "Smart Lock #2", 25, true, "18:45"),
            DeviceItem("lock-3", "Garage Lock", 100, false, "12:00")
        )
    )

    val devices: StateFlow<List<DeviceItem>> = _devices

    fun getDevice(deviceId: String): DeviceItem? {
        return _devices.value.find { it.id == deviceId }
    }
}

//на потом когда бэк будет готов уберу хардкод
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.ovi.data.local.dao.LockDao
//import com.example.ovi.data.mapper.toLockDomain
//import com.example.ovi.domain.model.EventType
//import com.example.ovi.domain.model.LockEvent
//import com.example.ovi.domain.model.SmartLock
//import com.example.ovi.domain.model.UnlockMethod
//import com.example.ovi.domain.repository.EventRepository
//import com.example.ovi.domain.repository.LockRepository
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.flow.SharingStarted
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.map
//import kotlinx.coroutines.flow.stateIn
//import kotlinx.coroutines.launch
//import javax.inject.Inject
//
//@HiltViewModel
//class DevicesViewModel @Inject constructor(
//    private val lockRepository: LockRepository,
//    private val lockDao: LockDao,
//    private val eventRepository: EventRepository
//): ViewModel(){
//
//    val devices: StateFlow<List<SmartLock>> = lockDao.getAllLocks()
//        .map { entities -> entities.map { it.toLockDomain() } }
//        .stateIn(
//            scope = viewModelScope,
//            started = SharingStarted.WhileSubscribed(5000),
//            initialValue = emptyList()
//        )
//
//    fun toggleLock(lockId: String){
//        viewModelScope.launch {
//            val lock = devices.value.find { it.id == lockId } ?: return@launch
//
//            val success = if (lock.isLocked) {
//                lockRepository.unlock(lockId)
//            } else {
//                lockRepository.lock(lockId)
//            }
//
//            eventRepository.addEvent(
//                LockEvent(
//                    id = "",
//                    lockId = lockId,
//                    timestamp = System.currentTimeMillis(),
//                    type = if (lock.isLocked) EventType.UNLOCK else EventType.LOCK,
//                    success = success,
//                    method = UnlockMethod.BLUETOOTH
//                )
//            )
//
//        }
//    }
//}