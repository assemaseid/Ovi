package com.example.ovi.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import com.example.ovi.data.api.AuthService
import com.example.ovi.data.api.LockService
import com.example.ovi.data.api.UserService
import com.example.ovi.data.ble.AndroidBleManager
import com.example.ovi.data.dto.TokenAuthenticator
import com.example.ovi.data.local.SessionManager
import com.example.ovi.data.local.database.AppDatabase
import com.example.ovi.data.local.dao.EventDao
import com.example.ovi.data.local.dao.LockDao
import com.example.ovi.data.repository.AuthRepositoryImpl
import com.example.ovi.data.repository.EventRepositoryImpl
import com.example.ovi.data.repository.LockRepositoryImpl
import com.example.ovi.data.websocket.WebSocketManager
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
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        sessionManager: SessionManager,
        authServiceProvider: Provider<AuthService>
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val token = sessionManager.getJwtToken()
                val request = if (token != null) {
                    chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                } else {
                    chain.request()
                }
                chain.proceed(request)
            }
            .authenticator(TokenAuthenticator(sessionManager, authServiceProvider))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .build()
    }
    @Provides
    @Singleton
    @Named("wsBaseUrl")
    fun provideWsBaseUrl(): String = "ws://172.22.100.87:8000/"

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://172.22.100.87:8000/api/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthService(retrofit: Retrofit): AuthService = retrofit.create(AuthService::class.java)

    @Provides
    @Singleton
    fun provideLockService(retrofit: Retrofit): LockService = retrofit.create(LockService::class.java)

    @Provides
    @Singleton
    fun provideUserService(retrofit: Retrofit): UserService = retrofit.create(UserService::class.java)

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getDatabase(context)

    @Provides
    @Singleton
    fun provideSharedPreferences(application: Application): SharedPreferences =
        application.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    @Provides
    @Singleton
    fun provideSessionManager(sharedPreferences: SharedPreferences): SessionManager =
        SessionManager(sharedPreferences)
    
    @Provides
    @Singleton
    fun provideConnectivityManager(@ApplicationContext context: Context): ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @Provides
    @Singleton
    fun provideAuthRepository(authService: AuthService, userService: UserService, sessionManager: SessionManager): AuthRepository =
        AuthRepositoryImpl(authService, userService, sessionManager)

    @Provides
    @Singleton
    fun provideLockDao(db: AppDatabase) = db.lockDao()

    @Provides
    @Singleton
    fun provideEventDao(db: AppDatabase) = db.eventDao()

    @Provides
    @Singleton
    fun provideLockRepository(
        ls: LockService,
        bm: BleManager,
        ld: LockDao,
        sm: SessionManager,
        cm: ConnectivityManager
    ): LockRepository = LockRepositoryImpl(ls, bm, ld, sm, cm)

    @Provides
    @Singleton
    fun provideEventRepository(ed: EventDao): EventRepository = EventRepositoryImpl(ed)

    @Provides
    @Singleton
    fun provideBleManager(@ApplicationContext context: Context): BleManager =
        AndroidBleManager(context)

    @Provides
    @Singleton
    fun provideWebSocketManager(
        okHttpClient: OkHttpClient,
        sessionManager: SessionManager,
        @Named("wsBaseUrl") wsBaseUrl: String
    ): WebSocketManager = WebSocketManager(okHttpClient, sessionManager, wsBaseUrl)
}