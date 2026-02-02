package com.example.ovi.data.repository

import com.example.ovi.data.local.database.AppDatabase
import com.example.ovi.data.local.entity.UserEntity
import com.example.ovi.data.mapper.toDomain
import com.example.ovi.data.mapper.toEntity
import com.example.ovi.domain.model.User
import com.example.ovi.domain.repository.AuthRepository
import java.util.UUID

class AuthRepositoryImpl (
    private val database: AppDatabase
): AuthRepository {
    private val userDao = database.userDao()

    override suspend fun login(
        email: String,
        password: String
    ): Result<User> {
        return try {
            val userEntity = userDao.getUserByEmail(email)

            if (userEntity == null) {
                Result.failure(Exception("User not found"))
            } else if (userEntity.passwordHash != password.hashCode().toString()) {
                Result.failure(Exception("Invalid password"))
            } else {
                userDao.updateLastLogin(userEntity.id, System.currentTimeMillis())
                val token = "mock-jwt-${System.currentTimeMillis()}"
                userDao.updateUserToken(userEntity.id, token)

                val user = userEntity.toDomain().copy(jwtToken = token)
                Result.success(user)
            }
        }
        catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        name: String
    ): Result<User> {
        return try {
            val existingUser = userDao.getUserByEmail(email)
            if (existingUser != null) {
                return Result.failure(Exception("User already exists"))
            }

            val userEntity = UserEntity(
                id = 0,
                email = email,
                name = name,
                passwordHash = password.hashCode().toString(),
                jwtToken = "mock-jwt-${System.currentTimeMillis()}"
            )
            userDao.insertUser(userEntity)

            val user = userEntity.toDomain()
            return Result.success(user)
        }catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun logout() {
    }

    override suspend fun getCurrentUser(): User? {
        return null
    }

    override suspend fun saveUser(user: User) {
        userDao.insertUser(user.toEntity())
    }

}