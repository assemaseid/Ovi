package com.example.ovi.ui.devices

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow

data class DeviceItem(
    val id: String,
    val name: String,
    val status: String,
    val locked: Boolean
)
class DevicesViewModel : ViewModel(){

    private val _devices = MutableStateFlow(
        listOf(
            DeviceItem("lock-1", "Smart Lock #1", "ON", true),
            DeviceItem("lock-2", "Smart Lock #2", "OFF", true),
            DeviceItem("lock-3", "Garage Lock", "ON", false)
        )
    )

    val devices: StateFlow<List<DeviceItem>> = _devices

    fun toggleLock(deviceId: String){
        _devices.value = _devices.value.map { device ->
            if (device.id == deviceId) {
                device.copy(locked = !device.locked)
            } else device
        }
    }

    fun getDevice(deviceId: String): DeviceItem? {
        return _devices.value.find { it.id == deviceId }
    }
}

