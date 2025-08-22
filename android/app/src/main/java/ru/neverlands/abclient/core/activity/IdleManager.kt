package ru.neverlands.abclient.core.activity

import android.app.Activity
import android.app.Application
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.neverlands.abclient.core.session.SessionKeepAliveManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Менеджер отслеживания активности пользователя
 * Эквивалент IdleManager из Windows версии
 * Отслеживает активность для предотвращения автоматического выхода
 */
@Singleton
class IdleManager @Inject constructor(
    private val sessionKeepAliveManager: SessionKeepAliveManager
) : Application.ActivityLifecycleCallbacks {
    
    private val _isUserActive = MutableStateFlow(true)
    val isUserActive: StateFlow<Boolean> = _isUserActive.asStateFlow()
    
    private val _activityCount = MutableStateFlow(0)
    val activityCount: StateFlow<Int> = _activityCount.asStateFlow()
    
    private var lastActivityTime = System.currentTimeMillis()
    private var activeActivitiesCount = 0
    
    companion object {
        private const val IDLE_TIMEOUT = 60_000L // 1 минута бездействия
    }
    
    /**
     * Инициализация менеджера
     */
    fun initialize(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }
    
    /**
     * Добавляет активность (эквивалент AddActivity из Windows версии)
     */
    fun addActivity() {
        synchronized(this) {
            _activityCount.value = _activityCount.value + 1
            updateLastActivity()
        }
    }
    
    /**
     * Убирает активность (эквивалент RemoveActivity из Windows версии)
     */
    fun removeActivity() {
        synchronized(this) {
            val currentCount = _activityCount.value
            if (currentCount > 0) {
                _activityCount.value = currentCount - 1
            }
            updateLastActivity()
        }
    }
    
    /**
     * Обновляет время последней активности
     */
    private fun updateLastActivity() {
        lastActivityTime = System.currentTimeMillis()
        _isUserActive.value = true
        
        // Уведомляем session manager об активности
        sessionKeepAliveManager.recordUserActivity()
    }
    
    /**
     * Проверяет, бездействует ли пользователь
     */
    fun isIdle(): Boolean {
        val timeSinceActivity = System.currentTimeMillis() - lastActivityTime
        return timeSinceActivity > IDLE_TIMEOUT && activeActivitiesCount == 0
    }
    
    /**
     * Получает время последней активности
     */
    fun getLastActivityTime(): Long = lastActivityTime
    
    /**
     * Получает время бездействия в миллисекундах
     */
    fun getIdleTime(): Long {
        return System.currentTimeMillis() - lastActivityTime
    }
    
    /**
     * Принудительно отмечает активность пользователя
     */
    fun markUserActive() {
        updateLastActivity()
    }
    
    /**
     * Получает количество активных операций
     */
    fun getActiveOperationsCount(): Int = _activityCount.value
    
    // Реализация ActivityLifecycleCallbacks
    
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        // Активность создана
    }
    
    override fun onActivityStarted(activity: Activity) {
        synchronized(this) {
            activeActivitiesCount++
            updateLastActivity()
        }
    }
    
    override fun onActivityResumed(activity: Activity) {
        synchronized(this) {
            _isUserActive.value = true
            updateLastActivity()
        }
    }
    
    override fun onActivityPaused(activity: Activity) {
        // Активность поставлена на паузу
        updateLastActivity()
    }
    
    override fun onActivityStopped(activity: Activity) {
        synchronized(this) {
            activeActivitiesCount--
            if (activeActivitiesCount <= 0) {
                activeActivitiesCount = 0
                checkIdleState()
            }
        }
    }
    
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // Сохранение состояния
    }
    
    override fun onActivityDestroyed(activity: Activity) {
        // Активность уничтожена
    }
    
    /**
     * Проверяет состояние бездействия
     */
    private fun checkIdleState() {
        if (activeActivitiesCount == 0) {
            _isUserActive.value = false
        }
    }
    
    /**
     * Получает статистику активности
     */
    fun getActivityStatistics(): ActivityStatistics {
        return ActivityStatistics(
            isActive = _isUserActive.value,
            activeOperations = _activityCount.value,
            lastActivityTime = lastActivityTime,
            idleTime = getIdleTime(),
            isIdle = isIdle()
        )
    }
}

/**
 * Статистика активности пользователя
 */
data class ActivityStatistics(
    val isActive: Boolean,
    val activeOperations: Int,
    val lastActivityTime: Long,
    val idleTime: Long,
    val isIdle: Boolean
) {
    /**
     * Форматирует время бездействия
     */
    fun getFormattedIdleTime(): String {
        val seconds = idleTime / 1000
        val minutes = seconds / 60
        
        return when {
            minutes > 0 -> "${minutes}м ${seconds % 60}с"
            else -> "${seconds}с"
        }
    }
    
    /**
     * Получает процент активности (условно)
     */
    fun getActivityPercentage(): Int {
        return when {
            isActive && activeOperations > 0 -> 100
            isActive -> 75
            idleTime < 30000 -> 50
            else -> 0
        }
    }
}