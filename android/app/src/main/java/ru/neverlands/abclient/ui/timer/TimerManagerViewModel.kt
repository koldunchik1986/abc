package ru.neverlands.abclient.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class TimerManagerViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(TimerManagerUiState())
    val uiState: StateFlow<TimerManagerUiState> = _uiState.asStateFlow()
    
    fun setTimerName(name: String) {
        _uiState.value = _uiState.value.copy(timerName = name)
        updateCanCreateTimer()
    }
    
    fun setTriggerHour(hour: String) {
        _uiState.value = _uiState.value.copy(triggerHour = hour)
        updateTriggerTime()
        updateCanCreateTimer()
    }
    
    fun setTriggerMinute(minute: String) {
        _uiState.value = _uiState.value.copy(triggerMinute = minute)
        updateTriggerTime()
        updateCanCreateTimer()
    }
    
    fun setActionType(type: TimerActionType) {
        _uiState.value = _uiState.value.copy(actionType = type)
        updateCanCreateTimer()
    }
    
    fun setPotion(potion: String) {
        _uiState.value = _uiState.value.copy(selectedPotion = potion)
        updateCanCreateTimer()
    }
    
    fun setDrinkCount(count: String) {
        _uiState.value = _uiState.value.copy(drinkCount = count)
        updateCanCreateTimer()
    }
    
    fun setIsRecur(recur: Boolean) {
        _uiState.value = _uiState.value.copy(isRecur = recur)
    }
    
    fun setDestination(destination: String) {
        val isValid = isValidCellNumber(destination)
        _uiState.value = _uiState.value.copy(
            destination = destination,
            isDestinationValid = isValid || destination.isEmpty()
        )
        updateCanCreateTimer()
    }
    
    fun setComplect(complect: String) {
        _uiState.value = _uiState.value.copy(complect = complect)
        updateCanCreateTimer()
    }
    
    /**
     * Создает таймер - аналог buttonOk_Click из Windows версии
     */
    fun createTimer(): Boolean {
        val state = _uiState.value
        
        if (!state.canCreateTimer) return false
        
        val appTimer = AppTimer()
        
        // Описание
        val description = if (state.timerName.trim().isNotEmpty()) {
            state.timerName.trim()
        } else {
            generateAutoDescription(state)
        }
        appTimer.description = description
        
        // Время срабатывания
        val triggerHour = state.triggerHour.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val triggerMin = state.triggerMinute.toIntOrNull()?.let { 
            if (it in 0..59) it else 0 
        } ?: 0
        
        val totalMinutes = (triggerHour * 60) + triggerMin
        appTimer.triggerTime = Calendar.getInstance().apply {
            add(Calendar.MINUTE, totalMinutes)
        }.time
        
        // Обработка типа действия
        when (state.actionType) {
            TimerActionType.NONE -> {
                // Простой таймер без действий
            }
            
            TimerActionType.POTION -> {
                if (state.selectedPotion.isEmpty() || state.selectedPotion == "Выберите зелье") {
                    return false
                }
                
                appTimer.potion = state.selectedPotion
                
                val drinkCount = state.drinkCount.toIntOrNull()?.coerceAtLeast(1) ?: 1
                appTimer.drinkCount = drinkCount
                appTimer.isRecur = state.isRecur
                
                if (appTimer.isRecur) {
                    appTimer.everyMinutes = totalMinutes
                }
            }
            
            TimerActionType.DESTINATION -> {
                if (state.destination.trim().isEmpty() || !state.isDestinationValid) {
                    return false
                }
                appTimer.destination = state.destination.trim()
            }
            
            TimerActionType.COMPLECT -> {
                if (state.complect.trim().isEmpty()) {
                    return false
                }
                appTimer.complect = state.complect.trim()
            }
        }
        
        // Добавляем таймер в менеджер (в реальности здесь будет вызов AppTimerManager.AddAppTimer)
        viewModelScope.launch {
            // TODO: Реализовать добавление таймера в систему
            // AppTimerManager.addAppTimer(appTimer)
        }
        
        return true
    }
    
    private fun updateTriggerTime() {
        val hour = _uiState.value.triggerHour.toIntOrNull() ?: 0
        val minute = _uiState.value.triggerMinute.toIntOrNull() ?: 0
        
        if (hour >= 0 && minute in 0..59) {
            val triggerTime = Calendar.getInstance().apply {
                add(Calendar.HOUR_OF_DAY, hour)
                add(Calendar.MINUTE, minute)
            }.time
            
            val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            _uiState.value = _uiState.value.copy(
                triggerTimeText = formatter.format(triggerTime)
            )
        } else {
            _uiState.value = _uiState.value.copy(triggerTimeText = "")
        }
    }
    
    private fun updateCanCreateTimer() {
        val state = _uiState.value
        
        // Проверяем базовые требования
        val hasValidTime = state.triggerHour.toIntOrNull()?.let { it >= 0 } ?: false &&
                          state.triggerMinute.toIntOrNull()?.let { it in 0..59 } ?: false
        
        if (!hasValidTime) {
            _uiState.value = _uiState.value.copy(canCreateTimer = false)
            return
        }
        
        // Проверяем требования в зависимости от типа действия
        val isActionValid = when (state.actionType) {
            TimerActionType.NONE -> true
            TimerActionType.POTION -> {
                state.selectedPotion.isNotEmpty() && 
                state.selectedPotion != "Выберите зелье" &&
                state.drinkCount.toIntOrNull()?.let { it > 0 } ?: false
            }
            TimerActionType.DESTINATION -> {
                state.destination.trim().isNotEmpty() && state.isDestinationValid
            }
            TimerActionType.COMPLECT -> {
                state.complect.trim().isNotEmpty()
            }
        }
        
        _uiState.value = _uiState.value.copy(canCreateTimer = isActionValid)
    }
    
    private fun generateAutoDescription(state: TimerManagerUiState): String {
        return when (state.actionType) {
            TimerActionType.POTION -> "Выпьем ${state.selectedPotion}"
            TimerActionType.DESTINATION -> "Идем на ${state.destination}"
            TimerActionType.COMPLECT -> "Одеваем комплект ${state.complect}"
            TimerActionType.NONE -> "Таймер"
        }
    }
    
    private fun isValidCellNumber(cellNumber: String): Boolean {
        return cellNumber.matches(Regex("\\d{1,2}-\\d{3}"))
    }
}

/**
 * UI состояние менеджера таймеров
 */
data class TimerManagerUiState(
    val timerName: String = "",
    val triggerHour: String = "0",
    val triggerMinute: String = "0",
    val triggerTimeText: String = "",
    val actionType: TimerActionType = TimerActionType.NONE,
    
    // Настройки зелий
    val availablePotions: List<String> = listOf(
        "Выберите зелье",
        "Малое лечение",
        "Среднее лечение", 
        "Большое лечение",
        "Малая мана",
        "Средняя мана",
        "Большая мана",
        "Антидот",
        "Лечение усталости",
        "Сила",
        "Ловкость",
        "Интеллект",
        "Выносливость",
        "Зелье невидимости",
        "Зелье ускорения"
    ),
    val selectedPotion: String = "Выберите зелье",
    val drinkCount: String = "1",
    val isRecur: Boolean = false,
    
    // Настройки перемещения
    val destination: String = "",
    val isDestinationValid: Boolean = true,
    
    // Настройки комплекта
    val complect: String = "",
    
    // Общее состояние
    val canCreateTimer: Boolean = false
) {
    fun getTotalMinutes(): Int {
        val hour = triggerHour.toIntOrNull() ?: 0
        val minute = triggerMinute.toIntOrNull() ?: 0
        return (hour * 60) + minute
    }
}

/**
 * Типы действий таймера - соответствуют радиокнопкам в Windows версии
 */
enum class TimerActionType(
    val title: String,
    val description: String
) {
    NONE(
        title = "Без действий",
        description = "Простое уведомление"
    ),
    POTION(
        title = "Выпить зелье",
        description = "Автоматически использовать зелье"
    ),
    DESTINATION(
        title = "Переместиться",
        description = "Перейти в указанную локацию"
    ),
    COMPLECT(
        title = "Одеть комплект",
        description = "Сменить экипировку"
    )
}

/**
 * Таймер приложения - аналог AppTimer из Windows версии
 */
data class AppTimer(
    var triggerTime: Date = Date(),
    var description: String = "",
    var potion: String = "",
    var drinkCount: Int = 1,
    var isRecur: Boolean = false,
    var everyMinutes: Int = 0,
    var destination: String = "",
    var complect: String = "",
    var id: Int = 0,
    var isHerb: Boolean = false
) {
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(id)
        if (isRecur) {
            sb.append('*')
        }
        sb.append(") Еще ")
        
        val now = Date()
        var triggerTime = this.triggerTime
        
        if (isHerb) {
            triggerTime = Date(triggerTime.time - 30 * 60 * 1000) // Subtract 30 minutes
        }
        
        if (triggerTime.before(now)) {
            if (isHerb) {
                val diffMs = this.triggerTime.time - now.time
                val hours = diffMs / (1000 * 60 * 60)
                val minutes = (diffMs % (1000 * 60 * 60)) / (1000 * 60)
                val seconds = (diffMs % (1000 * 60)) / 1000
                
                when {
                    hours > 0 -> sb.append("${hours}:${String.format("%02d", minutes)}:${String.format("%02d", seconds)} (?)")
                    minutes > 0 -> sb.append("${minutes}:${String.format("%02d", seconds)} (?)")
                    else -> sb.append("0:${String.format("%02d", seconds)} (?)")
                }
            } else {
                sb.append("0:00")
            }
        } else {
            val diffMs = triggerTime.time - now.time
            val hours = diffMs / (1000 * 60 * 60)
            val minutes = (diffMs % (1000 * 60 * 60)) / (1000 * 60)
            val seconds = (diffMs % (1000 * 60)) / 1000
            
            when {
                hours > 0 -> sb.append("${hours}:${String.format("%02d", minutes)}:${String.format("%02d", seconds)}")
                minutes > 0 -> sb.append("${minutes}:${String.format("%02d", seconds)}")
                else -> sb.append("0:${String.format("%02d", seconds)}")
            }
        }
        
        sb.append(" - ")
        sb.append(description)
        if (drinkCount > 1) {
            sb.append(" [${drinkCount}]")
        }
        
        return sb.toString()
    }
}