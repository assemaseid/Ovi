package com.example.ovi.data.mapper

import com.example.ovi.data.local.entity.LockEntity
import com.example.ovi.domain.model.SmartLock

fun LockEntity.toDomain(): SmartLock {
    return SmartLock(
        id = id,
        name = name,
        batteryLevel = batteryLevel,
        isConnected = false,
        isLocked = isLocked,
        lastSynced = lastSynced,
        macAddress = macAddress
    )
}

fun SmartLock.toEntity(): LockEntity {
    return LockEntity(
        id = id,
        name = name,
        batteryLevel = batteryLevel,
        isLocked = isLocked,
        macAddress = macAddress,
        lastSynced = System.currentTimeMillis()
    )
}