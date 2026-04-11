package com.example.ovi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locks")
data class LockEntity(
    @PrimaryKey val deviceId: String,
    val hardwareId: String,
    val ownerUuid: String,
    val name: String,
    val publicKey: String,
    val batteryLevel: Int,
    val isLocked: Boolean,
    val firmwareVersion: String?,
    val lastSynced: Long,
    val pendingDelete: Boolean = false
)

