package com.example.ovi.data.dto

import com.google.gson.annotations.SerializedName

data class RefreshResponse(
    @SerializedName("access_token")
    val accessToken: String
)
