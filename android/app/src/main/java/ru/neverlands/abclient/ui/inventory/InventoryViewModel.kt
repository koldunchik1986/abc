package ru.neverlands.abclient.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.neverlands.abclient.data.repository.ProfileRepository
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
            // В реальности здесь будет загрузка из HTML страницы инвентаря
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
            // Здесь будет вызов ссылки WearLink из Windows версии
            // location.href = item.wearLink
        }
    }
    
    fun useItem(item: InventoryItem) {
        viewModelScope.launch {
            // Здесь будет вызов магического использования предмета
            // magicreform(uid, name, vcode)
        }
    }
    
    fun sellItem(item: InventoryItem) {
        viewModelScope.launch {
            // Здесь будет вызов ссылки PssLink из Windows версии
            // location.href = item.pssLink
        }
    }
    
    fun dropItem(item: InventoryItem) {
        viewModelScope.launch {
            // Здесь будет вызов ссылки DropLink из Windows версии
            // if(confirm('Вы точно хотите продать < item.name > за item.price NV?'))
            // location.href = item.dropLink
        }
    }
    
    fun bulkSellItem(item: InventoryItem) {
        if (item.count <= 1) return
        
        viewModelScope.launch {
            // Аналог AddBulkSell из Windows версии
            // StartBulkSell(item.name, item.sellPrice, item.pssLink)
            val totalPrice = item.sellPrice * item.count
            // Показать подтверждение: "Вы точно хотите продать все предметы < ${item.name} > по ${item.sellPrice} NV за общую сумму $totalPrice NV?"
        }
    }
    
    fun bulkDropItem(item: InventoryItem) {
        if (item.count <= 1) return
        
        viewModelScope.launch {
            // Аналог AddBulkDelete из Windows версии
            // StartBulkDrop(item.name, item.price)
            // Показать подтверждение: "Вы точно хотите выбросить всю пачку < ${item.name} >?"
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
     * Создает mock данные для демонстрации - в реальности будет парсинг HTML
     */
    private fun createMockInventoryItems(): List<InventoryItem> {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val now = Date()
        val expiredDate = Date(now.time - 24 * 60 * 60 * 1000) // Вчера
        val futureDate = Date(now.time + 7 * 24 * 60 * 60 * 1000) // Через неделю
        
        return listOf(
            InventoryItem(
                name = "Меч Воина",
                imageUrl = "http://image.neverlands.ru/weapon/sword.gif",
                count = 1,
                price = 150,
                sellPrice = 120,
                level = 10,
                mass = 5,
                durability = "10/10",
                properties = "Урон: +15, Точность: +5",
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
                name = "Зелье Лечения",
                imageUrl = "http://image.neverlands.ru/potion/heal.gif",
                count = 5,
                price = 50,
                sellPrice = 40,
                level = 1,
                mass = 1,
                durability = "7/7",
                properties = "Восстанавливает 100 HP",
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
                name = "Просроченное Зелье",
                imageUrl = "http://image.neverlands.ru/potion/expired.gif",
                count = 2,
                price = 30,
                sellPrice = 25,
                level = 1,
                mass = 1,
                durability = "5/7",
                properties = "Восстанавливает 50 HP",
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
                name = "Кристальное Кольцо (ап)",
                imageUrl = "http://image.neverlands.ru/accessory/crystal_ring.gif",
                count = 1,
                price = 300,
                sellPrice = 270,
                level = 15,
                mass = 1,
                durability = "20/20",
                properties = "Мана: +50, Интеллект: +3",
                canWear = true,
                canUse = false,
                canSell = true,
                wearLink = "main.php?wear=126",
                pssLink = "main.php?sell=126",
                dropLink = "main.php?drop=126",
                expiryDate = "",
                isExpired = false
            ),
            InventoryItem(
                name = "Хлеб",
                imageUrl = "http://image.neverlands.ru/food/bread.gif",
                count = 10,
                price = 5,
                sellPrice = 4,
                level = 0,
                mass = 1,
                durability = "",
                properties = "Утоляет голод",
                canWear = false,
                canUse = true,
                canSell = true,
                wearLink = "",
                pssLink = "main.php?sell=127",
                dropLink = "main.php?drop=127",
                expiryDate = "",
                isExpired = false
            )
        )
    }
}

/**
 * UI состояние инвентаря
 */
data class InventoryUiState(
    val items: List<InventoryItem> = emptyList(),
    val filteredItems: List<InventoryItem> = emptyList(),
    val searchFilter: String = "",
    val showExpired: Boolean = false,
    val isCompactView: Boolean = false,
    val expiredItemsCount: Int = 0,
    val isLoading: Boolean = false
)

/**
 * Предмет инвентаря - аналог InvEntry из Windows версии
 */
data class InventoryItem(
    val name: String,
    val imageUrl: String,
    val count: Int = 1,
    val price: Int = 0,
    val sellPrice: Int = 0,
    val level: Int = 0,
    val mass: Int = 0,
    val durability: String = "",
    val properties: String = "",
    val canWear: Boolean = false,
    val canUse: Boolean = false,
    val canSell: Boolean = false,
    val wearLink: String = "",
    val pssLink: String = "",
    val dropLink: String = "",
    val expiryDate: String = "",
    val isExpired: Boolean = false
) {
    /**
     * Аналог Build() метода из Windows InvEntry
     */
    fun buildHtml(): String {
        // В реальности здесь будет построение HTML как в Windows версии
        return "<div>$name</div>"
    }
    
    /**
     * Проверяет истечение срока - аналог IsExpired() из Windows версии
     */
    fun checkExpiry(): Boolean {
        if (expiryDate.isEmpty()) return false
        
        return try {
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val expiry = dateFormat.parse(expiryDate)
            val now = Date()
            expiry?.before(now) ?: false
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Компаратор для сортировки предметов - аналог InvComparer из Windows версии
 */
class InventoryItemComparator : Comparator<InventoryItem> {
    override fun compare(o1: InventoryItem, o2: InventoryItem): Int {
        // Сначала по имени
        var result = o1.name.compareTo(o2.name)
        if (result != 0) return result
        
        // Затем по изображению
        result = o1.imageUrl.compareTo(o2.imageUrl)
        if (result != 0) return result
        
        // Просроченные вниз
        if (o1.isExpired != o2.isExpired) {
            return o1.isExpired.compareTo(o2.isExpired)
        }
        
        // По уровню
        result = o1.level.compareTo(o2.level)
        if (result != 0) return result
        
        // По свойствам
        result = o1.properties.compareTo(o2.properties)
        return result
    }
}