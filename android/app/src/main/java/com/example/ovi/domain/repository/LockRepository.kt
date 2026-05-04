package com.example.ovi.domain.repository

import com.example.ovi.domain.model.SmartLock
import kotlinx.coroutines.flow.Flow

interface LockRepository {
    suspend fun getPairedLocks(): List<SmartLock>
    suspend fun getLockById(lockId: String): SmartLock?
    fun observeAllLocks(): Flow<List<SmartLock>>
    suspend fun updateLockState(lockId: String, isLocked: Boolean)
    suspend fun updateLockFromStatus(lockId: String, battery: Int?, firmware: String?, lastSeen: String?, isLocked: Boolean? = null)
    suspend fun updateLockBattery(lockId: String, battery: Int)
    suspend fun addLock(lock: SmartLock)
    suspend fun syncDevicesFromServer()
    suspend fun unlock(lockId: String): Boolean
    suspend fun lock(lockId: String): Boolean
    suspend fun deleteDevice(lockId: String): Boolean
    suspend fun getDeviceInfo(): Boolean
    suspend fun remoteUnlock(lockId: String): Boolean
    suspend fun remoteLock(lockId: String): Boolean
}