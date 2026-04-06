package com.example.ovi.data.dto

import com.google.gson.annotations.SerializedName

data class RefreshRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)
