package com.example.ovi.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ovi.data.api.LockService
import com.example.ovi.data.dto.grant.GuestDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GuestManagementViewModel @Inject constructor(
    private val lockService: LockService,
) : ViewModel() {

    private val _guests = MutableStateFlow<List<GuestDto>>(emptyList())
    val guests: StateFlow<List<GuestDto>> = _guests.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun load(deviceUuid: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = lockService.getGuests(deviceUuid)
                if (response.isSuccessful) {
                    _guests.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Failed to load guests"
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun revokeGuest(deviceUuid: String, grantUuid: String) {
        viewModelScope.launch {
            try {
                val response = lockService.revokeGrant(grantUuid)
                if (response.isSuccessful) {
                    load(deviceUuid)
                } else {
                    _error.value = "Failed to revoke access"
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            }
        }
    }
}
