package com.koldunchik1986.ANL.ui.status.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koldunchik1986.ANL.core.session.SessionManager
import com.koldunchik1986.ANL.core.session.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StatusViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {
    
    val sessionState: StateFlow<SessionState> = sessionManager.sessionState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SessionState.INACTIVE
        )
    
    val isUserActive: StateFlow<Boolean> = sessionManager.isUserActive
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
}