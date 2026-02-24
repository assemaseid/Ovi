package com.example.ovi.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.example.ovi.domain.model.DeviceItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

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