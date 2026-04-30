package com.example.ovi.data.dto.ble

data class BleDeviceInfo(
    val cmd: String,
    val req_id: String,
    val data: BleData,
    val signature: String?
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
