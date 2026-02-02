package com.example.ovi.data.local.dao

import androidx.room.*
import com.example.ovi.data.local.entity.LockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LockDao {
    @Query("SELECT * FROM locks")
    fun getAllLocks(): Flow<List<LockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLock(lock: LockEntity)

    @Update
    suspend fun updateLock(lock: LockEntity)

    @Query("DELETE FROM locks WHERE id = :lockId")
    suspend fun deleteLock(lockId: String)
}