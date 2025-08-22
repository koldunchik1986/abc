package ru.neverlands.abclient.core.encoding

import java.net.URLEncoder
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Кодировщик никнеймов для API запросов
 * Эквивалент HelperConverters.NickEncode из Windows версии
 */
@Singleton
class NickEncoder @Inject constructor() {
    
    companion object {
        private val WINDOWS_1251 = Charset.forName("windows-1251")
    }
    
    /**
     * Кодирует никнейм для использования в URL запросах
     * Использует кодировку windows-1251 как в оригинальной версии
     */
    fun encode(nick: String): String {
        return try {
            URLEncoder.encode(nick, WINDOWS_1251.name())
        } catch (e: Exception) {
            // Fallback to UTF-8 if windows-1251 fails
            URLEncoder.encode(nick, "UTF-8")
        }
    }
    
    /**
     * Декодирует никнейм из URL
     */
    fun decode(encodedNick: String): String {
        return try {
            java.net.URLDecoder.decode(encodedNick, WINDOWS_1251.name())
        } catch (e: Exception) {
            // Fallback to UTF-8 if windows-1251 fails
            java.net.URLDecoder.decode(encodedNick, "UTF-8")
        }
    }
}