package com.koldunchik1986.ANL.core.encoding

import java.net.URLEncoder
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Кодировщик адресов для HTTP запросов
 * Аналог HelperConverters.AddressEncode из Windows клиента
 */
@Singleton
class AddressEncoder @Inject constructor() {
    
    companion object {
        private val WINDOWS_1251 = Charset.forName("windows-1251")
    }
    
    /**
     * Кодирование URL для использования в HTTP запросах
     * Обрабатывает различные части URL с учетом кодировки windows-1251
     */
    fun encode(address: String): String {
        return try {
            // Парсим URL как URI
            val uri = java.net.URI(address)
            val scheme = uri.scheme
            val host = uri.host
            val port = if (uri.port != -1) ":${uri.port}" else ""
            val path = uri.path ?: ""
            val query = uri.query ?: ""
            val fragment = uri.fragment ?: ""
            
            // Кодируем части URI, сохраняя структуру URL
            val encodedPath = if (path.isNotEmpty()) {
                encodePath(path)
            } else {
                path
            }
            
            val encodedQuery = if (query.isNotEmpty()) {
                "?" + encodeQueryString(query)
            } else {
                ""
            }
            
            val encodedFragment = if (fragment.isNotEmpty()) {
                "#" + URLEncoder.encode(fragment, WINDOWS_1251.name())
            } else {
                ""
            }
            
            "$scheme://$host$port$encodedPath$encodedQuery$encodedFragment"
        } catch (e: Exception) {
            // Если не удалось разобрать как URI, возвращаем исходный адрес
            address
        }
    }
    
    /**
     * Кодирование пути URL
     */
    private fun encodePath(path: String): String {
        val pathSegments = path.split("/")
        return pathSegments.joinToString("/") { segment ->
            if (segment.isNotEmpty() && containsCyrillic(segment)) {
                URLEncoder.encode(segment, WINDOWS_1251.name())
            } else {
                segment
            }
        }
    }
    
    /**
     * Кодирование query string
     */
    private fun encodeQueryString(query: String): String {
        val queryParams = query.split("&")
        return queryParams.joinToString("&") { param ->
            val keyValue = param.split("=", limit = 2)
            if (keyValue.size == 2) {
                val key = keyValue[0]
                val value = keyValue[1]
                if (containsCyrillic(value)) {
                    "$key=" + URLEncoder.encode(value, WINDOWS_1251.name())
                } else {
                    param
                }
            } else {
                param
            }
        }
    }
    
    /**
     * Проверка, содержит ли текст кириллические символы
     */
    private fun containsCyrillic(text: String): Boolean {
        return text.any { char ->
            char in 'А'..'Я' || char in 'а'..'я'
        }
    }
    
    /**
     * Декодирование URL строки
     */
    fun decode(encodedAddress: String): String {
        return try {
            java.net.URLDecoder.decode(encodedAddress, WINDOWS_1251.name())
        } catch (e: Exception) {
            encodedAddress
        }
    }
}