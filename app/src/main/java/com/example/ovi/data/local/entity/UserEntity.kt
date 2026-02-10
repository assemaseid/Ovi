package com.example.ovi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val name: String,
    val passwordHash: String?,
    val jwtToken: String? = null,
    val lastLogin: Long,
    val createdAt: Long,
)