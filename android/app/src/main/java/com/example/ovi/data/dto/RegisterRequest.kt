package com.example.ovi.data.dto

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    val email: String,
    @SerializedName("hashed_password")
    val password: String
)
