package com.example.ovi.data.repository

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.ovi.data.api.LockService
import com.example.ovi.data.dto.PinChangeRequest
import com.example.ovi.data.dto.UnlockTokenRequest
import com.example.ovi.data.local.SessionManager
import com.example.ovi.data.local.dao.LockDao
import com.example.ovi.data.mapper.toLockDomain
import com.example.ovi.data.mapper.toLockEntity
import com.example.ovi.domain.ble.BleManager
import com.example.ovi.domain.model.SmartLock
import com.example.ovi.domain.repository.LockRepository
import com.example.ovi.util.BleConstants
import com.example.ovi.util.CryptoUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import javax.inject.Inject

class LockRepositoryImpl @Inject constructor(
    private val lockService: LockService,
    private val bleManager: BleManager,
    private val lockDao: LockDao,
    private val sessionManager: SessionManager,
    private val connectivityManager: ConnectivityManager
) : LockRepository {

    override suspend fun getPairedLocks(): List<SmartLock> =
        lockDao.getAllLocks().first().map { it.toLockDomain() }

    override suspend fun addLock(lock: SmartLock) =
        lockDao.insertLock(lock.toLockEntity())
    

    override suspend fun unlock(lockId: String): Boolean {
        if (!isNetworkAvailable()) return false
        
        if (sessionManager.isJwtExpired()) return false
        
        val connectedAddress = bleManager.connectedDeviceAddress.value
        if (connectedAddress != null) {
            val rssi = bleManager.getRssi(connectedAddress)
            if (rssi != null && rssi < -80) return false
        }
        
        repeat(3) { attempt ->
            val success = attemptUnlock(lockId)
            if (success) return true
            if (attempt < 2) delay(1000L * (attempt + 1))
        }
        return false
    }

    private suspend fun attemptUnlock(lockId: String): Boolean {
        return try {
            val requestId = "req_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val clientTime = (System.currentTimeMillis() / 1000).toString()

            val response = lockService.getUnlockToken(
                requestId = requestId,
                clientTime = clientTime,
                request = UnlockTokenRequest(device_uuid = lockId)
            )

            if (!response.isSuccessful || response.body() == null) return false
            val body = response.body()!!
            
            val plainCommand = """{"v":1,"t":"unlock","n":"${body.token.nonce}","e":${body.token.expires_at},"s":"${body.signature.value}"}"""

            // Issue #2: AES-GCM encrypt the command before sending over BLE
            val sessionKey = sessionManager.getSessionKey(lockId)
            val blePayload = if (sessionKey != null) {
                CryptoUtils.aesGcmEncryptToBase64(plainCommand.toByteArray(Charsets.UTF_8), sessionKey)
            } else {
                plainCommand
            }

            val bleSuccess = bleManager.sendMessage(blePayload)
            if (!bleSuccess) return false
            
            val notified = withTimeoutOrNull(10_000L) {
                bleManager.notifications.first { (uuid, value) ->
                    uuid == BleConstants.CHAR_STATUS_NOTIFY && value.contains("unlock_success")
                }
            }

            if (notified != null) {
                lockDao.getLockById(lockId)?.let { entity ->
                    lockDao.updateLock(entity.copy(isLocked = false, lastSynced = System.currentTimeMillis()))
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    
    override suspend fun lock(lockId: String): Boolean {
        val nonce = UUID.randomUUID().toString().replace("-", "").take(16)
        val expires = (System.currentTimeMillis() / 1000) + 10

        val plainCommand = """{"v":1,"t":"lock","n":"$nonce","e":$expires}"""

        val sessionKey = sessionManager.getSessionKey(lockId)
        val blePayload = if (sessionKey != null) {
            CryptoUtils.aesGcmEncryptToBase64(plainCommand.toByteArray(Charsets.UTF_8), sessionKey)
        } else {
            plainCommand
        }
        
        repeat(3) { attempt ->
            val sent = bleManager.sendMessage(blePayload)
            if (sent) {
                lockDao.getLockById(lockId)?.let { entity ->
                    lockDao.updateLock(entity.copy(isLocked = true, lastSynced = System.currentTimeMillis()))
                }
                return true
            }
            if (attempt < 2) delay(500L * (attempt + 1))
        }
        return false
    }
    

    override suspend fun changePin(deviceId: String, newPin: String): Boolean {
        if (!isNetworkAvailable()) return false
        if (sessionManager.isJwtExpired()) return false
        return try {
            val response = lockService.changePin(deviceId, PinChangeRequest(new_pin = newPin))
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}