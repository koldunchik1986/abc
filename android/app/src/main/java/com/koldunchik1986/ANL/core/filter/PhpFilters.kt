package com.koldunchik1986.ANL.core.filter

import android.util.Log
import kotlinx.coroutines.runBlocking
import com.koldunchik1986.ANL.core.helpers.StringHelpers
import com.koldunchik1986.ANL.data.model.UserProfile
import com.koldunchik1986.ANL.data.preferences.UserPreferencesManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Фильтры для PHP страниц
 * Аналог Windows PostFilter для обработки PHP
 */
@Singleton
class PhpFilters @Inject constructor(
    private val userPreferencesManager: UserPreferencesManager,
    private val htmlFilters: HtmlFilters
) {
    
    companion object {
        private const val TAG = "PhpFilters"
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
     * Обработка game.php
     * Аналог GamePhp() из Windows клиента
     */
    fun processGamePhp(data: ByteArray): ByteArray {
        return try {
            val html = String(data, charset(CHARSET_WINDOWS_1251))
            // Пока не требуется специальная обработка game.php
            data
        } catch (e: Exception) {
            Log.e(TAG, "Error processing game.php", e)
            data
        }
    }

    /**
     * Обработка main.php - главная игровая страница
     * Аналог MainPhp() из Windows клиента
     */
    fun processMainPhp(url: String, data: ByteArray): ByteArray {
        return try {
            var html = String(data, charset(CHARSET_WINDOWS_1251))
            
            // Обработка действий в main.php
            html = processMainPhpActions(url, html)
            
            // Обработка времени ожидания
            html = processMainPhpWtime(url, html)
            
            // Обработка инвентаря
            html = processMainPhpInventory(html)
            
            // Обработка экипировки
            html = processMainPhpEquipment(html)
            
            // Обработка HP/MP
            html = processMainPhpHpMp(html)
            
            html.toByteArray(charset(CHARSET_WINDOWS_1251))
        } catch (e: Exception) {
            Log.e(TAG, "Error processing main.php", e)
            data
        }
    }

    /**
     * Обработка действий в main.php
     */
    private fun processMainPhpActions(url: String, html: String): String {
        var result = html
        
        // Извлекаем данные о бое
        val fightData = StringHelpers.subString(html, "var fight_ty = [", "];")
        if (!fightData.isNullOrEmpty()) {
            result = processFightActions(result, fightData)
        }
        
        return result
    }

    /**
     * Обработка боевых действий (ограбление, разбой)
     * Аналог MainPhpRob() и MainPhpRaz() из Windows клиента
     */
    private fun processFightActions(html: String, fightData: String): String {
        var result = html
        
        try {
            val fightArray = StringHelpers.parseJsString(fightData)
            
            // Обработка доступных действий (аналог MainPhpRob)
            if (fightArray != null && fightArray.size > 10 && fightArray[10].isNotEmpty()) {
                val robLink = buildRobLink(fightArray[10])
                if (robLink.isNotEmpty()) {
                    Log.d(TAG, "Rob action available: $robLink")
                    // Здесь можно добавить обработку ссылки на UI
                }
            }
            
            // Обработка доступных действий (аналог MainPhpRaz)
            if (fightArray != null && fightArray.size > 9 && fightArray[9].isNotEmpty()) {
                val razLink = buildRazLink(fightArray[9])
                if (razLink.isNotEmpty()) {
                    Log.d(TAG, "Raz action available: $razLink")
                    // Здесь можно добавить обработку ссылки на UI
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing fight actions", e)
        }
        
        return result
    }

    /**
     * Построение ссылки для ограбления
     */
    private fun buildRobLink(robData: List<String>): String {
        return if (robData.size > 3) {
            "http://www.neverlands.ru/main.php?get_id=17&type=0&p=${robData[3]}&uid=${robData[0]}&s=0&m=0&vcode=${robData[1]}"
        } else {
            ""
        }
    }

    /**
     * Построение ссылки для разбоя
     */
    private fun buildRazLink(razData: List<String>): String {
        return if (razData.size > 5) {
            "http://www.neverlands.ru/main.php?get_id=17&type=${razData[0]}&p=${razData[1]}&uid=${razData[2]}&s=${razData[3]}&m=${razData[4]}&vcode=${razData[5]}"
        } else {
            ""
        }
    }

    /**
     * Обработка времени ожидания
     * Аналог MainPhpWtime() из Windows клиента
     */
    private fun processMainPhpWtime(url: String, html: String): String {
        var result = html
        
        // Проверка на автоматическую рыбалку
        val currentProfile = getCurrentProfile()
        if (currentProfile?.fishAuto == true) {
            val fishReport = processFishReport(html)
            if (fishReport.isNotEmpty()) {
                return fishReport
            }
        }
        
        // Заменяем пустое поле ожидания
        result = result.replace(
            "id=wtime></div>",
            "id=wtime><i>Обработка данных...</i></div>"
        )
        
        return result
    }

    /**
     * Обработка отчета о рыбалке
     * Аналог MainPhpFishReport() из Windows клиента
     */
    private fun processFishReport(html: String): String {
        // Извлекаем информацию о рыбалке
        val fishInfo = StringHelpers.subString(html, "Рыбалка: Лов", "<br>")
        if (!fishInfo.isNullOrEmpty()) {
            Log.d(TAG, "Fish report: $fishInfo")
            // Здесь можно добавить обработку отчета о рыбалке
        }
        
        return ""
    }

    /**
     * Обработка инвентаря
     * Аналог MainPhpInv() из Windows клиента
     */
    private fun processMainPhpInventory(html: String): String {
        var result = html
        
        // Найти начало инвентаря
        val invStart = html.indexOf("</b></font></td></tr>")
        if (invStart != -1) {
            // Обработка предметов инвентаря
            result = processInventoryItems(html, invStart)
        }
        
        return result
    }

    /**
     * Обработка предметов инвентаря
     */
    private fun processInventoryItems(html: String, startPos: Int): String {
        var result = html
        
        // Здесь можно добавить обработку отдельных предметов инвентаря, если это необходимо
        // Аналог InvEntry из Windows клиента
        
        return result
    }

    /**
     * Обработка экипировки
     * Аналог MainPhpWear* функций из Windows клиента
     */
    private fun processMainPhpEquipment(html: String): String {
        var result = html
        
        // Проверка на автоматическую одежду для рыбалки
        val currentProfile = getCurrentProfile()
        if (currentProfile?.fishAutoWear == true) {
            result = processAutoWearFishing(result)
        }
        
        // Обработка наборов экипировки
        result = processEquipmentSets(result)
        
        return result
    }

    /**
     * Обработка автоматической одевки для рыбалки
     */
    private fun processAutoWearFishing(html: String): String {
        // Ищем удочки в инвентаре
        val fishingRods = findFishingRods(html)
        if (fishingRods.isNotEmpty()) {
            Log.d(TAG, "Found fishing rods: ${fishingRods.size}")
            // Здесь можно добавить обработку найденных удочек
        }
        
        return html
    }

    /**
     * Поиск удочек в инвентаре
     */
    private fun findFishingRods(html: String): List<String> {
        val rods = mutableListOf<String>()
        
        // Ищем ключевые слова "удочка", "удочки"
        val fishingKeywords = listOf("удочка", "удочки")
        
        for (keyword in fishingKeywords) {
            var searchPos = 0
            while (true) {
                val pos = html.indexOf(keyword, searchPos, ignoreCase = true)
                if (pos == -1) break
                
                // Извлекаем информацию об удочке из HTML
                val rodInfo = extractItemInfo(html, pos)
                if (rodInfo.isNotEmpty()) {
                    rods.add(rodInfo)
                }
                
                searchPos = pos + keyword.length
            }
        }
        
        return rods
    }

    /**
     * Извлечение информации об предмете из HTML
     */
    private fun extractItemInfo(html: String, pos: Int): String {
        // Здесь можно добавить извлечение информации о предмете из HTML
        return ""
    }

    /**
     * Обработка наборов экипировки
     */
    private fun processEquipmentSets(html: String): String {
        // Ищем вызовы compl_view для отображения наборов экипировки
        val complPattern = """compl_view\("([^"]+)""""
        val matches = Regex(complPattern).findAll(html)
        
        for (match in matches) {
            val setName = match.groupValues[1]
            Log.d(TAG, "Found equipment set: $setName")
        }
        
        return html
    }

    /**
     * Обработка HP/MP
     * Аналог MainPhpInsHp() из Windows клиента
     */
    private fun processMainPhpHpMp(html: String): String {
        // Ищем вызовы ins_hp для отображения HP/MP
        val hpPattern = """ins_hp\(([^)]+)\)"""
        val matches = Regex(hpPattern).findAll(html)
        
        for (match in matches) {
            val params = match.groupValues[1].split(",")
            if (params.size >= 6) {
                try {
                    val hp = params[4].trim().toDoubleOrNull()
                    val mp = params[5].trim().toDoubleOrNull()
                    
                    if (hp != null && mp != null) {
                        // Обновляем значения HP/MP в профиле
                        val currentProfile = getCurrentProfile()
                        if (currentProfile != null) {
                            currentProfile.currentHp = hp
                            currentProfile.currentMp = mp
                            Log.d(TAG, "Updated HP: $hp, MP: $mp")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing HP/MP", e)
                }
            }
        }
        
        return html
    }

    /**
     * Обработка msg.php - сообщение в чате
     * Аналог MsgPhp() из Windows клиента
     */
    fun processChatMessage(data: ByteArray): ByteArray {
        return try {
            var html = String(data, charset(CHARSET_WINDOWS_1251))
            
            // Восстанавливаем последнее сообщение в чате
            val currentProfile = getCurrentProfile()
            if (currentProfile?.chatKeepGame == true && currentProfile.lastChatMessage.isNotEmpty()) {
                html = html.replace(
                    " id=msg>",
                    " id=msg>${currentProfile.lastChatMessage}"
                )
            }
            
            html.toByteArray(charset(CHARSET_WINDOWS_1251))
        } catch (e: Exception) {
            Log.e(TAG, "Error processing msg.php", e)
            data
        }
    }

    /**
     * Обработка but.php - кнопка в чате
     * Аналог ButPhp() из Windows клиента
     */
    fun processChatButton(data: ByteArray): ByteArray {
        return try {
            val html = String(data, charset(CHARSET_WINDOWS_1251))
            // Здесь можно добавить обработку кнопки в чате
            data
        } catch (e: Exception) {
            Log.e(TAG, "Error processing but.php", e)
            data
        }
    }

    /**
     * Обработка trade.php - торговля
     * Аналог TradePhp() из Windows клиента
     */
    fun processTradePhp(data: ByteArray): ByteArray {
        return try {
            var html = String(data, charset(CHARSET_WINDOWS_1251))
            
            // Проверка на наличие кнопки "Вернуться в игру"
            if (html.contains("""onclick="location='../main.php'" value="Вернуться в игру"""", ignoreCase = true)) {
                html = processTradeTransaction(html)
            }
            
            html.toByteArray(charset(CHARSET_WINDOWS_1251))
        } catch (e: Exception) {
            Log.e(TAG, "Error processing trade.php", e)
            data
        }
    }

    /**
     * Обработка транзакции торговли
     */
    private fun processTradeTransaction(html: String): String {
        val salesNick = StringHelpers.subString(html, """<font color=#cc0000>Продажа предмета от """, " �� ")
        val priceStr = StringHelpers.subString(html, " �� ", "NV?</font>")
        val itemName = StringHelpers.subString(html, "NV?</font><br><br> ", "</b>")
        val itemLevel = StringHelpers.subString(html, "&nbsp;Уровень: <b>", "</b>")
        
        if (!priceStr.isNullOrEmpty()) {
            val price = priceStr.toIntOrNull()
            if (price != null) {
                Log.d(TAG, "Trade: $itemName (level $itemLevel) for $price NV from $salesNick")
                
                // Здесь можно добавить обработку предложения о продаже предмета
                // Аналог TradePhp() из Windows клиента
                val currentProfile = getCurrentProfile()
                if (currentProfile?.torgActive == true) {
                    return processAutoTrading(html, itemName, price, salesNick)
                }
            }
        }
        
        return html
    }

    /**
     * Обработка автоматической торговли
     */
    private fun processAutoTrading(html: String, itemName: String?, price: Int, salesNick: String?): String {
        // Здесь можно добавить обработку автоматической торговли предметами/денежными средствами
        // Аналог TradePhp() из Windows клиента
        return html
    }

    /**
     * Обработка map_act_ajax.php
     * Аналог MapActAjaxPhp() из Windows клиента
     */
    fun processMapActAjax(data: ByteArray): ByteArray {
        return try {
            val html = String(data, charset(CHARSET_WINDOWS_1251))
            // Здесь можно добавить обработку действий на карте
            data
        } catch (e: Exception) {
            Log.e(TAG, "Error processing map_act_ajax.php", e)
            data
        }
    }

    /**
     * Обработка fish_ajax.php
     * Аналог FishAjaxPhp() из Windows клиента
     */
    fun processFishAjax(data: ByteArray): ByteArray {
        return try {
            val html = String(data, charset(CHARSET_WINDOWS_1251))
            // Здесь можно добавить обработку рыбалки
            data
        } catch (e: Exception) {
            Log.e(TAG, "Error processing fish_ajax.php", e)
            data
        }
    }

    /**
     * Обработка shop_ajax.php
     * Аналог ShopAjaxPhp() из Windows клиента
     */
    fun processShopAjax(data: ByteArray): ByteArray {
        return try {
            val html = String(data, charset(CHARSET_WINDOWS_1251))
            // Здесь можно добавить обработку продажи старых предметов и очистку списка магазина
            // userProfile.bulkSellOldScript = ""
            // userProfile.shopList.clear()
            data
        } catch (e: Exception) {
            Log.e(TAG, "Error processing shop_ajax.php", e)
            data
        }
    }

    /**
     * Обработка roulette_ajax.php
     * Аналог RouletteAjaxPhp() из Windows клиента
     */
    fun processRouletteAjax(data: ByteArray): ByteArray {
        return try {
            val html = String(data, charset(CHARSET_WINDOWS_1251))
            
            // Здесь можно добавить обработку результатов рулетки
            val args = html.split("@")
            if (args.size > 2 && args[0] == "OK") {
                Log.d(TAG, "Рулетка: ${args[1]}")
            }
            
            data
        } catch (e: Exception) {
            Log.e(TAG, "Error processing roulette_ajax.php", e)
            data
        }
    }

    /**
     * Обработка ch.php?lo= - сообщение в чате
     * Аналог ChRoomPhp() из Windows клиента
     */
    fun processChatRoom(data: ByteArray): ByteArray {
        return try {
            val html = String(data, charset(CHARSET_WINDOWS_1251))
            // Здесь можно добавить обработку сообщений в чате
            data
        } catch (e: Exception) {
            Log.e(TAG, "Error processing chat room", e)
            data
        }
    }

    /**
     * Обработка ch.php?0
     * Аналог ChZero() из Windows клиента
     */
    fun processChatZero(data: ByteArray): ByteArray {
        return try {
            val html = String(data, charset(CHARSET_WINDOWS_1251))
            // Здесь можно добавить обработку запроса ch.php?0
            data
        } catch (e: Exception) {
            Log.e(TAG, "Error processing ch.php?0", e)
            data
        }
    }
}
