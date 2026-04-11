package com.example.ovi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.ovi.data.local.entity.LockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LockDao {
    @Query("SELECT * FROM locks WHERE pendingDelete = 0")
    fun getAllLocks(): Flow<List<LockEntity>>

    @Query("SELECT * FROM locks WHERE deviceId = :lockId LIMIT 1")
    suspend fun getLockById(lockId: String): LockEntity?

    @Query("SELECT * FROM locks WHERE hardwareId = :hardwareId LIMIT 1")
    suspend fun getLockByHardwareId(hardwareId: String): LockEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLock(lock: LockEntity)

    @Update
    suspend fun updateLock(lock: LockEntity)

    @Query("DELETE FROM locks WHERE deviceId = :lockId")
    suspend fun deleteLock(lockId: String)
}