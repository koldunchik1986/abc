package ru.neverlands.abclient.core.api

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.neverlands.abclient.core.network.GameHttpClient
import ru.neverlands.abclient.core.activity.IdleManager
import ru.neverlands.abclient.core.encoding.NickEncoder
import ru.neverlands.abclient.core.encoding.AddressEncoder
import ru.neverlands.abclient.data.model.UserInfo
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API клиент для взаимодействия с сервером neverlands.ru
 * Эквивалент NeverApi из Windows версии
 */
@Singleton
class NeverlandsApiClient @Inject constructor(
    private val httpClient: GameHttpClient,
    private val idleManager: IdleManager,
    private val nickEncoder: NickEncoder,
    private val addressEncoder: AddressEncoder
) {
    
    // Кэш nick -> id для оптимизации
    private val nameToIdCache = ConcurrentHashMap<String, String>()
    private val cacheMutex = Mutex()
    
    /**
     * Получает ID пользователя по нику
     * Эквивалент GetUserId из Windows версии
     */
    suspend fun getUserId(nick: String): String? {
        // Проверяем кэш
        nameToIdCache[nick]?.let { return it }
        
        val encodedNick = nickEncoder.encode(nick)
        val url = "http://www.neverlands.ru/modules/api/getid.cgi?$encodedNick"
        
        return try {
            idleManager.addActivity()
            
            val response = httpClient.get(url)
            if (!response.isSuccessful) {
                return null
            }
            
            val data = response.body?.string()
            if (data.isNullOrEmpty()) {
                return null
            }
            
            val parts = data.split('|')
            if (parts.size != 2) {
                return null
            }
            
            val id = parts[0]
            val name = parts[1]
            
            // Сохраняем в кэш
            cacheMutex.withLock {
                nameToIdCache[name] = id
            }
            
            id
        } catch (e: Exception) {
            null
        } finally {
            idleManager.removeActivity()
        }
    }
    
    /**
     * Получает полную информацию о пользователе
     * Эквивалент GetAll из Windows версии
     */
    suspend fun getFullUserInfo(nick: String): UserInfo? {
        val userId = getUserId(nick) ?: return null
        
        val url = "http://www.neverlands.ru/modules/api/info.cgi?" +
                "playerid=$userId&info=1&hmu=1&effects=1&slots=1"
        
        return try {
            idleManager.addActivity()
            
            val response = httpClient.get(url)
            if (!response.isSuccessful) {
                return null
            }
            
            val data = response.body?.string()
            if (data.isNullOrEmpty()) {
                return null
            }
            
            parseUserInfo(data)
        } catch (e: Exception) {
            null
        } finally {
            idleManager.removeActivity()
        }
    }
    
    /**
     * Получает информацию профиля
     * Эквивалент GetPInfo из Windows версии
     */
    suspend fun getProfileInfo(nick: String): String? {
        val url = addressEncoder.encode("http://neverlands.ru/pinfo.cgi?$nick")
        return getInfo(url)
    }
    
    /**
     * Получает лог боя
     * Эквивалент GetFlog из Windows версии
     */
    suspend fun getFightLog(flogId: String): String? {
        val url = addressEncoder.encode("http://neverlands.ru/logs.fcg?fid=$flogId")
        return getInfo(url)
    }
    
    /**
     * Универсальный метод получения информации
     * Эквивалент GetInfo из Windows версии
     */
    private suspend fun getInfo(url: String): String? {
        return try {
            idleManager.addActivity()
            
            val response = httpClient.get(url)
            if (!response.isSuccessful) {
                return null
            }
            
            var content = response.body?.string()
            
            // Проверяем на необходимость повторного запроса (как в Windows версии)
            if (content?.contains("Cookie...", ignoreCase = true) == true) {
                val retryResponse = httpClient.get(url)
                if (retryResponse.isSuccessful) {
                    content = retryResponse.body?.string()
                }
            }
            
            content
        } catch (e: Exception) {
            null
        } finally {
            idleManager.removeActivity()
        }
    }
    
    /**
     * Парсит информацию о пользователе из ответа API
     */
    private fun parseUserInfo(data: String): UserInfo? {
        val lines = data.split('\n')
        if (lines.size != 5) return null
        
        try {
            val userInfo = UserInfo()
            
            // Парсим слоты (строка 1)
            if (lines[0].length > 2) {
                val slotsData = lines[0].substring(2).split('@')
                if (slotsData.size >= 16) {
                    userInfo.slotsCodes = Array(16) { "" }
                    userInfo.slotsNames = Array(16) { "" }
                    
                    for (i in 0 until 16) {
                        val slotParts = slotsData[i].split(':')
                        if (slotParts.size >= 2) {
                            userInfo.slotsCodes[i] = slotParts[0]
                            userInfo.slotsNames[i] = slotParts[1]
                        }
                    }
                }
            }
            
            // Парсим эффекты (строка 2)
            if (lines[1].length > 2) {
                val effectsData = lines[1].substring(2).split('@')
                userInfo.effectsCodes = Array(effectsData.size) { "" }
                userInfo.effectsNames = Array(effectsData.size) { "" }
                userInfo.effectsSizes = Array(effectsData.size) { "" }
                userInfo.effectsLefts = Array(effectsData.size) { "" }
                
                for (i in effectsData.indices) {
                    val effectParts = effectsData[i].split('.')
                    if (effectParts.size >= 4) {
                        userInfo.effectsCodes[i] = effectParts[0]
                        userInfo.effectsNames[i] = effectParts[1]
                        userInfo.effectsSizes[i] = effectParts[2]
                        userInfo.effectsLefts[i] = effectParts[3]
                    }
                }
            }
            
            // Парсим основную информацию (строка 3)
            if (lines[2].length > 2) {
                val mainInfo = lines[2].substring(2).split('|')
                if (mainInfo.size >= 14) {
                    userInfo.nick = mainInfo[0].trim()
                    userInfo.level = mainInfo[1]
                    userInfo.align = mainInfo[2]
                    userInfo.clanCode = mainInfo[3]
                    userInfo.clanSign = mainInfo[4]
                    userInfo.clanName = mainInfo[5]
                    userInfo.clanStatus = mainInfo[6]
                    userInfo.sex = mainInfo[7]
                    userInfo.disabled = mainInfo[8] != "0"
                    userInfo.jailed = mainInfo[9] != "0"
                    userInfo.chatMuted = mainInfo[10]
                    userInfo.forumMuted = mainInfo[11]
                    userInfo.online = mainInfo[12] != "0"
                    userInfo.location = mainInfo[13]
                    if (mainInfo.size > 14) {
                        userInfo.fightLog = mainInfo[14]
                    }
                }
            }
            
            // Парсим HP/MP информацию (строка 4)
            if (lines[3].length > 2) {
                val hpMpInfo = lines[3].substring(2).split('|')
                if (hpMpInfo.size >= 5) {
                    userInfo.hpCur = hpMpInfo[0].toIntOrNull() ?: 0
                    userInfo.hpMax = hpMpInfo[1].toIntOrNull() ?: 0
                    userInfo.maCur = hpMpInfo[2].toIntOrNull() ?: 0
                    userInfo.maMax = hpMpInfo[3].toIntOrNull() ?: 0
                    val tied = hpMpInfo[4].toIntOrNull() ?: 0
                    userInfo.tied = 100 - tied
                }
            }
            
            return userInfo
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Очищает кэш nicknames
     */
    fun clearCache() {
        nameToIdCache.clear()
    }
    
    /**
     * Получает размер кэша
     */
    fun getCacheSize(): Int = nameToIdCache.size
}