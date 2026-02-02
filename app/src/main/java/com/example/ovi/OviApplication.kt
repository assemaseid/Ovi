package com.example.ovi

import android.app.Application
import com.example.ovi.data.local.database.AppDatabase
import com.example.ovi.data.repository.AuthRepositoryImpl
import com.example.ovi.domain.repository.LockRepository
import com.example.ovi.data.repository.FakeLockRepository
import java.util.concurrent.locks.Lock

class OviApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val authRepository by lazy { AuthRepositoryImpl(database) }
    val lockRepository: LockRepository by lazy { FakeLockRepository() }
}