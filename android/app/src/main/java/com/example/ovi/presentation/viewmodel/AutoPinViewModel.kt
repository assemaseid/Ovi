package com.example.ovi.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ovi.data.api.LockService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AutoPinViewModel @Inject constructor(
    private val lockService: LockService
) : ViewModel() {

    private val _currentPin = MutableStateFlow<String?>(null)
    val currentPin: StateFlow<String?> = _currentPin.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun refreshPin(deviceId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = lockService.getCurrentPin(deviceId)
                if (response.isSuccessful) {
                    _currentPin.value = response.body()?.message
                } else {
                    _error.value = when (response.code()) {
                        404 -> "Device secret not configured"
                        403 -> "Access denied"
                        else -> "Failed to fetch PIN (${response.code()})"
                    }
                }
            } catch (e: Exception) {
                _error.value = "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
