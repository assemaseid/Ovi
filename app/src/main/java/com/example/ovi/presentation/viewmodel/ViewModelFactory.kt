package com.example.ovi.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ovi.domain.repository.AuthRepository
import com.example.ovi.domain.repository.LockRepository
import com.example.ovi.presentation.ui.screens.devices.DevicesViewModel

class ViewModelFactory (
    private val authRepository: AuthRepository? = null,
    private val lockRepository: LockRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T{
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(authRepository!!) as T
            }
            modelClass.isAssignableFrom(DevicesViewModel::class.java) -> {
                DevicesViewModel(lockRepository!!) as T
            }
            else ->throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}