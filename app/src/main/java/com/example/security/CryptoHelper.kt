package com.example.security

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoHelper {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12

    private fun deriveKey(conversationId: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val seed = "RSET_SPARKS_CAMPUS_SECURE_SALT_$conversationId"
        val hash = digest.digest(seed.toByteArray(StandardCharsets.UTF_8))
        return SecretKeySpec(hash, ALGORITHM)
    }

    data class EncryptionResult(
        val cipherTextBase64: String,
        val ivBase64: String
    )

    fun encrypt(plainText: String, conversationId: String): EncryptionResult {
        return try {
            val key = deriveKey(conversationId)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
            EncryptionResult(
                cipherTextBase64 = Base64.encodeToString(cipherText, Base64.NO_WRAP),
                ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            )
        } catch (e: Exception) {
            EncryptionResult(
                cipherTextBase64 = Base64.encodeToString(plainText.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP),
                ivBase64 = ""
            )
        }
    }

    fun decrypt(cipherTextBase64: String, ivBase64: String, conversationId: String): String {
        return try {
            if (ivBase64.isBlank()) {
                val decoded = Base64.decode(cipherTextBase64, Base64.NO_WRAP)
                return String(decoded, StandardCharsets.UTF_8)
            }
            val key = deriveKey(conversationId)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val cipherText = Base64.decode(cipherTextBase64, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            val plainBytes = cipher.doFinal(cipherText)
            String(plainBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            "[Encrypted Message - Verification Pending]"
        }
    }

    fun buildConversationId(userAId: String, userBId: String): String {
        return if (userAId < userBId) "${userAId}_${userBId}" else "${userBId}_${userAId}"
    }
}
