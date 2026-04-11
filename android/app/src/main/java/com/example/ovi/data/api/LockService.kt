package com.example.ovi.data.api

import com.example.ovi.data.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface LockService {

    @GET("devices")
    suspend fun getDevices(): Response<List<DeviceOutDto>>

    @POST("devices/register_device")
    suspend fun registerDevice(@Body request: DeviceRegistrationRequest): Response<DeviceRegistrationResponse>

    @POST("commands/unlock")
    suspend fun getUnlockToken(
        @Body request: UnlockTokenRequest
    ): Response<UnlockTokenResponse>

    @POST("commands/remote_unlock")
    suspend fun remoteUnlock(@Body request: UnlockTokenRequest): Response<Unit>

    @POST("commands/remote_lock")
    suspend fun remoteLock(@Body request: UnlockTokenRequest): Response<Unit>

    @DELETE("devices/{device_uuid}")
    suspend fun deleteDevice(@Path("device_uuid") deviceUuid: String): Response<Unit>
}