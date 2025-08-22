package ru.neverlands.abclient.ui.settings.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.neverlands.abclient.ui.settings.SettingsUiState
import ru.neverlands.abclient.ui.settings.SettingsSection

/**
 * Настройки звука
 */
@Composable
fun SoundSettingsSection(
    uiState: SettingsUiState,
    onSoundEnabledChanged: (Boolean) -> Unit,
    onPlayDigitsChanged: (Boolean) -> Unit,
    onPlayAttackChanged: (Boolean) -> Unit,
    onPlaySndMsgChanged: (Boolean) -> Unit,
    onPlayRefreshChanged: (Boolean) -> Unit,
    onPlayAlarmChanged: (Boolean) -> Unit,
    onPlayTimerChanged: (Boolean) -> Unit
) {
    SettingsSection(
        title = "Настройки звука",
        icon = { Icon(Icons.Default.VolumeUp, contentDescription = null) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.soundEnabled,
                onCheckedChange = onSoundEnabledChanged
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Включить звуки")
        }
        
        if (uiState.soundEnabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = uiState.doPlayDigits,
                    onCheckedChange = onPlayDigitsChanged
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Озвучивать цифры")
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = uiState.doPlayAttack,
                    onCheckedChange = onPlayAttackChanged
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Звук атаки")
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = uiState.doPlaySndMsg,
                    onCheckedChange = onPlaySndMsgChanged
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Звук сообщений")
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = uiState.doPlayRefresh,
                    onCheckedChange = onPlayRefreshChanged
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Звук обновления")
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = uiState.doPlayAlarm,
                    onCheckedChange = onPlayAlarmChanged
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Звук тревоги")
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = uiState.doPlayTimer,
                    onCheckedChange = onPlayTimerChanged
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Звук таймера")
            }
        }
    }
}

/**
 * Настройки лечения
 */
@Composable
fun CureSettingsSection(
    uiState: SettingsUiState,
    onCureNV1Changed: (Int) -> Unit,
    onCureNV2Changed: (Int) -> Unit,
    onCureNV3Changed: (Int) -> Unit,
    onCureNV4Changed: (Int) -> Unit,
    onCureEnabled1Changed: (Boolean) -> Unit,
    onCureEnabled2Changed: (Boolean) -> Unit,
    onCureEnabled3Changed: (Boolean) -> Unit,
    onCureEnabled4Changed: (Boolean) -> Unit,
    onCureDisabledLowLevelsChanged: (Boolean) -> Unit
) {
    SettingsSection(
        title = "Настройки лечения",
        icon = { Icon(Icons.Default.MedicalServices, contentDescription = null) }
    ) {
        Text("Настройки автолечения", style = MaterialTheme.typography.titleSmall)
        
        for (i in 0..3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = when (i) {
                        0 -> uiState.cureEnabled[0]
                        1 -> uiState.cureEnabled[1]
                        2 -> uiState.cureEnabled[2]
                        else -> uiState.cureEnabled[3]
                    },
                    onCheckedChange = when (i) {
                        0 -> onCureEnabled1Changed
                        1 -> onCureEnabled2Changed
                        2 -> onCureEnabled3Changed
                        else -> onCureEnabled4Changed
                    }
                )
                
                OutlinedTextField(
                    value = when (i) {
                        0 -> uiState.cureNV[0].toString()
                        1 -> uiState.cureNV[1].toString()
                        2 -> uiState.cureNV[2].toString()
                        else -> uiState.cureNV[3].toString()
                    },
                    onValueChange = { newValue ->
                        newValue.toIntOrNull()?.let { value ->
                            when (i) {
                                0 -> onCureNV1Changed(value)
                                1 -> onCureNV2Changed(value)
                                2 -> onCureNV3Changed(value)
                                else -> onCureNV4Changed(value)
                            }
                        }
                    },
                    label = { Text("Лечение ${i + 1} (НВ)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.cureDisabledLowLevels,
                onCheckedChange = onCureDisabledLowLevelsChanged
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Отключить для низких уровней")
        }
    }
}

/**
 * Настройки автоответчика
 */
@Composable
fun AutoAnswerSettingsSection(
    uiState: SettingsUiState,
    onAutoAnswerChanged: (Boolean) -> Unit,
    onAutoAnswerTextChanged: (String) -> Unit
) {
    SettingsSection(
        title = "Автоответчик",
        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.doAutoAnswer,
                onCheckedChange = onAutoAnswerChanged
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Включить автоответчик")
        }
        
        if (uiState.doAutoAnswer) {
            OutlinedTextField(
                value = uiState.autoAnswer,
                onValueChange = onAutoAnswerTextChanged,
                label = { Text("Текст автоответа") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
        }
    }
}

/**
 * Настройки торговли
 */
@Composable
fun TradingSettingsSection(
    uiState: SettingsUiState,
    onTorgTableChanged: (String) -> Unit,
    onTorgMessageAdvChanged: (String) -> Unit,
    onTorgAdvTimeChanged: (Int) -> Unit,
    onTorgMessageNoMoneyChanged: (String) -> Unit,
    onTorgMessageTooExpChanged: (String) -> Unit,
    onTorgMessageThanksChanged: (String) -> Unit,
    onTorgMessageLess90Changed: (String) -> Unit,
    onTorgSlivChanged: (Boolean) -> Unit,
    onTorgMinLevelChanged: (Int) -> Unit,
    onTorgExChanged: (String) -> Unit,
    onTorgDenyChanged: (String) -> Unit
) {
    SettingsSection(
        title = "Настройки торговли",
        icon = { Icon(Icons.Default.Store, contentDescription = null) }
    ) {
        OutlinedTextField(
            value = uiState.torgTabl,
            onValueChange = onTorgTableChanged,
            label = { Text("Таблица товаров") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = uiState.torgMessageAdv,
            onValueChange = onTorgMessageAdvChanged,
            label = { Text("Сообщение рекламы") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2
        )
        
        OutlinedTextField(
            value = uiState.torgAdvTime.toString(),
            onValueChange = { newValue ->
                newValue.toIntOrNull()?.let { onTorgAdvTimeChanged(it) }
            },
            label = { Text("Время рекламы (сек)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = uiState.torgMessageNoMoney,
            onValueChange = onTorgMessageNoMoneyChanged,
            label = { Text("Сообщение 'нет денег'") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = uiState.torgMessageTooExp,
            onValueChange = onTorgMessageTooExpChanged,
            label = { Text("Сообщение 'слишком дорого'") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = uiState.torgMessageThanks,
            onValueChange = onTorgMessageThanksChanged,
            label = { Text("Сообщение благодарности") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = uiState.torgMessageLess90,
            onValueChange = onTorgMessageLess90Changed,
            label = { Text("Сообщение '<90%'") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.torgSliv,
                onCheckedChange = onTorgSlivChanged
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Режим слива")
        }
        
        OutlinedTextField(
            value = uiState.torgMinLevel.toString(),
            onValueChange = { newValue ->
                newValue.toIntOrNull()?.let { onTorgMinLevelChanged(it) }
            },
            label = { Text("Минимальный уровень") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = uiState.torgEx,
            onValueChange = onTorgExChanged,
            label = { Text("Исключения") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = uiState.torgDeny,
            onValueChange = onTorgDenyChanged,
            label = { Text("Запрещенные") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

/**
 * Настройки инвентаря
 */
@Composable
fun InventorySettingsSection(
    uiState: SettingsUiState,
    onInvPackChanged: (Boolean) -> Unit,
    onInvPackDolgChanged: (Boolean) -> Unit,
    onInvSortChanged: (Boolean) -> Unit
) {
    SettingsSection(
        title = "Настройки инвентаря",
        icon = { Icon(Icons.Default.Inventory, contentDescription = null) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.doInvPack,
                onCheckedChange = onInvPackChanged
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Автоупаковка инвентаря")
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.doInvPackDolg,
                onCheckedChange = onInvPackDolgChanged
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Упаковка долгов")
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.doInvSort,
                onCheckedChange = onInvSortChanged
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Автосортировка")
        }
    }
}

/**
 * Настройки быстрой атаки
 */
@Composable
fun FastAttackSettingsSection(
    uiState: SettingsUiState,
    onShowFastAttackChanged: (Boolean) -> Unit,
    onShowBloodChanged: (Boolean) -> Unit,
    onShowUltimateChanged: (Boolean) -> Unit,
    onShowClosedUltimateChanged: (Boolean) -> Unit,
    onShowClosedChanged: (Boolean) -> Unit,
    onShowFistChanged: (Boolean) -> Unit,
    onShowClosedFistChanged: (Boolean) -> Unit,
    onShowOpenNevidChanged: (Boolean) -> Unit,
    onShowPoisonChanged: (Boolean) -> Unit,
    onShowStrongChanged: (Boolean) -> Unit,
    onShowNevidChanged: (Boolean) -> Unit,
    onShowFogChanged: (Boolean) -> Unit,
    onShowZasChanged: (Boolean) -> Unit,
    onShowTotemChanged: (Boolean) -> Unit
) {
    SettingsSection(
        title = "Быстрая атака",
        icon = { Icon(Icons.Default.FlashOn, contentDescription = null) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.doShowFastAttack,
                onCheckedChange = onShowFastAttackChanged
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Показывать быструю атаку")
        }
        
        if (uiState.doShowFastAttack) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CheckboxRow("Кровавая", uiState.doShowFastAttackBlood, onShowBloodChanged)
                CheckboxRow("Ультимативная", uiState.doShowFastAttackUltimate, onShowUltimateChanged)
                CheckboxRow("Ультимативная закрытая", uiState.doShowFastAttackClosedUltimate, onShowClosedUltimateChanged)
                CheckboxRow("Закрытая", uiState.doShowFastAttackClosed, onShowClosedChanged)
                CheckboxRow("Кулачная", uiState.doShowFastAttackFist, onShowFistChanged)
                CheckboxRow("Кулачная закрытая", uiState.doShowFastAttackClosedFist, onShowClosedFistChanged)
                CheckboxRow("Открытая невидимка", uiState.doShowFastAttackOpenNevid, onShowOpenNevidChanged)
                CheckboxRow("Ядовитая", uiState.doShowFastAttackPoison, onShowPoisonChanged)
                CheckboxRow("Сильная", uiState.doShowFastAttackStrong, onShowStrongChanged)
                CheckboxRow("Невидимка", uiState.doShowFastAttackNevid, onShowNevidChanged)
                CheckboxRow("Туман", uiState.doShowFastAttackFog, onShowFogChanged)
                CheckboxRow("Заслон", uiState.doShowFastAttackZas, onShowZasChanged)
                CheckboxRow("Тотем", uiState.doShowFastAttackTotem, onShowTotemChanged)
            }
        }
    }
}

@Composable
private fun CheckboxRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}