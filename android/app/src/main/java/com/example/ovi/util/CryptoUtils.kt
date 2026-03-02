package com.example.ovi.util

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

object CryptoUtils {
    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_LENGTH_BYTES = 12   
    private const val GCM_TAG_LENGTH_BITS = 128
    
    fun generateSessionKey(): ByteArray {
        val kg = KeyGenerator.getInstance("AES")
        kg.init(256, SecureRandom())
        return kg.generateKey().encoded
    }
    
    fun aesGcmEncryptToBase64(plaintext: ByteArray, key: ByteArray): String {
        val iv = ByteArray(IV_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext)
        return Base64.encodeToString(iv + ciphertext, Base64.NO_WRAP)
    }
    
    fun aesGcmDecryptFromBase64(base64Data: String, key: ByteArray): ByteArray? {
        return try {
            val data = Base64.decode(base64Data, Base64.NO_WRAP)
            if (data.size <= IV_LENGTH_BYTES) return null
            val iv = data.copyOfRange(0, IV_LENGTH_BYTES)
            val ciphertext = data.copyOfRange(IV_LENGTH_BYTES, data.size)
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            null
        }
    }
    
    fun getJwtExpiry(jwt: String): Long? {
        return try {
            val parts = jwt.split(".")
            if (parts.size != 3) return null
            val payloadJson = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING))
            Regex(""""exp"\s*:\s*(\d+)""").find(payloadJson)?.groupValues?.get(1)?.toLongOrNull()
        } catch (e: Exception) {
            null
        }
    }
}