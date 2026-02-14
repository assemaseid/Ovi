package com.example.ovi.data.repository

import com.example.ovi.data.api.AuthService
import com.example.ovi.data.dto.LoginRequest
import com.example.ovi.data.dto.RegisterRequest
import com.example.ovi.data.local.SessionManager
import com.example.ovi.data.local.database.AppDatabase
import com.example.ovi.data.mapper.toDomain
import com.example.ovi.data.mapper.toEntity
import com.example.ovi.domain.model.User
import com.example.ovi.domain.repository.AuthRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authService: AuthService,
    private val database: AppDatabase,
    private val sessionManager: SessionManager
): AuthRepository {
    private val userDao = database.userDao()

    override suspend fun login(
        email: String,
        password: String
    ): Result<User> {
        return try {
            val request = LoginRequest(email = email, password = password)
            val response = authService.login(request)


            val user = response.user.toDomain(jwtToken = response.accessToken)

            sessionManager.saveUserSession(
                userId = user.id,
                email = user.email,
                name = user.name,
                jwtToken = response.accessToken
            )

            val userEntity = response.user.toEntity(jwtToken = response.accessToken)
            userDao.insertUser(userEntity)


            Result.success(user)

        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                401 -> "Invalid email or password"
                404 -> "User not found"
                500 -> "Server error. Please try again later"
                else -> "Login failed: ${e.message()}"
            }
            Result.failure(Exception(errorMessage))

        } catch (e: IOException) {
            Result.failure(Exception("Network error. Check your internet connection"))

        } catch (e: Exception) {
            Result.failure(Exception("Login failed: ${e.message}"))
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        name: String
    ): Result<User> {
        return try {
            val request = RegisterRequest(
                email = email,
                password = password,
                name = name
            )
            val response = authService.register(request)

//            val user = response.user.toDomain(jwtToken = response.accessToken)
//
////            sessionManager.saveUserSession(
////                userId = user.id,
////                email = user.email,
////                name = user.name,
////                jwtToken = response.accessToken
////            )
//
//            val userEntity = response.user.toEntity(jwtToken = response.accessToken)
////            userDao.insertUser(userEntity)
//
//            Result.success(user)
            login(email, password)

        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                400 -> "Invalid registration data"
                409 -> "User with this email already exists"
                500 -> "Server error. Please try again later"
                else -> "Registration failed: ${e.message()}"
            }
            Result.failure(Exception(errorMessage))

        } catch (e: IOException) {
            Result.failure(Exception("Network error. Check your internet connection"))

        } catch (e: Exception) {
            Result.failure(Exception("Registration failed: ${e.message}"))
        }
    }


    override suspend fun logout() {
        sessionManager.clearSession()
    }

    override suspend fun getCurrentUser(): User? {
        return try {
            if (!sessionManager.isLoggedIn()) {
                return null
            }

            val userId = sessionManager.getUserId()
            if (userId == -1) {
                return null
            }

            userDao.getUserById(userId)?.toDomain()
        } catch (e: Exception) {
            null
        }
    }
}