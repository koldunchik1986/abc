package ru.neverlands.abclient.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.nio.charset.Charset

/**
 * Интерцептор для специальной обработки запросов к игровому серверу
 * Обрабатывает кодировку и специфичные для игры заголовки
 */
class GameServerInterceptor : Interceptor {
    
    companion object {
        private val WINDOWS_1251 = Charset.forName("windows-1251")
        private val UTF_8 = Charset.forName("UTF-8")
    }
    
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()
        
        // Специальная обработка для neverlands.ru
        val request = if (isNeverlandsUrl(url)) {
            originalRequest.newBuilder()
                .addHeader("Accept-Language", "ru-RU,ru;q=0.9,en;q=0.8")
                .addHeader("Accept-Encoding", "gzip, deflate")
                .addHeader("Connection", "keep-alive")
                .build()
        } else {
            originalRequest
        }
        
        val response = chain.proceed(request)
        
        // Обрабатываем кодировку ответа для игровых страниц
        return if (isNeverlandsUrl(url) && isHtmlContent(response)) {
            processGameServerResponse(response)
        } else {
            response
        }
    }
    
    /**
     * Проверяет, является ли URL сайтом neverlands
     */
    private fun isNeverlandsUrl(url: String): Boolean {
        return url.contains("neverlands.ru", ignoreCase = true)
    }
    
    /**
     * Проверяет, является ли контент HTML
     */
    private fun isHtmlContent(response: Response): Boolean {
        val contentType = response.header("Content-Type") ?: ""
        return contentType.contains("text/html", ignoreCase = true)
    }
    
    /**
     * Обрабатывает ответ игрового сервера
     */
    private fun processGameServerResponse(response: Response): Response {
        // Для игрового сервера часто нужна кодировка windows-1251
        val contentType = response.header("Content-Type") ?: ""
        
        return if (!contentType.contains("charset", ignoreCase = true)) {
            // Если charset не указан, принудительно устанавливаем windows-1251
            response.newBuilder()
                .header("Content-Type", "$contentType; charset=windows-1251")
                .build()
        } else {
            response
        }
    }
}