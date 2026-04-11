package com.example.ovi.data.dto

import com.google.gson.annotations.SerializedName

data class EventOutDto(
    @SerializedName("event_uuid") val eventUuid: String,
    @SerializedName("msg_id") val msgId: String,
    @SerializedName("device_uuid") val deviceUuid: String,
    @SerializedName("event_type") val eventType: String,
    @SerializedName("event_data") val eventData: Map<String, Any?>,
    @SerializedName("verified") val verified: Boolean,
    @SerializedName("created_at") val createdAt: String
)
