package com.example.ovi.data.repository

import com.example.ovi.domain.model.SmartLock
import com.example.ovi.domain.repository.LockRepository

class FakeLockRepository : LockRepository {
    override suspend fun getPairedLocks(): List<SmartLock> {
        return listOf(
            SmartLock("1", "Office Lock", 85, isConnected = true, isLocked = true, lastSynced = System.currentTimeMillis()),
            SmartLock("2", "Home Front", 40, isConnected = true, isLocked = true, lastSynced = System.currentTimeMillis())
            )
    }

    override suspend fun unlock(lockId: String): Boolean = true
    override suspend fun lock(lockId: String): Boolean = true
}
