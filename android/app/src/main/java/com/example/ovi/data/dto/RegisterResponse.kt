package com.example.ovi.data.dto

import com.google.gson.annotations.SerializedName

data class RegisterResponse(
    @SerializedName("user_data")
    val user: UserDto
)
