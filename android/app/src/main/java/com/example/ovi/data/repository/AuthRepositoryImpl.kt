package com.example.ovi.data.repository

import com.example.ovi.data.api.AuthService
import com.example.ovi.data.dto.LoginRequest
import com.example.ovi.data.dto.LogoutRequest
import com.example.ovi.data.dto.RegisterRequest
import com.example.ovi.data.local.SessionManager
import com.example.ovi.data.mapper.toDomain
import com.example.ovi.domain.model.User
import com.example.ovi.domain.repository.AuthRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authService: AuthService,
    private val sessionManager: SessionManager
): AuthRepository {

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val response = authService.login(LoginRequest(email = email, password = password))
            val user = response.user.toDomain(jwtToken = response.accessToken)
            sessionManager.saveUserSession(
                userId = user.id,
                email = user.email,
                jwtToken = response.accessToken,
                refreshToken = response.refreshToken
            )
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

    override suspend fun register(email: String, password: String): Result<User> {
        return try {
            val response = authService.register(RegisterRequest(email = email, password = password))
            val user = response.user.toDomain(jwtToken = response.accessToken)
            sessionManager.saveUserSession(
                userId = user.id,
                email = user.email,
                jwtToken = response.accessToken,
                refreshToken = response.refreshToken
            )
            Result.success(user)
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

    override suspend fun logout(): Result<Exception> {
        try {
            val refreshToken = sessionManager.getRefreshToken()
            if (refreshToken != null) {
                authService.logout(LogoutRequest(refreshToken))
            }
            return Result.success(Exception("Logout successful"))
        } catch (e: Exception) {
            return Result.failure(Exception("Logout failed: ${e.message}"))
        } finally {
            sessionManager.clearSession()
        }
    }

    override suspend fun getCurrentUser(): User? {
        return try {
            if (!sessionManager.isLoggedIn()) return null
            val userId = sessionManager.getUserId() ?: return null
            User(
                id = userId,
                email = sessionManager.getEmail() ?: return null,
                jwtToken = sessionManager.getJwtToken()
            )
        } catch (e: Exception) {
            null
        }
    }
}
