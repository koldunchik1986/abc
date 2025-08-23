package com.koldunchik1986.ANL.core.network.cookie

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import com.koldunchik1986.ANL.data.preferences.UserPreferencesManager
import java.net.URLDecoder
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Менеджер cookies - аналог CookiesManager из Windows клиента
 * Отвечает за сохранение и восстановление cookies для игровых сессий
 */
@Singleton
class GameCookieManager @Inject constructor(
    private val context: Context,
    private val preferencesManager: UserPreferencesManager
) : CookieJar {
    
    // Хранилище cookies в памяти
    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()
    
    companion object {
        private const val NEVER_NICK_COOKIE = "NeverNick"
        private const val NEVERLANDS_HOST = "neverlands.ru"
        private const val WWW_NEVERLANDS_HOST = "www.neverlands.ru"
        private const val FORUM_NEVERLANDS_HOST = "forum.neverlands.ru"
    }
    
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = getHostKey(url.host)
        
        // Сохраняем cookies в памяти
        val hostCookies = cookieStore.getOrPut(host) { mutableListOf() }
        
        cookies.forEach { newCookie ->
            // Удаляем старые cookies с тем же именем
            hostCookies.removeAll { it.name == newCookie.name }
            hostCookies.add(newCookie)
            
            // Обрабатываем специальный NeverNick cookie
            if (newCookie.name == NEVER_NICK_COOKIE && host.contains(NEVERLANDS_HOST)) {
                handleNeverNickCookie(newCookie)
            }
        }
        
        // Сохраняем в постоянное хранилище
        saveCookiesToStorage(host, hostCookies)
    }
    
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = getHostKey(url.host)
        val hostCookies = cookieStore[host] ?: loadCookiesFromStorage(host)
        
        // Фильтруем только действующие cookies для данного URL
        return hostCookies?.filter { cookie ->
            cookie.matches(url) && (cookie.expiresAt == Long.MAX_VALUE || cookie.expiresAt > System.currentTimeMillis())
        } ?: emptyList()
    }
    
    /**
     * Обрабатываем NeverNick cookie для проверки аутентификации
     */
    private fun handleNeverNickCookie(cookie: Cookie) {
        try {
            val encodedNick = cookie.value
            // Декодируем как это делается в Windows версии (windows-1251)
            val decodedNick = URLDecoder.decode(encodedNick, "windows-1251")
            
            // Проверяем соответствие с текущим пользователем (аналог CookiesManager.cs)
            val currentUserNick = preferencesManager.getCurrentUserNick()
            if (currentUserNick != null && !decodedNick.equals(currentUserNick, ignoreCase = true)) {
                throw SecurityException("Неверное имя или пароль.")
            }
            
            // Сохраняем имя пользователя
            preferencesManager.setCurrentUserNick(decodedNick)
            
        } catch (e: Exception) {
            e.printStackTrace()
            // Критическая ошибка аутентификации
            if (e is SecurityException) {
                throw e
            }
        }
    }
    
    /**
     * Получаем ключ хоста для хранения cookies
     */
    private fun getHostKey(host: String): String {
        // Сводим все cookies к одному хосту для игровых серверов
        return when {
            host.equals(FORUM_NEVERLANDS_HOST, ignoreCase = true) -> WWW_NEVERLANDS_HOST
            host.equals(NEVERLANDS_HOST, ignoreCase = true) -> WWW_NEVERLANDS_HOST
            else -> host.lowercase()
        }
    }
    
    /**
     * Сохраняем cookies в постоянное хранилище
     */
    private fun saveCookiesToStorage(host: String, cookies: List<Cookie>) {
        try {
            val cookieStrings = cookies.map { "${it.name}=${it.value};${it.domain};${it.path};${it.expiresAt}" }
            preferencesManager.saveCookies(host, cookieStrings)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Загружаем cookies из постоянного хранилища
     */
    private fun loadCookiesFromStorage(host: String): MutableList<Cookie> {
        return try {
            val cookieStrings = preferencesManager.loadCookies(host)
            val cookies = mutableListOf<Cookie>()
            
            cookieStrings.forEach { cookieString ->
                parseCookieFromString(cookieString, host)?.let { cookie ->
                    if (cookie.expiresAt == Long.MAX_VALUE || cookie.expiresAt > System.currentTimeMillis()) {
                        cookies.add(cookie)
                    }
                }
            }
            
            // Сохраняем в памяти
            cookieStore[host] = cookies
            cookies
        } catch (e: Exception) {
            e.printStackTrace()
            mutableListOf()
        }
    }
    
    /**
     * Парсим cookie из строки
     */
    private fun parseCookieFromString(cookieString: String, host: String): Cookie? {
        return try {
            val parts = cookieString.split(";")
            if (parts.size >= 4) {
                val nameValue = parts[0].split("=", limit = 2)
                if (nameValue.size == 2) {
                    Cookie.Builder()
                        .name(nameValue[0])
                        .value(nameValue[1])
                        .domain(parts[1])
                        .path(parts[2])
                        .expiresAt(parts[3].toLongOrNull() ?: Long.MAX_VALUE)
                        .build()
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Очищаем все cookies игры
     */
    fun clearGameCookies() {
        val gameHosts = listOf(WWW_NEVERLANDS_HOST, NEVERLANDS_HOST, FORUM_NEVERLANDS_HOST)
        gameHosts.forEach { host ->
            cookieStore.remove(host)
            preferencesManager.clearCookies(host)
        }
    }
    
    /**
     * Получаем текущий NeverNick cookie
     */
    fun getCurrentNeverNick(): String? {
        val cookies = cookieStore[WWW_NEVERLANDS_HOST]
        return cookies?.find { it.name == NEVER_NICK_COOKIE }?.value
    }
    
    /**
     * Проверяем, авторизован ли пользователь
     */
    fun isAuthenticated(): Boolean {
        return getCurrentNeverNick() != null
    }
    
    /**
     * Получаем все cookies для отладки
     */
    fun getAllCookies(): Map<String, List<Cookie>> {
        return cookieStore.toMap()
    }
}