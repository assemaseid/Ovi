package com.example.ovi.data.dto


data class CurrentPinResponse(
    val pin: String,
    val valid_until: Long,
    val current_slot: Long
)

data class PinScheduleRequest(
    val rotation_hours: Int,
    val show_on_display: Boolean
)

data class PinScheduleResponse(
    val status: String,
    val rotation_hours: Int,
    val next_rotation_at: Long
)