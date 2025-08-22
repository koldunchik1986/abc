package com.koldunchik1986.ANL.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Модель информации о персонаже
 * Эквивалент UserInfo из Windows клиента
 */
@Parcelize
data class UserInfo(
    // Основные параметры персонажа
    var nick: String = "",
    var level: String = "",
    var align: String = "",
    var sex: String = "",
    var location: String = "",
    var fightLog: String = "",
    
    // Гильдия
    var clanCode: String = "",
    var clanSign: String = "",
    var clanName: String = "",
    var clanStatus: String = "",
    
    // Состояния
    var disabled: Boolean = false,
    var jailed: Boolean = false,
    var online: Boolean = false,
    var chatMuted: String = "",
    var forumMuted: String = "",
    
    // HP/MP параметры
    var hpCur: Int = 0,
    var hpMax: Int = 0,
    var maCur: Int = 0,
    var maMax: Int = 0,
    var tied: Int = 0,
    
    // Слоты экипировки
    var slotsCodes: Array<String> = emptyArray(),
    var slotsNames: Array<String> = emptyArray(),
    
    // Эффекты
    var effectsCodes: Array<String> = emptyArray(),
    var effectsNames: Array<String> = emptyArray(),
    var effectsSizes: Array<String> = emptyArray(),
    var effectsLefts: Array<String> = emptyArray()
) : Parcelable {
    
    /**
     * Проверка, активен ли персонаж
     */
    fun isActive(): Boolean = !disabled && !jailed
    
    /**
     * Проверка, состоит ли в гильдии
     */
    fun hasGuild(): Boolean = clanCode.isNotEmpty() && clanName.isNotEmpty()
    
    /**
     * Получение процента HP
     */
    fun getHpPercentage(): Int {
        return if (hpMax > 0) (hpCur * 100) / hpMax else 0
    }
    
    /**
     * Получение процента MP
     */
    fun getMpPercentage(): Int {
        return if (maMax > 0) (maCur * 100) / maMax else 0
    }
    
    /**
     * Проверка, жив ли персонаж
     */
    fun isAlive(): Boolean = hpCur > 0
    
    /**
     * Проверка, есть ли активные эффекты
     */
    fun hasActiveEffects(): Boolean = effectsCodes.isNotEmpty()
    
    /**
     * Получение количества экипированных предметов
     */
    fun getEquippedItemsCount(): Int {
        return slotsCodes.count { code ->
            code.isNotEmpty() && !code.startsWith("sl_")
        }
    }
    
    /**
     * Проверка, экипирован ли предмет в указанном слоте
     */
    fun isSlotEquipped(slotIndex: Int): Boolean {
        return slotIndex in slotsCodes.indices && 
               slotsCodes[slotIndex].isNotEmpty() && 
               !slotsCodes[slotIndex].startsWith("sl_")
    }
    
    /**
     * Получение названия предмета в указанном слоте
     */
    fun getSlotItemName(slotIndex: Int): String {
        return if (slotIndex in slotsNames.indices) {
            slotsNames[slotIndex]
        } else {
            ""
        }
    }
    
    /**
     * Получение текстового статуса онлайн
     */
    fun getOnlineStatusText(): String {
        return when {
            disabled -> "Заблокирован"
            jailed -> "В тюрьме"
            online -> "Онлайн"
            else -> "Оффлайн"
        }
    }
    
    /**
     * Получение информации о местоположении в красивом формате
     */
    fun getLocationInfo(): String {
        return location.ifEmpty { "Неизвестно" }
    }
    
    /**
     * Проверка, может ли персонаж писать в чат
     */
    fun canChat(): Boolean = chatMuted.isEmpty() || chatMuted == "0"
    
    /**
     * Проверка, может ли персонаж писать на форуме
     */
    fun canPost(): Boolean = forumMuted.isEmpty() || forumMuted == "0"
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as UserInfo
        
        if (nick != other.nick) return false
        if (level != other.level) return false
        if (!slotsCodes.contentEquals(other.slotsCodes)) return false
        if (!slotsNames.contentEquals(other.slotsNames)) return false
        if (!effectsCodes.contentEquals(other.effectsCodes)) return false
        if (!effectsNames.contentEquals(other.effectsNames)) return false
        if (!effectsSizes.contentEquals(other.effectsSizes)) return false
        if (!effectsLefts.contentEquals(other.effectsLefts)) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = nick.hashCode()
        result = 31 * result + level.hashCode()
        result = 31 * result + slotsCodes.contentHashCode()
        result = 31 * result + slotsNames.contentHashCode()
        result = 31 * result + effectsCodes.contentHashCode()
        result = 31 * result + effectsNames.contentHashCode()
        result = 31 * result + effectsSizes.contentHashCode()
        result = 31 * result + effectsLefts.contentHashCode()
        return result
    }
}