package com.example.ovi.data.repository

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.ovi.data.api.LockService
import com.example.ovi.data.dto.command.TokenData
import com.example.ovi.data.dto.command.UnlockTokenRequest
import org.json.JSONObject
import com.example.ovi.data.local.SessionManager
import com.example.ovi.data.local.dao.LockDao
import com.example.ovi.data.local.entity.LockEntity
import com.example.ovi.data.mapper.toLockDomain
import com.example.ovi.data.mapper.toLockEntity
import com.example.ovi.domain.ble.BleManager
import com.example.ovi.domain.model.SmartLock
import com.example.ovi.domain.repository.LockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.ovi.util.BleConstants
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

    override suspend fun getLockById(lockId: String): SmartLock? =
        lockDao.getLockById(lockId)?.toLockDomain()

    override fun observeAllLocks(): Flow<List<SmartLock>> =
        lockDao.getAllLocks().map { entities -> entities.map { it.toLockDomain() } }

    override suspend fun updateLockState(lockId: String, isLocked: Boolean) {
        lockDao.getLockById(lockId)?.let { entity ->
            lockDao.updateLock(entity.copy(isLocked = isLocked, lastSynced = System.currentTimeMillis()))
        }
    }

    override suspend fun updateLockFromStatus(lockId: String, battery: Int?, firmware: String?, lastSeen: String?, isLocked: Boolean?) {
        lockDao.getLockById(lockId)?.let { entity ->
            lockDao.updateLock(entity.copy(
                batteryLevel = battery ?: entity.batteryLevel,
                firmwareVersion = firmware ?: entity.firmwareVersion,
                lastSynced = parseIsoToMillis(lastSeen),
                isLocked = isLocked ?: entity.isLocked
            ))
        }
    }

    override suspend fun updateLockBattery(lockId: String, battery: Int) {
        lockDao.getLockById(lockId)?.let { entity ->
            lockDao.updateLock(entity.copy(batteryLevel = battery))
        }
    }

    private fun parseIsoToMillis(iso: String?): Long {
        iso ?: return System.currentTimeMillis()
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                .parse(iso.substringBefore('.').trimEnd('Z'))
                ?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    override suspend fun addLock(lock: SmartLock) {
        val existing = lockDao.getLockByHardwareId(lock.hardwareId)
        if (existing != null) {
            lockDao.updateLock(lock.toLockEntity().copy(deviceId = existing.deviceId))
        } else {
            lockDao.insertLock(lock.toLockEntity())
        }
    }

    override suspend fun clearAllDevices() {
        lockDao.deleteAllLocks()
    }

    override suspend fun syncDevicesFromServer() {
        if (!isNetworkAvailable()) return
        try {
            val response = lockService.getDevices()
            if (!response.isSuccessful) return
            val devices = response.body() ?: return
            val ownerUuid = sessionManager.getUserId() ?: ""

            val serverUuids = devices.map { it.deviceUuid }.toSet()

            // Remove local locks that are no longer accessible to this user
            for (entity in lockDao.getAllLocksOnce()) {
                if (entity.deviceId !in serverUuids && !entity.pendingDelete) {
                    lockDao.deleteLock(entity.deviceId)
                }
            }

            // Clean up pendingDelete records that no longer exist on server
            for (entity in lockDao.getPendingDeleteLocks()) {
                if (entity.deviceId !in serverUuids) lockDao.deleteLock(entity.deviceId)
            }

            for (dto in devices) {
                val actualOwnerUuid = dto.userUuid ?: ownerUuid
                val existing = lockDao.getLockById(dto.deviceUuid)
                if (existing != null) {
                    // Устройство помечено на удаление — повторяем удаление с сервера
                    if (existing.pendingDelete) {
                        val del = lockService.deleteDevice(dto.deviceUuid)
                        if (del.isSuccessful) lockDao.deleteLock(dto.deviceUuid)
                        continue
                    }
                    lockDao.updateLock(
                        existing.copy(
                            ownerUuid = actualOwnerUuid,
                            batteryLevel = dto.batteryLevel ?: existing.batteryLevel,
                            firmwareVersion = dto.firmwareVersion ?: existing.firmwareVersion,
                            lastSynced = System.currentTimeMillis()
                        )
                    )
                } else {
                    // Новое устройство с сервера (напр. зарегистрировано с другого телефона)
                    lockDao.insertLock(
                        LockEntity(
                            deviceId = dto.deviceUuid,
                            hardwareId = dto.hardwareId,
                            ownerUuid = actualOwnerUuid,
                            name = dto.hardwareId,
                            publicKey = "",
                            batteryLevel = dto.batteryLevel ?: 0,
                            isLocked = true,
                            firmwareVersion = dto.firmwareVersion,
                            lastSynced = System.currentTimeMillis()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    override suspend fun unlock(lockId: String): Boolean {
        val connectedAddress = bleManager.connectedDeviceAddress.value
        if (connectedAddress != null) {
            val rssi = bleManager.getRssi(connectedAddress)
            if (rssi != null && rssi < -80) return false
        }
        if (!isNetworkAvailable() || sessionManager.isJwtExpired()) {
            return attemptOfflineBleUnlock(lockId)
        }
        repeat(3) { attempt ->
            val success = attemptUnlock(lockId)
            if (success) return true
            if (attempt < 2) delay(1000L * (attempt + 1))
        }
        return false
    }

    private suspend fun attemptOfflineBleUnlock(lockId: String): Boolean {
        if (bleManager.connectedDeviceAddress.value == null) return false
        val entity = lockDao.getLockById(lockId) ?: return false
        val secret = entity.deviceSecret.ifEmpty { return false }
        val slot = System.currentTimeMillis() / 3_600_000L
        val token = computeHmacHex(secret, slot)
        val userUuid = sessionManager.getUserId()
        val cmd = org.json.JSONObject().apply {
            put("cmd", "unlock_offline")
            put("req_id", UUID.randomUUID().toString())
            put("timestamp", System.currentTimeMillis() / 1000)
            put("token", token)
            if (userUuid != null) put("user_uuid", userUuid)
        }.toString()
        return sendBleUnlockAndWait(lockId, cmd)
    }

    private suspend fun attemptUnlock(lockId: String): Boolean {
        return try {
            val response = lockService.getUnlockToken(UnlockTokenRequest(device_uuid = lockId))
            if (!response.isSuccessful || response.body() == null) return false
            val body = response.body()!!
            val command = buildJsonCommand("unlock", body.token, body.signature.value)
            sendBleUnlockAndWait(lockId, command)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Subscribe to notifications BEFORE writing to avoid the race where the ESP32 sends
    // "unlock_success" before the collector is active and SharedFlow drops it.
    private suspend fun sendBleUnlockAndWait(lockId: String, command: String): Boolean {
        var sent = false
        val notified = coroutineScope {
            val notifJob = async(start = CoroutineStart.UNDISPATCHED) {
                withTimeoutOrNull(10_000L) {
                    bleManager.notifications.first { (uuid, value) ->
                        uuid == BleConstants.CHAR_STATUS_NOTIFY && value.contains("unlock_success")
                    }
                }
            }
            sent = bleManager.sendMessage(command)
            if (sent) notifJob.await() else { notifJob.cancel(); null }
        }
        if (!sent) return false
        if (notified != null) {
            lockDao.getLockById(lockId)?.let { entity ->
                lockDao.updateLock(entity.copy(isLocked = false, lastSynced = System.currentTimeMillis()))
            }
            return true
        }
        return false
    }


    override suspend fun lock(lockId: String): Boolean {
        if (!isNetworkAvailable() || sessionManager.isJwtExpired()) {
            return attemptOfflineBleLock(lockId)
        }
        repeat(3) { attempt ->
            val success = attemptLock(lockId)
            if (success) return true
            if (attempt < 2) delay(1000L * (attempt + 1))
        }
        return false
    }

    private suspend fun attemptOfflineBleLock(lockId: String): Boolean {
        if (bleManager.connectedDeviceAddress.value == null) return false
        val entity = lockDao.getLockById(lockId) ?: return false
        val secret = entity.deviceSecret.ifEmpty { return false }
        val slot = System.currentTimeMillis() / 3_600_000L
        val token = computeHmacHex(secret, slot)
        val userUuid = sessionManager.getUserId()
        val cmd = org.json.JSONObject().apply {
            put("cmd", "lock_offline")
            put("req_id", UUID.randomUUID().toString())
            put("timestamp", System.currentTimeMillis() / 1000)
            put("token", token)
            if (userUuid != null) put("user_uuid", userUuid)
        }.toString()
        return sendBleLockAndWait(lockId, cmd)
    }

    private fun computeHmacHex(secretHex: String, slot: Long): String {
        val key = javax.crypto.spec.SecretKeySpec(
            secretHex.toByteArray(Charsets.UTF_8), "HmacSHA256"
        )
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(key)
        return mac.doFinal(slot.toString().toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private suspend fun attemptLock(lockId: String): Boolean {
        return try {
            val response = lockService.getLockToken(UnlockTokenRequest(device_uuid = lockId))
            if (!response.isSuccessful || response.body() == null) return false
            val body = response.body()!!
            val command = buildJsonCommand("lock", body.token, body.signature.value)
            sendBleLockAndWait(lockId, command)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun buildJsonCommand(cmd: String, token: TokenData, signature: String): String {
        val tokenObj = JSONObject().apply {
            put("version", token.version)
            put("action", token.action)
            put("device_uuid", token.device_uuid)
            put("user_uuid", token.user_uuid)
            put("nonce", token.nonce)
            put("issued_at", token.issued_at)
            put("expires_at", token.expires_at)
            put("session_id", token.session_id)
        }
        return JSONObject().apply {
            put("cmd", cmd)
            put("req_id", UUID.randomUUID().toString())
            put("timestamp", System.currentTimeMillis() / 1000)
            put("token", tokenObj)
            put("signature", signature)
        }.toString()
    }

    private suspend fun sendBleLockAndWait(lockId: String, command: String): Boolean {
        var sent = false
        val notified = coroutineScope {
            val notifJob = async(start = CoroutineStart.UNDISPATCHED) {
                withTimeoutOrNull(10_000L) {
                    bleManager.notifications.first { (uuid, value) ->
                        uuid == BleConstants.CHAR_STATUS_NOTIFY && value.contains("lock_success")
                    }
                }
            }
            sent = bleManager.sendMessage(command)
            if (sent) notifJob.await() else { notifJob.cancel(); null }
        }
        if (!sent) return false
        if (notified != null) {
            lockDao.getLockById(lockId)?.let { entity ->
                lockDao.updateLock(entity.copy(isLocked = true, lastSynced = System.currentTimeMillis()))
            }
            return true
        }
        return false
    }

    override suspend fun syncTime() {
        if (bleManager.connectedDeviceAddress.value == null) return
        val cmd = org.json.JSONObject().apply {
            put("cmd", "sync_time")
            put("req_id", UUID.randomUUID().toString())
            put("timestamp", System.currentTimeMillis() / 1000)
        }.toString()
        bleManager.sendMessage(cmd)
    }

    override suspend fun getDeviceInfo(): Boolean {
        if (bleManager.connectedDeviceAddress.value == null) return false
        return try {
            val command = """{"cmd":"get_info","req_id":"${UUID.randomUUID()}","timestamp":${System.currentTimeMillis() / 1000}}"""
            var sent = false
            val notified = coroutineScope {
                val notifJob = async(start = CoroutineStart.UNDISPATCHED) {
                    withTimeoutOrNull(10_000L) {
                        bleManager.notifications.first { (uuid, value) ->
                            uuid == BleConstants.CHAR_STATUS_NOTIFY && value.contains("info_response")
                        }
                    }
                }
                sent = bleManager.sendMessage(command)
                if (sent) notifJob.await() else { notifJob.cancel(); null }
            }
            if (!sent || notified == null) return false

            val json = org.json.JSONObject(notified.second)
            val data = json.optJSONObject("data") ?: return false
            val hardwareId = data.optString("device_id").ifEmpty { return false }
            val battery = data.optInt("battery_level", -1)
            val fw = data.optString("fw_version", "")

            lockDao.getLockByHardwareId(hardwareId)?.let { entity ->
                lockDao.updateLock(
                    entity.copy(
                        batteryLevel = if (battery >= 0) battery else entity.batteryLevel,
                        firmwareVersion = fw.ifEmpty { entity.firmwareVersion },
                        lastSynced = System.currentTimeMillis()
                    )
                )
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun remoteUnlock(lockId: String): Boolean {
        if (!isNetworkAvailable() || sessionManager.isJwtExpired()) return false
        return try {
            android.util.Log.d("REMOTE", "→ remoteUnlock: sending POST /commands/unlock for $lockId")
            val response = lockService.getUnlockToken(UnlockTokenRequest(device_uuid = lockId))
            android.util.Log.d("REMOTE", "← remoteUnlock: response ${response.code()} ${if (response.isSuccessful) "OK" else response.errorBody()?.string()}")
            response.isSuccessful
        } catch (e: Exception) {
            android.util.Log.e("REMOTE", "← remoteUnlock: exception ${e.message}")
            e.printStackTrace()
            false
        }
    }

    override suspend fun remoteLock(lockId: String): Boolean {
        if (!isNetworkAvailable() || sessionManager.isJwtExpired()) return false
        return try {
            android.util.Log.d("REMOTE", "→ remoteLock: sending POST /commands/lock for $lockId")
            val response = lockService.getLockToken(UnlockTokenRequest(device_uuid = lockId))
            android.util.Log.d("REMOTE", "← remoteLock: response ${response.code()} ${if (response.isSuccessful) "OK" else response.errorBody()?.string()}")
            response.isSuccessful
        } catch (e: Exception) {
            android.util.Log.e("REMOTE", "← remoteLock: exception ${e.message}")
            e.printStackTrace()
            false
        }
    }

    override suspend fun deleteDevice(lockId: String): Boolean {
        return try {
            val entity = lockDao.getLockById(lockId)
            val isOwner = entity?.ownerUuid == sessionManager.getUserId()

            if (isNetworkAvailable()) {
                val response = if (isOwner) {
                    lockService.deleteDevice(lockId)
                } else {
                    lockService.revokeOwnGrant(lockId)
                }
                if (!response.isSuccessful) return false
                lockDao.deleteLock(lockId)
            } else {
                lockDao.getLockById(lockId)?.let {
                    lockDao.updateLock(it.copy(pendingDelete = true))
                }
            }
            true
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