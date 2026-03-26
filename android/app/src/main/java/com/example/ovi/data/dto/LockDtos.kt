package com.example.ovi.data.dto

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

data class UnlockTokenRequest(val device_uuid: String)

data class UnlockTokenResponse(
    val token: TokenData,
    val signature: ServerSignature
)

data class TokenData(
    val version: Int,
    val nonce: String,
    val expires_at: Long,
    val issued_at: Long,
    val device_uuid: String,
    val user_uuid: String,
    val action: String = "unlock",
    val session_id: String
)

data class ServerSignature(
    val value: String,
    val algorithm: String,
    val curve: String,
    val public_key_id: String
)

data class BleDeviceInfo(
    val cmd: String,
    val req_id: String,
    val data: BleData,
    val signature: String
)

data class BleData(
    val device_id: String,
    val public_key: String,
    val fw_version: String,
    val hw_version: String,
    val battery_level: Int,
    val rssi: Int,
    val time_sync_required: Boolean
)