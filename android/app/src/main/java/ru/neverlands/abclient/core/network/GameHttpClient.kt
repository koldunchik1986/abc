package ru.neverlands.abclient.core.network

import android.content.Context
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.logging.HttpLoggingInterceptor
import ru.neverlands.abclient.core.browser.BrowserEmulationManager
import ru.neverlands.abclient.core.network.interceptor.CookieInterceptor
import ru.neverlands.abclient.core.network.interceptor.GameServerInterceptor
import ru.neverlands.abclient.core.api.NeverlandsApiClient
import ru.neverlands.abclient.core.filter.HttpFilter
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HTTP клиент с поддержкой cookies и эмуляцией браузера
 * Эквивалент CookieAwareWebClient из Windows версии
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
     * Создает настроенный HTTP клиент
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
        
        // Interceptor для эмуляции браузера
        builder.addInterceptor(browserEmulationManager.createBrowserHeadersInterceptor())
        
        // Interceptor для обработки игровых запросов
        builder.addInterceptor(GameServerInterceptor())
        
        // Interceptor для работы с cookies
        builder.addInterceptor(CookieInterceptor())
        
        // PostFilter system - модификация ответов neverlands.ru
        builder.addInterceptor(httpFilter)
        
        // Логирование в debug режиме
        try {
            val debugField = Class.forName("ru.neverlands.abclient.BuildConfig").getField("DEBUG")
            val isDebug = debugField.getBoolean(null)
            if (isDebug) {
                val loggingInterceptor = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.HEADERS
                }
                builder.addInterceptor(loggingInterceptor)
            }
        } catch (e: Exception) {
            // Если BuildConfig недоступен, пропускаем логирование
        }
        
        return builder.build()
    }
    
    /**
     * Выполняет GET запрос
     */
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): Response {
        val requestBuilder = Request.Builder().url(url).get()
        
        // Добавляем дополнительные заголовки
        headers.forEach { (name, value) ->
            requestBuilder.header(name, value)
        }
        
        return executeWithDelay(requestBuilder.build())
    }
    
    /**
     * Выполняет POST запрос
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
     * Выполняет запрос с человекоподобной задержкой
     */
    private suspend fun executeWithDelay(request: Request): Response {
        // Добавляем задержку для имитации человеческого поведения
        if (browserEmulationManager.isGameServerUrl(request.url.toString())) {
            kotlinx.coroutines.delay(browserEmulationManager.getHumanLikeDelay())
        }
        
        return client.newCall(request).execute()
    }
    
    /**
     * Создает RequestBody для form данных
     */
    fun createFormBody(params: Map<String, String>): RequestBody {
        val formBuilder = FormBody.Builder()
        params.forEach { (key, value) ->
            formBuilder.add(key, value)
        }
        return formBuilder.build()
    }
    
    /**
     * Получает информацию о игроке через API
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
     * Получает детальную информацию о игроке
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
     * Загружает данные как byte array
     * Эквивалент DownloadData из Windows версии
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
     * Загружает данные как строку с автоматическим определением кодировки
     * Эквивалент DownloadString из Windows версии
     */
    suspend fun downloadString(url: String, headers: Map<String, String> = emptyMap()): String? {
        return try {
            val response = get(url, headers)
            if (response.isSuccessful) {
                // Автоматически определяем кодировку или используем windows-1251 для neverlands.ru
                val contentType = response.header("Content-Type") ?: ""
                val charset = when {
                    contentType.contains("charset=windows-1251", ignoreCase = true) -> "windows-1251"
                    contentType.contains("charset=", ignoreCase = true) -> {
                        // Извлекаем charset из Content-Type
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
     * Выполняет POST запрос с form-encoded данными
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
     * Выполняет POST запрос с произвольным телом
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
     * Получает информацию о текущей комнате
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
     * Закрывает HTTP клиент
     */
    fun close() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}