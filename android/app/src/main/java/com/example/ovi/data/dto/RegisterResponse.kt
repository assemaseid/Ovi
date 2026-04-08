package com.example.ovi.data.dto

import com.google.gson.annotations.SerializedName

data class RegisterResponse(
    @SerializedName("access_token")
    val accessToken: String,

    @SerializedName("refresh_token")
    val refreshToken: String?,

    @SerializedName("user_data")
    val user: UserDto
)
