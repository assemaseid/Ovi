package com.example.ovi.data.dto

import com.google.gson.annotations.SerializedName

data class UserDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("email")
    val email: String,

    @SerializedName("name")
    val name: String?,

    @SerializedName("created_at")
    val createdAt: Long?,

    @SerializedName("last_login")
    val lastLogin: Long?
)