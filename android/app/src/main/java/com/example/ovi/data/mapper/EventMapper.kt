package com.example.ovi.data.mapper

import com.example.ovi.data.local.entity.EventEntity
import com.example.ovi.domain.model.EventType
import com.example.ovi.domain.model.LockEvent
import com.example.ovi.domain.model.UnlockMethod

fun EventEntity.toEventDomain(): LockEvent {
    return LockEvent(
        id = this.id.toString(),
        lockId = this.lockId,
        timestamp = this.timestamp,
        type = EventType.valueOf(this.type),
        success = this.success,
        method = UnlockMethod.valueOf(this.method)
    )
}

fun LockEvent.toEventEntity(): EventEntity {
    return EventEntity(
        lockId = this.lockId,
        type = this.type.name,
        timestamp = this.timestamp,
        success = this.success,
        method = this.method.name
    )
}