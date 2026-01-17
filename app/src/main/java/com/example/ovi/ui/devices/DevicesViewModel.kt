package com.example.ovi.ui.devices

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow

data class DeviceItem(
    val id: String,
    val name: String,
    val status: String
)
class DevicesViewModel : ViewModel(){

    private val _devices = MutableStateFlow(
        listOf(
            DeviceItem("lock-1", "Smart Lock #1", "Online"),
            DeviceItem("lock-2", "Smart Lock #2", "Offline"),
            DeviceItem("lock-3", "Garage Lock", "Online")
        )
    )

    val devices: StateFlow<List<DeviceItem>> = _devices
}

