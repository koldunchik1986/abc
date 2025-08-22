package com.koldunchik1986.ANL.ui.settings.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.koldunchik1986.ANL.ui.settings.SettingsUiState
import com.koldunchik1986.ANL.ui.settings.SettingsSection

/**
 * Основные настройки приложения
 */
@Composable
fun GeneralSettingsSection(
    uiState: SettingsUiState,
    onPromptExitChanged: (Boolean) -> Unit,
    onTrayChanged: (Boolean) -> Unit,
    onTrayBalloonsChanged: (Boolean) -> Unit
) {
    SettingsSection(
        title = "Основные настройки",
        icon = { Icon(Icons.Default.Settings, contentDescription = null) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.doPromptExit,
                onCheckedChange = onPromptExitChanged
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Подтверждение выхода")
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.doTray,
                onCheckedChange = onTrayChanged
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Сворачивать в трей")
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.showTrayBaloons,
                onCheckedChange = onTrayBalloonsChanged,
                enabled = uiState.doTray
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Показывать уведомления")
        }
    }
}

/**
 * Настройки чата
 */
@Composable
fun ChatSettingsSection(
    uiState: SettingsUiState,
    onKeepMovingChanged: (Boolean) -> Unit,
    onKeepGameChanged: (Boolean) -> Unit,
    onKeepLogChanged: (Boolean) -> Unit,
    onChatSizeChanged: (Int) -> Unit,
    onChatLevelsChanged: (Boolean) -> Unit
) {
    SettingsSection(
        title = "Настройки чата",
        icon = { Icon(Icons.Default.Chat, contentDescription = null) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.chatKeepMoving,
                onCheckedChange = onKeepMovingChanged
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Сохранять чат при перемещении")
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.chatKeepGame,
                onCheckedChange = onKeepGameChanged
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Сохранять игровой чат")
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.chatKeepLog,
                onCheckedChange = onKeepLogChanged
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Сохранять лог чата")
        }
        
        OutlinedTextField(
            value = uiState.chatSizeLog.toString(),
            onValueChange = { newValue ->
                newValue.toIntOrNull()?.let { onChatSizeChanged(it) }
            },
            label = { Text("Размер лога чата (строк)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.doChatLevels,
                onCheckedChange = onChatLevelsChanged
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Показывать уровни в чате")
        }
    }
}

/**
 * Настройки карты
 */
@Composable
fun MapSettingsSection(
    uiState: SettingsUiState,
    onExtendedMapChanged: (Boolean) -> Unit,
    onBigMapWidthChanged: (Int) -> Unit,
    onBigMapHeightChanged: (Int) -> Unit,
    onBigMapScaleChanged: (Float) -> Unit,
    onBigMapTransparencyChanged: (Float) -> Unit,
    onBackColorWhiteChanged: (Boolean) -> Unit,
    onDrawRegionChanged: (Boolean) -> Unit,
    onShowMiniMapChanged: (Boolean) -> Unit,
    onMiniMapWidthChanged: (Int) -> Unit,
    onMiniMapHeightChanged: (Int) -> Unit,
    onMiniMapScaleChanged: (Float) -> Unit
) {
    SettingsSection(
        title = "Настройки карты",
        icon = { Icon(Icons.Default.Map, contentDescription = null) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.mapShowExtended,
                onCheckedChange = onExtendedMapChanged
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Расширенная карта")
        }
        
        Text("Большая карта", style = MaterialTheme.typography.titleSmall)
        
        OutlinedTextField(
            value = uiState.mapBigWidth.toString(),
            onValueChange = { newValue ->
                newValue.toIntOrNull()?.let { onBigMapWidthChanged(it) }
            },
            label = { Text("Ширина") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = uiState.mapBigHeight.toString(),
            onValueChange = { newValue ->
                newValue.toIntOrNull()?.let { onBigMapHeightChanged(it) }
            },
            label = { Text("Высота") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Column {
            Text("Масштаб: ${uiState.mapBigScale}")
            Slider(
                value = uiState.mapBigScale,
                onValueChange = onBigMapScaleChanged,
                valueRange = 0.1f..5.0f,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Column {
            Text("Прозрачность: ${uiState.mapBigTransparency}")
            Slider(
                value = uiState.mapBigTransparency,
                onValueChange = onBigMapTransparencyChanged,
                valueRange = 0.0f..1.0f,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.mapShowBackColorWhite,
                onCheckedChange = onBackColorWhiteChanged
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Белый фон карты")
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.mapDrawRegion,
                onCheckedChange = onDrawRegionChanged
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Отображать регионы")
        }
        
        Divider()
        
        Text("Мини-карта", style = MaterialTheme.typography.titleSmall)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.mapShowMiniMap,
                onCheckedChange = onShowMiniMapChanged
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Отображать мини-карту")
        }
        
        if (uiState.mapShowMiniMap) {
            OutlinedTextField(
                value = uiState.mapMiniWidth.toString(),
                onValueChange = { newValue ->
                newValue.toIntOrNull()?.let { onMiniMapWidthChanged(it) }
            },
            label = { Text("Ширина мини-карты") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = uiState.mapMiniHeight.toString(),
            onValueChange = { newValue ->
                newValue.toIntOrNull()?.let { onMiniMapHeightChanged(it) }
            },
            label = { Text("Высота мини-карты") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Column {
            Text("Масштаб мини-карты: ${uiState.mapMiniScale}")
            Slider(
                value = uiState.mapMiniScale,
                onValueChange = onMiniMapScaleChanged,
                valueRange = 0.1f..2.0f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Настройки рыбалки
 */
@Composable
fun FishingSettingsSection(
    uiState: SettingsUiState,
    onFishTiedHighChanged: (Int) -> Unit,
    onFishTiedZeroChanged: (Boolean) -> Unit,
    onStopOverWeightChanged: (Boolean) -> Unit,
    onAutoWearChanged: (Boolean) -> Unit,
    onHandOneChanged: (String) -> Unit,
    onHandTwoChanged: (String) -> Unit,
    onChatReportChanged: (Boolean) -> Unit,
    onChatReportColorChanged: (Boolean) -> Unit
) {
    SettingsSection(
        title = "Настройки рыбалки",
        icon = { Icon(Icons.Default.Pool, contentDescription = null) }
    ) {
        OutlinedTextField(
            value = uiState.fishTiedHigh.toString(),
            onValueChange = { newValue ->
                newValue.toIntOrNull()?.let { onFishTiedHighChanged(it) }
            },
            label = { Text("Высокая усталость") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.fishTiedZero,
                onCheckedChange = onFishTiedZeroChanged
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Останавливаться при нулевой усталости")
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.fishStopOverWeight,
                onCheckedChange = onStopOverWeightChanged
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Останавливаться при перегрузе")
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.fishAutoWear,
                onCheckedChange = onAutoWearChanged
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Автоматически одевать снасти")
        }
        
        OutlinedTextField(
            value = uiState.fishHandOne,
            onValueChange = onHandOneChanged,
            label = { Text("Левая рука") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = uiState.fishHandTwo,
            onValueChange = onHandTwoChanged,
            label = { Text("Правая рука") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.fishChatReport,
                onCheckedChange = onChatReportChanged
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Отчет в чат")
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.fishChatReportColor,
                onCheckedChange = onChatReportColorChanged
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Цветной отчет")
        }
    }
}