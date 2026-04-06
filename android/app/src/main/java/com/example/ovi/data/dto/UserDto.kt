package com.example.ovi.data.dto

import com.google.gson.annotations.SerializedName

data class UserDto(
    @SerializedName("user_uuid")
    val id: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("name")
    val name: String?,

    @SerializedName("password")
    val password: String?,

    @SerializedName("public_key")
    val public_key: String?,

    @SerializedName("created_at")
    val created_at: String?,

    @SerializedName("last_login")
    val last_login: String?,

    @SerializedName("is_active")
    val is_active: Boolean?

)
