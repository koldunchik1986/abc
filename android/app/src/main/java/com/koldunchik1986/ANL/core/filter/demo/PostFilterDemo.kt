package com.koldunchik1986.ANL.core.filter.demo

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.koldunchik1986.ANL.core.network.GameHttpClient
import com.koldunchik1986.ANL.data.model.UserProfile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Демонстрационный модуль PostFilter системы
 * Показывает работу фильтров контента с neverlands.ru
 */
@Singleton
class PostFilterDemo @Inject constructor(
    private val gameHttpClient: GameHttpClient,
    private val userProfile: UserProfile
) {
    
    companion object {
        private const val TAG = "PostFilterDemo"
    }

    /**
     * Демонстрация работы фильтров контента
     */
    fun demonstrateFiltering() {
        CoroutineScope(Dispatchers.IO).launch {
            
            Log.d(TAG, "=== PostFilter System Demo ===")
            
            // 1. Демонстрация JavaScript фильтров
            demonstrateJavaScriptFilters()
            
            // 2. Демонстрация PHP фильтров
            demonstratePhpFilters()
            
            // 3. Демонстрация HTML фильтров
            demonstrateHtmlFilters()
            
            // 4. Демонстрация учета пользовательских настроек
            demonstrateUserSettings()
            
            Log.d(TAG, "=== Demo Complete ===")
        }
    }

    /**
     * Демонстрация JavaScript фильтров
     */
    private suspend fun demonstrateJavaScriptFilters() {
        Log.d(TAG, "--- JavaScript Filters ---")
        
        // Загрузка файла карты
        val mapJsResponse = gameHttpClient.get("http://www.neverlands.ru/js/map.js")
        if (mapJsResponse.isSuccessful) {
            val content = mapJsResponse.body?.string() ?: ""
            Log.d(TAG, "Map.js filtered (${content.length} chars): ${content.take(100)}...")
        }
        
        // Загрузка файла информации о персонаже
        val pinfoJsResponse = gameHttpClient.get("http://www.neverlands.ru/js/pinfo_v01.js")
        if (pinfoJsResponse.isSuccessful) {
            val content = pinfoJsResponse.body?.string() ?: ""
            Log.d(TAG, "Pinfo.js filtered: Contains InfoToolTip = ${content.contains("InfoToolTip")}")
        }
        
        // Загрузка файла персонажей
        val pvJsResponse = gameHttpClient.get("http://www.neverlands.ru/js/pv.js")
        if (pvJsResponse.isSuccessful) {
            val content = pvJsResponse.body?.string() ?: ""
            Log.d(TAG, "Pv.js filtered: Clan fix applied = ${!content.contains("'%clan% '")}")
        }
    }

    /**
     * Демонстрация PHP фильтров
     */
    private suspend fun demonstratePhpFilters() {
        Log.d(TAG, "--- PHP Filters ---")
        
        // Загрузка главной игровой страницы
        val mainPhpResponse = gameHttpClient.get("http://www.neverlands.ru/main.php")
        if (mainPhpResponse.isSuccessful) {
            val content = mainPhpResponse.body?.string() ?: ""
            Log.d(TAG, "Main.php filtered (${content.length} chars)")
            
            // Проверка модификации времени ожидания
            val hasWtimeModification = content.contains("Обработка данных...")
            Log.d(TAG, "Wtime modification applied: $hasWtimeModification")
            
            // Проверка наличия данных о бое
            val hasFightData = content.contains("var fight_ty = [")
            Log.d(TAG, "Fight data found: $hasFightData")
        }
        
        // Загрузка чата
        val chatResponse = gameHttpClient.get("http://www.neverlands.ru/ch/msg.php")
        if (chatResponse.isSuccessful) {
            val content = chatResponse.body?.string() ?: ""
            Log.d(TAG, "Chat filtered: Chat history integration = ${userProfile.chatKeepGame}")
        }
        
        // Загрузка торговли (если активна)
        if (userProfile.torgActive) {
            val tradeResponse = gameHttpClient.get("http://www.neverlands.ru/gameplay/trade.php")
            if (tradeResponse.isSuccessful) {
                val content = tradeResponse.body?.string() ?: ""
                Log.d(TAG, "Trade filtered: Auto-trading enabled")
            }
        } else {
            Log.d(TAG, "Trade filtering disabled in user settings")
        }
    }

    /**
     * Демонстрация HTML фильтров
     */
    private suspend fun demonstrateHtmlFilters() {
        Log.d(TAG, "--- HTML Filters ---")
        
        // Загрузка главной страницы
        val indexResponse = gameHttpClient.get("http://www.neverlands.ru/index.cgi")
        if (indexResponse.isSuccessful) {
            val content = indexResponse.body?.string() ?: ""
            val doctypeRemoved = !content.contains("<!DOCTYPE")
            Log.d(TAG, "Index.cgi: DOCTYPE removed = $doctypeRemoved")
            
            val ieCompatibility = content.contains("X-UA-Compatible")
            Log.d(TAG, "Index.cgi: IE compatibility added = $ieCompatibility")
        }
        
        // Загрузка информации о персонаже
        val pinfoResponse = gameHttpClient.get("http://www.neverlands.ru/pinfo.cgi?TestPlayer")
        if (pinfoResponse.isSuccessful) {
            val content = pinfoResponse.body?.string() ?: ""
            Log.d(TAG, "Pinfo.cgi filtered (${content.length} chars)")
            
            // Проверка наличия данных о персонаже
            val hasHpMpData = content.contains("var hpmp = [")
            val hasParamsData = content.contains("var params = [")
            val hasSlotsData = content.contains("var slots = [")
            
            Log.d(TAG, "Player data parsed - HP/MP: $hasHpMpData, Params: $hasParamsData, Slots: $hasSlotsData")
        }
        
        // Загрузка форума
        val forumResponse = gameHttpClient.get("http://forum.neverlands.ru/")
        if (forumResponse.isSuccessful) {
            val content = forumResponse.body?.string() ?: ""
            val doctypeRemoved = !content.contains("<!DOCTYPE")
            Log.d(TAG, "Forum: DOCTYPE removed = $doctypeRemoved")
        }
    }

    /**
     * Демонстрация учета пользовательских настроек
     */
    private fun demonstrateUserSettings() {
        Log.d(TAG, "--- User Settings Impact ---")
        
        Log.d(TAG, "Fishing auto-wear: ${userProfile.fishAutoWear}")
        Log.d(TAG, "Fishing automation: ${userProfile.fishAuto}")
        Log.d(TAG, "Trading active: ${userProfile.torgActive}")
        Log.d(TAG, "Chat keep in game: ${userProfile.chatKeepGame}")
        Log.d(TAG, "Inventory packing: ${userProfile.doInvPack}")
        Log.d(TAG, "Auto-answer: ${userProfile.doAutoAnswer}")
        
        // Фильтры изменяются в зависимости от настроек пользователя
        if (userProfile.fishAuto) {
            Log.d(TAG, "> Fish auto-processing enabled in main.php filter")
        }
        
        if (userProfile.torgActive) {
            Log.d(TAG, "> Trade auto-processing enabled in trade.php filter")
        }
        
        if (userProfile.chatKeepGame) {
            Log.d(TAG, "> Chat history integration enabled in msg.php filter")
        }
        
        if (userProfile.doInvPack) {
            Log.d(TAG, "> Inventory grouping enabled in main.php filter")
        }
    }

    /**
     * Демонстрация обработки игровых ситуаций
     */
    suspend fun demonstrateGameSituations() {
        Log.d(TAG, "--- Game Situations Demo ---")
        
        // Ситуация 1: Рыбалка
        if (userProfile.fishAuto) {
            Log.d(TAG, "Simulating fishing situation...")
            val fishingResponse = gameHttpClient.get("http://www.neverlands.ru/main.php?fish=1")
            if (fishingResponse.isSuccessful) {
                val content = fishingResponse.body?.string() ?: ""
                val hasFishData = content.contains("����")
                Log.d(TAG, "Fishing data detected: $hasFishData")
            }
        }
        
        // Ситуация 2: Бой
        Log.d(TAG, "Simulating combat situation...")
        val combatResponse = gameHttpClient.get("http://www.neverlands.ru/main.php?fight=1")
        if (combatResponse.isSuccessful) {
            val content = combatResponse.body?.string() ?: ""
            val hasCombatData = content.contains("var fight_ty = [")
            Log.d(TAG, "Combat data detected: $hasCombatData")
            
            if (hasCombatData) {
                Log.d(TAG, "> Fight actions will be parsed (rob/raz opportunities)")
            }
        }
        
        // Ситуация 3: Торговля
        if (userProfile.torgActive) {
            Log.d(TAG, "Simulating trade situation...")
            val tradeResponse = gameHttpClient.get("http://www.neverlands.ru/gameplay/trade.php?trade_id=123")
            if (tradeResponse.isSuccessful) {
                val content = tradeResponse.body?.string() ?: ""
                val isTradeOffer = content.contains("������ ���� �")
                Log.d(TAG, "Trade offer detected: $isTradeOffer")
                
                if (isTradeOffer) {
                    Log.d(TAG, "> Auto-trading logic will be applied")
                }
            }
        }
    }

    /**
     * Оценка производительности фильтров
     */
    suspend fun benchmarkFilters() {
        Log.d(TAG, "--- Performance Benchmark ---")
        
        val testUrls = listOf(
            "http://www.neverlands.ru/js/map.js",
            "http://www.neverlands.ru/main.php",
            "http://www.neverlands.ru/pinfo.cgi?Player",
            "http://www.neverlands.ru/ch/msg.php"
        )
        
        testUrls.forEach { url ->
            val startTime = System.currentTimeMillis()
            
            val response = gameHttpClient.get(url)
            if (response.isSuccessful) {
                val content = response.body?.string() ?: ""
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime
                
                Log.d(TAG, "URL: $url")
                Log.d(TAG, "  Size: ${content.length} chars")
                Log.d(TAG, "  Time: ${duration}ms")
                Log.d(TAG, "  Rate: ${content.length / maxOf(duration, 1)}c/ms")
            }
        }
    }
}
