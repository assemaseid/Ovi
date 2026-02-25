package com.example.ovi.domain.model

data class DeviceItem(
    val id: String,
    val name: String,
    val battery_level: Int,
    val locked: Boolean,
    val lastSeen: String,
)
