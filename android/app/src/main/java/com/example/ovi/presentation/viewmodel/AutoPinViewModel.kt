package com.example.ovi.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

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
        // Backend: msg = str(time_slot).encode() — slot as decimal string, not binary
        val input = currentSlot.toString().toByteArray(Charsets.UTF_8)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(deviceSecret, "HmacSHA256"))
        val hmac = mac.doFinal(input)
        // Backend: int.from_bytes(digest, "big") % 10^6 — all 32 bytes, big-endian
        var pinNum = 0L
        for (byte in hmac) pinNum = ((pinNum shl 8) or (byte.toLong() and 0xFF)) % 1_000_000L
        return String.format("%06d", pinNum)
    }
}
