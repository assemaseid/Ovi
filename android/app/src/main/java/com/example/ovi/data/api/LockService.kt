package com.example.ovi.data.api

import com.example.ovi.data.dto.*
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Response

interface LockService {
    @POST("api/v1/devices/register")
    suspend fun registerDevice(@Body request: DeviceRegistrationRequest): Response<DeviceRegistrationResponse>

    @POST("api/v1/commands/unlock")
    suspend fun getUnlockToken(@Body request: UnlockTokenRequest): Response<UnlockTokenResponse>
}