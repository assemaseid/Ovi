package com.example.ovi.data.api

import com.example.ovi.data.dto.auth.RefreshRequest
import com.example.ovi.data.local.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider

class TokenAuthenticator @Inject constructor(
    private val sessionManager: SessionManager,
    // Provider breaks the circular dependency: AppModule → OkHttpClient → TokenAuthenticator → AuthService → OkHttpClient
    private val authServiceProvider: Provider<AuthService>
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Если это уже повторный запрос после рефреша — не зацикливаемся
        if (response.request.header("X-Retry-Auth") != null) {
            sessionManager.clearSession()
            return null
        }

        // Если сам запрос рефреша вернул 401 — refresh token истёк, разлогиниваем
        if (response.request.url.toString().contains("auth/refresh")) {
            sessionManager.clearSession()
            return null
        }

        val refreshToken = sessionManager.getRefreshToken() ?: run {
            sessionManager.clearSession()
            return null
        }

        return runBlocking {
            try {
                val result = authServiceProvider.get().refresh(RefreshRequest(refreshToken))
                sessionManager.saveAccessToken(result.accessToken)

                response.request.newBuilder()
                    .header("Authorization", "Bearer ${result.accessToken}")
                    .header("X-Retry-Auth", "true")
                    .build()
            } catch (e: Exception) {
                sessionManager.clearSession()
                null
            }
        }
    }
}
