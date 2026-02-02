package com.example.ovi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.ovi.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDAO {
    @Query("SELECT * FROM events WHERE lockId = :lockId ORDER BY timestamp DESC")
    fun getEventsForLock(lockId: String): Flow<List<EventEntity>>

    @Insert
    suspend fun insertEvent(event: EventEntity)
}