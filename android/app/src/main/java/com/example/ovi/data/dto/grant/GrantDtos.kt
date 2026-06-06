package com.example.ovi.data.dto.grant

import com.google.gson.annotations.SerializedName

data class GuestRequestBody(@SerializedName("hardware_id") val hardwareId: String)

data class GuestRequestResponse(
    @SerializedName("device_uuid") val deviceUuid: String,
    val status: String
)

data class GuestJoinBody(val pin: String)

data class GrantDto(
    @SerializedName("grant_uuid") val grantUuid: String,
    @SerializedName("device_uuid") val deviceUuid: String,
    @SerializedName("user_uuid") val userUuid: String,
    val permissions: List<String>,
    @SerializedName("valid_from") val validFrom: String,
    @SerializedName("valid_until") val validUntil: String?,
    @SerializedName("created_by") val createdBy: String,
    @SerializedName("created_at") val createdAt: String
)

data class GuestDto(
    @SerializedName("grant_uuid") val grantUuid: String,
    @SerializedName("user_uuid") val userUuid: String,
    @SerializedName("user_name") val userName: String?,
    @SerializedName("user_email") val userEmail: String?,
    val permissions: List<String>,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("valid_until") val validUntil: String?
)
