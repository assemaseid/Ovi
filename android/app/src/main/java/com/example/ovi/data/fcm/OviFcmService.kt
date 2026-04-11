package com.example.ovi.data.fcm

import android.util.Log
import com.example.ovi.data.api.UserService
import com.example.ovi.data.dto.FcmTokenRequest
import com.example.ovi.data.local.SessionManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class OviFcmService : FirebaseMessagingService() {

    @Inject lateinit var userService: UserService
    @Inject lateinit var sessionManager: SessionManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Called when FCM token is created or refreshed
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: ${token.take(20)}...")
        if (sessionManager.isLoggedIn() && !sessionManager.isJwtExpired()) {
            sendTokenToServer(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("FCM", "Message from: ${message.from}, data: ${message.data}")
        // Notifications sent with "notification" payload are shown automatically by Firebase.
        // Data-only messages (e.g. "action": "show_pin") are handled here if needed.
        val action = message.data["action"]
        if (action == "show_pin") {
            // PIN rotation notification — app will fetch PIN on next open
            Log.d("FCM", "PIN rotation notification received for device: ${message.data["device_uuid"]}")
        }
    }

    fun sendTokenToServer(token: String) {
        scope.launch {
            try {
                userService.updateFcmToken(FcmTokenRequest(fcmToken = token))
                Log.d("FCM", "Token sent to server")
            } catch (e: Exception) {
                Log.e("FCM", "Failed to send token: ${e.message}")
            }
        }
    }
}
