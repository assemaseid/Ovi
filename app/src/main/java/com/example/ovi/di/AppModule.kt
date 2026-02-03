package com.example.ovi.di  // Укажите правильный пакет

import android.content.Context
import com.example.ovi.data.api.AuthService
import com.example.ovi.data.local.database.AppDatabase
import com.example.ovi.data.repository.AuthRepositoryImpl
import com.example.ovi.domain.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://ваш-api.com/")  // change URL
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
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        authService: AuthService,
        database: AppDatabase
    ): AuthRepository {
        return AuthRepositoryImpl(authService, database)
    }
}