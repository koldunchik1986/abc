package ru.neverlands.abclient.ui.fishadvisor

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
class FishAdvisorViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FishAdvisorUiState())
    val uiState: StateFlow<FishAdvisorUiState> = _uiState.asStateFlow()
    
    private val allFishTips = createFishTipsDatabase()
    private var currentProfile: UserProfile? = null
    
    init {
        loadProfile()
    }
    
    private fun loadProfile() {
        viewModelScope.launch {
            currentProfile = profileRepository.getCurrentProfile()
            currentProfile?.let { profile ->
                _uiState.value = _uiState.value.copy(
                    fishUmInput = profile.fishUm.toString(),
                    fishUm = profile.fishUm,
                    maxBotLevel = profile.fishMaxLevelBots,
                    isFishUmValid = true
                )
            }
        }
    }
    
    fun setFishUm(input: String) {
        val isValid = input.toIntOrNull()?.let { it > 0 } ?: false
        val fishUm = if (isValid) input.toInt() else 0
        
        _uiState.value = _uiState.value.copy(
            fishUmInput = input,
            fishUm = fishUm,
            isFishUmValid = isValid
        )
    }
    
    fun setMaxBotLevel(level: Int) {
        _uiState.value = _uiState.value.copy(
            maxBotLevel = level
        )
        
        // Сохраняем в профиль
        viewModelScope.launch {
            currentProfile?.let { profile ->
                val updatedProfile = profile.copy(fishMaxLevelBots = level)
                profileRepository.saveProfile(updatedProfile)
                currentProfile = updatedProfile
            }
        }
    }
    
    fun calculateFishTips() {
        if (!_uiState.value.isFishUmValid) return
        
        val fishUm = _uiState.value.fishUm
        val maxBotLevel = _uiState.value.maxBotLevel
        
        val filteredTips = allFishTips.filter { tip ->
            tip.fishUm <= fishUm && tip.maxBotLevel <= maxBotLevel
        }.sortedBy { it.money }
        
        _uiState.value = _uiState.value.copy(
            filteredFishTips = filteredTips
        )
    }
    
    /**
     * Создает базу данных рыбных советов - точная копия из FormFishAdvisor.cs
     */
    private fun createFishTipsDatabase(): List<FishTip> {
        return listOf(
            // Верхоплавка на хлеб
            FishTip(-42, 0, "8-224", 12, "Гобл 8-12", "Верхоплавка на хлеб"),
            FishTip(-42, 0, "8-384", 8, "Гобл 4-8", "Верхоплавка на хлеб"),
            FishTip(-42, 0, "12-208", 15, "Орки 15", "Верхоплавка на хлеб"),
            
            // Красноперка на мотыля
            FishTip(60, 20, "8-224", 12, "Гобл 8-12", "Красноперка на мотыля"),
            FishTip(60, 20, "8-384", 8, "Гобл 4-8", "Красноперка на мотыля"),
            
            // Бычок на опарыш
            FishTip(177, 30, "8-224", 12, "Гобл 8-12", "Бычок на опарыш"),
            FishTip(177, 30, "8-384", 8, "Гобл 4-8", "Бычок на опарыш"),
            FishTip(177, 30, "7-552", 19, "Огры 19", "Бычок на опарыш"),
            
            // Ёрш на червяка
            FishTip(311, 40, "8-224", 12, "Гобл 8-12", "Ёрш на червяка"),
            FishTip(311, 40, "8-384", 8, "Гобл 4-8", "Ёрш на червяка"),
            FishTip(311, 40, "8-413", 8, "Гобл 4-8", "Ёрш на червяка"),
            FishTip(311, 40, "8-414", 8, "Гобл 4-8", "Ёрш на червяка"),
            FishTip(311, 40, "7-552", 19, "Огры 19", "Ёрш на червяка"),
            
            // Плотва на червяка
            FishTip(466, 60, "8-326", 9, "Гобл 5-9", "Плотва на червяка"),
            FishTip(466, 60, "8-358", 9, "Гобл 5-9", "Плотва на червяка"),
            
            // Пескарь на хлеб
            FishTip(644, 80, "8-437", 9, "Гобл 7-9", "Пескарь на хлеб"),
            FishTip(644, 80, "8-467", 9, "Гобл 7-9", "Пескарь на хлеб"),
            
            // Карась на хлеб
            FishTip(848, 95, "8-326", 9, "Гобл 5-9", "Карась на хлеб"),
            FishTip(848, 95, "8-358", 9, "Гобл 5-9", "Карась на хлеб"),
            FishTip(848, 95, "7-552", 19, "Огры 19", "Карась на хлеб"),
            
            // Подлещик на крупного червяка
            FishTip(1084, 120, "8-326", 9, "Гобл 5-9", "Подлещик на крупного червяка"),
            FishTip(1084, 120, "8-358", 9, "Гобл 5-9", "Подлещик на крупного червяка"),
            
            // Карп на крупного червяка
            FishTip(1354, 140, "8-437", 9, "Гобл 7-9", "Карп на крупного червяка"),
            FishTip(1354, 140, "8-467", 9, "Гобл 7-9", "Карп на крупного червяка"),
            FishTip(1354, 140, "12-208", 15, "Орки 15", "Карп на крупного червяка"),
            
            // Окунь на мотыля с сачком
            FishTip(1675, 180, "8-326", 9, "Гобл 5-9", "Окунь на мотыля с сачком"),
            FishTip(1675, 180, "8-358", 9, "Гобл 5-9", "Окунь на мотыля с сачком"),
            
            // Лещ на донку с сачком
            FishTip(2142, 210, "8-384", 8, "Гобл 4-8", "Лещ на донку с сачком"),
            FishTip(2142, 210, "8-413", 8, "Гобл 4-8", "Лещ на донку с сачком"),
            FishTip(2142, 210, "8-414", 8, "Гобл 4-8", "Лещ на донку с сачком"),
            FishTip(2142, 210, "8-437", 9, "Гобл 7-9", "Лещ на донку с сачком"),
            FishTip(2142, 210, "8-467", 9, "Гобл 7-9", "Лещ на донку с сачком"),
            
            // Голавль на крупного червяка с сачком
            FishTip(2679, 250, "8-384", 8, "Гобл 4-8", "Голавль на крупного червяка с сачком"),
            FishTip(2679, 250, "8-413", 8, "Гобл 4-8", "Голавль на крупного червяка с сачком"),
            FishTip(2679, 250, "8-414", 8, "Гобл 4-8", "Голавль на крупного червяка с сачком"),
            FishTip(2679, 250, "7-552", 19, "Огры 19", "Голавль на крупного червяка с сачком"),
            
            // Налим на донку с сачком
            FishTip(3297, 370, "8-224", 12, "Гобл 8-12", "Налим на донку с сачком"),
            
            // Язь на мормышку с сачком
            FishTip(4008, 450, "8-326", 9, "Гобл 5-9", "Язь на мормышку с сачком"),
            FishTip(4008, 450, "7-552", 19, "Огры 19", "Язь на мормышку с сачком"),
            FishTip(4008, 450, "12-208", 15, "Орки 15", "Язь на мормышку с сачком"),
            
            // Щука на блесну с сачком
            FishTip(4825, 500, "8-326", 9, "Гобл 5-9", "Щука на блесну с сачком"),
            FishTip(4825, 500, "8-437", 9, "Гобл 7-9", "Щука на блесну с сачком"),
            FishTip(4825, 500, "8-467", 9, "Гобл 7-9", "Щука на блесну с сачком"),
            FishTip(4825, 500, "8-358", 9, "Гобл 5-9", "Щука на блесну с сачком"),
            FishTip(4825, 500, "7-552", 19, "Огры 19", "Щука на блесну с сачком"),
            
            // Линь на мормышку с сачком
            FishTip(5765, 600, "8-437", 9, "Гобл 7-9", "Линь на мормышку с сачком"),
            FishTip(5765, 600, "8-467", 9, "Гобл 7-9", "Линь на мормышку с сачком"),
            FishTip(5765, 600, "8-358", 9, "Гобл 5-9", "Линь на мормышку с сачком"),
            FishTip(5765, 600, "12-208", 15, "Орки 15", "Линь на мормышку с сачком"),
            
            // Судак на червяка с сачком
            FishTip(6846, 700, "8-437", 9, "Гобл 7-9", "Судак на червяка с сачком"),
            FishTip(6846, 700, "8-467", 9, "Гобл 7-9", "Судак на червяка с сачком"),
            
            // Сом на заговоренную блесну с сачком
            FishTip(8089, 800, "8-358", 9, "Гобл 5-9", "Сом на заговоренную блесну с сачком"),
            
            // Форель на блесну с сачком
            FishTip(9518, 1000, "8-384", 8, "Гобл 4-8", "Форель на блесну с сачком"),
            FishTip(9518, 1000, "8-413", 8, "Гобл 4-8", "Форель на блесну с сачком"),
            FishTip(9518, 1000, "8-414", 8, "Гобл 4-8", "Форель на блесну с сачком"),
            FishTip(9518, 1000, "12-208", 15, "Орки 15", "Форель на блесну с сачком")
        )
    }
}