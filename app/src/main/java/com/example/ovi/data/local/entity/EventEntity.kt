package com.example.ovi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lockId: String,
    val type: String,
    val timestamp: Long,
    val success: Boolean,
    val method: String
)