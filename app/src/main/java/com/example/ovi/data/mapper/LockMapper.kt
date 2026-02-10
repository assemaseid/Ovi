package com.example.ovi.data.mapper

import com.example.ovi.data.local.entity.LockEntity
import com.example.ovi.domain.model.SmartLock

fun LockEntity.toDomain(): SmartLock {
    return SmartLock(
        id = id,
        hardwareId = hardwareId,
        ownerUuid = ownerUuid,
        name = name,
        publicKey = publicKey,
        batteryLevel = batteryLevel,
        isLocked = isLocked,
        firmwareVersion = firmwareVersion,
        lastSynced = lastSynced
    )
}

fun SmartLock.toEntity(): LockEntity {
    return LockEntity(
        id = id,
        hardwareId = hardwareId,
        ownerUuid = ownerUuid,
        name = name,
        publicKey = publicKey,
        batteryLevel = batteryLevel,
        isLocked = isLocked,
        firmwareVersion = firmwareVersion,
        lastSynced = lastSynced
    )
}