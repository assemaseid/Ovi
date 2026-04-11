package com.example.ovi.data.websocket

sealed class WsEvent {
    object Connected : WsEvent()
    object Disconnected : WsEvent()
    data class DeviceEvent(
        val deviceUuid: String,
        val eventType: String,
        val eventData: Map<String, Any?>
    ) : WsEvent()

    data class DeviceStatus(
        val deviceUuid: String,
        val batteryLevel: Int?,
        val lastSeen: String?,
        val firmwareVersion: String?
    ) : WsEvent()
}
