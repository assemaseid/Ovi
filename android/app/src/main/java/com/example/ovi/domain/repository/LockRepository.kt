package com.example.ovi.domain.repository

import com.example.ovi.domain.model.SmartLock
import com.example.ovi.domain.model.LockEvent
import kotlinx.coroutines.flow.Flow

interface LockRepository {
    suspend fun getPairedLocks(): List<SmartLock>
    suspend fun addLock(lock: SmartLock)
    suspend fun unlock(lockId: String): Boolean
    suspend fun lock(lockId: String): Boolean
}
