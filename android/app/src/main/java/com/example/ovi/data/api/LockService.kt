package com.example.ovi.data.api

import com.example.ovi.data.dto.device.DeviceOutDto
import com.example.ovi.data.dto.device.DeviceRegistrationRequest
import com.example.ovi.data.dto.device.DeviceRegistrationResponse
import com.example.ovi.data.dto.device.PinResponse
import com.example.ovi.data.dto.command.UnlockTokenRequest
import com.example.ovi.data.dto.command.UnlockTokenResponse
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

    @POST("commands/lock")
    suspend fun getLockToken(
        @Body request: UnlockTokenRequest
    ): Response<UnlockTokenResponse>

    @DELETE("devices/{device_uuid}")
    suspend fun deleteDevice(@Path("device_uuid") deviceUuid: String): Response<Unit>

    @GET("devices/{device_uuid}/pin")
    suspend fun getCurrentPin(@Path("device_uuid") deviceUuid: String): Response<PinResponse>
}