package com.example.ovi.data.dto.event

import com.google.gson.annotations.SerializedName

data class MobileEventRequest(
    val msg_id: String,
    val device_uuid: String,
    val event_type: String,
    val event_data: Map<String, Any>,
    val created_at: String
)

data class MobileSyncRequest(
    val events: List<MobileEventRequest>
)

data class MobileSyncResponse(
    val received: Int,
    val duplicates: Int
)

data class EnrichedEventDto(
    @SerializedName("event_uuid") val eventUuid: String,
    @SerializedName("msg_id") val msgId: String,
    @SerializedName("device_uuid") val deviceUuid: String,
    @SerializedName("user_uuid") val userUuid: String?,
    @SerializedName("user_name") val userName: String?,
    @SerializedName("event_type") val eventType: String,
    @SerializedName("event_data") val eventData: Map<String, Any?>,
    val verified: Boolean,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("finger_name") val fingerName: String?
)
