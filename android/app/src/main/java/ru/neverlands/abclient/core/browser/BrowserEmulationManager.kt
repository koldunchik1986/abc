package ru.neverlands.abclient.core.browser

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
 * Отвечает за настройку HTTP клиента и WebView для имитации обычного браузера
 */
@Singleton
class BrowserEmulationManager @Inject constructor(
    private val context: Context
) {
    
    companion object {
        // User-Agent для эмуляции Chrome на Android
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 13; SM-G981B) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Mobile Safari/537.36"
        
        // Стандартные заголовки браузера
        private val BROWSER_HEADERS = mapOf(
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9," +
                    "image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
            "Accept-Language" to "ru-RU,ru;q=0.9,en;q=0.8",
            "Accept-Encoding" to "gzip, deflate, br",
            "DNT" to "1",
            "Connection" to "keep-alive",
            "Upgrade-Insecure-Requests" to "1",
            "Sec-Fetch-Dest" to "document",
            "Sec-Fetch-Mode" to "navigate",
            "Sec-Fetch-Site" to "none",
            "Sec-Fetch-User" to "?1"
        )
    }
    
    /**
     * Настройка WebView для максимального сходства с обычным браузером
     */
    fun configureWebView(webView: WebView) {
        val settings = webView.settings
        
        // Основные настройки
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.allowFileAccessFromFileURLs = false
        settings.allowUniversalAccessFromFileURLs = false
        
        // Настройки кэша
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        // setAppCacheEnabled удален в новых версиях Android
        
        // User-Agent
        settings.userAgentString = USER_AGENT
        
        // Настройки загрузки контента
        settings.loadsImagesAutomatically = true
        settings.blockNetworkImage = false
        settings.blockNetworkLoads = false
        
        // Поддержка зума
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        
        // Настройки текста
        settings.textZoom = 100
        settings.minimumFontSize = 8
        
        // Поддержка многих окон
        settings.setSupportMultipleWindows(false)
        
        // Настройки медиа
        settings.mediaPlaybackRequiresUserGesture = false
        
        // Смешанный контент
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
    }
    
    /**
     * Создает interceptor для OkHttp, добавляющий браузерные заголовки
     */
    fun createBrowserHeadersInterceptor(): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val requestBuilder = originalRequest.newBuilder()
            
            // Добавляем User-Agent если его нет
            if (originalRequest.header("User-Agent") == null) {
                requestBuilder.header("User-Agent", USER_AGENT)
            }
            
            // Добавляем стандартные браузерные заголовки
            BROWSER_HEADERS.forEach { (name, value) ->
                if (originalRequest.header(name) == null) {
                    requestBuilder.header(name, value)
                }
            }
            
            // Добавляем случайные заголовки для большей реалистичности
            addRandomHeaders(requestBuilder, originalRequest)
            
            chain.proceed(requestBuilder.build())
        }
    }
    
    /**
     * Добавляет случайные заголовки для имитации реального браузера
     */
    private fun addRandomHeaders(builder: Request.Builder, originalRequest: Request) {
        val random = Random()
        
        // Случайный порядок заголовков Cache-Control
        if (originalRequest.header("Cache-Control") == null && random.nextBoolean()) {
            val cacheValues = listOf("no-cache", "max-age=0", "no-store")
            builder.header("Cache-Control", cacheValues.random())
        }
        
        // Referer для neverlands.ru запросов
        if (originalRequest.url.host.contains("neverlands.ru")) {
            if (originalRequest.header("Referer") == null) {
                builder.header("Referer", "http://www.neverlands.ru/")
            }
        }
        
        // X-Requested-With для AJAX запросов
        if (originalRequest.url.pathSegments.any { it.contains("ajax") }) {
            builder.header("X-Requested-With", "XMLHttpRequest")
        }
    }
    
    /**
     * Генерирует реалистичную задержку между запросами
     */
    fun getHumanLikeDelay(): Long {
        val random = Random()
        // Задержка от 100ms до 2000ms для имитации человеческого поведения
        return (100 + random.nextInt(1900)).toLong()
    }
    
    /**
     * Проверяет, является ли URL игровым сервером
     */
    fun isGameServerUrl(url: String): Boolean {
        return url.contains("neverlands.ru", ignoreCase = true)
    }
    
    /**
     * Получает User-Agent строку
     */
    fun getUserAgent(): String = USER_AGENT
}