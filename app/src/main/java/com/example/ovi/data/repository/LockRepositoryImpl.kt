/*package com.example.ovi.data.repository

import com.example.ovi.data.local.dao.LockDao
import com.example.ovi.data.mapper.toDomain
import com.example.ovi.domain.model.SmartLock
import com.example.ovi.domain.repository.LockRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class LockRepositoryImpl @Inject constructor(
    private val lockDao: LockDao
) : LockRepository{

    override suspend fun getPairedLocks(): List<SmartLock> {
        return lockDao.getAllLocks().first().map { it.toDomain() }
    }

    override suspend fun unlock(lockId: String): Boolean {
        val lock = lockDao.getAllLocks().first().find {it.id == lockId}
        lock?.let {
            lockDao.updateLock(it.copy(isLocked = false, lastSynced = System.currentTimeMillis()))
        }
        return true
    }

    override suspend fun lock(lockId: String): Boolean {
        val lock = lockDao.getAllLocks().first().find { it.id == lockId }
        lock?.let {
            lockDao.updateLock(it.copy(isLocked = true, lastSynced = System.currentTimeMillis()))
        }
        return true
    }

}
maybe will be used in future
 */