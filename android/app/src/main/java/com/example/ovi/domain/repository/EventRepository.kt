package com.example.ovi.domain.repository

import com.example.ovi.domain.model.LockEvent
import kotlinx.coroutines.flow.Flow


interface EventRepository {
    fun getEventsForLock(lockId: String): Flow<List<LockEvent>>
    suspend fun addEvent(event: LockEvent)
}