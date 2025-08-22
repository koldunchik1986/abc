package com.koldunchik1986.ANL.ui.status.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.koldunchik1986.ANL.core.session.SessionState
import com.koldunchik1986.ANL.ui.status.viewmodel.StatusViewModel

/**
 * Индикатор онлайн статуса персонажа в игре
 */
@Composable
fun OnlineStatusIndicator(
    modifier: Modifier = Modifier,
    showDetails: Boolean = false,
    viewModel: StatusViewModel = hiltViewModel()
) {
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val isUserActive by viewModel.isUserActive.collectAsStateWithLifecycle()
    // TODO: Add sessionStatistics to ViewModel
    val sessionStats = null // Placeholder until ViewModel is updated
    
    if (showDetails) {
        OnlineStatusCard(
            sessionState = sessionState,
            isUserActive = isUserActive,
            sessionStats = sessionStats,
            onRestartSession = { /* TODO: Add restart functionality */ },
            modifier = modifier
        )
    } else {
        OnlineStatusBadge(
            sessionState = sessionState,
            isUserActive = isUserActive,
            modifier = modifier
        )
    }
}

@Composable
private fun OnlineStatusBadge(
    sessionState: SessionState,
    isUserActive: Boolean,
    modifier: Modifier = Modifier
) {
    val statusColor = getStatusColor(sessionState, isUserActive)
    val statusIcon = getStatusIcon(sessionState, isUserActive)
    
    // Анимация пульсации для активного состояния
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Surface(
        modifier = modifier,
        color = statusColor.copy(alpha = if (sessionState == SessionState.ACTIVE) alpha else 1f),
        shape = CircleShape,
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = getStatusDescription(sessionState, isUserActive),
                modifier = Modifier.size(16.dp),
                tint = Color.White
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnlineStatusCard(
    sessionState: SessionState,
    isUserActive: Boolean,
    sessionStats: Any?, // TODO: Заменить на SessionStatistics
    onRestartSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Заголовок и кнопка перезапуска
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OnlineStatusBadge(
                        sessionState = sessionState,
                        isUserActive = isUserActive
                    )
                    
                    Text(
                        text = getStatusText(sessionState, isUserActive),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (sessionState == SessionState.ERROR) {
                    TextButton(onClick = onRestartSession) {
                        Text("Перезапустить")
                    }
                }
            }
            
            // Детали сессии
            SessionDetailsSection(sessionState, isUserActive)
        }
    }
}

@Composable
private fun SessionDetailsSection(
    sessionState: SessionState,
    isUserActive: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Статус соединения
        DetailRow(
            icon = Icons.Default.Wifi,
            label = "Соединение",
            value = when (sessionState) {
                SessionState.ACTIVE -> "Активно"
                SessionState.INACTIVE -> "Неактивно"
                SessionState.ERROR -> "Ошибка"
            },
            valueColor = getStatusColor(sessionState, isUserActive)
        )
        
        // Активность пользователя
        DetailRow(
            icon = Icons.Default.TouchApp,
            label = "Активность",
            value = if (isUserActive) "Активен" else "Неактивен",
            valueColor = if (isUserActive) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Время сессии (заглушка)
        DetailRow(
            icon = Icons.Default.Schedule,
            label = "Время сессии",
            value = "45м 23с", // TODO: Получить из sessionStats
            valueColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Последняя активность (заглушка)
        DetailRow(
            icon = Icons.Default.Update,
            label = "Последняя активность",
            value = "2 минуты назад", // TODO: Получить из sessionStats
            valueColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

/**
 * Получение цвета статуса
 */
private fun getStatusColor(sessionState: SessionState, isUserActive: Boolean): Color {
    return when (sessionState) {
        SessionState.ACTIVE -> if (isUserActive) Color(0xFF4CAF50) else Color(0xFFFF9800)
        SessionState.INACTIVE -> Color(0xFF9E9E9E)
        SessionState.ERROR -> Color(0xFFE57373)
    }
}

/**
 * Получение иконки статуса
 */
private fun getStatusIcon(sessionState: SessionState, isUserActive: Boolean): ImageVector {
    return when (sessionState) {
        SessionState.ACTIVE -> if (isUserActive) Icons.Default.CheckCircle else Icons.Default.Schedule
        SessionState.INACTIVE -> Icons.Default.RadioButtonUnchecked
        SessionState.ERROR -> Icons.Default.Error
    }
}

/**
 * Получение описания статуса
 */
private fun getStatusDescription(sessionState: SessionState, isUserActive: Boolean): String {
    return when (sessionState) {
        SessionState.ACTIVE -> if (isUserActive) "Онлайн и активен" else "Онлайн, но неактивен"
        SessionState.INACTIVE -> "Не в игре"
        SessionState.ERROR -> "Ошибка соединения"
    }
}

/**
 * Получение текста статуса
 */
private fun getStatusText(sessionState: SessionState, isUserActive: Boolean): String {
    return when (sessionState) {
        SessionState.ACTIVE -> "В игре"
        SessionState.INACTIVE -> "Не в игре"
        SessionState.ERROR -> "Ошибка"
    }
}