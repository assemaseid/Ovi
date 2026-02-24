package com.example.ovi.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.ovi.data.local.entity.LockEntity


@Database(
    entities = [LockEntity::class],
    version = 3,
    exportSchema = false
)

abstract class AppDatabase: RoomDatabase() {

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
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}