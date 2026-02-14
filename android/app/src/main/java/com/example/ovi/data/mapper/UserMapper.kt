package com.example.ovi.data.mapper

import com.example.ovi.data.dto.UserDto
import com.example.ovi.data.local.entity.UserEntity
import com.example.ovi.domain.model.User

fun UserDto.toDomain(jwtToken: String? = null): User {
    return User(
        id = this.id,
        email = this.email,
        name = this.name ?: "User",
        jwtToken = jwtToken
    )
}

fun UserDto.toEntity(jwtToken: String? = null): UserEntity {
    return UserEntity(
        id = this.id,
        email = this.email,
        name = this.name ?: "User",
        passwordHash = "",
        jwtToken = jwtToken,
        lastLogin = this.lastLogin ?: System.currentTimeMillis(),
        createdAt = this.createdAt ?: System.currentTimeMillis()
    )
}
fun UserEntity.toDomain(): User {
    return User(
        id = id,
        email = email,
        name = name,
        jwtToken = this.jwtToken
    )
}

fun User.toEntity(): UserEntity {
    return UserEntity(
        id = this.id,
        email = this.email,
        name = this.name,
        passwordHash = "",
        jwtToken = this.jwtToken,
        lastLogin = System.currentTimeMillis(),
        createdAt = System.currentTimeMillis()
    )
}