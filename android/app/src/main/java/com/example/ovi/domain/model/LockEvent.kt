package com.example.ovi.domain.model

data class LockEvent(
    val id: String = "",
    val lockId: String,
    val timestamp: Long,
    val type: EventType,
    val success: Boolean,
    val method: UnlockMethod,
    val userUuid: String? = null,
    val userName: String? = null,
    val fingerName: String? = null,
    val msgId: String = java.util.UUID.randomUUID().toString()
)
