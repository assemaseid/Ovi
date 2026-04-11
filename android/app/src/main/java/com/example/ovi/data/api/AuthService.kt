package com.example.ovi.data.api

import com.example.ovi.data.dto.LoginRequest
import com.example.ovi.data.dto.LoginResponse
import com.example.ovi.data.dto.RegisterRequest
import com.example.ovi.data.dto.RegisterResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {
    @POST("login_user/")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("register_user/")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    @POST("logout/")
    suspend fun logout()
}


