package ru.neverlands.abclient.core.filter

import android.util.Log
import kotlinx.coroutines.runBlocking
import ru.neverlands.abclient.core.helpers.StringHelpers
import ru.neverlands.abclient.data.model.UserProfile
import ru.neverlands.abclient.data.preferences.UserPreferencesManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Фильтры для PHP страниц
 * Аналоги методов из Windows PostFilter для обработки PHP
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
     * Получает текущий профиль пользователя
     */
    private fun getCurrentProfile(): UserProfile? {
        return runBlocking {
            userPreferencesManager.getCurrentProfile()
        }
    }

    /**
     * Обработка game.php
     * Аналог GamePhp() из Windows версии
     */
    fun processGamePhp(data: ByteArray): ByteArray {
        return try {
            val html = String(data, charset(CHARSET_WINDOWS_1251))
            // Основные модификации игровой страницы
            data
        } catch (e: Exception) {
            Log.e(TAG, "Error processing game.php", e)
            data
        }
    }

    /**
     * Обработка main.php - основная игровая логика
     * Аналог MainPhp() из Windows версии
     */
    fun processMainPhp(url: String, data: ByteArray): ByteArray {
        return try {
            var html = String(data, charset(CHARSET_WINDOWS_1251))
            
            // Обработка различных действий в main.php
            html = processMainPhpActions(url, html)
            
            // Обработка времени действий
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
        
        // Обработка боевых действий
        val fightData = StringHelpers.subString(html, "var fight_ty = [", "];")
        if (!fightData.isNullOrEmpty()) {
            result = processFightActions(result, fightData)
        }
        
        return result
    }

    /**
     * Обработка боевых действий (грабеж, разделка)
     * Аналог MainPhpRob() и MainPhpRaz() из Windows версии
     */
    private fun processFightActions(html: String, fightData: String): String {
        var result = html
        
        try {
            val fightArray = StringHelpers.parseJsString(fightData)
            
            // Обработка возможности грабежа (аналог MainPhpRob)
            if (fightArray != null && fightArray.size > 10 && fightArray[10].isNotEmpty()) {
                val robLink = buildRobLink(fightArray[10])
                if (robLink.isNotEmpty()) {
                    Log.d(TAG, "Rob action available: $robLink")
                    // Здесь можно добавить кнопку грабежа в UI
                }
            }
            
            // Обработка возможности разделки (аналог MainPhpRaz)
            if (fightArray != null && fightArray.size > 9 && fightArray[9].isNotEmpty()) {
                val razLink = buildRazLink(fightArray[9])
                if (razLink.isNotEmpty()) {
                    Log.d(TAG, "Raz action available: $razLink")
                    // Здесь можно добавить кнопку разделки в UI
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing fight actions", e)
        }
        
        return result
    }

    /**
     * Строит ссылку для грабежа
     */
    private fun buildRobLink(robData: List<String>): String {
        return if (robData.size > 3) {
            "http://www.neverlands.ru/main.php?get_id=17&type=0&p=${robData[3]}&uid=${robData[0]}&s=0&m=0&vcode=${robData[1]}"
        } else {
            ""
        }
    }

    /**
     * Строит ссылку для разделки
     */
    private fun buildRazLink(razData: List<String>): String {
        return if (razData.size > 5) {
            "http://www.neverlands.ru/main.php?get_id=17&type=${razData[0]}&p=${razData[1]}&uid=${razData[2]}&s=${razData[3]}&m=${razData[4]}&vcode=${razData[5]}"
        } else {
            ""
        }
    }

    /**
     * Обработка времени действий
     * Аналог MainPhpWtime() из Windows версии
     */
    private fun processMainPhpWtime(url: String, html: String): String {
        var result = html
        
        // Автоматическая рыбалка
        val currentProfile = getCurrentProfile()
        if (currentProfile?.fishAuto == true) {
            val fishReport = processFishReport(html)
            if (fishReport.isNotEmpty()) {
                return fishReport
            }
        }
        
        // Добавляем индикатор выполнения действия
        result = result.replace(
            "id=wtime></div>",
            "id=wtime><i>Выполняется действие...</i></div>"
        )
        
        return result
    }

    /**
     * Обработка отчета о рыбалке
     * Аналог MainPhpFishReport() из Windows версии
     */
    private fun processFishReport(html: String): String {
        // Ищем информацию о рыбалке
        val fishInfo = StringHelpers.subString(html, "Вид ресурса: рыба", "<br>")
        if (!fishInfo.isNullOrEmpty()) {
            Log.d(TAG, "Fish report: $fishInfo")
            // Здесь можно добавить автоматическую логику рыбалки
        }
        
        return ""
    }

    /**
     * Обработка инвентаря
     * Аналог MainPhpInv() из Windows версии
     */
    private fun processMainPhpInventory(html: String): String {
        var result = html
        
        // Поиск начала инвентаря
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
        
        // Здесь можно добавить группировку предметов, массовые операции и т.д.
        // Аналогично InvEntry из Windows версии
        
        return result
    }

    /**
     * Обработка экипировки
     * Аналог MainPhpWear* методов из Windows версии
     */
    private fun processMainPhpEquipment(html: String): String {
        var result = html
        
        // Автоматическое одевание удочек для рыбалки
        val currentProfile = getCurrentProfile()
        if (currentProfile?.fishAutoWear == true) {
            result = processAutoWearFishing(result)
        }
        
        // Обработка комплектов экипировки
        result = processEquipmentSets(result)
        
        return result
    }

    /**
     * Автоматическое одевание снаряжения для рыбалки
     */
    private fun processAutoWearFishing(html: String): String {
        // Поиск удочек в инвентаре и автоматическое одевание
        val fishingRods = findFishingRods(html)
        if (fishingRods.isNotEmpty()) {
            Log.d(TAG, "Found fishing rods: ${fishingRods.size}")
            // Здесь можно добавить логику автоматического одевания
        }
        
        return html
    }

    /**
     * Поиск удочек в инвентаре
     */
    private fun findFishingRods(html: String): List<String> {
        val rods = mutableListOf<String>()
        
        // Поиск по ключевым словам "удочка", "спиннинг"
        val fishingKeywords = listOf("удочка", "спиннинг")
        
        for (keyword in fishingKeywords) {
            var searchPos = 0
            while (true) {
                val pos = html.indexOf(keyword, searchPos, ignoreCase = true)
                if (pos == -1) break
                
                // Извлекаем информацию о найденной удочке
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
     * Извлекает информацию о предмете
     */
    private fun extractItemInfo(html: String, pos: Int): String {
        // Здесь можно добавить парсинг предмета из HTML
        return ""
    }

    /**
     * Обработка комплектов экипировки
     */
    private fun processEquipmentSets(html: String): String {
        // Поиск вызовов compl_view для автоматического одевания комплектов
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
     * Аналог MainPhpInsHp() из Windows версии
     */
    private fun processMainPhpHpMp(html: String): String {
        // Поиск функций ins_hp для обновления HP/MP
        val hpPattern = """ins_hp\(([^)]+)\)"""
        val matches = Regex(hpPattern).findAll(html)
        
        for (match in matches) {
            val params = match.groupValues[1].split(",")
            if (params.size >= 6) {
                try {
                    val hp = params[4].trim().toDoubleOrNull()
                    val mp = params[5].trim().toDoubleOrNull()
                    
                    if (hp != null && mp != null) {
                        // Сохраняем актуальные HP/MP в профиле
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
     * Обработка msg.php - сообщения чата
     * Аналог MsgPhp() из Windows версии
     */
    fun processChatMessage(data: ByteArray): ByteArray {
        return try {
            var html = String(data, charset(CHARSET_WINDOWS_1251))
            
            // Сохранение истории чата в игре
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
     * Обработка but.php - кнопки чата
     * Аналог ButPhp() из Windows версии
     */
    fun processChatButton(data: ByteArray): ByteArray {
        return try {
            val html = String(data, charset(CHARSET_WINDOWS_1251))
            // Модификации для кнопок чата
            data
        } catch (e: Exception) {
            Log.e(TAG, "Error processing but.php", e)
            data
        }
    }

    /**
     * Обработка trade.php - торговля
     * Аналог TradePhp() из Windows версии
     */
    fun processTradePhp(data: ByteArray): ByteArray {
        return try {
            var html = String(data, charset(CHARSET_WINDOWS_1251))
            
            // Проверяем, что это страница покупки
            if (html.contains("""onclick="location='../main.php'" value="Отказаться от покупки"""", ignoreCase = true)) {
                html = processTradeTransaction(html)
            }
            
            html.toByteArray(charset(CHARSET_WINDOWS_1251))
        } catch (e: Exception) {
            Log.e(TAG, "Error processing trade.php", e)
            data
        }
    }

    /**
     * Обработка торговой транзакции
     */
    private fun processTradeTransaction(html: String): String {
        val salesNick = StringHelpers.subString(html, """<font color=#cc0000>Купить вещь у """, " за ")
        val priceStr = StringHelpers.subString(html, " за ", "NV?</font>")
        val itemName = StringHelpers.subString(html, "NV?</font><br><br> ", "</b>")
        val itemLevel = StringHelpers.subString(html, "&nbsp;Уровень: <b>", "</b>")
        
        if (!priceStr.isNullOrEmpty()) {
            val price = priceStr.toIntOrNull()
            if (price != null) {
                Log.d(TAG, "Trade: $itemName (level $itemLevel) for $price NV from $salesNick")
                
                // Здесь можно добавить логику автоматической торговли
                // на основе настроек пользователя
                val currentProfile = getCurrentProfile()
                if (currentProfile?.torgActive == true) {
                    return processAutoTrading(html, itemName, price, salesNick)
                }
            }
        }
        
        return html
    }

    /**
     * Автоматическая торговля
     */
    private fun processAutoTrading(html: String, itemName: String?, price: Int, salesNick: String?): String {
        // Здесь можно добавить логику автоматического принятия/отклонения торговых предложений
        // на основе таблицы цен пользователя
        return html
    }

    /**
     * Обработка map_act_ajax.php
     * Аналог MapActAjaxPhp() из Windows версии
     */
    fun processMapActAjax(data: ByteArray): ByteArray {
        return try {
            val html = String(data, charset(CHARSET_WINDOWS_1251))
            // Обработка действий на карте
            data
        } catch (e: Exception) {
            Log.e(TAG, "Error processing map_act_ajax.php", e)
            data
        }
    }

    /**
     * Обработка fish_ajax.php
     * Аналог FishAjaxPhp() из Windows версии
     */
    fun processFishAjax(data: ByteArray): ByteArray {
        return try {
            val html = String(data, charset(CHARSET_WINDOWS_1251))
            // Обработка рыбалки
            data
        } catch (e: Exception) {
            Log.e(TAG, "Error processing fish_ajax.php", e)
            data
        }
    }

    /**
     * Обработка shop_ajax.php
     * Аналог ShopAjaxPhp() из Windows версии
     */
    fun processShopAjax(data: ByteArray): ByteArray {
        return try {
            val html = String(data, charset(CHARSET_WINDOWS_1251))
            // Очистка списка магазина и подготовка к массовой продаже
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
     * Аналог RouletteAjaxPhp() из Windows версии
     */
    fun processRouletteAjax(data: ByteArray): ByteArray {
        return try {
            val html = String(data, charset(CHARSET_WINDOWS_1251))
            
            // Парсинг результата рулетки
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
     * Обработка ch.php?lo= - комнаты чата
     * Аналог ChRoomPhp() из Windows версии
     */
    fun processChatRoom(data: ByteArray): ByteArray {
        return try {
            val html = String(data, charset(CHARSET_WINDOWS_1251))
            // Обработка комнат чата
            data
        } catch (e: Exception) {
            Log.e(TAG, "Error processing chat room", e)
            data
        }
    }

    /**
     * Обработка ch.php?0
     * Аналог ChZero() из Windows версии
     */
    fun processChatZero(data: ByteArray): ByteArray {
        return try {
            val html = String(data, charset(CHARSET_WINDOWS_1251))
            // Специальная обработка для ch.php?0
            data
        } catch (e: Exception) {
            Log.e(TAG, "Error processing ch.php?0", e)
            data
        }
    }
}