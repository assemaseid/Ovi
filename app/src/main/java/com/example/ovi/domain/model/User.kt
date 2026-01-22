package com.example.ovi.domain.model

data class User(
    val id: Int,
    val email: String,
    val name: String,
    val jwtToken: String? = null
)
