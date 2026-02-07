package com.example.ovi.data.api


import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse
}

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val access_token: String,
    val refresh_token: String,
    val user_id: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String? = null,
    val phone: String? = null
)

data class RegisterResponse(
    val user_id: String,
    val email: String
)