package com.example.ovi.util

import java.util.UUID

object BleConstants {
    val SERVICE_UUID = UUID.fromString("00001810-0000-1000-8000-00805f9b34fb")

    val CHAR_INFO_READ = UUID.fromString("00002A00-0000-1000-8000-00805f9b34fb")
    val CHAR_COMMAND_WRITE = UUID.fromString("00002A01-0000-1000-8000-00805f9b34fb")
    val CHAR_STATUS_NOTIFY = UUID.fromString("00002A02-0000-1000-8000-00805f9b34fb")
    
    val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    const val MTU_SIZE = 247
    const val BLE_CONNECTION_TIMEOUT_MS = 30_000L
    const val BLE_OPERATION_TIMEOUT_MS = 5_000L
    const val MAX_RECONNECT_ATTEMPTS = 3
}