package com.example.ovi.domain.model

data class User(
    val id: String,
    val email: String,
    val jwtToken: String? = null
)
