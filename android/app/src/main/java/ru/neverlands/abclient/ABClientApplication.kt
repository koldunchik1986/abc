package ru.neverlands.abclient

import android.app.Application
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import ru.neverlands.abclient.core.network.ssl.TrustAllCertificates
import ru.neverlands.abclient.core.activity.IdleManager
import javax.inject.Inject

/**
 * Главный класс приложения ABClient
 * Инициализирует все необходимые компоненты для эмуляции браузера
 */
@HiltAndroidApp
class ABClientApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var injectedWorkManagerConfiguration: Configuration
    
    @Inject
    lateinit var idleManager: IdleManager
    
    override val workManagerConfiguration: Configuration
        get() = injectedWorkManagerConfiguration
    
    override fun onCreate() {
        super.onCreate()
        
        // Инициализация WebView для эмуляции браузера
        initializeWebView()
        
        // Настройка SSL для работы с neverlands.ru
        initializeSSL()
        
        // Инициализация Work Manager
        WorkManager.initialize(this, injectedWorkManagerConfiguration)
        
        // Инициализация IdleManager для отслеживания активности
        idleManager.initialize(this)
    }
    
    /**
     * Инициализация WebView с настройками для эмуляции обычного браузера
     */
    private fun initializeWebView() {
        try {
            // Включаем отладку WebView в debug режиме
            try {
                // Используем отражение чтобы проверить debug режим
                val debugField = Class.forName("${packageName}.BuildConfig").getField("DEBUG")
                val isDebug = debugField.getBoolean(null)
                if (isDebug) {
                    WebView.setWebContentsDebuggingEnabled(true)
                }
            } catch (e: Exception) {
                // Если BuildConfig недоступен, игнорируем
            }
            
            // Настраиваем CookieManager для работы с cookies
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(WebView(this), true)
            
        } catch (e: Exception) {
            // Обработка ошибок инициализации WebView
            e.printStackTrace()
        }
    }
    
    /**
     * Настройка SSL для работы с игровым сервером
     * Необходимо для обхода возможных проблем с сертификатами
     */
    private fun initializeSSL() {
        try {
            // Устанавливаем более мягкую политику SSL для совместимости
            TrustAllCertificates.install()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}