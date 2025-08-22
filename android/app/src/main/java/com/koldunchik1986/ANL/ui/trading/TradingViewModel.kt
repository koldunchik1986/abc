package com.koldunchik1986.ANL.ui.trading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.koldunchik1986.ANL.data.repository.ProfileRepository
import com.koldunchik1986.ANL.data.model.UserProfile
import javax.inject.Inject

@HiltViewModel
class TradingViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TradingUiState())
    val uiState: StateFlow<TradingUiState> = _uiState.asStateFlow()
    
    private var currentProfile: UserProfile? = null
    private var pricePairs: Array<TradingPricePair> = emptyArray()
    
    init {
        loadProfile()
    }
    
    private fun loadProfile() {
        viewModelScope.launch {
            currentProfile = profileRepository.getCurrentProfile()
            currentProfile?.let { profile ->
                _uiState.value = _uiState.value.copy(
                    tableString = profile.torgTable,
                    messageAdv = profile.torgMessageAdv,
                    advTime = profile.torgAdvTime.toString(),
                    messageNoMoney = profile.torgMessageNoMoney,
                    messageTooExp = profile.torgMessageTooExp,
                    messageThanks = profile.torgMessageThanks,
                    messageLess90 = profile.torgMessageLess90,
                    sliv = profile.torgSliv.toString(),
                    minLevel = profile.torgMinLevel.toString(),
                    ex = profile.torgEx,
                    deny = profile.torgDeny
                )
                
                // Парсим таблицу при загрузке
                parseTable(profile.torgTable)
            }
        }
    }
    
    fun setTableString(table: String) {
        _uiState.value = _uiState.value.copy(tableString = table)
    }
    
    fun validateTable() {
        val isValid = parseTable(_uiState.value.tableString)
        _uiState.value = _uiState.value.copy(isTableValid = isValid)
    }
    
    fun setMessageAdv(message: String) {
        _uiState.value = _uiState.value.copy(messageAdv = message)
    }
    
    fun setAdvTime(time: String) {
        _uiState.value = _uiState.value.copy(advTime = time)
    }
    
    fun setMessageNoMoney(message: String) {
        _uiState.value = _uiState.value.copy(messageNoMoney = message)
    }
    
    fun setMessageTooExp(message: String) {
        _uiState.value = _uiState.value.copy(messageTooExp = message)
    }
    
    fun setMessageThanks(message: String) {
        _uiState.value = _uiState.value.copy(messageThanks = message)
    }
    
    fun setMessageLess90(message: String) {
        _uiState.value = _uiState.value.copy(messageLess90 = message)
    }
    
    fun setSliv(sliv: String) {
        _uiState.value = _uiState.value.copy(sliv = sliv)
    }
    
    fun setMinLevel(level: String) {
        _uiState.value = _uiState.value.copy(minLevel = level)
    }
    
    fun setEx(ex: String) {
        _uiState.value = _uiState.value.copy(ex = ex)
    }
    
    fun setDeny(deny: String) {
        _uiState.value = _uiState.value.copy(deny = deny)
    }
    
    fun setTestPrice(price: String) {
        _uiState.value = _uiState.value.copy(testPrice = price, calculatedPrice = 0)
    }
    
    fun calculatePrice() {
        val testPrice = _uiState.value.testPrice.toIntOrNull() ?: return
        val calculatedPrice = calculatePriceByTable(testPrice)
        _uiState.value = _uiState.value.copy(calculatedPrice = calculatedPrice)
    }
    
    fun saveSettings() {
        viewModelScope.launch {
            currentProfile?.let { profile ->
                val updatedProfile = profile.copy(
                    torgTable = _uiState.value.tableString,
                    torgMessageAdv = _uiState.value.messageAdv,
                    torgAdvTime = _uiState.value.advTime.toIntOrNull() ?: 0,
                    torgMessageNoMoney = _uiState.value.messageNoMoney,
                    torgMessageTooExp = _uiState.value.messageTooExp,
                    torgMessageThanks = _uiState.value.messageThanks,
                    torgMessageLess90 = _uiState.value.messageLess90,
                    torgSliv = _uiState.value.sliv.toBooleanStrictOrNull() ?: false,
                    torgMinLevel = _uiState.value.minLevel.toIntOrNull() ?: 0,
                    torgEx = _uiState.value.ex,
                    torgDeny = _uiState.value.deny
                )
                
                profileRepository.saveProfile(updatedProfile)
                currentProfile = updatedProfile
            }
        }
    }
    
    /**
     * Парсит таблицу цен - аналог Parse() из Windows TorgList
     */
    private fun parseTable(torgString: String): Boolean {
        if (torgString.isEmpty()) {
            _uiState.value = _uiState.value.copy(isTableValid = false, pricePairs = emptyList())
            return false
        }
        
        try {
            val newTorgList = mutableListOf<TradingPricePair>()
            
            val work = torgString.replace("\n", "")
                .replace(" ", "")
                .replace("(0)", "(*0)")
                .replace("(-", "(*")
            
            val parts = work.split(Regex("[-(),*,]+"))
            var i = 0
            
            while (i < parts.size) {
                if (parts[i].isEmpty()) {
                    i++
                    continue
                }
                
                val lowValue = parts[i].toIntOrNull() ?: return false
                if (++i >= parts.size) return false
                
                val highValue = parts[i].toIntOrNull() ?: return false
                if (lowValue > highValue) return false
                
                if (++i >= parts.size) return false
                
                // Пропускаем пустые части
                while (i < parts.size && parts[i].isEmpty()) {
                    i++
                }
                
                if (i >= parts.size) return false
                
                val price = parts[i].toIntOrNull() ?: return false
                if (price > 0) return false // Должно быть отрицательным или нулем
                
                val torgPair = TradingPricePair(lowValue, highValue, price)
                newTorgList.add(torgPair)
                
                if (++i >= parts.size) break
            }
            
            if (newTorgList.isEmpty()) {
                _uiState.value = _uiState.value.copy(isTableValid = false, pricePairs = emptyList())
                return false
            }
            
            pricePairs = newTorgList.toTypedArray()
            _uiState.value = _uiState.value.copy(
                isTableValid = true, 
                pricePairs = newTorgList
            )
            return true
            
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isTableValid = false, pricePairs = emptyList())
            return false
        }
    }
    
    /**
     * Рассчитывает цену по таблице - аналог Calculate() из Windows TorgList
     */
    private fun calculatePriceByTable(price: Int): Int {
        if (pricePairs.isEmpty()) return price
        
        // Ищем точное попадание в диапазон
        for (pair in pricePairs) {
            if (price >= pair.priceLow && price <= pair.priceHigh) {
                return price + pair.bonus
            }
        }
        
        // Ищем ближайший диапазон
        var bonus = 0
        var diffMin = Int.MAX_VALUE
        
        for (pair in pricePairs) {
            if (price < pair.priceLow) continue
            
            val diff = price - pair.priceLow
            if (diff >= diffMin) continue
            
            diffMin = diff
            bonus = price + pair.bonus
        }
        
        return bonus
    }
    
    /**
     * Фильтрует сообщения - аналог DoFilter() из Windows TorgList
     */
    fun filterMessage(
        message: String,
        thing: String,
        thingLevel: String,
        price: Int,
        tablePrice: Int,
        thingRealDurability: Int,
        thingFullDurability: Int,
        price90: Int
    ): String {
        return message
            .replace("{таблица}", _uiState.value.tableString)
            .replace("{вещь}", thing)
            .replace("{вещьур}", thingLevel)
            .replace("{вещьдолг}", "$thingRealDurability/$thingFullDurability")
            .replace("{цена}", price.toString())
            .replace("{минцена}", tablePrice.toString())
            .replace("{цена90}", price90.toString())
    }
}

/**
 * UI состояние торговой системы
 */
data class TradingUiState(
    val tableString: String = "",
    val isTableValid: Boolean = true,
    val pricePairs: List<TradingPricePair> = emptyList(),
    
    // Автоответчик
    val messageAdv: String = "",
    val advTime: String = "600",
    val messageNoMoney: String = "Извините, недостаточно денег",
    val messageTooExp: String = "Слишком дорого",
    val messageThanks: String = "Спасибо за покупку!",
    val messageLess90: String = "Цена слишком низкая",
    
    // Настройки торговли
    val sliv: String = "10",
    val minLevel: String = "1",
    val ex: String = "",
    val deny: String = "",
    
    // Тестирование
    val testPrice: String = "",
    val calculatedPrice: Int = 0
)

/**
 * Пара цен - аналог TorgPair из Windows версии
 */
data class TradingPricePair(
    val priceLow: Int,
    val priceHigh: Int,
    val bonus: Int
)
