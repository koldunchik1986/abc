package com.koldunchik1986.ANL.core.security

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.koldunchik1986.ANL.data.model.UserProfile
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import java.security.MessageDigest
import java.util.*

/**
 * Менеджер шифрования профилей пользователей
 * Отвечает за шифрование и дешифрование профилей пользователей
 */
@Singleton
class ProfileEncryptionManager @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TRANSFORMATION = "AES/ECB/PKCS5Padding"
        private const val KEY_ALGORITHM = "AES"
        private const val HASH_ALGORITHM = "SHA-256"
    }
    
    /**
     * Запрашиваем пароль для шифрования и дешифрования
     * Возвращает null если пользователь отменил ввод пароля
     */
    suspend fun requestEncryptionPassword(): String? {
        return withContext(Dispatchers.Main) {
            // TODO: Реализовать запрос пароля от пользователя
            // Возвращает null если пользователь отменил шифрование
            null
        }
    }
    
    /**
     * Шифруем профиль пользователя
     */
    suspend fun encryptProfile(profile: UserProfile, password: String): UserProfile {
        return withContext(Dispatchers.Default) {
            try {
                val key = generateKeyFromPassword(password)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, key)
                
                val encryptedPassword = if (profile.userPassword.isNotEmpty()) {
                    Base64.getEncoder().encodeToString(
                        cipher.doFinal(profile.userPassword.toByteArray())
                    )
                } else {
                    ""
                }
                
                // Шифруем Flash пароль только для старых профилей
                val encryptedFlashPassword = if (profile.userPasswordFlash.isNotEmpty()) {
                    Base64.getEncoder().encodeToString(
                        cipher.doFinal(profile.userPasswordFlash.toByteArray())
                    )
                } else {
                    ""
                }
                
                profile.copy(
                    userPassword = "",
                    userPasswordFlash = encryptedFlashPassword, // Храним зашифрованный Flash пароль
                    configPassword = encryptedPassword,
                    isEncrypted = true,
                    configHash = generatePasswordHash(password)
                )
            } catch (e: Exception) {
                profile
            }
        }
    }
    
    /**
     * Дешифруем профиль пользователя
     */
    suspend fun decryptProfile(profile: UserProfile, password: String): UserProfile? {
        return withContext(Dispatchers.Default) {
            try {
                // Проверяем правильность пароля
                if (!verifyPassword(password, profile.configHash)) {
                    return@withContext null
                }
                
                val key = generateKeyFromPassword(password)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, key)
                
                val decryptedPassword = if (profile.configPassword.isNotEmpty()) {
                    String(cipher.doFinal(Base64.getDecoder().decode(profile.configPassword)))
                } else {
                    ""
                }
                
                // Дешифруем Flash пароль если он зашифрован
                val decryptedFlashPassword = if (profile.userPasswordFlash.isNotEmpty()) {
                    try {
                        // Проверяем, является ли это Base64 (зашифрованный)
                        Base64.getDecoder().decode(profile.userPasswordFlash)
                        // Если не возникло исключения, то это зашифрованный пароль
                        String(cipher.doFinal(Base64.getDecoder().decode(profile.userPasswordFlash)))
                    } catch (e: Exception) {
                        // Если не удалось декодировать Base64, значит это уже открытый текст
                        profile.userPasswordFlash
                    }
                } else {
                    ""
                }
                
                profile.copy(
                    userPassword = decryptedPassword,
                    userPasswordFlash = decryptedFlashPassword,
                    configPassword = "",
                    isEncrypted = false
                )
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Генерируем ключ шифрования из пароля
     */
    private fun generateKeyFromPassword(password: String): SecretKey {
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
        val keyBytes = digest.digest(password.toByteArray())
        // Обрезаем до 16 байт для AES-128
        val truncatedKeyBytes = keyBytes.copyOf(16)
        return SecretKeySpec(truncatedKeyBytes, KEY_ALGORITHM)
    }
    
    /**
     * Генерируем хэш пароля для проверки
     */
    private fun generatePasswordHash(password: String): String {
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
        val hashBytes = digest.digest(password.toByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }
    
    /**
     * Проверяем правильность пароля
     */
    private fun verifyPassword(password: String, storedHash: String): Boolean {
        return generatePasswordHash(password) == storedHash
    }
    
    /**
     * Проверяем, зашифрован ли профиль
     */
    fun isProfileEncrypted(profile: UserProfile): Boolean {
        return profile.isEncrypted && profile.configHash.isNotEmpty()
    }
    
    /**
     * Генерируем случайный пароль для шифрования
     */
    fun generateRandomPassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..16).map { chars.random() }.joinToString("")
    }
}