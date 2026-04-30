package com.example.ovi.data.repository

import com.example.ovi.data.local.dao.EventDao
import com.example.ovi.data.mapper.toEventDomain
import com.example.ovi.data.mapper.toEventEntity
import com.example.ovi.domain.model.LockEvent
import com.example.ovi.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao
) : EventRepository {

    override fun getEventsForLock(lockId: String): Flow<List<LockEvent>> {
        return eventDao.getEventsForLock(lockId).map { list ->
            list.map { it.toEventDomain() }
        }
    }

    override suspend fun addEvent(event: LockEvent) {
        eventDao.insertEvent(event.toEventEntity())
    }
}