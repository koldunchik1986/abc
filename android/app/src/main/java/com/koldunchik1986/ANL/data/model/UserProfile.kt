package com.koldunchik1986.ANL.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Модель профиля пользователя - эквивалент UserConfig из Windows клиента
 * Хранит все настройки пользователя для сохранения между сессиями
 */
@Parcelize
data class UserProfile(
    // Основные данные профиля
    val id: String = "",
    val userNick: String = "",
    val userPassword: String = "",
    val userPasswordFlash: String = "", // Flash-пароль (дополнительный пароль, если используется)
    val userKey: String = "", // Ключ шифрования для защиты конфиденциальных данных
    val userAutoLogon: Boolean = false,
    
    // Параметры шифрования
    val isEncrypted: Boolean = false,
    val configPassword: String = "", // Пароль для шифрования конфигурации
    val configHash: String = "", // Хэш пароля конфигурации
    
    // Параметры прокси
    val useProxy: Boolean = false,
    val proxyAddress: String = "",
    val proxyUserName: String = "",
    val proxyPassword: String = "",
    
    // Параметры карты
    val mapShowExtended: Boolean = true,
    val mapBigWidth: Int = 800,
    val mapBigHeight: Int = 600,
    val mapBigScale: Float = 1.0f,
    val mapBigTransparency: Float = 1.0f,
    val mapShowBackColorWhite: Boolean = false,
    val mapShowMiniMap: Boolean = true,
    val mapMiniWidth: Int = 200,
    val mapMiniHeight: Int = 150,
    val mapMiniScale: Float = 0.5f,
    val mapLocation: String = "",
    val mapDrawRegion: Boolean = true,
    
    // Параметры лечения
    val cureNV: List<Int> = listOf(0, 0, 0, 0), // Changed from List<String> to List<Int>
    val cureAsk: List<Boolean> = listOf(false, false, false, false),
    val cureAdvanced: Boolean = false,
    val cureAfter: Boolean = false,
    val cureBattle: Boolean = false,
    val cureEnabled: List<Boolean> = listOf(false, false, false, false),
    val cureDisabledLowLevels: Boolean = false,
    
    // Параметры автоответчика
    val doAutoAnswer: Boolean = false,
    val autoAnswer: String = "",
    
    // Параметры рыбалки
    val fishTiedHigh: Int = 60, // Changed from Boolean to Int
    val fishTiedZero: Boolean = false,
    val fishStopOverWeight: Boolean = false,
    val fishAutoWear: Boolean = false,
    val fishHandOne: String = "",
    val fishHandTwo: String = "",
    val fishEnabledPrims: Boolean = false,
    val fishUm: Int = 0, // Changed from Boolean to Int
    val fishMaxLevelBots: Int = 0,
    val fishChatReport: Boolean = false,
    val fishChatReportColor: Boolean = false, // Changed from String to Boolean
    val fishAuto: Boolean = false,
    
    // Параметры разделения
    val razdChatReport: Boolean = false,
    
    // Параметры чата
    val chatKeepGame: Boolean = true,
    val chatKeepMoving: Boolean = true,
    val chatKeepLog: Boolean = true,
    val chatSizeLog: Int = 1000,
    val chatHeight: Int = 200,
    val chatDelay: Int = 500,
    val chatMode: String = "normal",
    val doChatLevels: Boolean = false,
    
    // Параметры форума
    val lightForum: Boolean = false,
    
    // Параметры торговли
    val torgActive: Boolean = false,
    val torgTabl: String = "", // Changed from Boolean to String
    val torgTable: String = "", // Table for trading calculations
    val torgMessageTooExp: String = "",
    val torgMessageAdv: String = "",
    val torgAdvTime: Int = 600,
    val torgMessageThanks: String = "",
    val torgMessageNoMoney: String = "",
    val torgMessageLess90: String = "",
    val torgSliv: Boolean = false,
    val torgMinLevel: Int = 1,
    val torgEx: String = "", // Changed from Boolean to String
    val torgDeny: String = "",
    
    // Параметры окна
    val windowState: WindowState = WindowState.NORMAL,
    val windowLeft: Int = 0,
    val windowTop: Int = 0,
    val windowWidth: Int = 1024,
    val windowHeight: Int = 768,
    
    // Параметры сплиттера
    val splitterCollapsed: Boolean = false,
    val splitterWidth: Int = 200,
    
    // Параметры персонажа
    val persGuamod: Boolean = true,
    val persIntHP: Int = 2000,
    val persIntMA: Int = 9000,
    val persReady: Int = 0,
    val persLogReady: String = "",
    
    // Параметры навигатора
    val navigatorAllowTeleports: Boolean = true,
    
    // Параметры автообновления
    val autoAdvSec: Int = 600,
    val autoAdvPhraz: String = "",
    
    // Прочие параметры
    val doPromptExit: Boolean = true,
    val doTexLog: Boolean = true,
    val notepad: String = "",
    val doTray: Boolean = true,
    val showTrayBalloons: Boolean = true,
    val servDiff: Long = 0, // Разница времени между сервером и клиентом
    
    // Параметры звука
    val soundEnabled: Boolean = true,
    val doPlayAlarm: Boolean = true,
    val doPlayAttack: Boolean = true,
    val doPlayDigits: Boolean = true,
    val doPlayRefresh: Boolean = true,
    val doPlaySndMsg: Boolean = true,
    val doPlayTimer: Boolean = true,
    
    // Параметры инвентаря
    val doInvPack: Boolean = true,
    val doInvPackDolg: Boolean = true,
    val doInvSort: Boolean = true,
    
    // Параметры быстрой атаки
    val doShowFastAttack: Boolean = false,
    val doShowFastAttackBlood: Boolean = true,
    val doShowFastAttackUltimate: Boolean = true,
    val doShowFastAttackClosedUltimate: Boolean = true,
    val doShowFastAttackClosed: Boolean = true,
    val doShowFastAttackFist: Boolean = false,
    val doShowFastAttackClosedFist: Boolean = true,
    val doShowFastAttackOpenNevid: Boolean = true,
    val doShowFastAttackPoison: Boolean = true,
    val doShowFastAttackStrong: Boolean = true,
    val doShowFastAttackNevid: Boolean = true,
    val doShowFastAttackFog: Boolean = true,
    val doShowFastAttackZas: Boolean = true,
    val doShowFastAttackTotem: Boolean = true,
    
    // Дополнительные поля для PostFilter
    var currentHp: Double = 0.0,
    var currentMp: Double = 0.0,
    var lastChatMessage: String = "",
    val tabs: List<String> = emptyList(),
    val favLocations: List<String> = emptyList(),
    
    // Временные метки
    val configLastSaved: Long = System.currentTimeMillis(),
    val lastLogon: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable {
    
    companion object {
        /**
         * Создание нового профиля
         */
        fun createNew(nick: String = ""): UserProfile {
            return UserProfile(
                id = generateProfileId(),
                userNick = nick,
                configLastSaved = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
        
        /**
         * Генерация уникального ID профиля
         */
        private fun generateProfileId(): String {
            return "profile_${System.currentTimeMillis()}_${(Math.random() * 1000).toInt()}"
        }
    }
    
    /**
     * Проверка, заполнены ли данные для входа
     */
    fun isLoginDataComplete(): Boolean {
        return userNick.isNotBlank() && userPassword.isNotBlank()
    }
    
    /**
     * Проверка, настроен ли прокси
     */
    fun isProxyConfigured(): Boolean {
        return useProxy && proxyAddress.isNotBlank()
    }
    
    /**
     * Проверка, защищен ли паролем
     */
    fun isPasswordProtected(): Boolean {
        return isEncrypted && configHash.isNotBlank()
    }
    
    /**
     * Получение отображаемого имени
     */
    fun getDisplayName(): String {
        return if (userNick.isNotBlank()) userNick else "Новый профиль"
    }
    
    /**
     * Обновление временной метки
     */
    fun withUpdatedTimestamp(): UserProfile {
        return copy(
            updatedAt = System.currentTimeMillis(),
            configLastSaved = System.currentTimeMillis()
        )
    }
}

/**
 * Состояние окна
 */
enum class WindowState {
    NORMAL,
    MINIMIZED,
    MAXIMIZED
}