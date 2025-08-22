package com.koldunchik1986.ANL.ui.game.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

/**
 * Диалог автоматического входа в игру
 * Аналог FormAutoLogon из Windows клиента
 */
@Composable
fun AutoLoginDialog(
    userName: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    var countdown by remember { mutableStateOf(3) }
    
    // Обратный отсчет таймера (аналог timerCountDown из Windows)
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        // Когда таймер доходит до 0 - автоматически подтверждаем вход
        onConfirm()
    }
    
    Dialog(onDismissRequest = onCancel) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Иконка
                Icon(
                    imageVector = Icons.Default.Login,
                    contentDescription = "Авторизация",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                // Заголовок
                Text(
                    text = "Вход в игру",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                // Имя персонажа (аналог labelUsername)
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Текст "Вход в игру" (аналог label2)
                Text(
                    text = "Вход в игру",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                
                // Обратный отсчет
                if (countdown > 0) {
                    Text(
                        text = "Автоматический вход через $countdown сек",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Кнопки (аналог buttonOk и buttonCancel)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Кнопка "Остановить вход" (аналог buttonOk)
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (countdown > 0) {
                            Text("Остановить вход")
                        } else {
                            Text("Вход...")
                        }
                    }
                    
                    // Кнопка "Отмена" (аналог buttonCancel)
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Отмена")
                    }
                }
            }
        }
    }
}