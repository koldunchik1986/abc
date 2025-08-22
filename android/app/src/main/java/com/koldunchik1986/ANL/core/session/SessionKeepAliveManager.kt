package com.koldunchik1986.ANL.core.session

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.koldunchik1986.ANL.core.network.GameHttpClient
import com.koldunchik1986.ANL.core.network.cookie.GameCookieManager
import com.koldunchik1986.ANL.data.preferences.UserPreferencesManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Менеджер поддержания сессии активной
 * Отвечает за периодическую проверку активности сессии
 */
@Singleton
class SessionKeepAliveManager @Inject constructor(
    private val httpClient: GameHttpClient,
    private val cookieManager: GameCookieManager,
    private val preferencesManager: UserPreferencesManager
) {
    
    private var keepAliveJob: Job? = null
    private val _sessionState = MutableStateFlow(SessionState.INACTIVE)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()
    
    private val _lastActivity = MutableStateFlow(System.currentTimeMillis())
    val lastActivity: StateFlow<Long> = _lastActivity.asStateFlow()
    
    companion object {
        private const val KEEP_ALIVE_INTERVAL = 60_000L // 1 минута
        private const val SESSION_TIMEOUT = 300_000L // 5 минут неактивности
        private const val MAX_RETRY_ATTEMPTS = 3
    }
    
    /**
     * Запуск поддержания активности сессии
     */
    fun startKeepAlive() {
        if (keepAliveJob?.isActive == true) return
        
        _sessionState.value = SessionState.ACTIVE
        
        keepAliveJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            var retryCount = 0
            
            while (isActive && _sessionState.value == SessionState.ACTIVE) {
                try {
                    val success = performKeepAlive()
                    
                    if (success) {
                        retryCount = 0
                        _sessionState.value = SessionState.ACTIVE
                    } else {
                        retryCount++
                        if (retryCount >= MAX_RETRY_ATTEMPTS) {
                            _sessionState.value = SessionState.ERROR
                            break
                        }
                    }
                    
                } catch (e: Exception) {
                    retryCount++
                    if (retryCount >= MAX_RETRY_ATTEMPTS) {
                        _sessionState.value = SessionState.ERROR
                        break
                    }
                }
                
                delay(KEEP_ALIVE_INTERVAL)
            }
        }
    }
    
    /**
     * Остановка поддержания активности сессии
     */
    fun stopKeepAlive() {
        keepAliveJob?.cancel()
        _sessionState.value = SessionState.INACTIVE
    }
    
    /**
     * Запись активности пользователя
     */
    fun recordUserActivity() {
        _lastActivity.value = System.currentTimeMillis()
    }
    
    /**
     * Проверка, не истекла ли сессия
     */
    fun isSessionExpired(): Boolean {
        val timeSinceActivity = System.currentTimeMillis() - _lastActivity.value
        return timeSinceActivity > SESSION_TIMEOUT
    }
    
    /**
     * Выполнение проверки активности сессии
     */
    private suspend fun performKeepAlive(): Boolean {
        // Проверяем аутентификацию
        if (!cookieManager.isAuthenticated()) {
            return false
        }
        
        return try {
            // Отправляем запрос на главную страницу для поддержания сессии
            val response = httpClient.get("http://www.neverlands.ru/main.php", 
                mapOf("Cache-Control" to "no-cache"))
            
            if (response.isSuccessful) {
                val content = response.body?.string() ?: ""
                
                // Проверяем, что мы все еще в игре
                val isStillLoggedIn = !content.contains("Вход", ignoreCase = true) &&
                                    !content.contains("Авторизация", ignoreCase = true)
                
                if (isStillLoggedIn) {
                    recordUserActivity()
                    true
                } else {
                    // Сессия истекла, пробуем восстановить
                    attemptSessionRecovery()
                }
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Попытка восстановления сессии
     */
    private suspend fun attemptSessionRecovery(): Boolean {
        return try {
            val currentProfile = preferencesManager.getCurrentProfile()
            if (currentProfile == null || !currentProfile.isLoginDataComplete()) {
                return false
            }
            
            // Очищаем старые cookies
            cookieManager.clearGameCookies()
            
            // Повторная авторизация через API
            val userInfo = httpClient.getUserInfo(currentProfile.userNick)
            
            userInfo != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Выполнение задач обслуживания
     */
    suspend fun performMaintenanceTasks() {
        try {
            // Очистка старых кэш данных
            cleanupOldCacheData()
            
            // Проверка состояния соединения
            validateConnectionState()
            
        } catch (e: Exception) {
            // Игнорируем ошибки maintenance задач
        }
    }
    
    /**
     * Очистка старых кэш данных
     */
    private suspend fun cleanupOldCacheData() {
        // TODO: Реализовать очистку старых HTTP кэш данных
        // Удалять данные старше 24 часов
    }
    
    /**
     * Проверка состояния соединения
     */
    private suspend fun validateConnectionState(): Boolean {
        return try {
            val response = httpClient.get("http://www.neverlands.ru/")
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Получение статистики сессии
     */
    fun getSessionStatistics(): SessionStatistics {
        val currentTime = System.currentTimeMillis()
        val sessionDuration = currentTime - _lastActivity.value
        
        return SessionStatistics(
            isActive = _sessionState.value == SessionState.ACTIVE,
            sessionDuration = sessionDuration,
            lastActivityTime = _lastActivity.value,
            isExpired = isSessionExpired()
        )
    }
}

/**
 * Состояние сессии
 */
enum class SessionState {
    INACTIVE,   // Сессия не активна
    ACTIVE,     // Сессия активна и поддерживается
    ERROR       // Ошибка поддержания сессии
}

/**
 * Статистика сессии
 */
data class SessionStatistics(
    val isActive: Boolean,
    val sessionDuration: Long,
    val lastActivityTime: Long,
    val isExpired: Boolean
) {
    /**
     * Получение времени до истечения сессии
     */
    fun getTimeUntilExpiration(): Long {
        val timeLeft = (lastActivityTime + 300_000L) - System.currentTimeMillis()
        return maxOf(0L, timeLeft)
    }
    
    /**
     * Форматирование продолжительности сессии
     */
    fun getFormattedDuration(): String {
        val seconds = sessionDuration / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> "${hours}ч ${minutes % 60}м"
            minutes > 0 -> "${minutes}м ${seconds % 60}с"
            else -> "${seconds}с"
        }
    }
}