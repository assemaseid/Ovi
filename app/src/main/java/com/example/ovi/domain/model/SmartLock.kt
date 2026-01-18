package com.example.ovi.domain.model

data class SmartLock(
    val id: String,
    val name: String,
    val batteryLevel: Int, // 0-100%
    val isConnected: Boolean,
    val isLocked: Boolean,
    val lastSynced: Long, // timestamp
    val macAddress: String? = null,
    val publicKey: String? = null,
    val schedule: PinSchedule? = null
)
