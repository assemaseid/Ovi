package com.example.ovi.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ovi.data.local.SessionManager
import com.example.ovi.domain.model.User
import com.example.ovi.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    sealed class AuthState {
        object Initial : AuthState()
        object Loading : AuthState()
        data class Success(val message: String) : AuthState()
        data class Error(val message: String) : AuthState()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun checkLoginStatus(): Boolean {
        return sessionManager.isLoggedIn()
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            if (sessionManager.isLoggedIn()) {
                val user = authRepository.getCurrentUser()
                _currentUser.value = user
            }
        }
    }

    fun getUserName(): String {
        return sessionManager.getName() ?:"User"
    }
    fun getUserEmail():String {
        return sessionManager.getEmail() ?:"No email"
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            val result = authRepository.login(email, password)

            result.onSuccess { user ->
                _currentUser.value = user
                _authState.value = AuthState.Success("Login successful!")
            }.onFailure { error ->
                _authState.value = AuthState.Error(error.message ?: "Login failed")
            }
        }
    }

    fun register(email: String, password: String, name: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            val result = authRepository.register(email, password, name)

            result.onSuccess { user ->
                _currentUser.value = user
                _authState.value = AuthState.Success("Registration successful!")
            }.onFailure { error ->
                _authState.value = AuthState.Error(error.message ?: "Registration failed")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _currentUser.value = null
            _authState.value = AuthState.Initial
        }
    }

    fun resetState() {
        _authState.value = AuthState.Initial
    }
}

data class DeviceItem(
    val id: String,
    val name: String,
    val status: String,
    val locked: Boolean
)

