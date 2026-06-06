package com.example.ovi.data.dto.device

import com.google.gson.annotations.SerializedName

data class DeviceOutDto(
    @SerializedName("device_uuid") val deviceUuid: String,
    @SerializedName("user_uuid") val userUuid: String?,
    @SerializedName("hardware_id") val hardwareId: String,
    @SerializedName("firmware_version") val firmwareVersion: String?,
    @SerializedName("battery_level") val batteryLevel: Int?,
    @SerializedName("last_seen") val lastSeen: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("config") val config: Map<String, Any?> = emptyMap()
)

data class DeviceRegistrationRequest(
    val device: DeviceInfo,
    val owner_info: OwnerInfo
)

data class DeviceInfo(
    val hardware_id: String,
    val public_key: String,
    val type: String = "smart_lock_v2",
    val capabilities: List<String> = listOf("ble", "wifi", "keypad")
)

data class OwnerInfo(
    val user_uuid: String,
    val location: String,
    val timezone: String = "UTC"
)

data class DeviceRegistrationResponse(
    val status: String,
    val device_uuid: String,
    val server_public_key: String,
    val device_secret: String,
    val config: DeviceConfig,
    val mqtt_config: MqttConfig
)

data class DeviceConfig(
    val pin_length: Int,
    val rotation_hours: Int,
    val grace_period_minutes: Int,
    val max_attempts: Int,
    val lockout_seconds: Int
)

data class MqttConfig(
    val broker: String,
    val port: Int,
    val client_id: String,
    val topics: MqttTopics
)

data class MqttTopics(
    val commands: String,
    val events: String,
    val status: String
)

data class PinResponse(
    val message: String
)

data class PinScheduleRequest(
    val enabled: Boolean,
    val rotation_interval_hours: Int,
    val next_rotation_at: String
)

data class PinScheduleResponse(
    val enabled: Boolean,
    val rotation_interval_hours: Int,
    val next_rotation_at: String?,
    val current_pin: String?
)
