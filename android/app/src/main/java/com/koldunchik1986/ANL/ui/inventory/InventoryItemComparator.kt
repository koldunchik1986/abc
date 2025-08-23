package com.koldunchik1986.ANL.ui.inventory

import com.koldunchik1986.ANL.data.model.InventoryItem
import java.util.*

/**
 * Компаратор для сортировки предметов инвентаря
 */
class InventoryItemComparator : Comparator<InventoryItem> {
    override fun compare(item1: InventoryItem, item2: InventoryItem): Int {
        // Сначала сортируем по просроченности (просроченные в конце)
        if (item1.isExpired != item2.isExpired) {
            return if (item1.isExpired) 1 else -1
        }
        
        // Затем по уровню
        val levelComparison = item2.level.compareTo(item1.level)
        if (levelComparison != 0) {
            return levelComparison
        }
        
        // Затем по названию
        return item1.name.compareTo(item2.name, ignoreCase = true)
    }
}