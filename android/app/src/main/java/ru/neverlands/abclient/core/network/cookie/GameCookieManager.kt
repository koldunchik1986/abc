package ru.neverlands.abclient.core.network.cookie

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import ru.neverlands.abclient.data.preferences.UserPreferencesManager
import java.net.URLDecoder
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Менеджер cookies - эквивалент CookiesManager из Windows версии
 * Отвечает за сохранение и управление cookies для аутентификации
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
        
        // Сохраняем cookies в память
        val hostCookies = cookieStore.getOrPut(host) { mutableListOf() }
        
        cookies.forEach { newCookie ->
            // Удаляем старые cookies с тем же именем
            hostCookies.removeAll { it.name == newCookie.name }
            hostCookies.add(newCookie)
            
            // Специальная обработка NeverNick cookie
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
        
        // Фильтруем только валидные cookies для данного URL
        return hostCookies?.filter { cookie ->
            cookie.matches(url) && (cookie.expiresAt == Long.MAX_VALUE || cookie.expiresAt > System.currentTimeMillis())
        } ?: emptyList()
    }
    
    /**
     * Обрабатывает NeverNick cookie для проверки аутентификации
     */
    private fun handleNeverNickCookie(cookie: Cookie) {
        try {
            val encodedNick = cookie.value
            val decodedNick = URLDecoder.decode(encodedNick, "windows-1251")
            
            // Проверяем соответствие с текущим пользователем
            val currentUserNick = preferencesManager.getCurrentUserNick()
            if (currentUserNick != null && !decodedNick.equals(currentUserNick, ignoreCase = true)) {
                throw SecurityException("Неверное имя или пароль.")
            }
            
            // Сохраняем подтвержденный ник
            preferencesManager.setCurrentUserNick(decodedNick)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Получает ключ хоста для хранения cookies
     */
    private fun getHostKey(host: String): String {
        // Форум использует те же cookies что и основной сайт
        return when {
            host.equals(FORUM_NEVERLANDS_HOST, ignoreCase = true) -> WWW_NEVERLANDS_HOST
            host.equals(NEVERLANDS_HOST, ignoreCase = true) -> WWW_NEVERLANDS_HOST
            else -> host.lowercase()
        }
    }
    
    /**
     * Сохраняет cookies в постоянное хранилище
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
     * Загружает cookies из постоянного хранилища
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
            
            // Обновляем кэш в памяти
            cookieStore[host] = cookies
            cookies
        } catch (e: Exception) {
            e.printStackTrace()
            mutableListOf()
        }
    }
    
    /**
     * Парсит cookie из строки
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
     * Очищает все cookies игры
     */
    fun clearGameCookies() {
        val gameHosts = listOf(WWW_NEVERLANDS_HOST, NEVERLANDS_HOST, FORUM_NEVERLANDS_HOST)
        gameHosts.forEach { host ->
            cookieStore.remove(host)
            preferencesManager.clearCookies(host)
        }
    }
    
    /**
     * Получает текущий NeverNick cookie
     */
    fun getCurrentNeverNick(): String? {
        val cookies = cookieStore[WWW_NEVERLANDS_HOST]
        return cookies?.find { it.name == NEVER_NICK_COOKIE }?.value
    }
    
    /**
     * Проверяет, аутентифицирован ли пользователь
     */
    fun isAuthenticated(): Boolean {
        return getCurrentNeverNick() != null
    }
    
    /**
     * Получает все cookies для отладки
     */
    fun getAllCookies(): Map<String, List<Cookie>> {
        return cookieStore.toMap()
    }
}