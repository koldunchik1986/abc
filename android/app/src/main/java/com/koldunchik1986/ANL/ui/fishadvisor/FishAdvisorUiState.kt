package com.koldunchik1986.ANL.ui.fishadvisor

/**
 * UI состояние советника по рыбалке
 */
data class FishAdvisorUiState(
    val fishUmInput: String = "",
    val fishUm: Int = 0,
    val isFishUmValid: Boolean = true,
    val maxBotLevel: Int = 8,
    val filteredFishTips: List<FishTip> = emptyList()
)

/**
 * Наживка - аналог FishTip из Windows клиента
 */
data class FishTip(
    val money: Int,
    val fishUm: Int,
    val location: String,
    val maxBotLevel: Int,
    val botDescription: String,
    val tip: String
) : Comparable<FishTip> {
    override fun compareTo(other: FishTip): Int {
        return this.money.compareTo(other.money)
    }
}

/**
 * Элемент списка уровней рыбаков - аналог ListItemBotLevelEx из Windows клиента
 */
data class BotLevelItem(
    val botLevel: String,
    val botLevelValue: Int
) {
    constructor(level: Int) : this(level.toString(), level)
}