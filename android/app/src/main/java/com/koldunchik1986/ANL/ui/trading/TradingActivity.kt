package com.koldunchik1986.ANL.ui.trading

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import com.koldunchik1986.ANL.ui.theme.ABClientTheme

/**
 * Система торговли - функциональность на основе TorgList из Windows версии
 * Включает таблицы цен, автоответчик и расчет минимальных цен
 */
@AndroidEntryPoint
class TradingActivity : ComponentActivity() {
    
    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, TradingActivity::class.java)
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
                    TradingScreen(
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradingScreen(
    onBack: () -> Unit,
    viewModel: TradingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Система торговли") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveSettings() }
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Сохранить")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Таблица цен
            item {
                PriceTableCard(
                    uiState = uiState,
                    onTableStringChanged = viewModel::setTableString,
                    onValidateTable = viewModel::validateTable
                )
            }
            
            // Автоответчик
            item {
                AutoResponderCard(
                    uiState = uiState,
                    onMessageAdvChanged = viewModel::setMessageAdv,
                    onAdvTimeChanged = viewModel::setAdvTime,
                    onMessageNoMoneyChanged = viewModel::setMessageNoMoney,
                    onMessageTooExpChanged = viewModel::setMessageTooExp,
                    onMessageThanksChanged = viewModel::setMessageThanks,
                    onMessageLess90Changed = viewModel::setMessageLess90
                )
            }
            
            // Настройки торговли
            item {
                TradingSettingsCard(
                    uiState = uiState,
                    onSlivChanged = viewModel::setSliv,
                    onMinLevelChanged = viewModel::setMinLevel,
                    onExChanged = viewModel::setEx,
                    onDenyChanged = viewModel::setDeny
                )
            }
            
            // Тестирование цен
            item {
                PriceTestCard(
                    uiState = uiState,
                    onTestPriceChanged = viewModel::setTestPrice,
                    onCalculatePrice = viewModel::calculatePrice
                )
            }
            
            // Список пар цен
            if (uiState.pricePairs.isNotEmpty()) {
                item {
                    PricePairsCard(
                        pricePairs = uiState.pricePairs
                    )
                }
            }
        }
    }
}

@Composable
private fun PriceTableCard(
    uiState: TradingUiState,
    onTableStringChanged: (String) -> Unit,
    onValidateTable: () -> Unit
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
                    Icons.Default.TableChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Таблица цен",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            OutlinedTextField(
                value = uiState.tableString,
                onValueChange = onTableStringChanged,
                label = { Text("Строка таблицы") },
                placeholder = { Text("Например: 1-100(*-50),101-200(*-40),201-500(*-30)") },
                supportingText = {
                    Text("Формат: минЦена-максЦена(*бонус), где бонус может быть отрицательным")
                },
                isError = !uiState.isTableValid,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            if (!uiState.isTableValid && uiState.tableString.isNotEmpty()) {
                Text(
                    text = "Ошибка в формате таблицы",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Button(
                onClick = onValidateTable,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Проверить таблицу")
            }
        }
    }
}

@Composable
private fun AutoResponderCard(
    uiState: TradingUiState,
    onMessageAdvChanged: (String) -> Unit,
    onAdvTimeChanged: (String) -> Unit,
    onMessageNoMoneyChanged: (String) -> Unit,
    onMessageTooExpChanged: (String) -> Unit,
    onMessageThanksChanged: (String) -> Unit,
    onMessageLess90Changed: (String) -> Unit
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
                    Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Автоответчик",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Рекламное сообщение
            OutlinedTextField(
                value = uiState.messageAdv,
                onValueChange = onMessageAdvChanged,
                label = { Text("Рекламное сообщение") },
                supportingText = { Text("Доступные переменные: {таблица}, {вещь}, {цена}") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            
            // Время рекламы
            OutlinedTextField(
                value = uiState.advTime,
                onValueChange = onAdvTimeChanged,
                label = { Text("Интервал рекламы (секунды)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            
            // Сообщение "нет денег"
            OutlinedTextField(
                value = uiState.messageNoMoney,
                onValueChange = onMessageNoMoneyChanged,
                label = { Text("Сообщение при недостатке денег") },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Сообщение "слишком дорого"
            OutlinedTextField(
                value = uiState.messageTooExp,
                onValueChange = onMessageTooExpChanged,
                label = { Text("Сообщение при завышенной цене") },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Сообщение благодарности
            OutlinedTextField(
                value = uiState.messageThanks,
                onValueChange = onMessageThanksChanged,
                label = { Text("Сообщение благодарности") },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Сообщение при цене < 90%
            OutlinedTextField(
                value = uiState.messageLess90,
                onValueChange = onMessageLess90Changed,
                label = { Text("Сообщение при цене менее 90%") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TradingSettingsCard(
    uiState: TradingUiState,
    onSlivChanged: (String) -> Unit,
    onMinLevelChanged: (String) -> Unit,
    onExChanged: (String) -> Unit,
    onDenyChanged: (String) -> Unit
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
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Настройки торговли",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = uiState.sliv,
                    onValueChange = onSlivChanged,
                    label = { Text("Слив (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                
                OutlinedTextField(
                    value = uiState.minLevel,
                    onValueChange = onMinLevelChanged,
                    label = { Text("Мин. уровень") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            
            OutlinedTextField(
                value = uiState.ex,
                onValueChange = onExChanged,
                label = { Text("Исключения (через запятую)") },
                supportingText = { Text("Предметы, которые не покупаем") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = uiState.deny,
                onValueChange = onDenyChanged,
                label = { Text("Запрещенные игроки (через запятую)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PriceTestCard(
    uiState: TradingUiState,
    onTestPriceChanged: (String) -> Unit,
    onCalculatePrice: () -> Unit
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
                    Icons.Default.Calculate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Тестирование цен",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.testPrice,
                    onValueChange = onTestPriceChanged,
                    label = { Text("Тестовая цена") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                
                Button(
                    onClick = onCalculatePrice,
                    enabled = uiState.testPrice.toIntOrNull() != null
                ) {
                    Text("Рассчитать")
                }
            }
            
            if (uiState.calculatedPrice > 0) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "Цена по таблице: ${uiState.calculatedPrice} NV",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PricePairsCard(
    pricePairs: List<TradingPricePair>
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
                    Icons.AutoMirrored.Filled.List,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Пары цен (${pricePairs.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            pricePairs.forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${pair.priceLow} - ${pair.priceHigh} NV",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (pair.bonus >= 0) "+${pair.bonus} NV" else "${pair.bonus} NV",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (pair.bonus >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
