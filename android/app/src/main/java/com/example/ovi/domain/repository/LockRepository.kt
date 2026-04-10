package com.example.ovi.domain.repository

import com.example.ovi.domain.model.SmartLock

interface LockRepository {
    suspend fun getPairedLocks(): List<SmartLock>
    suspend fun addLock(lock: SmartLock)
    suspend fun syncDevicesFromServer()
    suspend fun unlock(lockId: String): Boolean
    suspend fun lock(lockId: String): Boolean
    suspend fun deleteDevice(lockId: String): Boolean
    suspend fun getDeviceInfo(): Boolean
}