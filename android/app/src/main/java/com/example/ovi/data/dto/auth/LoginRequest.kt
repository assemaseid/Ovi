package com.example.ovi.data.dto.auth

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val email: String,
    @SerializedName("hashed_password")
    val password: String
)
