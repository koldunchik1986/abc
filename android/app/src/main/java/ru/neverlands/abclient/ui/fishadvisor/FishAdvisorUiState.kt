package ru.neverlands.abclient.ui.fishadvisor

/**
 * UI состояние советника рыбака
 */
data class FishAdvisorUiState(
    val fishUmInput: String = "",
    val fishUm: Int = 0,
    val isFishUmValid: Boolean = true,
    val maxBotLevel: Int = 8,
    val filteredFishTips: List<FishTip> = emptyList()
)

/**
 * Совет рыбака - аналог FishTip из Windows версии
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
 * Элемент списка уровней ботов - аналог ListItemBotLevelEx из Windows версии
 */
data class BotLevelItem(
    val botLevel: String,
    val botLevelValue: Int
) {
    constructor(level: Int) : this(level.toString(), level)
}