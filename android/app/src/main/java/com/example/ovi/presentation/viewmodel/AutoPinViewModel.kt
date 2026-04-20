package com.example.ovi.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ovi.data.local.dao.LockDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

@HiltViewModel
class AutoPinViewModel @Inject constructor(
    private val lockDao: LockDao
) : ViewModel() {

    private val _currentPin = MutableStateFlow<String?>(null)
    val currentPin: StateFlow<String?> = _currentPin.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _rotationHours = MutableStateFlow(24)
    val rotationHours: StateFlow<Int> = _rotationHours.asStateFlow()

    fun refreshPin(deviceId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val entity = lockDao.getLockById(deviceId)
                if (entity == null) {
                    _error.value = "Device not found"
                    return@launch
                }
                if (entity.deviceSecret.isEmpty()) {
                    _error.value = "Device secret not available. Re-pair the lock to enable Auto PIN."
                    return@launch
                }
                _rotationHours.value = entity.rotationHours
                _currentPin.value = computePin(entity.deviceSecret, entity.rotationHours)
            } catch (e: Exception) {
                _error.value = "Failed to compute PIN"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getNextRotationMillis(rotationHours: Int): Long {
        val rotationSeconds = rotationHours * 3600L
        val currentSlot = System.currentTimeMillis() / 1000L / rotationSeconds
        return (currentSlot + 1) * rotationSeconds * 1000L
    }

    private fun computePin(deviceSecret: String, rotationHours: Int): String {
        val rotationSeconds = rotationHours * 3600L
        val timeSlot = System.currentTimeMillis() / 1000L / rotationSeconds
        val keyBytes = deviceSecret.toByteArray(Charsets.UTF_8)
        val messageBytes = ByteBuffer.allocate(8).putLong(timeSlot).array()
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(keyBytes, "HmacSHA256"))
        val hashBytes = mac.doFinal(messageBytes)
        val pinNum = BigInteger(1, hashBytes).mod(BigInteger.valueOf(1_000_000L)).toLong()
        return String.format("%06d", pinNum)
    }
}
