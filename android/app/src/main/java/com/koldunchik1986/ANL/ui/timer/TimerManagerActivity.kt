package com.koldunchik1986.ANL.ui.timer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import com.koldunchik1986.ANL.ui.theme.ABClientTheme

/**
 * Менеджер таймеров - полный аналог FormNewTimer из Windows версии
 * Позволяет создавать таймеры для зелий, перемещений и комплектов
 */
@AndroidEntryPoint
class TimerManagerActivity : ComponentActivity() {
    
    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, TimerManagerActivity::class.java)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            ABClientTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TimerManagerScreen(
                        onBack = { finish() },
                        onTimerCreated = { 
                            setResult(RESULT_OK)
                            finish() 
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerManagerScreen(
    onBack: () -> Unit,
    onTimerCreated: () -> Unit,
    viewModel: TimerManagerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новый таймер") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (viewModel.createTimer()) {
                                onTimerCreated()
                            }
                        },
                        enabled = uiState.canCreateTimer
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Создать")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Основные настройки таймера
            TimerBasicsCard(
                uiState = uiState,
                onNameChanged = viewModel::setTimerName,
                onHourChanged = viewModel::setTriggerHour,
                onMinuteChanged = viewModel::setTriggerMinute
            )
            
            // Выбор типа действия
            TimerActionCard(
                uiState = uiState,
                onActionTypeChanged = viewModel::setActionType
            )
            
            // Настройки для зелий
            if (uiState.actionType == TimerActionType.POTION) {
                PotionSettingsCard(
                    uiState = uiState,
                    onPotionChanged = viewModel::setPotion,
                    onDrinkCountChanged = viewModel::setDrinkCount,
                    onRecurChanged = viewModel::setIsRecur
                )
            }
            
            // Настройки для перемещения
            if (uiState.actionType == TimerActionType.DESTINATION) {
                DestinationSettingsCard(
                    uiState = uiState,
                    onDestinationChanged = viewModel::setDestination
                )
            }
            
            // Настройки для комплекта
            if (uiState.actionType == TimerActionType.COMPLECT) {
                ComplectSettingsCard(
                    uiState = uiState,
                    onComplectChanged = viewModel::setComplect
                )
            }
            
            // Кнопка создания
            Button(
                onClick = {
                    if (viewModel.createTimer()) {
                        onTimerCreated()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = uiState.canCreateTimer
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Создать таймер",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun TimerBasicsCard(
    uiState: TimerManagerUiState,
    onNameChanged: (String) -> Unit,
    onHourChanged: (String) -> Unit,
    onMinuteChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Основные настройки",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Название таймера
            OutlinedTextField(
                value = uiState.timerName,
                onValueChange = onNameChanged,
                label = { Text("Название таймера") },
                placeholder = { Text("Автоматически генерируется") },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Время срабатывания
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = uiState.triggerHour,
                    onValueChange = onHourChanged,
                    label = { Text("Часы") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                
                OutlinedTextField(
                    value = uiState.triggerMinute,
                    onValueChange = onMinuteChanged,
                    label = { Text("Минуты") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("0-59") },
                    isError = uiState.triggerMinute.toIntOrNull()?.let { it > 59 } ?: false,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Предварительная информация
            if (uiState.triggerTimeText.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "Срабатывание: ${uiState.triggerTimeText}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerActionCard(
    uiState: TimerManagerUiState,
    onActionTypeChanged: (TimerActionType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Тип действия",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimerActionType.values().forEach { actionType ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = uiState.actionType == actionType,
                                onClick = { onActionTypeChanged(actionType) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.actionType == actionType,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column {
                            Text(
                                text = actionType.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (uiState.actionType == actionType) FontWeight.Medium else FontWeight.Normal
                            )
                            Text(
                                text = actionType.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PotionSettingsCard(
    uiState: TimerManagerUiState,
    onPotionChanged: (String) -> Unit,
    onDrinkCountChanged: (String) -> Unit,
    onRecurChanged: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocalDrink,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Настройки зелья",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Выбор зелья
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = uiState.selectedPotion,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Зелье") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    uiState.availablePotions.forEach { potion ->
                        DropdownMenuItem(
                            text = { Text(potion) },
                            onClick = {
                                onPotionChanged(potion)
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            // Количество применений
            OutlinedTextField(
                value = uiState.drinkCount,
                onValueChange = onDrinkCountChanged,
                label = { Text("Количество применений") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            
            // Повторять
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = uiState.isRecur,
                    onCheckedChange = onRecurChanged
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Повторять каждые ${uiState.getTotalMinutes()} минут",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun DestinationSettingsCard(
    uiState: TimerManagerUiState,
    onDestinationChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Настройки перемещения",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            OutlinedTextField(
                value = uiState.destination,
                onValueChange = onDestinationChanged,
                label = { Text("Локация (например: 8-259)") },
                isError = !uiState.isDestinationValid,
                supportingText = {
                    if (!uiState.isDestinationValid && uiState.destination.isNotEmpty()) {
                        Text(
                            text = "Неверный формат локации",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ComplectSettingsCard(
    uiState: TimerManagerUiState,
    onComplectChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Checkroom,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Настройки комплекта",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            OutlinedTextField(
                value = uiState.complect,
                onValueChange = onComplectChanged,
                label = { Text("Название комплекта") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
