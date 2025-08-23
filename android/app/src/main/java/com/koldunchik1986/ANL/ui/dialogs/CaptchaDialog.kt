package com.koldunchik1986.ANL.ui.dialogs

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Диалог для ввода капчи
 * Аналог CaptchaDialog из эталонной реализации
 */
@Composable
fun CaptchaDialog(
    captchaBytes: ByteArray?,
    onResult: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var captchaCode by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Dialog(onDismissRequest = onDismiss) {
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
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Введите код с картинки",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Изображение капчи
                captchaBytes?.let { bytes ->
                    val bitmap = remember {
                        try {
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        } catch (e: Exception) {
                            Log.e("CaptchaDialog", "Error loading captcha image", e)
                            null
                        }
                    }
                    
                    bitmap?.let {
                        // Увеличиваем изображение для лучшей видимости (как в эталоне)
                        val scaledBitmap = remember {
                            try {
                                val matrix = Matrix()
                                matrix.postScale(3f, 3f)
                                android.graphics.Bitmap.createBitmap(
                                    it, 0, 0, it.width, it.height, matrix, false
                                )
                            } catch (e: Exception) {
                                Log.e("CaptchaDialog", "Error scaling captcha image", e)
                                it // Возвращаем исходное изображение при ошибке
                            }
                        }
                        
                        Image(
                            bitmap = scaledBitmap.asImageBitmap(),
                            contentDescription = "Капча",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        )
                    } ?: run {
                        Text(
                            text = "Ошибка загрузки изображения",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Поле ввода (только цифры, как в эталоне)
                OutlinedTextField(
                    value = captchaCode,
                    onValueChange = { newValue ->
                        // Ограничиваем ввод только цифрами и максимум 5 символов
                        if (newValue.all { it.isDigit() } && newValue.length <= 5) {
                            captchaCode = newValue
                            
                            // Автоотправка при вводе 5 цифр (как в эталоне)
                            if (newValue.length == 5) {
                                Handler(Looper.getMainLooper()).postDelayed({
                                    onResult(newValue)
                                }, 500)
                            }
                        }
                    },
                    label = { Text("Код капчи") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (captchaCode.isNotEmpty() && captchaCode.all { it.isDigit() }) {
                                keyboardController?.hide()
                                onResult(captchaCode)
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Кнопки
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(
                        onClick = { onResult(null) }
                    ) {
                        Text("Отмена")
                    }
                    
                    TextButton(
                        onClick = { onResult("REFRESH") }
                    ) {
                        Text("Обновить")
                    }
                    
                    Button(
                        onClick = {
                            if (captchaCode.isNotEmpty() && captchaCode.all { it.isDigit() }) {
                                onResult(captchaCode)
                            }
                        },
                        enabled = captchaCode.isNotEmpty() && captchaCode.all { it.isDigit() }
                    ) {
                        Text("Отправить")
                    }
                }
            }
        }
    }
    
    // Автофокус на поле ввода
    LaunchedEffect(Unit) {
        // Клавиатура появится автоматически при фокусе на TextField
    }
}

/**
 * Callback интерфейс для обработки капчи (аналог эталона)
 */
interface CaptchaCallback {
    fun onCaptchaEntered(code: String)
    fun onCaptchaRefresh()
    fun onCaptchaCancelled()
}