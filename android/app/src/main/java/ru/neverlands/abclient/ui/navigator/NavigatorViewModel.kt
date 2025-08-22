package ru.neverlands.abclient.ui.navigator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.neverlands.abclient.data.repository.ProfileRepository
import ru.neverlands.abclient.data.model.UserProfile
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
            
            // Обновляем заголовок для компаса
            val title = if (nick != null) {
                "Возможное местонахождение $nick"
            } else {
                "Навигатор"
            }
            
            _uiState.value = _uiState.value.copy(
                title = title,
                currentDestination = initialDestination,
                destinationInput = initialDestination
            )
            
            // Если это поиск по компасу
            if (location == "Природа" && location2 != null) {
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
                title = "Навигатор"
            )
            return
        }
        
        viewModelScope.launch {
            // Имитация расчета маршрута (в реальности здесь будет обращение к Map.Cells)
            val routeInfo = calculateRouteInfo(destination)
            val title = if (routeInfo.pathExists) {
                "Маршрут до $destination"
            } else {
                "Навигатор"
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
                // Здесь будет реализована логика автодвижения
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
        
        // Запомненные локации
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
                        title = "Запомненные локации",
                        locations = favoriteLocations
                    )
                )
            }
        }
        
        // Форпост
        groups.add(
            LocationGroup(
                title = "Форпост",
                locations = listOf(
                    LocationInfo("8-259", "8-259. Форпост", "Форпост"),
                    LocationInfo("8-294", "8-294. Форпост", "Форпост")
                )
            )
        )
        
        // Деревня
        groups.add(
            LocationGroup(
                title = "Деревня",
                locations = listOf(
                    LocationInfo("8-197", "8-197. Деревня", "Деревня")
                )
            )
        )
        
        // Октал
        groups.add(
            LocationGroup(
                title = "Октал",
                locations = listOf(
                    LocationInfo("12-428", "12-428. Октал", "Октал"),
                    LocationInfo("12-494", "12-494. Октал", "Октал"),
                    LocationInfo("12-521", "12-521. Октал", "Октал")
                )
            )
        )
        
        // Замки (примерные данные)
        groups.add(
            LocationGroup(
                title = "Замки",
                locations = getLocationsByPattern("замок")
            )
        )
        
        // Форты группы 1, 2, 3
        groups.add(
            LocationGroup(
                title = "Форты группы 1",
                locations = getLocationsByPattern("ФортGA")
            )
        )
        
        groups.add(
            LocationGroup(
                title = "Форты группы 2",
                locations = getLocationsByPattern("ФортGB")
            )
        )
        
        groups.add(
            LocationGroup(
                title = "Форты группы 3",
                locations = getLocationsByPattern("ФортGC")
            )
        )
        
        // Боты (будет реализовано при интеграции с картой)
        groups.add(
            LocationGroup(
                title = "Боты",
                locations = getBotLocations()
            )
        )
        
        // Травы (будет реализовано при интеграции с картой)
        groups.add(
            LocationGroup(
                title = "Травы",
                locations = getHerbLocations()
            )
        )
        
        // Шахты
        groups.add(
            LocationGroup(
                title = "Шахты",
                locations = getLocationsByPattern("шахта")
            )
        )
        
        // Рыба
        groups.add(
            LocationGroup(
                title = "Рыба",
                locations = getFishingLocations()
            )
        )
        
        // Причалы
        groups.add(
            LocationGroup(
                title = "Причалы",
                locations = getLocationsByPattern("причал")
            )
        )
        
        // Лесопилки
        groups.add(
            LocationGroup(
                title = "Лесопилки",
                locations = getLocationsByPattern("лесопилка")
            )
        )
        
        // Телепорты
        groups.add(
            LocationGroup(
                title = "Телепорты",
                locations = getTeleportLocations()
            )
        )
        
        // Объекты
        groups.add(
            LocationGroup(
                title = "Объекты",
                locations = listOf(
                    LocationInfo("8-227", "8-227. Объект", "Объект"),
                    LocationInfo("2-482", "2-482. Объект", "Объект"),
                    LocationInfo("9-494", "9-494. Объект", "Объект"),
                    LocationInfo("26-430", "26-430. Объект", "Объект")
                )
            )
        )
        
        // Ресурсы охотников
        groups.add(
            LocationGroup(
                title = "Ресурсы охотников",
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
                    title = "Подходящие названия",
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
        // Имитация расчета маршрута
        // В реальности здесь будет использоваться MapPath из Windows версии
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
        // Заглушка - в реальности будет использоваться Map.Cells[cellNumber].Tooltip
        return when (cellNumber) {
            "8-259", "8-294" -> "Форпост"
            "8-197" -> "Деревня"
            "12-428", "12-494", "12-521" -> "Октал"
            else -> "Неизвестная локация"
        }
    }
    
    private fun getLocationsByPattern(pattern: String): List<LocationInfo> {
        // Заглушка - в реальности будет поиск в Map.Cells
        return when (pattern) {
            "замок" -> listOf(
                LocationInfo("8-300", "8-300. Замок", "Замок"),
                LocationInfo("8-301", "8-301. Замок", "Замок")
            )
            "ФортGA" -> listOf(
                LocationInfo("8-400", "8-400. ФортGA", "ФортGA"),
                LocationInfo("8-401", "8-401. ФортGA", "ФортGA")
            )
            "ФортGB" -> listOf(
                LocationInfo("8-410", "8-410. ФортGB", "ФортGB"),
                LocationInfo("8-411", "8-411. ФортGB", "ФортGB")
            )
            "ФортGC" -> listOf(
                LocationInfo("8-420", "8-420. ФортGC", "ФортGC"),
                LocationInfo("8-421", "8-421. ФортGC", "ФортGC")
            )
            "шахта" -> listOf(
                LocationInfo("8-500", "8-500. Шахта", "Шахта"),
                LocationInfo("8-501", "8-501. Рудник провал", "Рудник провал")
            )
            "причал" -> listOf(
                LocationInfo("8-600", "8-600. Причал", "Причал"),
                LocationInfo("8-601", "8-601. Причал", "Причал")
            )
            "лесопилка" -> listOf(
                LocationInfo("8-700", "8-700. Лесопилка", "Лесопилка"),
                LocationInfo("8-701", "8-701. Лесопилка", "Лесопилка")
            )
            else -> emptyList()
        }
    }
    
    private fun getLocationsByTooltipPattern(pattern: String): List<LocationInfo> {
        // Заглушка - в реальности будет поиск в Map.Cells по Tooltip
        return listOf(
            LocationInfo("8-999", "8-999. $pattern", pattern)
        )
    }
    
    private fun getBotLocations(): List<LocationInfo> {
        // Заглушка - в реальности будет загрузка из _bots коллекции
        return listOf(
            LocationInfo("8-800", "8-800. Гоблины (8-12)", "Гоблины"),
            LocationInfo("8-801", "8-801. Орки (15)", "Орки"),
            LocationInfo("8-802", "8-802. Огры (19)", "Огры")
        )
    }
    
    private fun getHerbLocations(): List<LocationInfo> {
        // Заглушка - в реальности будет загрузка из _herbs коллекции
        return listOf(
            LocationInfo("8-850", "8-850. Травы (уровень 1)", "Травы"),
            LocationInfo("8-851", "8-851. Травы (уровень 2)", "Травы")
        )
    }
    
    private fun getFishingLocations(): List<LocationInfo> {
        // Заглушка - в реальности будет поиск в Map.Cells по HasFish
        return listOf(
            LocationInfo("8-900", "8-900. Озеро", "Озеро"),
            LocationInfo("8-901", "8-901. Река", "Река")
        )
    }
    
    private fun getTeleportLocations(): List<LocationInfo> {
        // Заглушка - в реальности будет загрузка из Map.Teleports
        return listOf(
            LocationInfo("8-950", "8-950. Телепорт", "Телепорт"),
            LocationInfo("8-951", "8-951. Телепорт", "Телепорт")
        )
    }
    
    private fun getHunterResourceLocations(): List<LocationInfo> {
        // Заглушка - соответствует PopulateSkinRes из Windows версии
        return listOf(
            LocationInfo("8-200", "(5шт) Крысиные хвосты (0+)", "Крысы"),
            LocationInfo("8-201", "(3шт) Крысиные лапы (15+)", "Крысы"),
            LocationInfo("8-202", "(4шт) Мясо кабана (50+)", "Кабаны"),
            LocationInfo("8-203", "(3шт) Шкуры кабана (65+)", "Кабаны"),
            LocationInfo("8-204", "(2шт) Клык кабана (80+)", "Кабаны"),
            LocationInfo("8-205", "(2шт) Копыта (100+)", "Кабаны"),
            LocationInfo("8-206", "(6шт) Кости скелетов (120+)", "Скелеты"),
            LocationInfo("8-207", "(4шт) Черепа (130+)", "Скелеты"),
            LocationInfo("8-208", "(3шт) Зубы (150+)", "Скелеты"),
            LocationInfo("8-209", "(5шт) Трупный яд (180+)", "Зомби"),
            LocationInfo("8-210", "(4шт) Костный мозг (200+)", "Зомби"),
            LocationInfo("8-211", "(3шт) Гниль (220+)", "Зомби"),
            LocationInfo("8-212", "(7шт) Паучьи лапы (235+)", "Пауки"),
            LocationInfo("8-213", "(5шт) Хитиновые панцири (250+)", "Пауки"),
            LocationInfo("8-214", "(6шт) Паутина (260+)", "Пауки"),
            LocationInfo("8-215", "(4шт) Жвала (280+)", "Пауки"),
            LocationInfo("8-216", "(3шт) Паучьи яйца (300+)", "Пауки"),
            LocationInfo("8-217", "(2шт) Ядовитые железы (320+)", "Пауки"),
            LocationInfo("8-218", "(2шт) Крысиные глаза (350+)", "Крысы"),
            LocationInfo("8-219", "(4шт) Медвежье мясо (400+)", "Медведи"),
            LocationInfo("8-220", "(3шт) Шкуры медведей (500+)", "Медведи"),
            LocationInfo("8-221", "(2шт) Медвежий жир (600+)", "Медведи"),
            LocationInfo("8-222", "(1шт) Клыки (700+)", "Медведи")
        )
    }
}