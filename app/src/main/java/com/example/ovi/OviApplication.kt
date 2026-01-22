package com.example.ovi

import android.app.Application
import com.example.ovi.data.local.database.AppDatabase
import com.example.ovi.data.repository.AuthRepositoryImpl

class OviApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val authRepository by lazy { AuthRepositoryImpl(database) }
}