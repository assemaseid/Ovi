package com.example.ovi.data.dto

import com.google.gson.annotations.SerializedName

data class FcmTokenRequest(
    @SerializedName("fcm_token")
    val fcmToken: String
)
