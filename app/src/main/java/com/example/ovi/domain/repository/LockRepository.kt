package com.example.ovi.domain.repository

import com.example.ovi.domain.model.SmartLock

interface LockRepository {
    suspend fun getPairedLocks(): List<SmartLock>
    suspend fun unlock(lockId: String): Boolean
    suspend fun lock(lockId: String): Boolean
}