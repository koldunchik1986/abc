package com.koldunchik1986.ANL.core.auth

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import com.koldunchik1986.ANL.data.model.UserProfile
import com.koldunchik1986.ANL.core.network.GameHttpClient
import com.koldunchik1986.ANL.data.repository.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Менеджер авторизации - аналог AuthManager из эталонной реализации
 * Отвечает за полный процесс авторизации с поддержкой капчи
 */
@Singleton
class AuthManager @Inject constructor(
    private val context: Context,
    private val gameHttpClient: GameHttpClient,
    private val profileRepository: ProfileRepository,
    private val authHelper: AuthenticationHelper
) {
    
    companion object {
        private const val TAG = "AuthManager"
        private const val BASE_URL = "http://www.neverlands.ru/"
        private const val LOGIN_URL = "${BASE_URL}game.php"
        private const val MAIN_PHP_URL = "${BASE_URL}main.php"
        
        // User-Agent как в эталоне (критически важно!)
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }
    
    /**
     * Сохранение профилей
     */
    suspend fun saveProfiles(profiles: List<UserProfile>) {
        withContext(Dispatchers.IO) {
            try {
                profiles.forEach { profile ->
                    profileRepository.saveProfile(profile)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving profiles", e)
                throw e
            }
        }
    }
    
    /**
     * Загрузка профилей
     */
    suspend fun loadProfiles(): List<UserProfile> {
        return withContext(Dispatchers.IO) {
            try {
                profileRepository.getAllProfiles()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading profiles", e)
                emptyList()
            }
        }
    }
    
    /**
     * Авторизация пользователя (аналог эталонного AuthManager.login)
     */
    suspend fun login(userProfile: UserProfile, callback: AuthCallback) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting login process for user: ${userProfile.userNick}")
                
                // Первый запрос - получение главной страницы
                val mainPageResponse = gameHttpClient.get(BASE_URL)
                if (!mainPageResponse.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        callback.onError("Ошибка загрузки главной страницы: ${mainPageResponse.code}")
                    }
                    return@withContext
                }
                mainPageResponse.close()
                
                // Авторизация
                val loginFormData = mapOf(
                    "player_nick" to userProfile.userNick,
                    "player_password" to userProfile.userPassword
                )
                
                val loginRequest = Request.Builder()
                    .url(LOGIN_URL)
                    .post(gameHttpClient.createFormBody(loginFormData))
                    .header("Referer", BASE_URL)
                    .build()
                
                val loginResponse = gameHttpClient.client.newCall(loginRequest).execute()
                val loginResponseBody = loginResponse.body?.string() ?: ""
                
                if (loginResponse.isSuccessful) {
                    // Проверка результата авторизации
                    val loginResult = authHelper.checkLoginResponse(loginResponseBody)
                    
                    when (loginResult) {
                        LoginResult.SUCCESS -> {
                            // Успешная авторизация
                            val updatedProfile = userProfile.copy(
                                configLastSaved = System.currentTimeMillis(),
                                lastLogon = System.currentTimeMillis()
                            )
                            profileRepository.saveProfile(updatedProfile)
                            
                            withContext(Dispatchers.Main) {
                                callback.onSuccess("Авторизация успешна")
                            }
                        }
                        LoginResult.INVALID_CREDENTIALS -> {
                            withContext(Dispatchers.Main) {
                                callback.onError("Неверный логин или пароль")
                            }
                        }
                        LoginResult.CAPTCHA_REQUIRED -> {
                            // Требуется ввод капчи
                            handleCaptcha(loginResponseBody, userProfile, callback)
                        }
                        LoginResult.ERROR -> {
                            withContext(Dispatchers.Main) {
                                callback.onError("Ошибка авторизации")
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        callback.onError("Ошибка сервера: ${loginResponse.code}")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Login error", e)
                withContext(Dispatchers.Main) {
                    callback.onError("Ошибка подключения: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Обработка капчи (аналог эталонного AuthManager.handleCaptcha)
     */
    private suspend fun handleCaptcha(html: String, userProfile: UserProfile, callback: AuthCallback) {
        try {
            val captchaUrl = authHelper.extractCaptchaUrl(html)
            
            if (captchaUrl != null) {
                Log.d(TAG, "Captcha required, loading image from: $captchaUrl")
                
                // Загружаем изображение капчи
                val captchaBytes = gameHttpClient.downloadData(captchaUrl)
                
                withContext(Dispatchers.Main) {
                    callback.onCaptchaRequired(captchaBytes, userProfile)
                }
            } else {
                withContext(Dispatchers.Main) {
                    callback.onError("Не удалось найти изображение капчи")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling captcha", e)
            withContext(Dispatchers.Main) {
                callback.onError("Ошибка обработки капчи: ${e.message}")
            }
        }
    }
    
    /**
     * Отправка капчи (аналог эталонного AuthManager.submitCaptcha)
     */
    suspend fun submitCaptcha(userProfile: UserProfile, captchaCode: String, callback: AuthCallback) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Submitting captcha code for user: ${userProfile.userNick}")
                
                val captchaFormData = mapOf(
                    "player_nick" to userProfile.userNick,
                    "player_password" to userProfile.userPassword,
                    "captcha" to captchaCode
                )
                
                val captchaRequest = Request.Builder()
                    .url(LOGIN_URL)
                    .post(gameHttpClient.createFormBody(captchaFormData))
                    .header("Referer", BASE_URL)
                    .build()
                
                val captchaResponse = gameHttpClient.client.newCall(captchaRequest).execute()
                val responseBody = captchaResponse.body?.string() ?: ""
                
                if (captchaResponse.isSuccessful) {
                    val loginResult = authHelper.checkLoginResponse(responseBody)
                    
                    when (loginResult) {
                        LoginResult.SUCCESS -> {
                            // Успешная авторизация после капчи
                            val updatedProfile = userProfile.copy(
                                configLastSaved = System.currentTimeMillis(),
                                lastLogon = System.currentTimeMillis()
                            )
                            profileRepository.saveProfile(updatedProfile)
                            
                            withContext(Dispatchers.Main) {
                                callback.onSuccess("Авторизация успешна")
                            }
                        }
                        LoginResult.CAPTCHA_REQUIRED -> {
                            // Неверный код капчи - требуется повторный ввод
                            handleCaptcha(responseBody, userProfile, callback)
                        }
                        else -> {
                            withContext(Dispatchers.Main) {
                                callback.onError("Неверный код капчи")
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        callback.onError("Ошибка отправки капчи: ${captchaResponse.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Captcha submit error", e)
                withContext(Dispatchers.Main) {
                    callback.onError("Ошибка отправки капчи: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Получение cookies для веб-интерфейса (аналог эталона)
     */
    fun getCookies(): String {
        // В нашей реализации cookies управляются автоматически через GameCookieManager
        // Этот метод можно использовать для получения cookie строки при необходимости
        return ""
    }
    
    /**
     * Проверка статуса авторизации через main.php
     */
    suspend fun checkAuthenticationStatus(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = gameHttpClient.get(MAIN_PHP_URL)
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    // Проверяем, что пользователь авторизован
                    !responseBody.contains("Вход", ignoreCase = true) && 
                    !responseBody.contains("Авторизация", ignoreCase = true)
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking auth status", e)
                false
            }
        }
    }
}