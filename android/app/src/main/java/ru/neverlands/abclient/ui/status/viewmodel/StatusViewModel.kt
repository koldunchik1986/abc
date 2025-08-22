package ru.neverlands.abclient.ui.status.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.neverlands.abclient.core.session.SessionKeepAliveManager
import ru.neverlands.abclient.core.session.SessionState
import ru.neverlands.abclient.core.activity.IdleManager
import ru.neverlands.abclient.service.OnlineStatus
import javax.inject.Inject

/**
 * ViewModel для управления статусом онлайн подключения
 */
@HiltViewModel
class StatusViewModel @Inject constructor(
    private val sessionKeepAliveManager: SessionKeepAliveManager,
    private val idleManager: IdleManager
) : ViewModel() {
    
    private val _onlineStatus = MutableStateFlow<OnlineStatus?>(null)
    val onlineStatus: StateFlow<OnlineStatus?> = _onlineStatus.asStateFlow()
    
    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()
    
    val sessionState: StateFlow<SessionState> = sessionKeepAliveManager.sessionState
    val isUserActive: StateFlow<Boolean> = idleManager.isUserActive
    val activityCount: StateFlow<Int> = idleManager.activityCount
    
    /**
     * Объединенный статус для UI
     */
    val combinedStatus: StateFlow<CombinedStatus> = combine(
        sessionState,
        isUserActive,
        onlineStatus,
        isServiceRunning
    ) { session, active, online, serviceRunning ->
        CombinedStatus(
            sessionState = session,
            isUserActive = active,
            onlineStatus = online,
            isServiceRunning = serviceRunning
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CombinedStatus()
    )
    
    /**
     * Обновляет статус онлайн подключения
     */
    fun updateOnlineStatus(status: OnlineStatus) {
        _onlineStatus.value = status
    }
    
    /**
     * Устанавливает состояние службы
     */
    fun setServiceRunning(isRunning: Boolean) {
        _isServiceRunning.value = isRunning
    }
    
    /**
     * Запускает сессию keep-alive
     */
    fun startKeepAlive() {
        sessionKeepAliveManager.startKeepAlive()
    }
    
    /**
     * Останавливает сессию keep-alive
     */
    fun stopKeepAlive() {
        sessionKeepAliveManager.stopKeepAlive()
    }
    
    /**
     * Записывает активность пользователя
     */
    fun recordUserActivity() {
        idleManager.addActivity()
        sessionKeepAliveManager.recordUserActivity()
    }
    
    /**
     * Получает статистику сессии
     */
    fun getSessionStatistics() = sessionKeepAliveManager.getSessionStatistics()
    
    /**
     * Форматирует статус для отображения
     */
    fun getStatusText(): String {
        val status = _onlineStatus.value
        return when {
            status == null -> "Статус неизвестен"
            status.hasError -> "Ошибка: ${status.message}"
            status.isOnline && status.isInBattle -> "В игре (бой)"
            status.isOnline -> "В игре"
            else -> "Не в сети"
        }
    }
    
    /**
     * Получает цвет индикатора статуса
     */
    fun getStatusColor(): androidx.compose.ui.graphics.Color {
        val status = _onlineStatus.value
        return when {
            status == null -> androidx.compose.ui.graphics.Color.Gray
            status.hasError -> androidx.compose.ui.graphics.Color.Red
            status.isOnline && status.isInBattle -> androidx.compose.ui.graphics.Color.Yellow
            status.isOnline -> androidx.compose.ui.graphics.Color.Green
            else -> androidx.compose.ui.graphics.Color.Gray
        }
    }
}

/**
 * Объединенный статус для UI
 */
data class CombinedStatus(
    val sessionState: SessionState = SessionState.INACTIVE,
    val isUserActive: Boolean = true,
    val onlineStatus: OnlineStatus? = null,
    val isServiceRunning: Boolean = false
) {
    /**
     * Проверяет, активна ли система поддержания онлайн статуса
     */
    fun isOnlineSystemActive(): Boolean {
        return sessionState == SessionState.ACTIVE || isServiceRunning
    }
    
    /**
     * Получает общий статус здоровья системы
     */
    fun getHealthStatus(): HealthStatus {
        return when {
            onlineStatus?.hasError == true -> HealthStatus.ERROR
            !isOnlineSystemActive() -> HealthStatus.INACTIVE
            onlineStatus?.isOnline == true -> HealthStatus.HEALTHY
            else -> HealthStatus.WARNING
        }
    }
}

/**
 * Статус здоровья системы
 */
enum class HealthStatus {
    HEALTHY,    // Все работает нормально
    WARNING,    // Есть предупреждения
    ERROR,      // Есть ошибки
    INACTIVE    // Система неактивна
}