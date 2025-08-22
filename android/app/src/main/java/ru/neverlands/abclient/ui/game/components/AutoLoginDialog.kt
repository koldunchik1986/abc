package ru.neverlands.abclient.ui.game.components

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
 * Аналог FormAutoLogon из Windows версии
 */
@Composable
fun AutoLoginDialog(
    userName: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    var countdown by remember { mutableStateOf(3) }
    
    // Таймер обратного отсчета (аналог timerCountDown из Windows)
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        // Когда счетчик дошел до 0 - автоматически подтверждаем вход
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
                    contentDescription = "Автовход",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                // Заголовок
                Text(
                    text = "Автовход в игру",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                // Имя пользователя (аналог labelUsername)
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Текст "входит в игру" (аналог label2)
                Text(
                    text = "входит в игру",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                
                // Счетчик времени
                if (countdown > 0) {
                    Text(
                        text = "Автовход через $countdown сек",
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
                    // Кнопка "Войти сейчас" (аналог buttonOk)
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (countdown > 0) {
                            Text("Войти сейчас")
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