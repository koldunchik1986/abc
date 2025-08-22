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
            
            // ��������� ��������� ��� �������
            val title = if (nick != null) {
                "��������� ��������������� $nick"
            } else {
                "���������"
            }
            
            _uiState.value = _uiState.value.copy(
                title = title,
                currentDestination = initialDestination,
                destinationInput = initialDestination
            )
            
            // ���� ��� ����� �� �������
            if (location == "�������" && location2 != null) {
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
                title = "���������"
            )
            return
        }
        
        viewModelScope.launch {
            // �������� ������� �������� (� ���������� ����� ����� ��������� � Map.Cells)
            val routeInfo = calculateRouteInfo(destination)
            val title = if (routeInfo.pathExists) {
                "������� �� $destination"
            } else {
                "���������"
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
                // ����� ����� ����������� ������ ������������
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
        
        // ����������� �������
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
                        title = "����������� �������",
                        locations = favoriteLocations
                    )
                )
            }
        }
        
        // �������
        groups.add(
            LocationGroup(
                title = "�������",
                locations = listOf(
                    LocationInfo("8-259", "8-259. �������", "�������"),
                    LocationInfo("8-294", "8-294. �������", "�������")
                )
            )
        )
        
        // �������
        groups.add(
            LocationGroup(
                title = "�������",
                locations = listOf(
                    LocationInfo("8-197", "8-197. �������", "�������")
                )
            )
        )
        
        // �����
        groups.add(
            LocationGroup(
                title = "�����",
                locations = listOf(
                    LocationInfo("12-428", "12-428. �����", "�����"),
                    LocationInfo("12-494", "12-494. �����", "�����"),
                    LocationInfo("12-521", "12-521. �����", "�����")
                )
            )
        )
        
        // ����� (��������� ������)
        groups.add(
            LocationGroup(
                title = "�����",
                locations = getLocationsByPattern("�����")
            )
        )
        
        // ����� ������ 1, 2, 3
        groups.add(
            LocationGroup(
                title = "����� ������ 1",
                locations = getLocationsByPattern("����GA")
            )
        )
        
        groups.add(
            LocationGroup(
                title = "����� ������ 2",
                locations = getLocationsByPattern("����GB")
            )
        )
        
        groups.add(
            LocationGroup(
                title = "����� ������ 3",
                locations = getLocationsByPattern("����GC")
            )
        )
        
        // ���� (����� ����������� ��� ���������� � ������)
        groups.add(
            LocationGroup(
                title = "����",
                locations = getBotLocations()
            )
        )
        
        // ����� (����� ����������� ��� ���������� � ������)
        groups.add(
            LocationGroup(
                title = "�����",
                locations = getHerbLocations()
            )
        )
        
        // �����
        groups.add(
            LocationGroup(
                title = "�����",
                locations = getLocationsByPattern("�����")
            )
        )
        
        // ����
        groups.add(
            LocationGroup(
                title = "����",
                locations = getFishingLocations()
            )
        )
        
        // �������
        groups.add(
            LocationGroup(
                title = "�������",
                locations = getLocationsByPattern("������")
            )
        )
        
        // ���������
        groups.add(
            LocationGroup(
                title = "���������",
                locations = getLocationsByPattern("���������")
            )
        )
        
        // ���������
        groups.add(
            LocationGroup(
                title = "���������",
                locations = getTeleportLocations()
            )
        )
        
        // �������
        groups.add(
            LocationGroup(
                title = "�������",
                locations = listOf(
                    LocationInfo("8-227", "8-227. ������", "������"),
                    LocationInfo("2-482", "2-482. ������", "������"),
                    LocationInfo("9-494", "9-494. ������", "������"),
                    LocationInfo("26-430", "26-430. ������", "������")
                )
            )
        )
        
        // ������� ���������
        groups.add(
            LocationGroup(
                title = "������� ���������",
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
                    title = "���������� ��������",
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
        // �������� ������� ��������
        // � ���������� ����� ����� �������������� MapPath �� Windows ������
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
        // �������� - � ���������� ����� �������������� Map.Cells[cellNumber].Tooltip
        return when (cellNumber) {
            "8-259", "8-294" -> "�������"
            "8-197" -> "�������"
            "12-428", "12-494", "12-521" -> "�����"
            else -> "����������� �������"
        }
    }
    
    private fun getLocationsByPattern(pattern: String): List<LocationInfo> {
        // �������� - � ���������� ����� ����� � Map.Cells
        return when (pattern) {
            "�����" -> listOf(
                LocationInfo("8-300", "8-300. �����", "�����"),
                LocationInfo("8-301", "8-301. �����", "�����")
            )
            "����GA" -> listOf(
                LocationInfo("8-400", "8-400. ����GA", "����GA"),
                LocationInfo("8-401", "8-401. ����GA", "����GA")
            )
            "����GB" -> listOf(
                LocationInfo("8-410", "8-410. ����GB", "����GB"),
                LocationInfo("8-411", "8-411. ����GB", "����GB")
            )
            "����GC" -> listOf(
                LocationInfo("8-420", "8-420. ����GC", "����GC"),
                LocationInfo("8-421", "8-421. ����GC", "����GC")
            )
            "�����" -> listOf(
                LocationInfo("8-500", "8-500. �����", "�����"),
                LocationInfo("8-501", "8-501. ������ ������", "������ ������")
            )
            "������" -> listOf(
                LocationInfo("8-600", "8-600. ������", "������"),
                LocationInfo("8-601", "8-601. ������", "������")
            )
            "���������" -> listOf(
                LocationInfo("8-700", "8-700. ���������", "���������"),
                LocationInfo("8-701", "8-701. ���������", "���������")
            )
            else -> emptyList()
        }
    }
    
    private fun getLocationsByTooltipPattern(pattern: String): List<LocationInfo> {
        // �������� - � ���������� ����� ����� � Map.Cells �� Tooltip
        return listOf(
            LocationInfo("8-999", "8-999. $pattern", pattern)
        )
    }
    
    private fun getBotLocations(): List<LocationInfo> {
        // �������� - � ���������� ����� �������� �� _bots ���������
        return listOf(
            LocationInfo("8-800", "8-800. ������� (8-12)", "�������"),
            LocationInfo("8-801", "8-801. ���� (15)", "����"),
            LocationInfo("8-802", "8-802. ���� (19)", "����")
        )
    }
    
    private fun getHerbLocations(): List<LocationInfo> {
        // �������� - � ���������� ����� �������� �� _herbs ���������
        return listOf(
            LocationInfo("8-850", "8-850. ����� (������� 1)", "�����"),
            LocationInfo("8-851", "8-851. ����� (������� 2)", "�����")
        )
    }
    
    private fun getFishingLocations(): List<LocationInfo> {
        // �������� - � ���������� ����� ����� � Map.Cells �� HasFish
        return listOf(
            LocationInfo("8-900", "8-900. �����", "�����"),
            LocationInfo("8-901", "8-901. ����", "����")
        )
    }
    
    private fun getTeleportLocations(): List<LocationInfo> {
        // �������� - � ���������� ����� �������� �� Map.Teleports
        return listOf(
            LocationInfo("8-950", "8-950. ��������", "��������"),
            LocationInfo("8-951", "8-951. ��������", "��������")
        )
    }
    
    private fun getHunterResourceLocations(): List<LocationInfo> {
        // �������� - ������������� PopulateSkinRes �� Windows ������
        return listOf(
            LocationInfo("8-200", "(5��) �������� ������ (0+)", "�����"),
            LocationInfo("8-201", "(3��) �������� ���� (15+)", "�����"),
            LocationInfo("8-202", "(4��) ���� ������ (50+)", "������"),
            LocationInfo("8-203", "(3��) ����� ������ (65+)", "������"),
            LocationInfo("8-204", "(2��) ���� ������ (80+)", "������"),
            LocationInfo("8-205", "(2��) ������ (100+)", "������"),
            LocationInfo("8-206", "(6��) ����� �������� (120+)", "�������"),
            LocationInfo("8-207", "(4��) ������ (130+)", "�������"),
            LocationInfo("8-208", "(3��) ���� (150+)", "�������"),
            LocationInfo("8-209", "(5��) ������� �� (180+)", "�����"),
            LocationInfo("8-210", "(4��) ������� ���� (200+)", "�����"),
            LocationInfo("8-211", "(3��) ����� (220+)", "�����"),
            LocationInfo("8-212", "(7��) ������ ���� (235+)", "�����"),
            LocationInfo("8-213", "(5��) ��������� ������� (250+)", "�����"),
            LocationInfo("8-214", "(6��) ������� (260+)", "�����"),
            LocationInfo("8-215", "(4��) ����� (280+)", "�����"),
            LocationInfo("8-216", "(3��) ������ ���� (300+)", "�����"),
            LocationInfo("8-217", "(2��) �������� ������ (320+)", "�����"),
            LocationInfo("8-218", "(2��) �������� ����� (350+)", "�����"),
            LocationInfo("8-219", "(4��) �������� ���� (400+)", "�������"),
            LocationInfo("8-220", "(3��) ����� �������� (500+)", "�������"),
            LocationInfo("8-221", "(2��) �������� ��� (600+)", "�������"),
            LocationInfo("8-222", "(1��) ����� (700+)", "�������")
        )
    }
}
