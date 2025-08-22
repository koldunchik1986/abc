package com.koldunchik1986.ANL.ui.navigator

/**
 * UI ��������� ����������
 */
data class NavigatorUiState(
    val title: String = "���������",
    val currentDestination: String = "",
    val destinationInput: String = "",
    val isDestinationValid: Boolean = true,
    val searchText: String = "",
    val canStartMoving: Boolean = false,
    val locationGroups: List<LocationGroup> = emptyList(),
    val routeInfo: RouteInfo = RouteInfo()
)

/**
 * ���������� � ��������
 */
data class RouteInfo(
    val pathExists: Boolean = false,
    val jumps: Int? = null,
    val botLevel: Int? = null,
    val tiedPercentage: Int? = null
)

/**
 * ������ �������
 */
data class LocationGroup(
    val title: String,
    val locations: List<LocationInfo>
)

/**
 * ���������� � �������
 */
data class LocationInfo(
    val cellNumber: String,
    val name: String,
    val tooltip: String
)

/**
 * ������ ����� (������ GroupCell �� Windows ������)
 */
data class GroupCell(
    val title: String,
    val level: Int = 0,
    val cells: MutableMap<String, String> = mutableMapOf()
) {
    fun addCell(cellNumber: String) {
        cells[cellNumber] = cellNumber
    }
    
    fun getCells(): String {
        return cells.keys.joinToString("|")
    }
    
    override fun toString(): String {
        return if (level > 0) {
            "$title $level"
        } else {
            title
        }
    }
}

/**
 * ������ ����� (���������� ������ Cell �� Windows)
 */
data class MapCell(
    val cellNumber: String,
    val name: String,
    val tooltip: String,
    val herbGroup: String = "",
    val mapBots: List<MapBot> = emptyList(),
    val hasFish: Boolean = false,
    val x: Int = 0,
    val y: Int = 0
) {
    fun isBot(pattern: String): Boolean {
        return mapBots.any { bot ->
            bot.name.contains(pattern, ignoreCase = true)
        }
    }
}

/**
 * ��� �� �����
 */
data class MapBot(
    val name: String,
    val maxLevel: Int,
    val minLevel: Int = 0
)

/**
 * ���� �� ����� (������ MapPath �� Windows ������)
 */
data class MapPath(
    val start: String,
    val destinations: List<String>
) {
    var pathExists: Boolean = false
        private set
    var destination: String = ""
        private set
    var jumps: Int = 0
        private set
    var botLevel: Int = 0
        private set
    
    init {
        calculatePath()
    }
    
    private fun calculatePath() {
        // ���������� ������ ������� ����
        // � ���������� ����� ����� ������� ������ ������ ���� �� �����
        if (destinations.isNotEmpty() && isValidCellNumber(start)) {
            val dest = destinations.first()
            if (isValidCellNumber(dest)) {
                pathExists = true
                destination = dest
                jumps = kotlin.random.Random.nextInt(1, 15)
                botLevel = kotlin.random.Random.nextInt(8, 20)
            }
        }
    }
    
    private fun isValidCellNumber(cellNumber: String): Boolean {
        return cellNumber.matches(Regex("\\d{1,2}-\\d{3}"))
    }
}
