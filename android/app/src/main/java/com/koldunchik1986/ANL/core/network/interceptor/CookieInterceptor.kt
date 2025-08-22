package com.koldunchik1986.ANL.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Интерцептор для обработки cookies
 * Дополнительный интерцептор для управления специальными случаями cookies
 */
class CookieInterceptor : Interceptor {
    
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()
        
        // Добавляем заголовки для игровых серверов
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
        
        // Проверяем, содержит ли ответ указание на необходимость обновления cookies
        val responseBody = response.peekBody(8192)
        val content = responseBody.string()
        
        if (content.contains("Cookie...", ignoreCase = true)) {
            // Нужно обновить cookies - повторяем запрос
            return chain.proceed(request)
        }
        
        return response
    }
    
    /**
     * Проверяем, относится ли URL к игровым серверам
     */
    private fun isGameServerUrl(url: String): Boolean {
        return url.contains("neverlands.ru", ignoreCase = true)
    }
}