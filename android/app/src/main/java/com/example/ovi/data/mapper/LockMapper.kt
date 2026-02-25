package com.example.ovi.data.mapper

import com.example.ovi.data.local.entity.LockEntity
import com.example.ovi.domain.model.SmartLock

fun LockEntity.toLockDomain(): SmartLock {
    return SmartLock(
        id = this.deviceId,
        hardwareId = this.hardwareId,
        ownerUuid = this.ownerUuid,
        name = this.name,
        publicKey = this.publicKey,
        batteryLevel = this.batteryLevel,
        isLocked = this.isLocked,
        firmwareVersion = this.firmwareVersion,
        lastSynced = this.lastSynced
    )
}

fun SmartLock.toLockEntity(): LockEntity {
    return LockEntity(
        deviceId = this.id,
        hardwareId = this.hardwareId,
        ownerUuid = this.ownerUuid,
        name = this.name,
        publicKey = this.publicKey,
        batteryLevel = this.batteryLevel,
        isLocked = this.isLocked,
        firmwareVersion = this.firmwareVersion,
        lastSynced = this.lastSynced
    )
}