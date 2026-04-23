package com.example.ovi.data.websocket

import android.util.Log
import com.example.ovi.data.local.SessionManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class WebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val sessionManager: SessionManager,
    @Named("wsBaseUrl") private val wsBaseUrl: String
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()

    private val _events = MutableSharedFlow<WsEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<WsEvent> = _events.asSharedFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var webSocket: WebSocket? = null
    private var isManualDisconnect = false
    private var reconnectAttempts = 0

    fun connect() {
        // Уже подключены — ничего не делаем
        if (_isConnected.value) return

        val token = sessionManager.getJwtToken() ?: run {
            Log.w("WS", "No JWT token — skipping WebSocket connection")
            return
        }
        if (sessionManager.isJwtExpired()) {
            Log.w("WS", "JWT expired — skipping WebSocket connection")
            return
        }

        // Закрываем старый мёртвый сокет если есть
        webSocket?.cancel()
        webSocket = null

        Log.d("WS", "Connecting...")
        isManualDisconnect = false
        reconnectAttempts = 0
        openSocket()
    }

    fun disconnect() {
        isManualDisconnect = true
        webSocket?.close(1000, "User logged out")
        webSocket = null
        _isConnected.value = false
    }

    private fun openSocket() {
        // Берём свежий токен при каждом открытии сокета
        val token = sessionManager.getJwtToken() ?: run {
            Log.w("WS", "No token on openSocket — aborting")
            return
        }
        if (sessionManager.isJwtExpired()) {
            Log.w("WS", "Token expired on openSocket — aborting")
            return
        }

        val request = Request.Builder()
            .url("${wsBaseUrl}ws/events?token=$token")
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d("WS", "Connected")
                _isConnected.value = true
                reconnectAttempts = 0
            }

            override fun onMessage(ws: WebSocket, text: String) {
                Log.d("WS", "<<< recv: $text")
                handleMessage(text)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e("WS", "Failure: ${t.message}")
                _isConnected.value = false
                webSocket = null  // сбрасываем чтобы connect() мог переподключить
                scheduleReconnect()
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d("WS", "Closed: $code $reason")
                _isConnected.value = false
                webSocket = null  // сбрасываем чтобы connect() мог переподключить
                if (!isManualDisconnect) scheduleReconnect()
            }
        })
    }

    private fun handleMessage(text: String) {
        try {
            val map: Map<String, Any?> = gson.fromJson(
                text, object : TypeToken<Map<String, Any?>>() {}.type
            )
            val type = map["type"] as? String

            when {
                type == "connected" -> _events.tryEmit(WsEvent.Connected)

                type == "ping" -> {
                    Log.d("WS", ">>> send: {\"type\":\"pong\"}")
                    webSocket?.send("{\"type\":\"pong\"}")
                }

                type == "device_status" -> {
                    val deviceUuid = map["device_uuid"] as? String ?: return
                    val batteryLevel = (map["battery_level"] as? Number)?.toInt()
                    val lastSeen = map["last_seen"] as? String
                    val firmwareVersion = map["firmware_version"] as? String
                    _events.tryEmit(WsEvent.DeviceStatus(deviceUuid, batteryLevel, lastSeen, firmwareVersion))
                }

                map.containsKey("device_uuid") && map.containsKey("event") -> {
                    val deviceUuid = map["device_uuid"] as? String ?: return
                    @Suppress("UNCHECKED_CAST")
                    val event = map["event"] as? Map<String, Any?> ?: return
                    val eventType = event["type"] as? String ?: return
                    Log.d("WS", "device_event: $eventType for $deviceUuid")
                    _events.tryEmit(WsEvent.DeviceEvent(deviceUuid, eventType, event))
                }

                else -> Log.d("WS", "Unknown message: $text")
            }
        } catch (e: Exception) {
            Log.e("WS", "Parse error: ${e.message}")
        }
    }

    private fun scheduleReconnect() {
        if (isManualDisconnect) return
        if (reconnectAttempts >= 10) {
            // После 10 попыток сбрасываем счётчик — connect() сможет попробовать снова
            Log.w("WS", "Max reconnect attempts reached, resetting")
            reconnectAttempts = 0
            webSocket = null
            return
        }
        reconnectAttempts++
        val delayMs = (reconnectAttempts * 2000L).coerceAtMost(30_000L)
        Log.d("WS", "Reconnecting in ${delayMs}ms (attempt $reconnectAttempts)")
        scope.launch {
            delay(delayMs)
            if (!isManualDisconnect) openSocket()
        }
    }
}
