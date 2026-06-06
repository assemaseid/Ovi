package com.example.ovi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.ovi.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE lockId = :lockId ORDER BY timestamp DESC")
    fun getEventsForLock(lockId: String): Flow<List<EventEntity>>

    @Insert
    suspend fun insertEvent(event: EventEntity)

    @Query("DELETE FROM events WHERE lockId = :lockId")
    suspend fun deleteEventsForLock(lockId: String)

    @Query("SELECT * FROM events WHERE synced = 0")
    suspend fun getUnsyncedEvents(): List<EventEntity>

    @Query("UPDATE events SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Int)
}
