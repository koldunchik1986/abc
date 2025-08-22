package com.koldunchik1986.ANL.core.encoding

import java.net.URLEncoder
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Кодировщик никнеймов для API запросов
 * Аналог HelperConverters.NickEncode из Windows клиента
 */
@Singleton
class NickEncoder @Inject constructor() {
    
    companion object {
        private val WINDOWS_1251 = Charset.forName("windows-1251")
    }
    
    /**
     * Кодирование никнейма для использования в URL запросах
     * Использует кодировку windows-1251 для русских символов
     */
    fun encode(nick: String): String {
        return try {
            URLEncoder.encode(nick, WINDOWS_1251.name())
        } catch (e: Exception) {
            // Откат к UTF-8 если windows-1251 не работает
            URLEncoder.encode(nick, "UTF-8")
        }
    }
    
    /**
     * Декодирование никнейма из URL
     */
    fun decode(encodedNick: String): String {
        return try {
            java.net.URLDecoder.decode(encodedNick, WINDOWS_1251.name())
        } catch (e: Exception) {
            // Откат к UTF-8 если windows-1251 не работает
            java.net.URLDecoder.decode(encodedNick, "UTF-8")
        }
    }
}