package com.koldunchik1986.ANL.data.model

/**
 * Модель предмета инвентаря
 */
data class InventoryItem(
    val name: String,
    val imageUrl: String,
    val count: Int,
    val price: Int,
    val sellPrice: Int,
    val level: Int,
    val mass: Int,
    val durability: String,
    val properties: String,
    val canWear: Boolean,
    val canUse: Boolean,
    val canSell: Boolean,
    val wearLink: String,
    val pssLink: String,
    val dropLink: String,
    val expiryDate: String,
    val isExpired: Boolean
)