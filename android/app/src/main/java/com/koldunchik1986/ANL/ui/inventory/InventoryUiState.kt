package com.koldunchik1986.ANL.ui.inventory

import com.koldunchik1986.ANL.data.model.InventoryItem

/**
 * Состояние UI для экрана инвентаря
 */
data class InventoryUiState(
    val items: List<InventoryItem> = emptyList(),
    val filteredItems: List<InventoryItem> = emptyList(),
    val searchFilter: String = "",
    val showExpired: Boolean = false,
    val isCompactView: Boolean = false,
    val expiredItemsCount: Int = 0
)