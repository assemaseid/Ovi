package com.example.ovi.data.fcm

import android.util.Log
import com.example.ovi.data.api.UserService
import com.example.ovi.data.dto.auth.FcmTokenRequest
import com.example.ovi.data.local.SessionManager
import com.example.ovi.util.LockNotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

        // When the app is in the foreground Firebase does NOT auto-display the notification
        // payload — we must show it ourselves via LockNotificationHelper.
        val title = message.notification?.title
        val body  = message.notification?.body

        if (title != null && body != null) {
            LockNotificationHelper.show(this, title, body)
            return
        }

        // Fallback for data-only messages
        val (t, b) = when (message.data["event_type"]) {
            "unlock_success"  -> "Lock opened"    to "Your door was unlocked"
            "lock_success"    -> "Lock closed"    to "Your door was locked"
            "tamper_detected" -> "Security Alert" to "Tamper detected on your lock!"
            "battery_low"     -> "Battery Low"    to "Your lock battery is running low"
            "pin_rotation"    -> "New PIN Code"   to "Your lock PIN has been rotated"
            else              -> return
        }
        LockNotificationHelper.show(this, t, b)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun sendTokenToServer(token: String) {
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
