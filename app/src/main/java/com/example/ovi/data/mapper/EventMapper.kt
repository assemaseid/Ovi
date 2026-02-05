package com.example.ovi.data.mapper

import com.example.ovi.data.local.entity.EventEntity
import com.example.ovi.domain.model.EventType
import com.example.ovi.domain.model.LockEvent
import com.example.ovi.domain.model.UnlockMethod

fun EventEntity.toDomain(): LockEvent {
    return LockEvent(
        id = id.toString(),
        lockId = lockId,
        timestamp = timestamp,
        type = EventType.valueOf(type),
        success = success,
        method = UnlockMethod.valueOf(method)
    )
}

fun LockEvent.toEntity(): EventEntity {
    return EventEntity(
        lockId = lockId,
        type = type.name,
        timestamp = timestamp,
        success = success,
        method = method.name
    )
}