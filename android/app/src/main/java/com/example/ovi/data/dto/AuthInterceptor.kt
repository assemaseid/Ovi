package com.example.ovi.data.dto

import com.example.ovi.data.local.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request().newBuilder()
            sessionManager.getJwtToken()?.let {
                request.addHeader("Authorization", "Bearer $it")
            }
            return chain.proceed((request.build()))
        }
}