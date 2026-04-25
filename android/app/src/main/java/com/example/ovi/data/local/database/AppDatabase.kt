package com.example.ovi.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.ovi.data.local.dao.*
import com.example.ovi.data.local.entity.*
import com.example.ovi.data.local.entity.LockEntity


@Database(
    entities = [EventEntity:: class, LockEntity::class],
    version = 6,
    exportSchema = false
)

abstract class AppDatabase: RoomDatabase() {
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