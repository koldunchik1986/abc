package com.koldunchik1986.ANL.core.browser

import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Менеджер эмуляции браузера
 * Отвечает за настройку HTTP заголовков и WebView для имитации обычного браузера
 */
@Singleton
class BrowserEmulationManager @Inject constructor(
    private val context: Context
) {
    
    companion object {
        // User-Agent для совместимости с Windows версией (критически важно!)
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
        
        // Стандартные заголовки браузера (согласно эталону)
        private val BROWSER_HEADERS = mapOf(
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language" to "ru-RU,ru;q=0.9,en;q=0.8",
            "Accept-Encoding" to "gzip, deflate",
            "Connection" to "keep-alive"
        )
    }
    
    /**
     * Настройка WebView для имитации обычного браузера
     */
    fun configureWebView(webView: WebView) {
        val settings = webView.settings
        
        // Включаем JavaScript
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.allowFileAccessFromFileURLs = false
        settings.allowUniversalAccessFromFileURLs = false
        
        // Настройки кэша
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        // setAppCacheEnabled больше не используется в новых версиях Android
        
        // User-Agent
        settings.userAgentString = USER_AGENT
        
        // Настройки загрузки изображений
        settings.loadsImagesAutomatically = true
        settings.blockNetworkImage = false
        settings.blockNetworkLoads = false
        
        // Настройки масштабирования
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        
        // Настройки текста
        settings.textZoom = 100
        settings.minimumFontSize = 8
        
        // Настройки нескольких окон
        settings.setSupportMultipleWindows(false)
        
        // Настройки медиа
        settings.mediaPlaybackRequiresUserGesture = false
        
        // Настройки смешанного контента
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
    }
    
    /**
     * Создание interceptor для OkHttp, имитирующего заголовки браузера
     */
    fun createBrowserHeadersInterceptor(): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val requestBuilder = originalRequest.newBuilder()
            
            // Добавляем User-Agent если его нет
            if (originalRequest.header("User-Agent") == null) {
                requestBuilder.header("User-Agent", USER_AGENT)
            }
            
            // Добавляем стандартные заголовки браузера
            BROWSER_HEADERS.forEach { (name, value) ->
                if (originalRequest.header(name) == null) {
                    requestBuilder.header(name, value)
                }
            }
            
            // Критически важный Referer для neverlands.ru (согласно эталону)
            if (originalRequest.url.host.contains("neverlands.ru")) {
                if (originalRequest.header("Referer") == null) {
                    requestBuilder.header("Referer", "http://www.neverlands.ru/")
                }
            }
            
            chain.proceed(requestBuilder.build())
        }
    }
    
    /**
     * Добавление дополнительных заголовков (упрощено)
     */
    private fun addRandomHeaders(builder: Request.Builder, originalRequest: Request) {
        // X-Requested-With для AJAX запросов
        if (originalRequest.url.pathSegments.any { it.contains("ajax") }) {
            builder.header("X-Requested-With", "XMLHttpRequest")
        }
    }
    
    /**
     * Генерация человеческой задержки для запросов
     */
    fun getHumanLikeDelay(): Long {
        val random = Random()
        // Задержка от 100ms до 2000ms для имитации человеческой задержки
        return (100 + random.nextInt(1900)).toLong()
    }
    
    /**
     * Проверка, является ли URL игровым сервером
     */
    fun isGameServerUrl(url: String): Boolean {
        return url.contains("neverlands.ru", ignoreCase = true)
    }
    
    /**
     * Получение User-Agent строки
     */
    fun getUserAgent(): String = USER_AGENT
}