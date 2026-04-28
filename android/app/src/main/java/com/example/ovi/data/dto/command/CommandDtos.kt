package com.example.ovi.data.dto.command

data class UnlockTokenRequest(
    val device_uuid: String
)

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
