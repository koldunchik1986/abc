package ru.neverlands.abclient.core.filter

import android.util.Log
import kotlinx.coroutines.runBlocking
import ru.neverlands.abclient.core.helpers.StringHelpers
import ru.neverlands.abclient.data.model.UserProfile
import ru.neverlands.abclient.data.preferences.UserPreferencesManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Фильтры для JavaScript файлов
 * Аналоги методов из Windows PostFilter для обработки JS
 */
@Singleton
class JavaScriptFilters @Inject constructor(
    private val userPreferencesManager: UserPreferencesManager
) {
    
    companion object {
        private const val TAG = "JavaScriptFilters"
        private const val CHARSET_WINDOWS_1251 = "windows-1251"
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
     * Обработка hp.js
     * Аналог HpJs() из Windows версии
     */
    fun processHpJs(data: ByteArray): ByteArray {
        return try {
            val html = String(data, charset(CHARSET_WINDOWS_1251))
            // Здесь можно добавить специфическую обработку hp.js
            data
        } catch (e: Exception) {
            Log.e(TAG, "Error processing hp.js", e)
            data
        }
    }

    /**
     * Обработка map.js - замена на кастомную карту
     * Аналог MapJs() из Windows версии
     */
    fun processMapJs(data: ByteArray): ByteArray {
        return try {
            // В Windows версии возвращается Resources.map
            // Здесь можно вернуть кастомный JavaScript для карты
            val customMapScript = """
                // Custom ABClient map script
                var map_scale = 1;
                var map_width = 800;
                var map_height = 600;
                // Дополнительная логика карты...
            """.trimIndent()
            
            customMapScript.toByteArray(charset(CHARSET_WINDOWS_1251))
        } catch (e: Exception) {
            Log.e(TAG, "Error processing map.js", e)
            data
        }
    }

    /**
     * Обработка arena.js
     * Аналог ArenaJs() из Windows версии
     */
    fun processArenaJs(data: ByteArray): ByteArray {
        return try {
            val html = String(data, charset(CHARSET_WINDOWS_1251))
            // Добавляем JSON2 поддержку как в Windows версии
            val modifiedHtml = "var JSON=JSON||{}; $html"
            modifiedHtml.toByteArray(charset(CHARSET_WINDOWS_1251))
        } catch (e: Exception) {
            Log.e(TAG, "Error processing arena.js", e)
            data
        }
    }

    /**
     * Обработка game.js
     * Аналог GameJs() из Windows версии
     */
    fun processGameJs(data: ByteArray): ByteArray {
        return try {
            val html = String(data, charset(CHARSET_WINDOWS_1251))
            // Здесь можно добавить модификации для основного игрового JS
            data
        } catch (e: Exception) {
            Log.e(TAG, "Error processing game.js", e)
            data
        }
    }

    /**
     * Обработка pinfo_v01.js - информация о персонаже
     * Аналог PinfoJs() из Windows версии
     */
    fun processPinfoJs(data: ByteArray): ByteArray {
        return try {
            var html = String(data, charset(CHARSET_WINDOWS_1251))
            
            // Модификация для подсказок как в Windows версии
            html = html.replace(
                "+alt+",
                "+window.external.InfoToolTip(arr[0],alt)+"
            )
            
            html.toByteArray(charset(CHARSET_WINDOWS_1251))
        } catch (e: Exception) {
            Log.e(TAG, "Error processing pinfo.js", e)
            data
        }
    }

    /**
     * Обработка fight_v*.js - бой
     * Аналог FightJs() из Windows версии
     */
    fun processFightJs(data: ByteArray): ByteArray {
        return try {
            val html = String(data, charset(CHARSET_WINDOWS_1251))
            // Модификации для системы боя
            data
        } catch (e: Exception) {
            Log.e(TAG, "Error processing fight.js", e)
            data
        }
    }

    /**
     * Обработка building*.js - здания
     * Аналог BuildingJs() из Windows версии
     */
    fun processBuildingJs(data: ByteArray): ByteArray {
        return try {
            var html = String(data, charset(CHARSET_WINDOWS_1251))
            // Добавляем JSON2 поддержку для зданий
            html = "var JSON=JSON||{}; $html"
            html.toByteArray(charset(CHARSET_WINDOWS_1251))
        } catch (e: Exception) {
            Log.e(TAG, "Error processing building.js", e)
            data
        }
    }

    /**
     * Обработка hpmp.js - здоровье и мана
     * Аналог HpmpJs() из Windows версии
     */
    fun processHpmpJs(data: ByteArray): ByteArray {
        return try {
            // В Windows версии возвращается кастомный скрипт
            val customHpmpScript = """
                // Custom ABClient HPMP script
                function updateHpMp(hp, mp) {
                    // Логика обновления HP/MP
                }
            """.trimIndent()
            
            customHpmpScript.toByteArray(charset(CHARSET_WINDOWS_1251))
        } catch (e: Exception) {
            Log.e(TAG, "Error processing hpmp.js", e)
            data
        }
    }

    /**
     * Обработка ch_msg_v01.js - сообщения чата
     * Аналог ChMsgJs() из Windows версии
     */
    fun processChMsgJs(data: ByteArray): ByteArray {
        return try {
            val html = String(data, charset(CHARSET_WINDOWS_1251))
            // Модификации для чата
            data
        } catch (e: Exception) {
            Log.e(TAG, "Error processing ch_msg.js", e)
            data
        }
    }

    /**
     * Обработка pv.js - приватные сообщения
     * Аналог PvJs() из Windows версии
     */
    fun processPvJs(data: ByteArray): ByteArray {
        return try {
            var html = String(data, charset(CHARSET_WINDOWS_1251))
            
            // Исправление для клановых сообщений как в Windows версии
            html = html.replace("'%clan% '", "'%clan%'")
            
            html.toByteArray(charset(CHARSET_WINDOWS_1251))
        } catch (e: Exception) {
            Log.e(TAG, "Error processing pv.js", e)
            data
        }
    }

    /**
     * Обработка ch_list.js - список чата
     * Аналог ChListJs() из Windows версии
     */
    fun processChListJs(data: ByteArray): ByteArray {
        return try {
            // В Windows версии возвращается кастомный скрипт списка чата
            val customChListScript = """
                // Custom ABClient chat list script
                function refreshChatList() {
                    // Логика обновления списка чата
                }
            """.trimIndent()
            
            customChListScript.toByteArray(charset(CHARSET_WINDOWS_1251))
        } catch (e: Exception) {
            Log.e(TAG, "Error processing ch_list.js", e)
            data
        }
    }

    /**
     * Обработка svitok.js - свитки
     * Аналог SvitokJs() из Windows версии
     */
    fun processSvitokJs(data: ByteArray): ByteArray {
        return try {
            var html = String(data, charset(CHARSET_WINDOWS_1251))
            
            // Модификация формы передачи свитков как в Windows версии
            html = html.replace(
                """document.all("transfer").innerHTML = '<form action=main.php method=POST><input type=hidden name=magicrestart value="1">""",
                """document.all("transfer").innerHTML = '<form action=main.php method=POST><input type=hidden name=magicrestart value="1">"""
            )
            
            html.toByteArray(charset(CHARSET_WINDOWS_1251))
        } catch (e: Exception) {
            Log.e(TAG, "Error processing svitok.js", e)
            data
        }
    }

    /**
     * Обработка slots.js - слоты экипировки
     * Аналог SlotsJs() из Windows версии
     */
    fun processSlotsJs(data: ByteArray): ByteArray {
        return try {
            val html = String(data, charset(CHARSET_WINDOWS_1251))
            // Модификации для отображения слотов
            data
        } catch (e: Exception) {
            Log.e(TAG, "Error processing slots.js", e)
            data
        }
    }

    /**
     * Обработка logs*.js - логи
     * Аналог LogsJs() из Windows версии
     */
    fun processLogsJs(data: ByteArray): ByteArray {
        return try {
            val html = String(data, charset(CHARSET_WINDOWS_1251))
            // Модификации для логов
            data
        } catch (e: Exception) {
            Log.e(TAG, "Error processing logs.js", e)
            data
        }
    }

    /**
     * Обработка shop*.js - магазин
     * Аналог ShopJs() из Windows версии
     */
    fun processShopJs(data: ByteArray): ByteArray {
        return try {
            var html = String(data, charset(CHARSET_WINDOWS_1251))
            
            // Добавляем поддержку массовой продажи как в Windows версии
            html = html.replace(
                "AjaxPost('shop_ajax.php', data, function(xdata) {",
                "AjaxPost('shop_ajax.php', data, function(xdata){ var arg1 = window.external.BulkSellOldArg1(); var arg2 = window.external.BulkSellOldArg2(); if (arg1 > 0) shop_item_sell(arg1, arg2);"
            )
            
            html.toByteArray(charset(CHARSET_WINDOWS_1251))
        } catch (e: Exception) {
            Log.e(TAG, "Error processing shop.js", e)
            data
        }
    }

    /**
     * Обработка forum_topic.js - форум
     * Аналог ForumTopicJs() из Windows версии
     */
    fun processForumTopicJs(data: ByteArray): ByteArray {
        return try {
            val html = String(data, charset(CHARSET_WINDOWS_1251))
            // Модификации для форума
            data
        } catch (e: Exception) {
            Log.e(TAG, "Error processing forum_topic.js", e)
            data
        }
    }
}