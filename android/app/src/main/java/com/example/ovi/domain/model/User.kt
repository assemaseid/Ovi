package com.example.ovi.domain.model

data class User(
    val id: String,
    val email: String,
    val name: String? = null,
    val jwtToken: String? = null
)
