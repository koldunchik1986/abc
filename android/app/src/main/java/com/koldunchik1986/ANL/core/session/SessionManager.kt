package com.koldunchik1986.ANL.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Состояние сессии
 */
enum class SessionState {
    ACTIVE,
    INACTIVE,
    ERROR
}

/**
 * Менеджер сессии для управления состоянием подключения к игре
 */
@Singleton
class SessionManager @Inject constructor() {
    
    private val _sessionState = MutableStateFlow(SessionState.INACTIVE)
    val sessionState: StateFlow<SessionState> = _sessionState
    
    private val _isUserActive = MutableStateFlow(false)
    val isUserActive: StateFlow<Boolean> = _isUserActive
    
    fun updateSessionState(state: SessionState) {
        _sessionState.value = state
    }
    
    fun updateUserActivity(isActive: Boolean) {
        _isUserActive.value = isActive
    }
    
    fun startSession() {
        updateSessionState(SessionState.ACTIVE)
    }
    
    fun endSession() {
        updateSessionState(SessionState.INACTIVE)
    }
    
    fun sessionError() {
        updateSessionState(SessionState.ERROR)
    }
}