package com.example.ovi.data.dto.device

import com.google.gson.annotations.SerializedName

data class FingerprintDto(
    @SerializedName("finger_id") val fingerId: Int,
    val name: String,
    @SerializedName("created_at") val createdAt: String
)

data class FingerprintEnrollResponse(
    @SerializedName("finger_id") val fingerId: Int,
    val status: String
)

data class FingerprintRenameRequest(
    val name: String
)
