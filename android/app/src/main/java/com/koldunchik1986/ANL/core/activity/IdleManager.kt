package com.koldunchik1986.ANL.core.activity

import android.app.Activity
import android.app.Application
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.koldunchik1986.ANL.core.session.SessionKeepAliveManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Менеджер отслеживания пользовательской активности
 * Аналог IdleManager из Windows клиента
 * Отслеживает активность для корректного функционирования игровых систем
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
     * Добавление активности (аналог AddActivity из Windows клиента)
     */
    fun addActivity() {
        synchronized(this) {
            _activityCount.value = _activityCount.value + 1
            updateLastActivity()
        }
    }
    
    /**
     * Удаление активности (аналог RemoveActivity из Windows клиента)
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
     * Обновление времени последней активности
     */
    private fun updateLastActivity() {
        lastActivityTime = System.currentTimeMillis()
        _isUserActive.value = true
        
        // Уведомляем session manager об активности
        sessionKeepAliveManager.recordUserActivity()
    }
    
    /**
     * Проверка, находится ли пользователь в состоянии бездействия
     */
    fun isIdle(): Boolean {
        val timeSinceActivity = System.currentTimeMillis() - lastActivityTime
        return timeSinceActivity > IDLE_TIMEOUT && activeActivitiesCount == 0
    }
    
    /**
     * Получение времени последней активности
     */
    fun getLastActivityTime(): Long = lastActivityTime
    
    /**
     * Получение времени бездействия
     */
    fun getIdleTime(): Long {
        return System.currentTimeMillis() - lastActivityTime
    }
    
    /**
     * Пометить пользователя как активного
     */
    fun markUserActive() {
        updateLastActivity()
    }
    
    /**
     * Получение количества активных операций
     */
    fun getActiveOperationsCount(): Int = _activityCount.value
    
    // Реализация ActivityLifecycleCallbacks
    
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        // Обработка создания
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
        // Обновляем активность на паузе
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
        // Обработка сохранения состояния
    }
    
    override fun onActivityDestroyed(activity: Activity) {
        // Обработка уничтожения
    }
    
    /**
     * Проверка состояния бездействия
     */
    private fun checkIdleState() {
        if (activeActivitiesCount == 0) {
            _isUserActive.value = false
        }
    }
    
    /**
     * Получение статистики активности
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
 * Статистика пользовательской активности
 */
data class ActivityStatistics(
    val isActive: Boolean,
    val activeOperations: Int,
    val lastActivityTime: Long,
    val idleTime: Long,
    val isIdle: Boolean
) {
    /**
     * Форматированное время бездействия
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
     * Статус активности (текстовый)
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
