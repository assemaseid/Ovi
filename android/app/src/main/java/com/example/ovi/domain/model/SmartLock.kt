package com.example.ovi.domain.model

data class SmartLock(
    val id: String,
    val hardwareId: String,
    val ownerUuid: String,
    val name: String,
    val publicKey: String,
    val batteryLevel: Int, // 0-100%
    val isLocked: Boolean,
    val firmwareVersion: String?,
    val lastSynced: Long
)
