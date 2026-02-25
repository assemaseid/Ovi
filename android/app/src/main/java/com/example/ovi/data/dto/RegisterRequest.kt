package com.example.ovi.data.dto

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String? = null
)
