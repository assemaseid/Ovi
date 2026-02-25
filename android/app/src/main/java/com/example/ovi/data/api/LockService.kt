package com.example.ovi.data.api

import com.example.ovi.data.dto.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.Response

interface LockService {
    @POST("devices/register")
    suspend fun registerDevice(@Body request: DeviceRegistrationRequest): Response<DeviceRegistrationResponse>

    @POST("commands/unlock")
    suspend fun getUnlockToken(@Body request: UnlockTokenRequest): Response<UnlockTokenResponse>

    @GET("devices/{deviceId}/pin/current")
    suspend fun getCurrentPin(@Path("deviceId") deviceId: String): Response<CurrentPinResponse>

    @POST("devices/{deviceId}/pin/schedule")
    suspend fun updatePinSchedule(
        @Path("deviceId") deviceId: String,
        @Body request: PinScheduleRequest
    ): Response<PinScheduleResponse>
}