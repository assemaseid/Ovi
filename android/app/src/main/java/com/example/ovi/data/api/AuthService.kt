package com.example.ovi.data.api

import com.example.ovi.data.dto.auth.LoginRequest
import com.example.ovi.data.dto.auth.LoginResponse
import com.example.ovi.data.dto.auth.LogoutRequest
import com.example.ovi.data.dto.auth.RefreshRequest
import com.example.ovi.data.dto.auth.RefreshResponse
import com.example.ovi.data.dto.auth.RegisterRequest
import com.example.ovi.data.dto.auth.RegisterResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {
    @POST("auth/login_user/")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/register_user/")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    @POST("auth/refresh/")
    suspend fun refresh(@Body request: RefreshRequest): RefreshResponse

    @POST("auth/logout/")
    suspend fun logout(@Body request: LogoutRequest)
}


