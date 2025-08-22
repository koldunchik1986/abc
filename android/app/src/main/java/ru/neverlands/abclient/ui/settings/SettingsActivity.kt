package ru.neverlands.abclient.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import ru.neverlands.abclient.ui.theme.ABClientTheme

/**
 * Activity для настроек приложения
 * Эквивалент FormSettings из Windows версии
 */
@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    
    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, SettingsActivity::class.java)
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
                    SettingsScreen(
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Настройки подключения
            SettingsSection(title = "Подключение") {
                SettingsSwitch(
                    title = "Автоматическое переподключение",
                    subtitle = "Переподключаться при обрыве соединения",
                    checked = true,
                    onCheckedChange = { /* TODO */ }
                )
                
                SettingsSwitch(
                    title = "Использовать прокси",
                    subtitle = "Подключение через прокси сервер",
                    checked = false,
                    onCheckedChange = { /* TODO */ }
                )
            }
            
            // Настройки игры
            SettingsSection(title = "Игра") {
                SettingsSwitch(
                    title = "Автоматическое обновление",
                    subtitle = "Обновлять страницу игры автоматически",
                    checked = true,
                    onCheckedChange = { /* TODO */ }
                )
                
                SettingsSwitch(
                    title = "Звуковые уведомления",
                    subtitle = "Воспроизводить звуки игровых событий",
                    checked = false,
                    onCheckedChange = { /* TODO */ }
                )
            }
            
            // Настройки интерфейса
            SettingsSection(title = "Интерфейс") {
                SettingsSwitch(
                    title = "Темная тема",
                    subtitle = "Использовать темное оформление",
                    checked = false,
                    onCheckedChange = { /* TODO */ }
                )
                
                SettingsSwitch(
                    title = "Показывать статус онлайн",
                    subtitle = "Отображать индикатор подключения",
                    checked = true,
                    onCheckedChange = { /* TODO */ }
                )
            }
            
            // Настройки безопасности
            SettingsSection(title = "Безопасность") {
                SettingsSwitch(
                    title = "Шифрование данных",
                    subtitle = "Шифровать сохраненные пароли",
                    checked = true,
                    onCheckedChange = { /* TODO */ }
                )
                
                SettingsSwitch(
                    title = "Эмуляция браузера",
                    subtitle = "Расширенная эмуляция для обхода защит",
                    checked = true,
                    onCheckedChange = { /* TODO */ }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Информация о приложении
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "О приложении",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ABClient для Android",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Версия 1.0.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SettingsSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}