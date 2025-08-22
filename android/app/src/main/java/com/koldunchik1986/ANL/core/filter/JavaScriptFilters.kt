package com.koldunchik1986.ANL.core.filter

import android.util.Log
import kotlinx.coroutines.runBlocking
import com.koldunchik1986.ANL.core.helpers.StringHelpers
import com.koldunchik1986.ANL.data.model.UserProfile
import com.koldunchik1986.ANL.data.preferences.UserPreferencesManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Фильтры для JavaScript файлов
 * Аналог Windows PostFilter для обработки JS
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
     * Получение текущего профиля пользователя
     */
    private fun getCurrentProfile(): UserProfile? {
        return runBlocking {
            userPreferencesManager.getCurrentProfile()
        }
    }

    /**
     * Обработка hp.js
     * Аналог HpJs() из Windows клиента
     */
    fun processHpJs(data: ByteArray): ByteArray {
        return try {
            val html = String(data, charset(CHARSET_WINDOWS_1251))
            // Пока не требуется специальная обработка hp.js
            data
        } catch (e: Exception) {
            Log.e(TAG, "Error processing hp.js", e)
            data
        }
    }

    /**
     * Обработка map.js - карта игрового мира
     * Аналог MapJs() из Windows клиента
     */
    fun processMapJs(data: ByteArray): ByteArray {
        return try {
            // В Windows клиенте используется Resources.map
            // Здесь нужно добавить кастомный JavaScript для карты
            val customMapScript = """
                // Custom ABClient map script
                var map_scale = 1;
                var map_width = 800;
                var map_height = 600;
                // Добавляем кастомные функции для карты...
            """.trimIndent()
            
            customMapScript.toByteArray(charset(CHARSET_WINDOWS_1251))
        } catch (e: Exception) {
            Log.e(TAG, "Error processing map.js", e)
            data
        }
    }

    /**
     * Обработка arena.js
     * Аналог ArenaJs() из Windows клиента
     */
    fun processArenaJs(data: ByteArray): ByteArray {
        return try {
            val html = String(data, charset(CHARSET_WINDOWS_1251))
            // Добавляем JSON2 библиотеку как в Windows клиенте
            val modifiedHtml = "var JSON=JSON||{}; $html"
            modifiedHtml.toByteArray(charset(CHARSET_WINDOWS_1251))
        } catch (e: Exception) {
            Log.e(TAG, "Error processing arena.js", e)
            data
        }
    }

    /**
     * Обработка game.js
     * Аналог GameJs() из Windows клиента
     */
    fun processGameJs(data: ByteArray): ByteArray {
        return try {
            val html = String(data, charset(CHARSET_WINDOWS_1251))
            // Пока не требуется специальная обработка игровых JS
            data
        } catch (e: Exception) {
            Log.e(TAG, "Error processing game.js", e)
            data
        }
    }

    /**
     * Обработка pinfo_v01.js - информация о персонаже
     * Аналог PinfoJs() из Windows клиента
     */
    fun processPinfoJs(data: ByteArray): ByteArray {
        return try {
            var html = String(data, charset(CHARSET_WINDOWS_1251))
            
            // Заменяем вызовы как в Windows клиенте
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
     * Аналог FightJs() из Windows клиента
     */
    fun processFightJs(data: ByteArray): ByteArray {
        return try {
            val html = String(data, charset(CHARSET_WINDOWS_1251))
            // Добавляем специальные обработчики для боя
            data
        } catch (e: Exception) {
            Log.e(TAG, "Error processing fight.js", e)
            data
        }
    }

    /**
     * Обработка building*.js - строения
     * Аналог BuildingJs() из Windows клиента
     */
    fun processBuildingJs(data: ByteArray): ByteArray {
        return try {
            var html = String(data, charset(CHARSET_WINDOWS_1251))
            // Добавляем JSON2 библиотеку для строений
            html = "var JSON=JSON||{}; $html"
            html.toByteArray(charset(CHARSET_WINDOWS_1251))
        } catch (e: Exception) {
            Log.e(TAG, "Error processing building.js", e)
            data
        }
    }

    /**
     * Обработка hpmp.js - здоровье и мана
     * Аналог HpmpJs() из Windows клиента
     */
    fun processHpmpJs(data: ByteArray): ByteArray {
        return try {
            // В Windows клиенте используется кастомный скрипт
            val customHpmpScript = """
                // Custom ABClient HPMP script
                function updateHpMp(hp, mp) {
                    // Обновляем отображение HP/MP
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
     * Аналог ChMsgJs() из Windows клиента
     */
    fun processChMsgJs(data: ByteArray): ByteArray {
        return try {
            val html = String(data, charset(CHARSET_WINDOWS_1251))
            // Добавляем специальные обработчики для чата
            data
        } catch (e: Exception) {
            Log.e(TAG, "Error processing ch_msg.js", e)
            data
        }
    }

    /**
     * Обработка pv.js - информация о персонажах
     * Аналог PvJs() из Windows клиента
     */
    fun processPvJs(data: ByteArray): ByteArray {
        return try {
            var html = String(data, charset(CHARSET_WINDOWS_1251))
            
            // Заменяем вызовы как в Windows клиенте
            html = html.replace("'%clan% '", "'%clan%'")
            
            html.toByteArray(charset(CHARSET_WINDOWS_1251))
        } catch (e: Exception) {
            Log.e(TAG, "Error processing pv.js", e)
            data
        }
    }

    /**
     * Обработка ch_list.js - список чата
     * Аналог ChListJs() из Windows клиента
     */
    fun processChListJs(data: ByteArray): ByteArray {
        return try {
            // В Windows клиенте используется кастомный скрипт
            val customChListScript = """
                // Custom ABClient chat list script
                function refreshChatList() {
                    // Обновляем отображение списка чата
                }
            """.trimIndent()
            
            customChListScript.toByteArray(charset(CHARSET_WINDOWS_1251))
        } catch (e: Exception) {
            Log.e(TAG, "Error processing ch_list.js", e)
            data
        }
    }

    /**
     * Обработка svitok.js - свиток
     * Аналог SvitokJs() из Windows клиента
     */
    fun processSvitokJs(data: ByteArray): ByteArray {
        return try {
            var html = String(data, charset(CHARSET_WINDOWS_1251))
            
            // Заменяем вызовы как в Windows клиенте
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
     * Обработка slots.js - слоты
     * Аналог SlotsJs() из Windows клиента
     */
    fun processSlotsJs(data: ByteArray): ByteArray {
        return try {
            val html = String(data, charset(CHARSET_WINDOWS_1251))
            // Добавляем специальные обработчики для слотов
            data
        } catch (e: Exception) {
            Log.e(TAG, "Error processing slots.js", e)
            data
        }
    }

    /**
     * Обработка logs*.js - логи
     * Аналог LogsJs() из Windows клиента
     */
    fun processLogsJs(data: ByteArray): ByteArray {
        return try {
            val html = String(data, charset(CHARSET_WINDOWS_1251))
            // Добавляем специальные обработчики для логов
            data
        } catch (e: Exception) {
            Log.e(TAG, "Error processing logs.js", e)
            data
        }
    }

    /**
     * Обработка shop*.js - магазин
     * Аналог ShopJs() из Windows клиента
     */
    fun processShopJs(data: ByteArray): ByteArray {
        return try {
            var html = String(data, charset(CHARSET_WINDOWS_1251))
            
            // Добавляем специальные обработчики для магазина
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
     * Обработка forum_topic.js - тема форума
     * Аналог ForumTopicJs() из Windows клиента
     */
    fun processForumTopicJs(data: ByteArray): ByteArray {
        return try {
            val html = String(data, charset(CHARSET_WINDOWS_1251))
            // Добавляем специальные обработчики для темы форума
            data
        } catch (e: Exception) {
            Log.e(TAG, "Error processing forum_topic.js", e)
            data
        }
    }
}
