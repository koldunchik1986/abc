package ru.neverlands.abclient.core.session

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ru.neverlands.abclient.core.network.GameHttpClient
import ru.neverlands.abclient.core.network.cookie.GameCookieManager
import ru.neverlands.abclient.data.preferences.UserPreferencesManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Менеджер поддержания активной игровой сессии
 * Предотвращает автоматический выход из игры из-за неактивности
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
        private const val SESSION_TIMEOUT = 300_000L // 5 минут бездействия
        private const val MAX_RETRY_ATTEMPTS = 3
    }
    
    /**
     * Запускает механизм поддержания сессии
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
     * Останавливает механизм поддержания сессии
     */
    fun stopKeepAlive() {
        keepAliveJob?.cancel()
        _sessionState.value = SessionState.INACTIVE
    }
    
    /**
     * Отмечает активность пользователя
     */
    fun recordUserActivity() {
        _lastActivity.value = System.currentTimeMillis()
    }
    
    /**
     * Проверяет, истекло ли время сессии
     */
    fun isSessionExpired(): Boolean {
        val timeSinceActivity = System.currentTimeMillis() - _lastActivity.value
        return timeSinceActivity > SESSION_TIMEOUT
    }
    
    /**
     * Выполняет поддержание сессии живой
     */
    private suspend fun performKeepAlive(): Boolean {
        // Проверяем аутентификацию
        if (!cookieManager.isAuthenticated()) {
            return false
        }
        
        return try {
            // Делаем легкий запрос для поддержания сессии
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
                    // Сессия истекла, пытаемся восстановить
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
     * Пытается восстановить сессию
     */
    private suspend fun attemptSessionRecovery(): Boolean {
        return try {
            val currentProfile = preferencesManager.getCurrentProfile()
            if (currentProfile == null || !currentProfile.isLoginDataComplete()) {
                return false
            }
            
            // Очищаем старые cookies
            cookieManager.clearGameCookies()
            
            // Пытаемся войти заново через API
            val userInfo = httpClient.getUserInfo(currentProfile.userNick)
            
            userInfo != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Выполняет периодическую очистку кэша
     */
    suspend fun performMaintenanceTasks() {
        try {
            // Очищаем старые данные из кэша
            cleanupOldCacheData()
            
            // Проверяем состояние соединения
            validateConnectionState()
            
        } catch (e: Exception) {
            // Игнорируем ошибки maintenance задач
        }
    }
    
    /**
     * Очищает старые данные кэша
     */
    private suspend fun cleanupOldCacheData() {
        // TODO: Реализовать очистку кэша HTTP клиента
        // Очищаем данные старше 24 часов
    }
    
    /**
     * Проверяет состояние соединения
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
     * Получает статистику сессии
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
     * Получает время до истечения сессии в миллисекундах
     */
    fun getTimeUntilExpiration(): Long {
        val timeLeft = (lastActivityTime + 300_000L) - System.currentTimeMillis()
        return maxOf(0L, timeLeft)
    }
    
    /**
     * Форматирует длительность сессии
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