package com.example.ovi.data.websocket

sealed class WsEvent {
    object Connected : WsEvent()
    object Disconnected : WsEvent()
    data class DeviceEvent(
        val deviceUuid: String,
        val eventType: String,
        val eventData: Map<String, Any?>
    ) : WsEvent()
}
