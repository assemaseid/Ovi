package com.example.ovi.domain.model

data class PinSchedule(
    val rotationPeriodHours: Int,
    val pinLength: Int,
    val lastRotationTime: Long
)
