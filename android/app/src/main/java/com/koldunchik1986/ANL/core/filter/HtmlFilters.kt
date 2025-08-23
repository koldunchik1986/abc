package com.koldunchik1986.ANL.core.filter

import android.util.Log
import kotlinx.coroutines.runBlocking
import com.koldunchik1986.ANL.core.helpers.StringHelpers
import com.koldunchik1986.ANL.data.model.UserProfile
import com.koldunchik1986.ANL.data.preferences.UserPreferencesManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Фильтры для HTML страниц
 * Аналог Windows PostFilter для обработки HTML
 */
@Singleton
class HtmlFilters @Inject constructor(
    private val userPreferencesManager: UserPreferencesManager
) {
    
    companion object {
        private const val TAG = "HtmlFilters"
        private const val CHARSET_WINDOWS_1251 = "windows-1251"
        
        // DOCTYPE регулярное выражение для удаления (для совместимости)
        private val DOC_TYPE_REGEX = Regex("""<!DOCTYPE[^>]*?(?:\[[^\]]*\])?>""")
    }
    
    /**
     * Получение HTML заголовка
     */
    fun getHtmlHead(): String {
        return """<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=windows-1251">
    <title>Neverlands</title>
</head>
<body>"""
    }
    
    /**
     * Получение текущего профиля пользователя
     */
    private fun getCurrentProfile(): UserProfile? {
        return runBlocking {
            userPreferencesManager.getCurrentProfile()
        }
    }

    /**
     * Обработка index.cgi - главная страница
     * Аналог IndexCgi() из Windows клиента
     */
    fun processIndexCgi(data: ByteArray): ByteArray {
        return try {
            var html = String(data, charset(CHARSET_WINDOWS_1251))
            
            // Удаляем DOCTYPE для совместимости
            html = removeDoctype(html)
            
            // Добавляем мета-теги для корректного отображения
            html = html.replace(
                "<head>",
                """<head><meta http-equiv="X-UA-Compatible" content="IE=edge">"""
            )
            
            html.toByteArray(charset(CHARSET_WINDOWS_1251))
        } catch (e: Exception) {
            Log.e(TAG, "Error processing index.cgi", e)
            data
        }
    }

    /**
     * Обработка pinfo.cgi - информация о персонаже
     * Аналог Pinfo() из Windows клиента
     */
    fun processPlayerInfo(data: ByteArray): ByteArray {
        return try {
            var html = String(data, charset(CHARSET_WINDOWS_1251))
            
            // Удаляем DOCTYPE
            html = removeDoctype(html)
            
            // Парсим информацию о персонаже
            html = parsePlayerInformation(html)
            
            html.toByteArray(charset(CHARSET_WINDOWS_1251))
        } catch (e: Exception) {
            Log.e(TAG, "Error processing pinfo.cgi", e)
            data
        }
    }

    /**
     * Парсинг информации о персонаже
     */
    private fun parsePlayerInformation(html: String): String {
        var result = html
        
        // Извлекаем JavaScript данные о персонаже
        val presentsData = StringHelpers.subString(html, "var presents = [", "];")
        val hpmpData = StringHelpers.subString(html, "var hpmp = [", "];")
        val paramsData = StringHelpers.subString(html, "var params = [", "];")
        val slotsData = StringHelpers.subString(html, "var slots = [", "];")
        
        if (!presentsData.isNullOrEmpty()) {
            Log.d(TAG, "Player presents data found")
        }
        
        if (!hpmpData.isNullOrEmpty()) {
            // Парсим HP/MP данные
            val hpmpValues = parseHpMpData(hpmpData)
            if (hpmpValues.isNotEmpty()) {
                Log.d(TAG, "Player HP/MP: $hpmpValues")
            }
        }
        
        if (!paramsData.isNullOrEmpty()) {
            // Парсим параметры персонажа (сила, ловкость, и т.д.)
            val playerParams = parsePlayerParams(paramsData)
            Log.d(TAG, "Player params parsed: ${playerParams.size} categories")
        }
        
        if (!slotsData.isNullOrEmpty()) {
            // Парсим слоты экипировки
            val equipment = parseEquipmentSlots(slotsData)
            Log.d(TAG, "Equipment slots parsed: ${equipment.size} items")
        }
        
        return result
    }

    /**
     * Парсинг HP/MP данных
     */
    private fun parseHpMpData(hpmpData: String): List<Int> {
        return try {
            hpmpData.split(",").mapNotNull { it.trim().toIntOrNull() }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing HP/MP data", e)
            emptyList()
        }
    }

    /**
     * Парсинг параметров персонажа
     */
    private fun parsePlayerParams(paramsData: String): List<Map<String, Any>> {
        val params = mutableListOf<Map<String, Any>>()
        
        try {
            // Парсим многоуровневый массив параметров персонажа
            val parsed = StringHelpers.parseJsString(paramsData)
            parsed?.forEach { paramGroup ->
                if (paramGroup.size >= 3) {
                    val paramMap = mapOf<String, Any>(
                        "name" to paramGroup[0],
                        "value1" to (paramGroup.getOrNull(1)?.toIntOrNull() ?: 0),
                        "value2" to (paramGroup.getOrNull(2)?.toIntOrNull() ?: 0)
                    )
                    params.add(paramMap)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing player params", e)
        }
        
        return params
    }

    /**
     * Парсинг слотов экипировки
     */
    private fun parseEquipmentSlots(slotsData: String): List<Map<String, String>> {
        val equipment = mutableListOf<Map<String, String>>()
        
        try {
            // Парсим строки слотов экипировки
            val slots = slotsData.split("@")
            
            for (slot in slots) {
                if (slot.contains(":")) {
                    val parts = slot.split(":")
                    if (parts.size >= 2) {
                        val itemMap: Map<String, String> = mapOf(
                            "image" to parts[0],
                            "name" to parts[1],
                            "stats" to (parts.getOrNull(2) ?: "")
                        )
                        equipment.add(itemMap)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing equipment slots", e)
        }
        
        return equipment
    }

    /**
     * Обработка pbots.cgi - информация о ботах
     * Аналог отсутствует в Windows клиенте
     */
    fun processPlayerBots(data: ByteArray): ByteArray {
        return try {
            var html = String(data, charset(CHARSET_WINDOWS_1251))
            
            // Удаляем DOCTYPE
            html = removeDoctype(html)
            
            html.toByteArray(charset(CHARSET_WINDOWS_1251))
        } catch (e: Exception) {
            Log.e(TAG, "Error processing pbots.cgi", e)
            data
        }
    }

    /**
     * Обработка форума
     * Аналог отсутствует в Windows клиенте
     */
    fun processForum(data: ByteArray): ByteArray {
        return try {
            var html = String(data, charset(CHARSET_WINDOWS_1251))
            
            // Удаляем DOCTYPE
            html = removeDoctype(html)
            
            // Добавляем кастомные стили для мобильного отображения
            html = html.replace(
                "</head>",
                """<style>
                   .mobile-responsive { max-width: 100%; overflow-x: auto; }
                   .forum-post { word-wrap: break-word; }
                   </style></head>"""
            )
            
            html.toByteArray(charset(CHARSET_WINDOWS_1251))
        } catch (e: Exception) {
            Log.e(TAG, "Error processing forum", e)
            data
        }
    }

    /**
     * Удаление DOCTYPE для совместимости с мобильным WebView
     */
    private fun removeDoctype(html: String): String {
        return html.replace(DOC_TYPE_REGEX, "")
    }
}
