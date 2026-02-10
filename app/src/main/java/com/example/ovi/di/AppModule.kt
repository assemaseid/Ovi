package com.example.ovi.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import coil.intercept.Interceptor
import com.example.ovi.data.api.AuthService
import com.example.ovi.data.api.LockService
import com.example.ovi.data.ble.AndroidBleManager
import com.example.ovi.data.local.SessionManager
import com.example.ovi.data.local.dao.EventDao
import com.example.ovi.data.local.dao.LockDao
import com.example.ovi.data.local.dao.UserDao
import com.example.ovi.data.local.database.AppDatabase
import com.example.ovi.data.remote.AuthInterceptor
import com.example.ovi.data.repository.AuthRepositoryImpl
import com.example.ovi.data.repository.EventRepositoryImpl
import com.example.ovi.data.repository.LockRepositoryImpl
import com.example.ovi.domain.ble.BleManager
import com.example.ovi.domain.repository.AuthRepository
import com.example.ovi.domain.repository.EventRepository
import com.example.ovi.domain.repository.LockRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit. SECONDS)
            .readTimeout(30, TimeUnit. SECONDS)
            .build()
    }
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.com/")  // change URL
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthService(retrofit: Retrofit): AuthService {
      return retrofit.create(AuthService::class.java)
    }

    @Provides
    @Singleton
    fun provideLockService(retrofit: Retrofit): LockService {
        return retrofit.create(LockService::class.java)
    }


    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(
        application: Application
    ): SharedPreferences {
        return application.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    }


    @Provides
    @Singleton
    fun provideSessionManager(
        sharedPreferences: SharedPreferences
    ): SessionManager {
        return SessionManager(sharedPreferences)
    }


    @Provides
    fun provideEventDao(database: AppDatabase): EventDao {
        return database.eventDao()
    }

    @Provides
    fun provideLockDao(database: AppDatabase): LockDao {
        return database.lockDao()
    }

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        authService: AuthService,
        userDao: UserDao,
        sessionManager: SessionManager
    ): AuthRepository {
        return AuthRepositoryImpl(authService, userDao, sessionManager)
    }

    @Provides
    @Singleton
    fun provideLockRepository(
        lockDao: LockDao,
        lockService: LockService,
        bleManager: BleManager
    ): LockRepository {
        return LockRepositoryImpl(lockService, bleManager, lockDao)
    }

    @Provides
    @Singleton
    fun provideEventRepository(
        eventDao: EventDao
    ): EventRepository {
        return EventRepositoryImpl(eventDao)
    }

    @Provides
    @Singleton
    fun provideBleManager(
        @ApplicationContext context: Context
    ): BleManager {
        return AndroidBleManager(context)
    }

}