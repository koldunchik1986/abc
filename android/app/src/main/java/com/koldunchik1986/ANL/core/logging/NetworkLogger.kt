package com.koldunchik1986.ANL.core.logging

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Логгер для записи сетевого трафика в файл log.txt
 * Записывает все HTTP запросы и ответы для отладки авторизации
 */
@Singleton
class NetworkLogger @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "NetworkLogger"
        private const val LOG_FILE_NAME = "log.txt"
        private const val MAX_LOG_SIZE = 10 * 1024 * 1024 // 10MB максимум
    }
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val logFile: File by lazy {
        File(context.getExternalFilesDir(null), LOG_FILE_NAME)
    }
    
    /**
     * Логирование исходящего HTTP запроса
     */
    suspend fun logRequest(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String? = null
    ) {
        val logEntry = buildString {
            appendLine("=".repeat(80))
            appendLine("🔄 ИСХОДЯЩИЙ ЗАПРОС")
            appendLine("Время: ${getCurrentTimestamp()}")
            appendLine("Метод: $method")
            appendLine("URL: $url")
            appendLine("Заголовки:")
            headers.forEach { (key, value) ->
                appendLine("  $key: $value")
            }
            if (!body.isNullOrEmpty()) {
                appendLine("Тело запроса:")
                appendLine(body)
            }
            appendLine("=".repeat(80))
        }
        
        writeToFile(logEntry)
        Log.d(TAG, "Запрос отправлен: $method $url")
    }
    
    /**
     * Логирование входящего HTTP ответа
     */
    suspend fun logResponse(
        url: String,
        responseCode: Int,
        headers: Map<String, String>,
        body: String? = null,
        duration: Long
    ) {
        val logEntry = buildString {
            appendLine("📥 ВХОДЯЩИЙ ОТВЕТ")
            appendLine("Время: ${getCurrentTimestamp()}")
            appendLine("URL: $url")
            appendLine("Код ответа: $responseCode")
            appendLine("Длительность: ${duration}ms")
            appendLine("Заголовки:")
            headers.forEach { (key, value) ->
                appendLine("  $key: $value")
            }
            if (!body.isNullOrEmpty()) {
                val truncatedBody = if (body.length > 2000) {
                    body.take(2000) + "\n... (тело обрезано, полная длина: ${body.length})"
                } else {
                    body
                }
                appendLine("Тело ответа:")
                appendLine(truncatedBody)
            }
            appendLine("=".repeat(80))
            appendLine()
        }
        
        writeToFile(logEntry)
        Log.d(TAG, "Ответ получен: $responseCode для $url")
    }
    
    /**
     * Логирование этапа авторизации
     */
    suspend fun logAuthStep(step: String, details: String = "") {
        val logEntry = buildString {
            appendLine("🔐 ЭТАП АВТОРИЗАЦИИ")
            appendLine("Время: ${getCurrentTimestamp()}")
            appendLine("Этап: $step")
            if (details.isNotEmpty()) {
                appendLine("Детали: $details")
            }
            appendLine("-".repeat(40))
            appendLine()
        }
        
        writeToFile(logEntry)
        Log.i(TAG, "Авторизация: $step")
    }
    
    /**
     * Логирование ошибки
     */
    suspend fun logError(error: String, exception: Throwable? = null) {
        val logEntry = buildString {
            appendLine("❌ ОШИБКА")
            appendLine("Время: ${getCurrentTimestamp()}")
            appendLine("Ошибка: $error")
            exception?.let {
                appendLine("Исключение: ${it.javaClass.simpleName}")
                appendLine("Сообщение: ${it.message}")
                appendLine("Stack trace:")
                appendLine(it.stackTraceToString())
            }
            appendLine("-".repeat(40))
            appendLine()
        }
        
        writeToFile(logEntry)
        Log.e(TAG, "Ошибка: $error", exception)
    }
    
    /**
     * Логирование информационного сообщения
     */
    suspend fun logInfo(message: String) {
        val logEntry = buildString {
            appendLine("ℹ️ INFO")
            appendLine("Время: ${getCurrentTimestamp()}")
            appendLine("Сообщение: $message")
            appendLine("-".repeat(40))
            appendLine()
        }
        
        writeToFile(logEntry)
        Log.i(TAG, "Info: $message")
    }
    
    /**
     * Очистка лог файла
     */
    suspend fun clearLog() {
        withContext(Dispatchers.IO) {
            try {
                if (logFile.exists()) {
                    logFile.delete()
                }
                logInfo("Лог файл очищен")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка очистки лог файла", e)
            }
        }
    }
    
    /**
     * Получение содержимого лог файла
     */
    suspend fun getLogContent(): String {
        return withContext(Dispatchers.IO) {
            try {
                if (logFile.exists()) {
                    logFile.readText()
                } else {
                    "Лог файл пуст"
                }
            } catch (e: Exception) {
                "Ошибка чтения лог файла: ${e.message}"
            }
        }
    }
    
    /**
     * Запись в файл
     */
    private suspend fun writeToFile(content: String) {
        withContext(Dispatchers.IO) {
            try {
                // Проверяем размер файла
                if (logFile.exists() && logFile.length() > MAX_LOG_SIZE) {
                    // Обрезаем файл, оставляя последние 70% содержимого
                    val existingContent = logFile.readText()
                    val trimmedContent = existingContent.takeLast((MAX_LOG_SIZE * 0.7).toInt())
                    logFile.writeText("... (начало лога обрезано)\n\n" + trimmedContent)
                }
                
                // Добавляем новую запись
                FileOutputStream(logFile, true).use { fos ->
                    fos.write(content.toByteArray(Charsets.UTF_8))
                    fos.flush()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка записи в лог файл", e)
            }
        }
    }
    
    /**
     * Получение текущего времени
     */
    private fun getCurrentTimestamp(): String {
        return dateFormat.format(Date())
    }
    
    /**
     * Получение пути к лог файлу
     */
    fun getLogFilePath(): String {
        return logFile.absolutePath
    }
}