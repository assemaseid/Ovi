package com.example.ovi.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

@HiltViewModel
class AutoPinViewModel @Inject constructor() : ViewModel() {

    private val _currentPin = MutableStateFlow<String?>(null)
    val currentPin: StateFlow<String?> = _currentPin.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Matches firmware: for (int i = 0; i < 32; i++) deviceSecret[i] = (uint8_t)(i * 7 + 13);
    private val deviceSecret = ByteArray(32) { i -> (i * 7 + 13).toByte() }

    // Matches firmware ROTATION_SECONDS = 86400
    private val rotationSeconds = 86400L

    fun refreshPin() {
        viewModelScope.launch {
            _isLoading.value = true
            _currentPin.value = computeCurrentPin()
            _isLoading.value = false
        }
    }

    private fun computeCurrentPin(): String {
        val currentSlot = System.currentTimeMillis() / 1000L / rotationSeconds
        val input = ByteBuffer.allocate(8).putLong(currentSlot).array()

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(deviceSecret, "HmacSHA256"))
        val hmac = mac.doFinal(input)

        var pinNum = 0L
        for (i in 0..3) pinNum = (pinNum shl 8) or (hmac[i].toLong() and 0xFF)
        return String.format("%06d", pinNum % 1_000_000)
    }
}
