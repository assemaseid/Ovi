package com.example.ovi.domain.model

data class LockEvent(
    val id: String,
    val lockId: String,
    val timestamp: Long,
    val type: EventType,
    val userId: String? = null,
    val success: Boolean,
    val method: UnlockMethod
)
