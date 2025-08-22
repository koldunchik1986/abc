package ru.neverlands.abclient.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Модель информации о пользователе
 * Эквивалент UserInfo из Windows версии
 */
@Parcelize
data class UserInfo(
    // Основная информация о персонаже
    var nick: String = "",
    var level: String = "",
    var align: String = "",
    var sex: String = "",
    var location: String = "",
    var fightLog: String = "",
    
    // Информация о клане
    var clanCode: String = "",
    var clanSign: String = "",
    var clanName: String = "",
    var clanStatus: String = "",
    
    // Статусы
    var disabled: Boolean = false,
    var jailed: Boolean = false,
    var online: Boolean = false,
    var chatMuted: String = "",
    var forumMuted: String = "",
    
    // HP/MP информация
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
     * Проверяет, активен ли персонаж
     */
    fun isActive(): Boolean = !disabled && !jailed
    
    /**
     * Проверяет, есть ли у персонаж клан
     */
    fun hasGuild(): Boolean = clanCode.isNotEmpty() && clanName.isNotEmpty()
    
    /**
     * Получает процент HP
     */
    fun getHpPercentage(): Int {
        return if (hpMax > 0) (hpCur * 100) / hpMax else 0
    }
    
    /**
     * Получает процент MP
     */
    fun getMpPercentage(): Int {
        return if (maMax > 0) (maCur * 100) / maMax else 0
    }
    
    /**
     * Проверяет, жив ли персонаж
     */
    fun isAlive(): Boolean = hpCur > 0
    
    /**
     * Проверяет, есть ли активные эффекты
     */
    fun hasActiveEffects(): Boolean = effectsCodes.isNotEmpty()
    
    /**
     * Получает количество экипированных предметов
     */
    fun getEquippedItemsCount(): Int {
        return slotsCodes.count { code ->
            code.isNotEmpty() && !code.startsWith("sl_")
        }
    }
    
    /**
     * Проверяет, экипирован ли предмет в указанном слоте
     */
    fun isSlotEquipped(slotIndex: Int): Boolean {
        return slotIndex in slotsCodes.indices && 
               slotsCodes[slotIndex].isNotEmpty() && 
               !slotsCodes[slotIndex].startsWith("sl_")
    }
    
    /**
     * Получает название предмета в указанном слоте
     */
    fun getSlotItemName(slotIndex: Int): String {
        return if (slotIndex in slotsNames.indices) {
            slotsNames[slotIndex]
        } else {
            ""
        }
    }
    
    /**
     * Получает статус онлайн как строку
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
     * Получает информацию о положении в игровом мире
     */
    fun getLocationInfo(): String {
        return location.ifEmpty { "Неизвестно" }
    }
    
    /**
     * Проверяет, может ли персонаж общаться в чате
     */
    fun canChat(): Boolean = chatMuted.isEmpty() || chatMuted == "0"
    
    /**
     * Проверяет, может ли персонаж писать на форуме
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