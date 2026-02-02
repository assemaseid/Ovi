package com.example.ovi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locks")
data class LockEntity(
    @PrimaryKey val id: String,
    val name: String,
    val batteryLevel: Int,
    val isLocked: Boolean,
    val macAddress: String?,
    val lastSynced: Long
)