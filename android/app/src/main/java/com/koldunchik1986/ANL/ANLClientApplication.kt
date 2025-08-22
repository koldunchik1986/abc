package com.koldunchik1986.ANL

import android.app.Application
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import com.koldunchik1986.ANL.core.network.ssl.TrustAllCertificates
import com.koldunchik1986.ANL.core.activity.IdleManager
import javax.inject.Inject

/**
 * Главный класс приложения ANL-Client
 * Инициализирует все необходимые компоненты при старте приложения
 */
@HiltAndroidApp
class ANLClientApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var injectedWorkManagerConfiguration: Configuration
    
    @Inject
    lateinit var idleManager: IdleManager
    
    override val workManagerConfiguration: Configuration
        get() = injectedWorkManagerConfiguration
    
    override fun onCreate() {
        super.onCreate()
        
        // Инициализация WebView для корректной работы
        initializeWebView()
        
        // Настройка SSL для работы с neverlands.ru
        initializeSSL()
        
        // Инициализация Work Manager
        WorkManager.initialize(this, injectedWorkManagerConfiguration)
        
        // Инициализация IdleManager для отслеживания активности
        idleManager.initialize(this)
    }
    
    /**
     * Инициализация WebView с настройками для корректной работы
     */
    private fun initializeWebView() {
        try {
            // Включение WebView debugging в debug режиме
            try {
                // Проверяем значение debug флага из BuildConfig
                val debugField = Class.forName("${packageName}.BuildConfig").getField("DEBUG")
                val isDebug = debugField.getBoolean(null)
                if (isDebug) {
                    WebView.setWebContentsDebuggingEnabled(true)
                }
            } catch (e: Exception) {
                // Если BuildConfig недоступен, продолжаем
            }
            
            // Инициализация CookieManager для работы с cookies
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(WebView(this), true)
            
        } catch (e: Exception) {
            // Обработка ошибок инициализации WebView
            e.printStackTrace()
        }
    }
    
    /**
     * Настройка SSL для работы с игровыми серверами
     * Необходима для корректного соединения с игровыми серверами
     */
    private fun initializeSSL() {
        try {
            // Установка доверительных сертификатов SSL для соединений
            TrustAllCertificates.install()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}