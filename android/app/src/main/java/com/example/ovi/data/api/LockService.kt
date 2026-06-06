package com.example.ovi.data.api

import com.example.ovi.data.dto.device.DeviceOutDto
import com.example.ovi.data.dto.device.DeviceRegistrationRequest
import com.example.ovi.data.dto.device.DeviceRegistrationResponse
import com.example.ovi.data.dto.device.FingerprintDto
import com.example.ovi.data.dto.device.FingerprintEnrollResponse
import com.example.ovi.data.dto.device.FingerprintRenameRequest
import com.example.ovi.data.dto.device.PinResponse
import com.example.ovi.data.dto.device.PinScheduleRequest
import com.example.ovi.data.dto.device.PinScheduleResponse
import com.example.ovi.data.dto.command.UnlockTokenRequest
import com.example.ovi.data.dto.command.UnlockTokenResponse
import com.example.ovi.data.dto.event.EnrichedEventDto
import com.example.ovi.data.dto.event.MobileSyncRequest
import com.example.ovi.data.dto.event.MobileSyncResponse
import com.example.ovi.data.dto.grant.GrantDto
import com.example.ovi.data.dto.grant.GuestDto
import com.example.ovi.data.dto.grant.GuestJoinBody
import com.example.ovi.data.dto.grant.GuestRequestBody
import com.example.ovi.data.dto.grant.GuestRequestResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

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

    @DELETE("grants/self/{device_uuid}")
    suspend fun revokeOwnGrant(@Path("device_uuid") deviceUuid: String): Response<Unit>

    @GET("devices/{device_uuid}/pin")
    suspend fun getCurrentPin(@Path("device_uuid") deviceUuid: String): Response<PinResponse>

    @GET("devices/{device_uuid}/reconfig")
    suspend fun getDeviceReconfig(
        @Path("device_uuid") deviceUuid: String
    ): Response<DeviceRegistrationResponse>

    @POST("events/mobile-sync")
    suspend fun syncMobileEvents(@Body request: MobileSyncRequest): Response<MobileSyncResponse>

    @POST("devices/{device_uuid}/rotate-pin")
    suspend fun rotatePinOnServer(@Path("device_uuid") deviceUuid: String): Response<PinResponse>

    @GET("devices/{device_uuid}/pin-schedule")
    suspend fun getPinSchedule(@Path("device_uuid") deviceUuid: String): Response<PinScheduleResponse>

    @PATCH("devices/{device_uuid}/pin-schedule")
    suspend fun updatePinSchedule(
        @Path("device_uuid") deviceUuid: String,
        @Body request: PinScheduleRequest
    ): Response<PinScheduleResponse>

    @GET("devices/{device_uuid}/fingerprints")
    suspend fun getFingerprints(@Path("device_uuid") deviceUuid: String): Response<List<FingerprintDto>>

    @POST("devices/{device_uuid}/fingerprints/enroll")
    suspend fun enrollFingerprint(@Path("device_uuid") deviceUuid: String): Response<FingerprintEnrollResponse>

    @PATCH("devices/{device_uuid}/fingerprints/{finger_id}/name")
    suspend fun renameFingerprint(
        @Path("device_uuid") deviceUuid: String,
        @Path("finger_id") fingerId: Int,
        @Body request: FingerprintRenameRequest
    ): Response<FingerprintDto>

    @DELETE("devices/{device_uuid}/fingerprints/{finger_id}")
    suspend fun deleteFingerprint(
        @Path("device_uuid") deviceUuid: String,
        @Path("finger_id") fingerId: Int
    ): Response<Unit>

    @POST("grants/guest-request")
    suspend fun requestGuestAccess(@Body request: GuestRequestBody): Response<GuestRequestResponse>

    @POST("grants/{device_uuid}/guest-join")
    suspend fun joinAsGuest(
        @Path("device_uuid") deviceUuid: String,
        @Body request: GuestJoinBody
    ): Response<GrantDto>

    @GET("devices/{device_uuid}/guests")
    suspend fun getGuests(@Path("device_uuid") deviceUuid: String): Response<List<GuestDto>>

    @DELETE("grants/{grant_uuid}")
    suspend fun revokeGrant(@Path("grant_uuid") grantUuid: String): Response<Unit>

    @GET("events/{device_uuid}/enriched")
    suspend fun getEnrichedEvents(
        @Path("device_uuid") deviceUuid: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<List<EnrichedEventDto>>
}