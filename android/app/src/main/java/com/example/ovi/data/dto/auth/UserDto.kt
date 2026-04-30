package com.example.ovi.data.dto.auth

import com.google.gson.annotations.SerializedName

data class UserDto(
    @SerializedName("user_uuid")
    val id: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("is_active")
    val isActive: Boolean
)
