package com.example.ovi

import android.app.Application
import com.example.ovi.data.local.database.AppDatabase
import com.example.ovi.data.repository.AuthRepositoryImpl
import com.example.ovi.util.LockNotificationHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class OviApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        LockNotificationHelper.createChannel(this)
    }
}