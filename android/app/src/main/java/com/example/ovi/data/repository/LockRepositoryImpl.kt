package com.example.ovi.data.repository

import com.example.ovi.data.api.LockService
import com.example.ovi.data.dto.UnlockTokenRequest
import com.example.ovi.data.local.dao.LockDao
import com.example.ovi.data.mapper.toDomain
import com.example.ovi.data.mapper.toEntity
import com.example.ovi.data.mapper.toLockDomain
import com.example.ovi.data.mapper.toLockEntity
import com.example.ovi.domain.ble.BleManager
import com.example.ovi.domain.model.SmartLock
import com.example.ovi.domain.repository.LockRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class LockRepositoryImpl @Inject constructor(
    private val lockService: LockService,
    private val bleManager: BleManager,
    private val lockDao: LockDao
) : LockRepository {

    override suspend fun getPairedLocks(): List<SmartLock> =
        lockDao.getAllLocks().first().map { it.toLockDomain() }

    override suspend fun addLock(lock: SmartLock) =
        lockDao.insertLock(lock.toLockEntity())

    override suspend fun unlock(lockId: String): Boolean {
        return try {

            val request = UnlockTokenRequest(device_uuid = lockId)

            val response = lockService.getUnlockToken(request)

            if (!response.isSuccessful || response.body() == null) {
                return false
            }

            val body = response.body() ?: return false


            val bleCommandJson = """
                {
                   "v": 1,
                   "t": "unlock",
                   "n": "${body.token.nonce}",
                   "e": ${body.token.expires_at},
                   "s": "${body.signature.value}" 
                }
            """.trimIndent()

            val bleSuccess = bleManager.sendMessage(bleCommandJson)

            if (bleSuccess) {
                lockDao.getLockById(lockId)?.let { entity ->
                    lockDao.updateLock(entity.copy(
                        isLocked = false,
                        lastSynced = System.currentTimeMillis()
                    ))
                }
            }
            bleSuccess
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    override suspend fun lock(lockId: String): Boolean {
        return try {
            lockDao.getLockById(lockId)?.let { entity ->
                lockDao.updateLock(entity.copy(
                    isLocked = true,
                    lastSynced = System.currentTimeMillis()
                ))
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
}