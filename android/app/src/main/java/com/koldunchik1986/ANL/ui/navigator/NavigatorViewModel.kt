package com.koldunchik1986.ANL.ui.navigator

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
class NavigatorViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(NavigatorUiState())
    val uiState: StateFlow<NavigatorUiState> = _uiState.asStateFlow()
    
    private var currentProfile: UserProfile? = null
    
    init {
        loadProfile()
        loadStandardLocations()
    }
    
    fun initialize(
        destination: String?,
        location: String?,
        location2: String?,
        nick: String?
    ) {
        viewModelScope.launch {
            val initialDestination = destination ?: currentProfile?.mapLocation ?: ""
            
            // Установка заголовка в зависимости от наличия никнейма
            val title = if (nick != null) {
                "Поиск по никнейму $nick"
            } else {
                "Поиск"
            }
            
            _uiState.value = _uiState.value.copy(
                title = title,
                currentDestination = initialDestination,
                destinationInput = initialDestination
            )
            
            // Если есть координаты
            if (location == "Компас" && location2 != null) {
                findCompassLocations(location2)
            } else {
                calculateRoute()
            }
        }
    }
    
    fun setDestination(cellNumber: String) {
        _uiState.value = _uiState.value.copy(
            currentDestination = cellNumber,
            destinationInput = cellNumber,
            isDestinationValid = isValidCellNumber(cellNumber)
        )
        calculateRoute()
    }
    
    fun setSearchText(text: String) {
        _uiState.value = _uiState.value.copy(
            searchText = text
        )
        
        if (text.isEmpty()) {
            loadStandardLocations()
        } else {
            searchLocations(text)
        }
    }
    
    fun calculateRoute() {
        val destination = _uiState.value.currentDestination
        if (destination.isEmpty() || !isValidCellNumber(destination)) {
            _uiState.value = _uiState.value.copy(
                routeInfo = RouteInfo(),
                canStartMoving = false,
                title = "Поиск"
            )
            return
        }
        
        viewModelScope.launch {
            // Расчет маршрута (в реальном проекте будет взято из Map.Cells)
            val routeInfo = calculateRouteInfo(destination)
            val title = if (routeInfo.pathExists) {
                "Маршрут на $destination"
            } else {
                "Поиск"
            }
            
            _uiState.value = _uiState.value.copy(
                routeInfo = routeInfo,
                canStartMoving = routeInfo.pathExists,
                title = title
            )
        }
    }
    
    fun startAutoMoving() {
        val destination = _uiState.value.currentDestination
        if (destination.isNotEmpty() && _uiState.value.canStartMoving) {
            viewModelScope.launch {
                // Установка глобальной переменной и запуск автодвижения
                // AppVars.AutoMovingDestinaton = destination
                // AppVars.AutoMoving = true
            }
        }
    }
    
    fun addToFavorites() {
        val destination = _uiState.value.currentDestination
        if (destination.isNotEmpty()) {
            viewModelScope.launch {
                currentProfile?.let { profile ->
                    val updatedFavorites = (profile.favLocations + destination).distinct()
                    val updatedProfile = profile.copy(favLocations = updatedFavorites)
                    profileRepository.saveProfile(updatedProfile)
                    currentProfile = updatedProfile
                    loadStandardLocations()
                }
            }
        }
    }
    
    fun clearFavorites() {
        viewModelScope.launch {
            currentProfile?.let { profile ->
                val updatedProfile = profile.copy(favLocations = emptyList())
                profileRepository.saveProfile(updatedProfile)
                currentProfile = updatedProfile
                loadStandardLocations()
            }
        }
    }
    
    private fun loadProfile() {
        viewModelScope.launch {
            currentProfile = profileRepository.getCurrentProfile()
        }
    }
    
    private fun loadStandardLocations() {
        val groups = mutableListOf<LocationGroup>()
        
        // Избранные локации
        currentProfile?.favLocations?.let { favorites ->
            if (favorites.isNotEmpty()) {
                val favoriteLocations = favorites.map { cellNumber ->
                    LocationInfo(
                        cellNumber = cellNumber,
                        name = "$cellNumber. ${getLocationTooltip(cellNumber)}",
                        tooltip = getLocationTooltip(cellNumber)
                    )
                }
                groups.add(
                    LocationGroup(
                        title = "Избранные локации",
                        locations = favoriteLocations
                    )
                )
            }
        }
        
        // Города
        groups.add(
            LocationGroup(
                title = "Города",
                locations = listOf(
                    LocationInfo("8-259", "8-259. Столица", "Столица"),
                    LocationInfo("8-294", "8-294. Столица", "Столица")
                )
            )
        )
        
        // Деревни
        groups.add(
            LocationGroup(
                title = "Деревни",
                locations = listOf(
                    LocationInfo("8-197", "8-197. Деревня", "Деревня")
                )
            )
        )
        
        // Порты
        groups.add(
            LocationGroup(
                title = "Порты",
                locations = listOf(
                    LocationInfo("12-428", "12-428. Порт", "Порт"),
                    LocationInfo("12-494", "12-494. Порт", "Порт"),
                    LocationInfo("12-521", "12-521. Порт", "Порт")
                )
            )
        )
        
        // Леса (различные типы)
        groups.add(
            LocationGroup(
                title = "Леса",
                locations = getLocationsByPattern("лес")
            )
        )
        
        // Шахта уровня 1, 2, 3
        groups.add(
            LocationGroup(
                title = "Шахта уровня 1",
                locations = getLocationsByPattern("ШахтA")
            )
        )
        
        groups.add(
            LocationGroup(
                title = "Шахта уровня 2",
                locations = getLocationsByPattern("ШахтB")
            )
        )
        
        groups.add(
            LocationGroup(
                title = "Шахта уровня 3",
                locations = getLocationsByPattern("ШахтC")
            )
        )
        
        // Боты (будут добавляться при интеграции с картой)
        groups.add(
            LocationGroup(
                title = "Боты",
                locations = getBotLocations()
            )
        )
        
        // Травы (будут добавляться при интеграции с картой)
        groups.add(
            LocationGroup(
                title = "Травы",
                locations = getHerbLocations()
            )
        )
        
        // Пещеры
        groups.add(
            LocationGroup(
                title = "Пещеры",
                locations = getLocationsByPattern("пещера")
            )
        )
        
        // Рыба
        groups.add(
            LocationGroup(
                title = "Рыба",
                locations = getFishingLocations()
            )
        )
        
        // Ресурсы
        groups.add(
            LocationGroup(
                title = "Ресурсы",
                locations = getLocationsByPattern("руда")
            )
        )
        
        // Подземелье
        groups.add(
            LocationGroup(
                title = "Подземелье",
                locations = getLocationsByPattern("подземелье")
            )
        )
        
        // Телепорты
        groups.add(
            LocationGroup(
                title = "Телепорты",
                locations = getTeleportLocations()
            )
        )
        
        // Магазины
        groups.add(
            LocationGroup(
                title = "Магазины",
                locations = listOf(
                    LocationInfo("8-227", "8-227. Магазин", "Магазин"),
                    LocationInfo("2-482", "2-482. Магазин", "Магазин"),
                    LocationInfo("9-494", "9-494. Магазин", "Магазин"),
                    LocationInfo("26-430", "26-430. Магазин", "Магазин")
                )
            )
        )
        
        // Ресурсы охотника
        groups.add(
            LocationGroup(
                title = "Ресурсы охотника",
                locations = getHunterResourceLocations()
            )
        )
        
        _uiState.value = _uiState.value.copy(
            locationGroups = groups
        )
    }
    
    private fun searchLocations(pattern: String) {
        val filteredLocations = getLocationsByTooltipPattern(pattern)
        
        val groups = if (filteredLocations.isNotEmpty()) {
            listOf(
                LocationGroup(
                    title = "Найденные локации",
                    locations = filteredLocations
                )
            )
        } else {
            emptyList()
        }
        
        _uiState.value = _uiState.value.copy(
            locationGroups = groups
        )
    }
    
    private fun findCompassLocations(location2: String) {
        val compassLocations = getLocationsByTooltipPattern(location2)
        
        if (compassLocations.isNotEmpty()) {
            val firstLocation = compassLocations.first()
            _uiState.value = _uiState.value.copy(
                currentDestination = firstLocation.cellNumber,
                destinationInput = firstLocation.cellNumber
            )
            calculateRoute()
        }
    }
    
    private fun calculateRouteInfo(destination: String): RouteInfo {
        // Расчет маршрута
        // в реальном проекте будет взято из MapPath в Windows
        val startLocation = currentProfile?.mapLocation ?: "8-259"
        
        return if (isValidCellNumber(destination)) {
            RouteInfo(
                pathExists = true,
                jumps = kotlin.random.Random.nextInt(1, 10),
                botLevel = kotlin.random.Random.nextInt(8, 20),
                tiedPercentage = kotlin.random.Random.nextInt(20, 80)
            )
        } else {
            RouteInfo()
        }
    }
    
    private fun isValidCellNumber(cellNumber: String): Boolean {
        return cellNumber.matches(Regex("\\d{1,2}-\\d{3}"))
    }
    
    private fun getLocationTooltip(cellNumber: String): String {
        // Расчет - из Map.Cells[cellNumber].Tooltip
        return when (cellNumber) {
            "8-259", "8-294" -> "Столица"
            "8-197" -> "Деревня"
            "12-428", "12-494", "12-521" -> "Порт"
            else -> "Неизвестная локация"
        }
    }
    
    private fun getLocationsByPattern(pattern: String): List<LocationInfo> {
        // Расчет - из Map.Cells
        return when (pattern) {
            "лес" -> listOf(
                LocationInfo("8-300", "8-300. Лес", "Лес"),
                LocationInfo("8-301", "8-301. Лес", "Лес")
            )
            "ШахтA" -> listOf(
                LocationInfo("8-400", "8-400. ШахтаA", "ШахтаA"),
                LocationInfo("8-401", "8-401. ШахтаA", "ШахтаA")
            )
            "ШахтB" -> listOf(
                LocationInfo("8-410", "8-410. ШахтаB", "ШахтаB"),
                LocationInfo("8-411", "8-411. ШахтаB", "ШахтаB")
            )
            "ШахтC" -> listOf(
                LocationInfo("8-420", "8-420. ШахтаC", "ШахтаC"),
                LocationInfo("8-421", "8-421. ШахтаC", "ШахтаC")
            )
            "пещера" -> listOf(
                LocationInfo("8-500", "8-500. Пещера", "Пещера"),
                LocationInfo("8-501", "8-501. Подземелье", "Подземелье")
            )
            "руда" -> listOf(
                LocationInfo("8-600", "8-600. Руда", "Руда"),
                LocationInfo("8-601", "8-601. Руда", "Руда")
            )
            "подземелье" -> listOf(
                LocationInfo("8-700", "8-700. Подземелье", "Подземелье"),
                LocationInfo("8-701", "8-701. Подземелье", "Подземелье")
            )
            else -> emptyList()
        }
    }
    
    private fun getLocationsByTooltipPattern(pattern: String): List<LocationInfo> {
        // Расчет - из Map.Cells по Tooltip
        return listOf(
            LocationInfo("8-999", "8-999. $pattern", pattern)
        )
    }
    
    private fun getBotLocations(): List<LocationInfo> {
        // Расчет - из ботов в Map.Bots
        return listOf(
            LocationInfo("8-800", "8-800. Бот (8-12)", "Бот"),
            LocationInfo("8-801", "8-801. Бот (15)", "Бот"),
            LocationInfo("8-802", "8-802. Бот (19)", "Бот")
        )
    }
    
    private fun getHerbLocations(): List<LocationInfo> {
        // Расчет - из трав в Map.Herbs
        return listOf(
            LocationInfo("8-850", "8-850. Трава (Трава 1)", "Трава"),
            LocationInfo("8-851", "8-851. Трава (Трава 2)", "Трава")
        )
    }
    
    private fun getFishingLocations(): List<LocationInfo> {
        // Расчет - из Map.Cells по HasFish
        return listOf(
            LocationInfo("8-900", "8-900. Рыба", "Рыба"),
            LocationInfo("8-901", "8-901. Рыба", "Рыба")
        )
    }
    
    private fun getTeleportLocations(): List<LocationInfo> {
        // Расчет - из Map.Teleports
        return listOf(
            LocationInfo("8-950", "8-950. Телепорт", "Телепорт"),
            LocationInfo("8-951", "8-951. Телепорт", "Телепорт")
        )
    }
    
    private fun getHunterResourceLocations(): List<LocationInfo> {
        // Расчет - PopulateSkinRes в Windows
        return listOf(
            LocationInfo("8-200", "(5 шт) Сокровище ресурсов (0+)", "Ресурс"),
            LocationInfo("8-201", "(3 шт) Сокровище ресурсов (15+)", "Ресурс"),
            LocationInfo("8-202", "(4 шт) Сокровище ресурсов (50+)", "Ресурс"),
            LocationInfo("8-203", "(3 шт) Сокровище ресурсов (65+)", "Ресурс"),
            LocationInfo("8-204", "(2 шт) Сокровище ресурсов (80+)", "Ресурс"),
            LocationInfo("8-205", "(2 шт) Сокровище ресурсов (100+)", "Ресурс"),
            LocationInfo("8-206", "(6 шт) Сокровище ресурсов (120+)", "Ресурс"),
            LocationInfo("8-207", "(4 шт) Сокровище ресурсов (130+)", "Ресурс"),
            LocationInfo("8-208", "(3 шт) Сокровище ресурсов (150+)", "Ресурс"),
            LocationInfo("8-209", "(5 шт) Сокровище ресурсов (180+)", "Ресурс"),
            LocationInfo("8-210", "(4 шт) Сокровище ресурсов (200+)", "Ресурс"),
            LocationInfo("8-211", "(3 шт) Сокровище ресурсов (220+)", "Ресурс"),
            LocationInfo("8-212", "(7 шт) Сокровище ресурсов (235+)", "Ресурс"),
            LocationInfo("8-213", "(5 шт) Сокровище ресурсов (250+)", "Ресурс"),
            LocationInfo("8-214", "(6 шт) Сокровище ресурсов (260+)", "Ресурс"),
            LocationInfo("8-215", "(4 шт) Сокровище ресурсов (280+)", "Ресурс"),
            LocationInfo("8-216", "(3 шт) Сокровище ресурсов (300+)", "Ресурс"),
            LocationInfo("8-217", "(2 шт) Сокровище ресурсов (320+)", "Ресурс"),
            LocationInfo("8-218", "(2 шт) Сокровище ресурсов (350+)", "Ресурс"),
            LocationInfo("8-219", "(4 шт) Сокровище ресурсов (400+)", "Ресурс"),
            LocationInfo("8-220", "(3 шт) Сокровище ресурсов (500+)", "Ресурс"),
            LocationInfo("8-221", "(2 шт) Сокровище ресурсов (600+)", "Ресурс"),
            LocationInfo("8-222", "(1 шт) Сокровище ресурсов (700+)", "Ресурс")
        )
    }
}
