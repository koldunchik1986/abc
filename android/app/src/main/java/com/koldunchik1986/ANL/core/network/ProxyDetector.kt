package com.koldunchik1986.ANL.core.network

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import java.net.Proxy
import java.net.ProxySelector
import java.net.URI

/**
 * Детектор настроек прокси системы
 * Аналог метода DetectProxy() из Windows версии FormProfile.cs
 */
@Singleton
class ProxyDetector @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val GAME_URL = "http://www.neverlands.ru/"
        private const val TAG = "ProxyDetector"
    }
    
    /**
     * Определение настроек прокси для игрового сервера
     * Аналог DetectProxy() из Windows FormProfile.cs
     */
    suspend fun detectSystemProxy(): ProxySettings? {
        return withContext(Dispatchers.IO) {
            try {
                // Получаем системный ProxySelector
                val proxySelector = ProxySelector.getDefault()
                
                if (proxySelector == null) {
                    return@withContext null
                }
                
                // Проверяем прокси для игрового URL
                val gameUri = URI.create(GAME_URL)
                val proxyList = proxySelector.select(gameUri)
                
                if (proxyList.isEmpty()) {
                    return@withContext null
                }
                
                // Ищем HTTP прокси
                val httpProxy = proxyList.find { proxy ->
                    proxy.type() == Proxy.Type.HTTP
                }
                
                if (httpProxy == null || httpProxy == Proxy.NO_PROXY) {
                    return@withContext null
                }
                
                // Извлекаем адрес и порт прокси
                val socketAddress = httpProxy.address()
                if (socketAddress != null) {
                    val address = socketAddress.toString()
                    // Формат: /hostname:port, убираем "/" в начале
                    val cleanAddress = address.removePrefix("/")
                    
                    return@withContext ProxySettings(
                        address = cleanAddress,
                        isDetected = true
                    )
                }
                
                return@withContext null
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Failed to detect system proxy", e)
                return@withContext null
            }
        }
    }
    
    /**
     * Проверка доступности прокси
     */
    suspend fun testProxyConnection(proxyAddress: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (proxyAddress.isBlank()) {
                    return@withContext false
                }
                
                // TODO: Реализовать тестирование подключения к прокси
                // Пока просто проверяем формат адреса
                val parts = proxyAddress.split(":")
                if (parts.size != 2) {
                    return@withContext false
                }
                
                val host = parts[0].trim()
                val port = parts[1].trim().toIntOrNull()
                
                return@withContext host.isNotBlank() && port != null && port in 1..65535
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Failed to test proxy connection", e)
                return@withContext false
            }
        }
    }
}

/**
 * Настройки прокси
 * Аналог данных из Windows DetectProxy()
 */
data class ProxySettings(
    val address: String,
    val username: String = "",
    val password: String = "",
    val isDetected: Boolean = false
)