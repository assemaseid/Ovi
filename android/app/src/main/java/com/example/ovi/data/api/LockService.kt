package com.example.ovi.data.api

import com.example.ovi.data.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface LockService {

    @POST("devices/register")
    suspend fun registerDevice(@Body request: DeviceRegistrationRequest): Response<DeviceRegistrationResponse>

    // Issue #7: X-Request-ID and X-Client-Time required by spec
    @POST("commands/unlock")
    suspend fun getUnlockToken(
        @Header("X-Request-ID") requestId: String,
        @Header("X-Client-Time") clientTime: String,
        @Body request: UnlockTokenRequest
    ): Response<UnlockTokenResponse>

    @GET("devices/{deviceId}/pin/current")
    suspend fun getCurrentPin(@Path("deviceId") deviceId: String): Response<CurrentPinResponse>

    @POST("devices/{deviceId}/pin/schedule")
    suspend fun updatePinSchedule(
        @Path("deviceId") deviceId: String,
        @Body request: PinScheduleRequest
    ): Response<PinScheduleResponse>
    
    @POST("devices/{deviceId}/pin/change")
    suspend fun changePin(
        @Path("deviceId") deviceId: String,
        @Body request: PinChangeRequest
    ): Response<PinChangeResponse>
}