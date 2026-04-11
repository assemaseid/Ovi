package com.example.ovi.data.api

import com.example.ovi.data.dto.FcmTokenRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PUT

interface UserService {

    @PUT("users/me/fcm-token")
    suspend fun updateFcmToken(@Body request: FcmTokenRequest): Response<Unit>
}
