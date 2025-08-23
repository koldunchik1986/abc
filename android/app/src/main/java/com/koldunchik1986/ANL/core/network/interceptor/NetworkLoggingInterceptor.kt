package com.koldunchik1986.ANL.core.network.interceptor

import com.koldunchik1986.ANL.core.logging.NetworkLogger
import com.koldunchik1986.ANL.core.notifications.NetworkNotificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.nio.charset.Charset
import javax.inject.Inject

/**
 * Interceptor для детального логирования HTTP трафика
 * Записывает все запросы и ответы в лог и показывает уведомления
 */
class NetworkLoggingInterceptor @Inject constructor(
    private val networkLogger: NetworkLogger,
    private val notificationService: NetworkNotificationService
) : Interceptor {
    
    companion object {
        private const val UTF8 = "UTF-8"
    }
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()
        
        // Логируем запрос
        logRequest(request)
        
        // Выполняем запрос
        val response = try {
            chain.proceed(request)
        } catch (e: Exception) {
            // Логируем ошибку соединения
            coroutineScope.launch {
                networkLogger.logError("Ошибка выполнения запроса: ${request.url}", e)
                notificationService.showErrorNotification("Ошибка соединения: ${e.message}")
            }
            throw e
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // Логируем ответ
        logResponse(request, response, duration)
        
        return response
    }
    
    /**
     * Логирование исходящего запроса
     */
    private fun logRequest(request: Request) {
        coroutineScope.launch {
            val method = request.method
            val url = request.url.toString()
            
            // Собираем заголовки
            val headers = mutableMapOf<String, String>()
            request.headers.forEach { (name, value) ->
                headers[name] = value
            }
            
            // Извлекаем тело запроса если есть
            val requestBody = request.body
            val bodyString = if (requestBody != null) {
                try {
                    val buffer = Buffer()
                    requestBody.writeTo(buffer)
                    
                    val contentType = requestBody.contentType()
                    val charset = contentType?.charset(Charset.forName(UTF8)) ?: Charset.forName(UTF8)
                    
                    if (isPlaintext(buffer)) {
                        buffer.readString(charset)
                    } else {
                        "[Бинарные данные, размер: ${requestBody.contentLength()} байт]"
                    }
                } catch (e: Exception) {
                    "[Ошибка чтения тела запроса: ${e.message}]"
                }
            } else {
                null
            }
            
            // Логируем в файл
            networkLogger.logRequest(method, url, headers, bodyString)
            
            // Показываем уведомление
            notificationService.showRequestToast(method, url)
            
            // Специальная обработка для игровых запросов
            when {
                url.contains("game.php") -> {
                    if (bodyString?.contains("player_nick") == true) {
                        networkLogger.logAuthStep("Отправка данных авторизации", "Логин и пароль")
                        notificationService.showAuthStepNotification("Отправка логина и пароля")
                    } else if (bodyString?.contains("flcheck") == true) {
                        networkLogger.logAuthStep("Отправка Flash пароля")
                        notificationService.showAuthStepNotification("Отправка Flash пароля")
                    }
                }
                url.contains("index.cgi") || url.endsWith("neverlands.ru/") -> {
                    networkLogger.logAuthStep("Переход на главную страницу")
                    notificationService.showAuthStepNotification("Загрузка главной страницы")
                }
                url.contains("main.php") -> {
                    networkLogger.logAuthStep("Проверка статуса авторизации")
                    notificationService.showAuthStepNotification("Проверка статуса игры")
                }
            }
        }
    }
    
    /**
     * Логирование входящего ответа
     */
    private fun logResponse(request: Request, response: Response, duration: Long) {
        coroutineScope.launch {
            val url = request.url.toString()
            val responseCode = response.code
            
            // Собираем заголовки ответа
            val headers = mutableMapOf<String, String>()
            response.headers.forEach { (name, value) ->
                headers[name] = value
            }
            
            // Читаем тело ответа
            val responseBody = response.body
            val bodyString = if (responseBody != null) {
                try {
                    val source = responseBody.source()
                    source.request(Long.MAX_VALUE)
                    val buffer = source.buffer.clone()
                    
                    val contentType = responseBody.contentType()
                    val charset = contentType?.charset(Charset.forName(UTF8)) ?: Charset.forName(UTF8)
                    
                    if (isPlaintext(buffer)) {
                        buffer.readString(charset)
                    } else {
                        "[Бинарные данные, размер: ${responseBody.contentLength()} байт]"
                    }
                } catch (e: Exception) {
                    "[Ошибка чтения тела ответа: ${e.message}]"
                }
            } else {
                null
            }
            
            // Логируем в файл
            networkLogger.logResponse(url, responseCode, headers, bodyString, duration)
            
            // Показываем уведомление
            notificationService.showResponseToast(url, responseCode, duration)
            
            // Анализируем ответ для определения этапа авторизации
            analyzeResponse(url, responseCode, bodyString)
        }
    }
    
    /**
     * Анализ ответа для определения состояния авторизации
     */
    private suspend fun analyzeResponse(url: String, responseCode: Int, body: String?) {
        if (responseCode != 200 || body.isNullOrEmpty()) {
            if (responseCode >= 400) {
                networkLogger.logError("HTTP ошибка $responseCode для $url")
                notificationService.showErrorNotification("HTTP $responseCode")
            }
            return
        }
        
        when {
            url.contains("game.php") -> {
                when {
                    body.contains("show_warn", ignoreCase = true) -> {
                        // Извлекаем сообщение об ошибке
                        val errorPattern = Regex("show_warn\\s*\\(\\s*[\"']([^\"']+)[\"']\\s*\\)")
                        val errorMatch = errorPattern.find(body)
                        val errorMessage = errorMatch?.groupValues?.get(1) ?: "Неизвестная ошибка авторизации"
                        
                        networkLogger.logError("Ошибка авторизации: $errorMessage")
                        notificationService.showErrorNotification("Ошибка: $errorMessage")
                    }
                    body.contains("Cookie...", ignoreCase = true) -> {
                        networkLogger.logAuthStep("Проблема с cookies, требуется повторная загрузка")
                        notificationService.showAuthStepNotification("Проблема с cookies")
                    }
                    body.contains("flashvars", ignoreCase = true) -> {
                        // Проверяем, требуется ли Flash пароль
                        val flashPattern = Regex("flashvars=\"plid=(\\d+)\"")
                        val flashMatch = flashPattern.find(body)
                        if (flashMatch != null) {
                            val playerId = flashMatch.groupValues[1]
                            networkLogger.logAuthStep("Требуется Flash пароль для игрока ID: $playerId")
                            notificationService.showAuthStepNotification("Требуется Flash пароль")
                        }
                    }
                    body.contains("<canvas", ignoreCase = true) || body.length > 10000 -> {
                        networkLogger.logAuthStep("Успешный вход в игру - загружен игровой контент")
                        notificationService.showAuthStepNotification("✅ Успешный вход в игру!")
                    }
                }
            }
            url.contains("main.php") -> {
                when {
                    body.contains("Вход", ignoreCase = true) || body.contains("Авторизация", ignoreCase = true) -> {
                        networkLogger.logAuthStep("Сессия не активна - требуется авторизация")
                        notificationService.showAuthStepNotification("Требуется авторизация")
                    }
                    body.contains("Локация", ignoreCase = true) || body.contains("Персонаж", ignoreCase = true) -> {
                        networkLogger.logAuthStep("Авторизация успешна - игрок в игре")
                        notificationService.showAuthStepNotification("✅ Игрок авторизован")
                    }
                }
            }
            url.endsWith("neverlands.ru/") || url.contains("index.cgi") -> {
                if (body.contains("auth_form", ignoreCase = true) || body.contains("player_nick", ignoreCase = true)) {
                    networkLogger.logAuthStep("Загружена страница авторизации")
                    notificationService.showAuthStepNotification("Страница авторизации загружена")
                }
            }
        }
    }
    
    /**
     * Проверка, является ли контент текстовым
     */
    private fun isPlaintext(buffer: Buffer): Boolean {
        return try {
            val prefix = Buffer()
            val byteCount = minOf(buffer.size, 64)
            buffer.copyTo(prefix, 0, byteCount)
            
            for (i in 0 until 16) {
                if (prefix.exhausted()) {
                    break
                }
                val codePoint = prefix.readUtf8CodePoint()
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}