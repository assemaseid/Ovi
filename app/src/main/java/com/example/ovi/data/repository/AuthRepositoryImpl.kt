package com.example.ovi.data.repository

import com.example.ovi.data.api.AuthService
import com.example.ovi.data.local.SessionManager
import com.example.ovi.data.local.dao.UserDao
import com.example.ovi.data.local.entity.UserEntity
import com.example.ovi.data.mapper.toDomain
import com.example.ovi.data.remote.dto.LoginRequest
import com.example.ovi.data.remote.dto.RegisterRequest
import com.example.ovi.domain.model.User
import com.example.ovi.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authService: AuthService,
    private val userDao: UserDao,
    private val sessionManager: SessionManager
): AuthRepository {

    override suspend fun login(
        email: String,
        password: String
    ): Result<User> {
        return try {
            val response = authService.login(LoginRequest(email, password))

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                sessionManager.saveUserSession(
                    userId = body.user_id,
                    email = email,
                    name = "User",
                    jwtToken = body.access_token
                )

                val userEntity = UserEntity(
                    id = body.user_id,
                    email = email,
                    name = "User",
                    passwordHash = null,
                    jwtToken = body.access_token,
                    lastLogin = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis()
                )
                userDao.insertUser(userEntity)

                Result.success(userEntity.toDomain())
            } else {
                Result.failure(Exception("Login failed: ${response.code()}"))
            }
        }
        catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        name: String
    ): Result<User> {
        return try {
            val request = RegisterRequest(email, password, name)
            val response = authService.register(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val user = User(
                    id = body.user_id,
                    email = body.email,
                    name = name,
                    jwtToken = null
                )
                Result.success(user)
            } else {
                Result.failure(Exception("Failed to register user"))

            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun logout() {
        sessionManager.clearSession()
    }

    override suspend fun getCurrentUser(): User? {
        if (!sessionManager.isLoggedIn()) return null
        val userId = sessionManager.getUserId() ?: return null
        return userDao.getUserById(userId)?.toDomain()
    }
}