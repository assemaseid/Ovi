package com.example.ovi.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ovi.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

//sealed class AuthState {
//    object Initial : AuthState()
//    object Loading : AuthState()
//    data class Success(val message: String) : AuthState()
//    data class Error(val message: String) : AuthState()
//}

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    sealed class AuthState {
        object Initial : AuthState()
        object Loading : AuthState()
        data class Success(val message: String) : AuthState()
        data class Error(val message: String) : AuthState()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            val result = authRepository.login(email, password)

            result.onSuccess { user ->
                _authState.value = AuthState.Success("Login successful!")
                // Здесь можно сохранить пользователя в SharedPreferences
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
                _authState.value = AuthState.Success("Registration successful!")
            }.onFailure { error ->
                _authState.value = AuthState.Error(error.message ?: "Registration failed")
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Initial
    }
}