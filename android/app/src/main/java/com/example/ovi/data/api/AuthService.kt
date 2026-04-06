package com.example.ovi.data.api

import com.example.ovi.data.dto.LoginRequest
import com.example.ovi.data.dto.LoginResponse
import com.example.ovi.data.dto.LogoutRequest
import com.example.ovi.data.dto.RefreshRequest
import com.example.ovi.data.dto.RefreshResponse
import com.example.ovi.data.dto.RegisterRequest
import com.example.ovi.data.dto.RegisterResponse
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


