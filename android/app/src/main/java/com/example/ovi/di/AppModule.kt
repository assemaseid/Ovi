package com.example.ovi.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.example.ovi.data.api.AuthService
import com.example.ovi.data.local.SessionManager
//import com.example.ovi.data.local.database.AppDatabase
import com.example.ovi.data.repository.AuthRepositoryImpl
import com.example.ovi.domain.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(sessionManager: SessionManager): OkHttpClient {
        return OkHttpClient.Builder()
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

            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8000/api/v1/auth/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthService(retrofit: Retrofit): AuthService {
        return retrofit.create(AuthService::class.java)
    }

//    @Provides
//    @Singleton
//    fun provideAppDatabase(
//        @ApplicationContext context: Context
//    ): AppDatabase {
//        return AppDatabase.getDatabase(context)
//    }

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
    @Singleton
    fun provideAuthRepository(
        authService: AuthService,
        sessionManager: SessionManager
    ): AuthRepository {
        return AuthRepositoryImpl(authService,sessionManager)
    }
}