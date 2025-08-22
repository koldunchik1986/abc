package ru.neverlands.abclient.ui.fishadvisor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import ru.neverlands.abclient.ui.theme.ABClientTheme

/**
 * Советник рыбака - полный аналог FormFishAdvisor из Windows версии
 * Содержит базу рыбных мест и советы по рыбалке
 */
@AndroidEntryPoint
class FishAdvisorActivity : ComponentActivity() {
    
    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, FishAdvisorActivity::class.java)
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
                    FishAdvisorScreen(
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FishAdvisorScreen(
    onBack: () -> Unit,
    viewModel: FishAdvisorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Советник рыбака") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
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
            // Настройки рыбалки
            FishingSettingsCard(
                uiState = uiState,
                onFishUmChanged = viewModel::setFishUm,
                onMaxBotLevelChanged = viewModel::setMaxBotLevel,
                onCalculateClicked = viewModel::calculateFishTips
            )
            
            // Список советов
            FishTipsCard(
                modifier = Modifier.weight(1f),
                fishTips = uiState.filteredFishTips
            )
        }
    }
}

@Composable
private fun FishingSettingsCard(
    uiState: FishAdvisorUiState,
    onFishUmChanged: (String) -> Unit,
    onMaxBotLevelChanged: (Int) -> Unit,
    onCalculateClicked: () -> Unit
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
                    text = "Настройки рыбалки",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Умение рыбалки
            OutlinedTextField(
                value = uiState.fishUmInput,
                onValueChange = onFishUmChanged,
                label = { Text("Умение рыбалки") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = !uiState.isFishUmValid,
                supportingText = {
                    if (!uiState.isFishUmValid) {
                        Text(
                            text = "Умение рыбалки - это целое число больше нуля, например, 65",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Выбор максимального уровня ботов
            Column {
                Text(
                    text = "Максимальный уровень ботов для сражения:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val botLevels = listOf(8, 9, 12, 15, 19)
                    botLevels.forEach { level ->
                        FilterChip(
                            onClick = { onMaxBotLevelChanged(level) },
                            label = { Text(level.toString()) },
                            selected = uiState.maxBotLevel == level
                        )
                    }
                }
            }
            
            // Кнопка расчета
            Button(
                onClick = onCalculateClicked,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.isFishUmValid
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Рассчитать советы")
            }
        }
    }
}

@Composable
private fun FishTipsCard(
    modifier: Modifier = Modifier,
    fishTips: List<FishTip>
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.List,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Советы по рыбалке",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (fishTips.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${fishTips.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (fishTips.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Нажмите \"Рассчитать советы\" для получения рекомендаций",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    // Заголовок таблицы
                    item {
                        FishTipHeaderRow()
                    }
                    
                    // Строки с советами
                    itemsIndexed(fishTips) { index, fishTip ->
                        FishTipRow(
                            fishTip = fishTip,
                            isEven = index % 2 == 0
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FishTipHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Доход",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = "Локация",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = "Боты",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = "Совет",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(2f),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun FishTipRow(
    fishTip: FishTip,
    isEven: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isEven) {
                    MaterialTheme.colorScheme.surface
                } else {
                    Color(0xFFF0F0F0)
                }
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "${fishTip.money} NV",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = fishTip.location,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = fishTip.botDescription,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${fishTip.tip} (${fishTip.fishUm})",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(2f)
        )
    }
}