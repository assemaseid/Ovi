package com.example.ovi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.ovi.data.local.entity.UserEntity

//многие методы могут быть и не нужны
//можно оставить мин: добавление; изменение; удаление; получение
@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("UPDATE users SET jwtToken = :token WHERE id = :userId")
    suspend fun updateUserToken(userId: String, token: String?)

    @Query("UPDATE users SET lastLogin = :timestamp WHERE id = :userId")
    suspend fun updateLastLogin(userId: String, timestamp: Long)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: String)

}
//"/login
//email, pass
//return: token
//"/register
//

//list<key>
//key by id -> battery info
//
//user info endpoint
//
//bluetooth animation - lottie
//