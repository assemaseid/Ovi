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
        val firmwareVersion: String?,
        val isLocked: Boolean?
    ) : WsEvent()

    data class PinRotated(
        val deviceUuid: String,
        val newPin: String,
        val nextRotationAt: String?
    ) : WsEvent()

    data class Notification(
        val title: String,
        val body: String,
        val timestamp: String?
    ) : WsEvent()
}
