package com.example.ovi.data.dto.auth

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    @SerializedName("name")
    val name: String,
    val email: String,
    @SerializedName("hashed_password")
    val password: String
)
