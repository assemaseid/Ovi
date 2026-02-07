package com.example.ovi.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.example.ovi.domain.ble.BleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val bleManager: BleManager
) : ViewModel() {

    val isScanning = bleManager.isScanning
    val scannedDevices = bleManager.scannedDevices
    val connectedAddress = bleManager.connectedDeviceAddress

    fun startBleScan() = bleManager.startScan()
    fun stopBleScan() = bleManager.stopScan()

    fun connectToDevice(device: android.bluetooth.BluetoothDevice) {
        bleManager.connect(device.address)
    }

    override fun onCleared() {
        super.onCleared()
        bleManager.stopScan()
    }
}