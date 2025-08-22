package com.koldunchik1986.ANL.core.network

import android.content.Context
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.logging.HttpLoggingInterceptor
import com.koldunchik1986.ANL.core.browser.BrowserEmulationManager
import com.koldunchik1986.ANL.core.network.interceptor.CookieInterceptor
import com.koldunchik1986.ANL.core.network.interceptor.GameServerInterceptor
import com.koldunchik1986.ANL.core.api.NeverlandsApiClient
import com.koldunchik1986.ANL.core.filter.HttpFilter
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HTTP клиент с поддержкой cookies и игровой эмуляцией
 * Аналог CookieAwareWebClient из Windows клиента
 */
@Singleton
class GameHttpClient @Inject constructor(
    private val context: Context,
    private val browserEmulationManager: BrowserEmulationManager,
    private val cookieJar: CookieJar,
    private val httpFilter: HttpFilter
) {
    
    private val client: OkHttpClient by lazy {
        createHttpClient()
    }
    
    /**
     * Создание настроенного HTTP клиента
     */
    private fun createHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .cookieJar(cookieJar)
        
        // Interceptor для добавления заголовков браузера
        builder.addInterceptor(browserEmulationManager.createBrowserHeadersInterceptor())
        
        // Interceptor для обработки игровых запросов
        builder.addInterceptor(GameServerInterceptor())
        
        // Interceptor для работы с cookies
        builder.addInterceptor(CookieInterceptor())
        
        // PostFilter system - обработка запросов к neverlands.ru
        builder.addInterceptor(httpFilter)
        
        // Логирование в debug режиме
        try {
            val debugField = Class.forName("com.koldunchik1986.ANL.BuildConfig").getField("DEBUG"))
            val isDebug = debugField.getBoolean(null)
            if (isDebug) {
                val loggingInterceptor = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.HEADERS
                }
                builder.addInterceptor(loggingInterceptor)
            }
        } catch (e: Exception) {
            // Если BuildConfig недоступен, продолжаем без логирования
        }
        
        return builder.build()
    }
    
    /**
     * Выполнение GET запроса
     */
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): Response {
        val requestBuilder = Request.Builder().url(url).get()
        
        // Добавляем пользовательские заголовки
        headers.forEach { (name, value) ->
            requestBuilder.header(name, value)
        }
        
        return executeWithDelay(requestBuilder.build())
    }
    
    /**
     * Выполнение POST запроса
     */
    suspend fun post(
        url: String, 
        body: RequestBody,
        headers: Map<String, String> = emptyMap()
    ): Response {
        val requestBuilder = Request.Builder().url(url).post(body)
        
        headers.forEach { (name, value) ->
            requestBuilder.header(name, value)
        }
        
        return executeWithDelay(requestBuilder.build())
    }
    
    /**
     * Выполнение запроса с человекоподобными задержками
     */
    private suspend fun executeWithDelay(request: Request): Response {
        // Добавляем задержку для игровых запросов
        if (browserEmulationManager.isGameServerUrl(request.url.toString())) {
            kotlinx.coroutines.delay(browserEmulationManager.getHumanLikeDelay())
        }
        
        return client.newCall(request).execute()
    }
    
    /**
     * Создание RequestBody для form запроса
     */
    fun createFormBody(params: Map<String, String>): RequestBody {
        val formBuilder = FormBody.Builder()
        params.forEach { (key, value) ->
            formBuilder.add(key, value)
        }
        return formBuilder.build()
    }
    
    /**
     * Получение информации о пользователе через API
     */
    suspend fun getUserInfo(nick: String): String? {
        return try {
            val encodedNick = java.net.URLEncoder.encode(nick, "windows-1251")
            val url = "http://www.neverlands.ru/modules/api/getid.cgi?$encodedNick"
            
            val response = get(url)
            if (response.isSuccessful) {
                response.body?.string()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Получение деталей игрока через API
     */
    suspend fun getPlayerDetails(playerId: String): String? {
        return try {
            val url = "http://www.neverlands.ru/modules/api/info.cgi?" +
                    "playerid=$playerId&info=1&hmu=1&effects=1&slots=1"
            
            val response = get(url)
            if (response.isSuccessful) {
                response.body?.string()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Загрузка данных в виде byte array
     * Аналог DownloadData из Windows клиента
     */
    suspend fun downloadData(url: String, headers: Map<String, String> = emptyMap()): ByteArray? {
        return try {
            val response = get(url, headers)
            if (response.isSuccessful) {
                response.body?.bytes()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Загрузка данных в виде строки с автоматическим определением кодировки
     * Аналог DownloadString из Windows клиента
     */
    suspend fun downloadString(url: String, headers: Map<String, String> = emptyMap()): String? {
        return try {
            val response = get(url, headers)
            if (response.isSuccessful) {
                // Определение кодировки ответа для корректной обработки windows-1251 от neverlands.ru
                val contentType = response.header("Content-Type") ?: ""
                val charset = when {
                    contentType.contains("charset=windows-1251", ignoreCase = true) -> "windows-1251"
                    contentType.contains("charset=", ignoreCase = true) -> {
                        // Извлечение charset из Content-Type
                        val charsetMatch = Regex("charset=([^;\\s]+)").find(contentType)
                        charsetMatch?.groupValues?.get(1) ?: "UTF-8"
                    }
                    browserEmulationManager.isGameServerUrl(url) -> "windows-1251"
                    else -> "UTF-8"
                }
                
                val bytes = response.body?.bytes()
                if (bytes != null) {
                    String(bytes, charset(charset))
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Выполнение POST запроса с form-encoded данными
     */
    suspend fun postForm(
        url: String,
        formData: Map<String, String>,
        headers: Map<String, String> = emptyMap()
    ): Response {
        val body = createFormBody(formData)
        return post(url, body, headers)
    }
    
    /**
     * Выполнение POST запроса с текстовым содержимым
     */
    suspend fun postString(
        url: String,
        content: String,
        contentType: String = "text/plain",
        headers: Map<String, String> = emptyMap()
    ): Response {
        val mediaType = contentType.toMediaType()
        val body = RequestBody.create(mediaType, content)
        return post(url, body, headers)
    }
    
    /**
     * Получение информации о комнате
     */
    suspend fun getRoomInfo(): String? {
        return try {
            val url = "http://www.neverlands.ru/ch.php?lo=1&"
            
            val response = get(url)
            if (response.isSuccessful) {
                response.body?.string()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Закрытие HTTP клиента
     */
    fun close() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}
