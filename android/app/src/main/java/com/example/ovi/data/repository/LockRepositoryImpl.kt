package com.example.ovi.data.repository

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.ovi.data.api.LockService
import com.example.ovi.data.dto.UnlockTokenRequest
import com.example.ovi.data.local.SessionManager
import com.example.ovi.data.local.dao.LockDao
import com.example.ovi.data.local.entity.LockEntity
import com.example.ovi.data.mapper.toLockDomain
import com.example.ovi.data.mapper.toLockEntity
import com.example.ovi.domain.ble.BleManager
import com.example.ovi.domain.model.SmartLock
import com.example.ovi.domain.repository.LockRepository
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import com.example.ovi.util.BleConstants
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

    override suspend fun addLock(lock: SmartLock) {
        val existing = lockDao.getLockByHardwareId(lock.hardwareId)
        if (existing != null) {
            lockDao.updateLock(lock.toLockEntity().copy(deviceId = existing.deviceId))
        } else {
            lockDao.insertLock(lock.toLockEntity())
        }
    }

    override suspend fun syncDevicesFromServer() {
        if (!isNetworkAvailable()) return
        try {
            val response = lockService.getDevices()
            if (!response.isSuccessful) return
            val devices = response.body() ?: return
            val ownerUuid = sessionManager.getUserId() ?: ""

            for (dto in devices) {
                val existing = lockDao.getLockById(dto.deviceUuid)
                if (existing != null) {
                    // Устройство уже есть локально — обновляем только данные с сервера,
                    // сохраняем локальное name и isLocked
                    lockDao.updateLock(
                        existing.copy(
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
                            ownerUuid = ownerUuid,
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

        // Try server-authenticated unlock when backend is reachable
        if (isNetworkAvailable() && !sessionManager.isJwtExpired()) {
            repeat(3) { attempt ->
                val success = attemptUnlock(lockId)
                if (success) return true
                if (attempt < 2) delay(1000L * (attempt + 1))
            }
        }

        // Fallback: direct BLE unlock — firmware accepts cmd:"unlock" without token validation
        return attemptDirectBleUnlock(lockId)
    }

    private suspend fun attemptUnlock(lockId: String): Boolean {
        return try {
            val clientTimestamp = System.currentTimeMillis() / 1000

            val response = lockService.getUnlockToken(
                request = UnlockTokenRequest(device_uuid = lockId)
            )

            if (!response.isSuccessful || response.body() == null) return false
            val body = response.body()!!

            val command = """{"cmd":"unlock","req_id":"${UUID.randomUUID()}","timestamp":${clientTimestamp},"token":{"nonce":"${body.token.nonce}","expires":${body.token.expires_at},"device_uuid":"${body.token.device_uuid}","user_uuid":"${body.token.user_uuid}","action":"unlock"},"signature":"${body.signature.value}"}"""

            sendBleUnlockAndWait(lockId, command)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun attemptDirectBleUnlock(lockId: String): Boolean {
        val requestId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis() / 1000
        val command = """{"cmd":"unlock","req_id":"$requestId","timestamp":$timestamp}"""
        return try {
            sendBleUnlockAndWait(lockId, command)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

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
        val command = """{"cmd":"lock","req_id":"${UUID.randomUUID()}","timestamp":${System.currentTimeMillis() / 1000}}"""

        repeat(3) { attempt ->
            val sent = bleManager.sendMessage(command)
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

    override suspend fun deleteDevice(lockId: String): Boolean {
        return try {
            if (isNetworkAvailable()) {
                val response = lockService.deleteDevice(lockId)
                if (!response.isSuccessful) return false
            }
            lockDao.deleteLock(lockId)
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