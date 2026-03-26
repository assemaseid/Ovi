package com.example.ovi.data.api

import com.example.ovi.data.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface LockService {

    @POST("devices/register")
    suspend fun registerDevice(@Body request: DeviceRegistrationRequest): Response<DeviceRegistrationResponse>

    @POST("commands/unlock")
    suspend fun getUnlockToken(
        @Header("X-Request-ID") requestId: String,
        @Header("X-Client-Time") clientTime: String,
        @Body request: UnlockTokenRequest
    ): Response<UnlockTokenResponse>
}