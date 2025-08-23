package com.koldunchik1986.ANL.core.auth

/**
 * Интерфейс обратных вызовов для авторизации
 * Аналог AuthCallback из эталонной реализации
 */
interface AuthCallback {
    /**
     * Успешная авторизация
     */
    fun onSuccess(message: String)
    
    /**
     * Ошибка авторизации
     */
    fun onError(message: String)
    
    /**
     * Требуется ввод капчи
     */
    fun onCaptchaRequired(captchaBytes: ByteArray?, userProfile: com.koldunchik1986.ANL.data.model.UserProfile)
}

/**
 * Результат проверки страницы входа
 */
enum class LoginResult {
    SUCCESS,
    INVALID_CREDENTIALS,
    CAPTCHA_REQUIRED,
    ERROR
}