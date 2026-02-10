package com.example.ovi.data.local

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_NAME = "name"
        private const val KEY_JWT_TOKEN = "jwt_token"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    fun saveUserSession(
        userId: String,
        email: String,
        name: String,
        jwtToken: String?
    ) {
        sharedPreferences.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_EMAIL, email)
            .putString(KEY_NAME, name)
            .putString(KEY_JWT_TOKEN, jwtToken)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false) && getJwtToken() != null
    }

    fun getUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }

    fun getEmail(): String? {
        return sharedPreferences.getString(KEY_EMAIL, null)
    }

    fun getName(): String? {
        return sharedPreferences.getString(KEY_NAME, null)
    }
    fun getJwtToken(): String? {
        return sharedPreferences.getString(KEY_JWT_TOKEN, null)
    }

    fun clearSession() {
        sharedPreferences.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_EMAIL)
            .remove(KEY_NAME)
            .remove(KEY_JWT_TOKEN)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .apply()
    }

    fun getUserData(): Map<String, String?> {
        return mapOf(
            "userId" to getUserId(),
            "email" to getEmail(),
            "name" to getName(),
            "jwtToken" to getJwtToken()
        )
    }

}