package com.koldunchik1986.ANL.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.koldunchik1986.ANL.data.repository.ProfileRepository
import com.koldunchik1986.ANL.data.model.InventoryItem
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()
    
    fun loadInventory() {
        viewModelScope.launch {
            // В реальном приложении данные будут загружаться из HTML ответа сервера
            val mockItems = createMockInventoryItems()
            
            _uiState.value = _uiState.value.copy(
                items = mockItems,
                filteredItems = filterItems(mockItems, _uiState.value.searchFilter, _uiState.value.showExpired),
                expiredItemsCount = mockItems.count { it.isExpired }
            )
        }
    }
    
    fun refreshInventory() {
        loadInventory()
    }
    
    fun sortInventory() {
        val sortedItems = _uiState.value.items.sortedWith(InventoryItemComparator())
        _uiState.value = _uiState.value.copy(
            items = sortedItems,
            filteredItems = filterItems(sortedItems, _uiState.value.searchFilter, _uiState.value.showExpired)
        )
    }
    
    fun setFilter(filter: String) {
        _uiState.value = _uiState.value.copy(
            searchFilter = filter,
            filteredItems = filterItems(_uiState.value.items, filter, _uiState.value.showExpired)
        )
    }
    
    fun toggleShowExpired() {
        val newShowExpired = !_uiState.value.showExpired
        _uiState.value = _uiState.value.copy(
            showExpired = newShowExpired,
            filteredItems = filterItems(_uiState.value.items, _uiState.value.searchFilter, newShowExpired)
        )
    }
    
    fun toggleViewMode() {
        _uiState.value = _uiState.value.copy(
            isCompactView = !_uiState.value.isCompactView
        )
    }
    
    fun wearItem(item: InventoryItem) {
        viewModelScope.launch {
            // Здесь будет реализация WearLink из Windows клиента
            // location.href = item.wearLink
        }
    }
    
    fun useItem(item: InventoryItem) {
        viewModelScope.launch {
            // Здесь будет реализация функции использования предмета
            // magicreform(uid, name, vcode)
        }
    }
    
    fun sellItem(item: InventoryItem) {
        viewModelScope.launch {
            // Здесь будет реализация PssLink из Windows клиента
            // location.href = item.pssLink
        }
    }
    
    fun dropItem(item: InventoryItem) {
        viewModelScope.launch {
            // Здесь будет реализация DropLink из Windows клиента
            // if(confirm('Вы хотите удалить предмет < item.name > за item.price NV?'))
            // location.href = item.dropLink
        }
    }
    
    fun bulkSellItem(item: InventoryItem) {
        if (item.count <= 1) return
        
        viewModelScope.launch {
            // Аналог AddBulkSell из Windows клиента
            // StartBulkSell(item.name, item.sellPrice, item.pssLink)
            val totalPrice = item.sellPrice * item.count
            // Отправить подтверждение: "Вы хотите продать все предметы < ${item.name} > по ${item.sellPrice} NV за общую сумму $totalPrice NV?"
        }
    }
    
    fun bulkDropItem(item: InventoryItem) {
        if (item.count <= 1) return
        
        viewModelScope.launch {
            // Аналог AddBulkDelete из Windows клиента
            // StartBulkDrop(item.name, item.price)
            // Отправить подтверждение: "Вы хотите удалить все предметы < ${item.name} >?"
        }
    }
    
    private fun filterItems(items: List<InventoryItem>, filter: String, showExpired: Boolean): List<InventoryItem> {
        return items.filter { item ->
            val matchesFilter = filter.isEmpty() || item.name.contains(filter, ignoreCase = true)
            val matchesExpired = showExpired || !item.isExpired
            matchesFilter && matchesExpired
        }
    }
    
    /**
     * Создание mock предметов для демонстрации - в реальном приложении данные будут из HTML
     */
    private fun createMockInventoryItems(): List<InventoryItem> {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val now = Date()
        val expiredDate = Date(now.time - 24 * 60 * 60 * 1000) // Вчера
        val futureDate = Date(now.time + 7 * 24 * 60 * 60 * 1000) // Через неделю
        
        return listOf(
            InventoryItem(
                name = "Железный меч",
                imageUrl = "http://image.neverlands.ru/weapon/sword.gif",
                count = 1,
                price = 150,
                sellPrice = 120,
                level = 10,
                mass = 5,
                durability = "10/10",
                properties = "Урон: +15, Защита: +5",
                canWear = true,
                canUse = false,
                canSell = true,
                wearLink = "main.php?wear=123",
                pssLink = "main.php?sell=123",
                dropLink = "main.php?drop=123",
                expiryDate = "",
                isExpired = false
            ),
            InventoryItem(
                name = "Лечебное зелье",
                imageUrl = "http://image.neverlands.ru/potion/heal.gif",
                count = 5,
                price = 50,
                sellPrice = 40,
                level = 1,
                mass = 1,
                durability = "7/7",
                properties = "Восстановление 100 HP",
                canWear = false,
                canUse = true,
                canSell = true,
                wearLink = "",
                pssLink = "main.php?sell=124",
                dropLink = "main.php?drop=124",
                expiryDate = dateFormat.format(futureDate),
                isExpired = false
            ),
            InventoryItem(
                name = "Просроченное зелье",
                imageUrl = "http://image.neverlands.ru/potion/expired.gif",
                count = 2,
                price = 30,
                sellPrice = 25,
                level = 1,
                mass = 1,
                durability = "5/7",
                properties = "Восстановление 50 HP",
                canWear = false,
                canUse = false,
                canSell = true,
                wearLink = "",
                pssLink = "main.php?sell=125",
                dropLink = "main.php?drop=125",
                expiryDate = dateFormat.format(expiredDate),
                isExpired = true
            ),
            InventoryItem(
                name = "Хрустальный кольце (но)",
                imageUrl = "http://image.neverlands.ru/accessory/crystal_ring.gif",
                count = 1,
                price = 300,
                sellPrice = 270,
                level = 15,
                mass = 2,
                durability = "1/1",
                properties = "Маг. урон: +20, Защита: +10",
                canWear = true,
                canUse = false,
                canSell = true,
                wearLink = "main.php?wear=126",
                pssLink = "main.php?sell=126",
                dropLink = "main.php?drop=126",
                expiryDate = "",
                isExpired = false
            )
        )
    }
}