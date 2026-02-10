package com.example.ovi.data.mapper

import com.example.ovi.data.local.entity.UserEntity
import com.example.ovi.domain.model.User

fun UserEntity.toDomain(): User {
    return User(
        id = id,
        email = email,
        name = name,
        jwtToken = jwtToken
    )
}

fun User.toEntity(): UserEntity {
    return UserEntity(
        id = id,
        email = email,
        name = name,
        passwordHash = null,
        jwtToken = jwtToken,
        lastLogin = System.currentTimeMillis(),
        createdAt = System.currentTimeMillis()
    )
}