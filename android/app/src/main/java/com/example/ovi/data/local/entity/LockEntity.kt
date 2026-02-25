package com.example.ovi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locks")
data class LockEntity(
    @PrimaryKey
    val deviceId: String,
    val name: String,
    val isOnline: Boolean = false,
    val batteryLevel: Int? = null,
    val lastSeen: Long? = null
)