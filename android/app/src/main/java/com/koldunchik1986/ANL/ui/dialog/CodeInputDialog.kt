package com.koldunchik1986.ANL.ui.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Диалог ввода кода - аналог FormCode из Windows клиента
 * Используется для ввода CAPTCHA кода
 */
@Composable
fun CodeInputDialog(
    onDismiss: () -> Unit,
    onCodeEntered: (String) -> Unit,
    onMaximize: () -> Unit,
    codePngData: ByteArray? = null
) {
    var codeInput by remember { mutableStateOf("") }
    var isCodeValid by remember { mutableStateOf(false) }
    
    // Проверка кода для валидации
    LaunchedEffect(codeInput) {
        val code = codeInput.toIntOrNull()
        isCodeValid = code != null && code in 0..99999
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
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
                // Заголовок
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Введите код с картинки",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Картинка с кодом
                CodeImageSection(
                    codePngData = codePngData,
                    onRefresh = { /* TODO: Реализовать обновление картинки */ }
                )
                
                // Поле ввода кода
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it },
                    label = { Text("Код") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = codeInput.isNotEmpty() && !isCodeValid,
                    supportingText = {
                        if (codeInput.isNotEmpty() && !isCodeValid) {
                            Text(
                                text = "Код должен быть числом от 0 до 99999",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Кнопки
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Кнопка "Развернуть"
                    OutlinedButton(
                        onClick = onMaximize,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Fullscreen, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Развернуть")
                    }
                    
                    // Кнопка "ОК"
                    Button(
                        onClick = { onCodeEntered(codeInput) },
                        enabled = isCodeValid,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ОК")
                    }
                }
                
                // Подсказка
                Text(
                    text = "Нажмите \"Развернуть\" для увеличения картинки или \"ОК\" для отправки кода",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CodeImageSection(
    codePngData: ByteArray?,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Код безопасности",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                IconButton(onClick = onRefresh) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Обновить код",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Отображение картинки с кодом
            if (codePngData != null) {
                val bitmap = remember(codePngData) {
                    BitmapFactory.decodeByteArray(codePngData, 0, codePngData.size)
                        ?.asImageBitmap()
                }
                
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Код безопасности",
                        modifier = Modifier
                            .size(width = 134.dp, height = 60.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    CodePlaceholder()
                }
            } else {
                CodePlaceholder()
            }
        }
    }
}

@Composable
private fun CodePlaceholder() {
    Box(
        modifier = Modifier
            .size(width = 134.dp, height = 60.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Default.Image,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Загрузка...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Результат ввода кода
 */
sealed class CodeInputResult {
    data class CodeEntered(val code: String) : CodeInputResult()
    object Maximize : CodeInputResult()
    object Dismissed : CodeInputResult()
}

/**
 * Состояние ввода кода - аналог AppVars.CodePng из Windows клиента
 */
data class CodeInputState(
    val isVisible: Boolean = false,
    val codePngData: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CodeInputState

        if (isVisible != other.isVisible) return false
        if (codePngData != null) {
            if (other.codePngData == null) return false
            if (!codePngData.contentEquals(other.codePngData)) return false
        } else if (other.codePngData != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isVisible.hashCode()
        result = 31 * result + (codePngData?.contentHashCode() ?: 0)
        return result
    }
}