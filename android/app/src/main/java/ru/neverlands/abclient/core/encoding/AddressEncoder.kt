package ru.neverlands.abclient.core.encoding

import java.net.URLEncoder
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Кодировщик адресов для HTTP запросов
 * Эквивалент HelperConverters.AddressEncode из Windows версии
 */
@Singleton
class AddressEncoder @Inject constructor() {
    
    companion object {
        private val WINDOWS_1251 = Charset.forName("windows-1251")
    }
    
    /**
     * Кодирует URL адрес для использования в HTTP запросах
     * Обрабатывает русские символы в URL согласно стандартам игрового сервера
     */
    fun encode(address: String): String {
        return try {
            // Разбираем URL на части
            val uri = java.net.URI(address)
            val scheme = uri.scheme
            val host = uri.host
            val port = if (uri.port != -1) ":${uri.port}" else ""
            val path = uri.path ?: ""
            val query = uri.query ?: ""
            val fragment = uri.fragment ?: ""
            
            // Кодируем только те части, которые могут содержать русские символы
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
            // Если не удается распарсить как URI, просто возвращаем исходный адрес
            address
        }
    }
    
    /**
     * Кодирует путь URL
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
     * Кодирует query string
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
     * Проверяет, содержит ли строка кириллические символы
     */
    private fun containsCyrillic(text: String): Boolean {
        return text.any { char ->
            char in 'А'..'я' || char in 'Ё'..'ё'
        }
    }
    
    /**
     * Декодирует URL адрес
     */
    fun decode(encodedAddress: String): String {
        return try {
            java.net.URLDecoder.decode(encodedAddress, WINDOWS_1251.name())
        } catch (e: Exception) {
            encodedAddress
        }
    }
}