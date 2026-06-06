package com.example.ovi.data.repository

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.ovi.data.api.LockService
import com.example.ovi.data.dto.event.MobileEventRequest
import com.example.ovi.data.dto.event.MobileSyncRequest
import com.example.ovi.data.local.dao.EventDao
import com.example.ovi.data.mapper.toEventDomain
import com.example.ovi.data.mapper.toEventEntity
import com.example.ovi.domain.model.LockEvent
import com.example.ovi.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao,
    private val lockService: LockService,
    private val connectivityManager: ConnectivityManager
) : EventRepository {

    override fun getEventsForLock(lockId: String): Flow<List<LockEvent>> {
        return eventDao.getEventsForLock(lockId).map { list ->
            list.map { it.toEventDomain() }
        }
    }

    override suspend fun addEvent(event: LockEvent) {
        eventDao.insertEvent(event.toEventEntity())
    }

    override suspend fun syncPendingEvents() {
        if (!isNetworkAvailable()) return
        val unsynced = eventDao.getUnsyncedEvents()
        if (unsynced.isEmpty()) return

        val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val dtos = unsynced.map { entity ->
            val eventData = buildMap<String, Any> {
                put("method", entity.method)
                put("success", entity.success)
                entity.userUuid?.let { put("user_uuid", it) }
            }
            val eventTypeName = when (entity.type) {
                "UNLOCK" -> "unlock_success"
                "LOCK"   -> "lock_success"
                else     -> entity.type.lowercase()
            }
            MobileEventRequest(
                msg_id = entity.msgId,
                device_uuid = entity.lockId,
                event_type = eventTypeName,
                event_data = eventData,
                created_at = isoFmt.format(Date(entity.timestamp))
            )
        }

        try {
            val response = lockService.syncMobileEvents(MobileSyncRequest(dtos))
            if (response.isSuccessful) {
                unsynced.forEach { eventDao.markSynced(it.id) }
            }
        } catch (_: Exception) { }
    }

    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
