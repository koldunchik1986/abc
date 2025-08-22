package ru.neverlands.abclient.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Интерцептор для обработки cookies
 * Дополняет стандартный CookieJar для специальной обработки игровых cookies
 */
class CookieInterceptor : Interceptor {
    
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()
        
        // Специальная обработка для игровых запросов
        val request = if (isGameServerUrl(url)) {
            originalRequest.newBuilder()
                .addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                .addHeader("Pragma", "no-cache")
                .addHeader("Expires", "0")
                .build()
        } else {
            originalRequest
        }
        
        val response = chain.proceed(request)
        
        // Проверяем, требует ли сервер повторную авторизацию
        val responseBody = response.peekBody(8192)
        val content = responseBody.string()
        
        if (content.contains("Cookie...", ignoreCase = true)) {
            // Сервер просит обновить cookies - делаем повторный запрос
            return chain.proceed(request)
        }
        
        return response
    }
    
    /**
     * Проверяет, является ли URL игровым сервером
     */
    private fun isGameServerUrl(url: String): Boolean {
        return url.contains("neverlands.ru", ignoreCase = true)
    }
}