package com.example.ovi.data.mapper

import com.example.ovi.data.dto.auth.UserDto
import com.example.ovi.domain.model.User

fun UserDto.toDomain(jwtToken: String? = null): User {
    return User(
        id = this.id,
        email = this.email,
        name = this.name,
        jwtToken = jwtToken
    )
}
