package com.koldunchik1986.ANL.ui.tabs.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Типы вкладок в приложении
 * Эквивалент TabType из Windows версии
 */
enum class TabType {
    GAME,           // Основная игровая вкладка
    FORUM,          // Форум
    PINFO,          // Информация о персонаже
    FIGHT_LOG,      // Логи боев
    CHAT,           // Чат
    TODAY_CHAT,     // Сегодняшний чат
    NOTEPAD,        // Блокнот
    CUSTOM          // Пользовательская вкладка
}

/**
 * Модель вкладки
 * Эквивалент TabClass из Windows версии
 */
@Parcelize
data class GameTab(
    val id: String,
    val type: TabType,
    val title: String,
    val url: String? = null,
    val isActive: Boolean = false,
    val isCloseable: Boolean = true,
    val hasNewContent: Boolean = false,
    val lastVisited: Long = System.currentTimeMillis(),
    val favicon: String? = null
) : Parcelable {
    
    companion object {
        /**
         * Создает главную игровую вкладку
         */
        fun createGameTab(): GameTab {
            return GameTab(
                id = "game_main",
                type = TabType.GAME,
                title = "Игра",
                url = "http://www.neverlands.ru/",
                isActive = true,
                isCloseable = false
            )
        }
        
        /**
         * Создает вкладку форума
         */
        fun createForumTab(url: String): GameTab {
            return GameTab(
                id = "forum_${System.currentTimeMillis()}",
                type = TabType.FORUM,
                title = "Форум",
                url = url,
                isCloseable = true
            )
        }
        
        /**
         * Создает вкладку информации о персонаже
         */
        fun createPInfoTab(nick: String, url: String): GameTab {
            return GameTab(
                id = "pinfo_${nick}_${System.currentTimeMillis()}",
                type = TabType.PINFO,
                title = "Информация: $nick",
                url = url,
                isCloseable = true
            )
        }
        
        /**
         * Создает вкладку чата
         */
        fun createChatTab(): GameTab {
            return GameTab(
                id = "chat_main",
                type = TabType.CHAT,
                title = "Чат",
                isCloseable = true
            )
        }
        
        /**
         * Создает вкладку блокнота
         */
        fun createNotepadTab(): GameTab {
            return GameTab(
                id = "notepad_main",
                type = TabType.NOTEPAD,
                title = "Блокнот",
                isCloseable = true
            )
        }
        
        /**
         * Создает пользовательскую вкладку
         */
        fun createCustomTab(title: String, url: String): GameTab {
            return GameTab(
                id = "custom_${System.currentTimeMillis()}",
                type = TabType.CUSTOM,
                title = title,
                url = url,
                isCloseable = true
            )
        }
    }
    
    /**
     * Проверяет, является ли вкладка игровой
     */
    fun isGameTab(): Boolean = type == TabType.GAME
    
    /**
     * Проверяет, требует ли вкладка WebView
     */
    fun requiresWebView(): Boolean {
        return when (type) {
            TabType.GAME, TabType.FORUM, TabType.PINFO, TabType.FIGHT_LOG, TabType.CUSTOM -> true
            TabType.CHAT, TabType.TODAY_CHAT, TabType.NOTEPAD -> false
        }
    }
    
    /**
     * Получает базовый URL для типа вкладки
     */
    fun getBaseUrl(): String? {
        return when (type) {
            TabType.GAME -> "http://www.neverlands.ru/"
            TabType.FORUM -> "http://forum.neverlands.ru/"
            TabType.PINFO -> "http://www.neverlands.ru/pinfo.cgi?"
            TabType.FIGHT_LOG -> "http://www.neverlands.ru/logs.fcg?fid="
            else -> url
        }
    }
}
