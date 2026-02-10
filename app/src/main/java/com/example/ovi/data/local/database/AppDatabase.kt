package com.example.ovi.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.ovi.data.local.dao.EventDao
import com.example.ovi.data.local.dao.LockDao
import com.example.ovi.data.local.dao.UserDao
import com.example.ovi.data.local.entity.EventEntity
import com.example.ovi.data.local.entity.LockEntity
import com.example.ovi.data.local.entity.UserEntity

@Database(
    entities = [UserEntity::class, EventEntity::class, LockEntity::class],
    version = 2,
    exportSchema = false
)

abstract class AppDatabase: RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun eventDao(): EventDao
    abstract fun lockDao(): LockDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ovi_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}