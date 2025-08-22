package ru.neverlands.abclient.core.filter

import android.util.Log
import kotlinx.coroutines.runBlocking
import ru.neverlands.abclient.core.helpers.StringHelpers
import ru.neverlands.abclient.data.model.UserProfile
import ru.neverlands.abclient.data.preferences.UserPreferencesManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Фильтры для HTML страниц
 * Аналоги методов из Windows PostFilter для обработки HTML
 */
@Singleton
class HtmlFilters @Inject constructor(
    private val userPreferencesManager: UserPreferencesManager
) {
    
    companion object {
        private const val TAG = "HtmlFilters"
        private const val CHARSET_WINDOWS_1251 = "windows-1251"
        
        // DOCTYPE регулярное выражение для удаления (исправлено экранирование)
        private val DOC_TYPE_REGEX = Regex("""<!DOCTYPE[^>]*?(?:\[[^\]]*\])?>""")
    }
    
    /**
     * Получает текущий профиль пользователя
     */
    private fun getCurrentProfile(): UserProfile? {
        return runBlocking {
            userPreferencesManager.getCurrentProfile()
        }
    }

    /**
     * Обработка index.cgi - главная страница
     * Аналог IndexCgi() из Windows версии
     */
    fun processIndexCgi(data: ByteArray): ByteArray {
        return try {
            var html = String(data, charset(CHARSET_WINDOWS_1251))
            
            // Удаляем DOCTYPE для совместимости
            html = removeDoctype(html)
            
            // Добавляем метатеги для улучшенной совместимости
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
     * Аналог Pinfo() из Windows версии
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
            // Парсим HP/MP информацию
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
            // Парсим экипировку
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
            // Парсим многомерный массив параметров персонажа
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
            // Парсим строку слотов экипировки
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
     * Аналог аналогичного метода из Windows версии
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
     * Аналог обработки forum.neverlands.ru из Windows версии
     */
    fun processForum(data: ByteArray): ByteArray {
        return try {
            var html = String(data, charset(CHARSET_WINDOWS_1251))
            
            // Удаляем DOCTYPE для совместимости
            html = removeDoctype(html)
            
            html.toByteArray(charset(CHARSET_WINDOWS_1251))
        } catch (e: Exception) {
            Log.e(TAG, "Error processing forum", e)
            data
        }
    }

    /**
     * Удаление DOCTYPE из HTML
     * Аналог RemoveDoctype() из Windows версии
     */
    private fun removeDoctype(html: String): String {
        return DOC_TYPE_REGEX.replace(html, "")
    }

    /**
     * Генерирует стандартный HTML заголовок
     * Аналог HelperErrors.Head() из Windows версии
     */
    fun getHtmlHead(): String {
        return """
            <html><head>
            <META Http-Equiv="Cache-Control" Content="No-Cache">
            <META Http-Equiv="Pragma" Content="No-Cache">
            <META Http-Equiv="Expires" Content="0">
            <style type="text/css">
            body {
              font-family:Tahoma, Verdana, Arial, Helvetica, sans-serif;
              font-size:11px;
              text-decoration:none;
              color:black;
              background-color:#F5F5F5;
              margin:10px;
            }
            .massm {
              background-color:#FFCCCC;
              color:#CC0000;
              font-weight:bold;
              border:1px solid #CC0000;
              padding:2px;
            }
            .nickname {
              font-family:Tahoma, Verdana, Arial, Helvetica, sans-serif;
              font-size:11px;
              font-weight:bold;
              color:#000000;
            }
            .weaponch {
              font-family:Tahoma, Verdana, Arial, Helvetica, sans-serif;
              font-size:11px;
              color:#000000;
            }
            </style>
            </head><body>
        """.trimIndent()
    }

    /**
     * Генерирует маркер ABClient
     * Аналог HelperErrors.Marker() из Windows версии
     */
    fun getAbclientMarker(): String {
        return """<SPAN class=massm>&nbsp;ABClient&nbsp;</SPAN> """
    }

    /**
     * Обработка ошибок HTTP
     */
    fun processHttpError(errorCode: Int, errorMessage: String): ByteArray {
        val html = buildString {
            append(getHtmlHead())
            append(getAbclientMarker())
            append("<h2>Ошибка HTTP $errorCode</h2>")
            append("<p>$errorMessage</p>")
            append("<p>Попробуйте обновить страницу или обратитесь к администратору.</p>")
            append("</body></html>")
        }
        
        return html.toByteArray(charset(CHARSET_WINDOWS_1251))
    }

    /**
     * Обработка страницы с сообщением
     */
    fun processMessagePage(title: String, message: String, autoRedirect: String? = null): ByteArray {
        val html = buildString {
            append(getHtmlHead())
            append(getAbclientMarker())
            append("<h2>$title</h2>")
            append("<p>$message</p>")
            
            if (!autoRedirect.isNullOrEmpty()) {
                append("""<script language="JavaScript">""")
                append("""setTimeout(function() { window.location = "$autoRedirect"; }, 3000);""")
                append("</script>")
                append("<p><i>Автоматическое перенаправление через 3 секунды...</i></p>")
            }
            
            append("</body></html>")
        }
        
        return html.toByteArray(charset(CHARSET_WINDOWS_1251))
    }

    /**
     * Обработка страницы логина
     */
    fun processLoginPage(data: ByteArray): ByteArray {
        return try {
            var html = String(data, charset(CHARSET_WINDOWS_1251))
            
            // Улучшения для страницы входа
            html = html.replace(
                "<head>",
                """<head><meta http-equiv="X-UA-Compatible" content="IE=edge">"""
            )
            
            // Добавляем автозаполнение если включено
            val currentProfile = getCurrentProfile()
            if (currentProfile?.userAutoLogon == true && currentProfile.userNick.isNotEmpty()) {
                html = addAutoLoginScript(html, currentProfile)
            }
            
            html.toByteArray(charset(CHARSET_WINDOWS_1251))
        } catch (e: Exception) {
            Log.e(TAG, "Error processing login page", e)
            data
        }
    }

    /**
     * Добавляет скрипт автоматического входа
     */
    private fun addAutoLoginScript(html: String, userProfile: UserProfile): String {
        val autoLoginScript = """
            <script type="text/javascript">
            window.onload = function() {
                var nickField = document.querySelector('input[name="player_nick"]');
                var passField = document.querySelector('input[name="player_password"]');
                
                if (nickField && passField) {
                    nickField.value = '${userProfile.userNick}';
                    if ('${userProfile.userPassword}'.length > 0) {
                        passField.value = '${userProfile.userPassword}';
                        
                        // Автоматическая отправка формы через 3 секунды
                        setTimeout(function() {
                            document.querySelector('form').submit();
                        }, 3000);
                    }
                }
            };
            </script>
        """.trimIndent()
        
        return html.replace("</head>", "$autoLoginScript</head>")
    }

    /**
     * Обработка страницы с капчей
     */
    fun processCaptchaPage(data: ByteArray): ByteArray {
        return try {
            var html = String(data, charset(CHARSET_WINDOWS_1251))
            
            // Добавляем уведомление о капче
            val captchaNotification = """
                <div style="background-color: #ffcccc; border: 1px solid #cc0000; padding: 10px; margin: 10px;">
                    <strong>ABClient:</strong> Обнаружена капча. Пожалуйста, введите код вручную.
                </div>
            """.trimIndent()
            
            html = html.replace("<body>", "<body>$captchaNotification")
            
            html.toByteArray(charset(CHARSET_WINDOWS_1251))
        } catch (e: Exception) {
            Log.e(TAG, "Error processing captcha page", e)
            data
        }
    }

    /**
     * Инъекция JavaScript для расширенной функциональности
     */
    fun injectAbclientScripts(html: String): String {
        val abclientScript = """
            <script type="text/javascript">
            // ABClient JavaScript extensions
            window.abclient = {
                version: '1.0.0',
                platform: 'Android',
                
                // Функции для взаимодействия с нативным кодом
                log: function(message) {
                    console.log('[ABClient] ' + message);
                },
                
                // Уведомления пользователю
                notify: function(title, message) {
                    // Здесь можно добавить вызов нативного уведомления
                    this.log('Notify: ' + title + ' - ' + message);
                },
                
                // Получение настроек пользователя
                getUserSetting: function(key) {
                    // Заглушка для получения настроек
                    return null;
                },
                
                // Сохранение настроек пользователя
                setUserSetting: function(key, value) {
                    // Заглушка для сохранения настроек
                }
            };
            
            // Инициализация ABClient
            window.abclient.log('ABClient initialized');
            </script>
        """.trimIndent()
        
        return html.replace("</head>", "$abclientScript</head>")
    }
}