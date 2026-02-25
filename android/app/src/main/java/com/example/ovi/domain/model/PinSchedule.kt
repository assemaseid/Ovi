package com.example.ovi.domain.model

data class PinSchedule(
    val rotationHours: Int,
    val showOnDisplay: Boolean,
    val currentSlot: Long,
    val nextRotationAt: Long
)
