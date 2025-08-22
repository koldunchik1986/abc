package com.koldunchik1986.ANL.ui.fishadvisor

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
        
        // Сохранение в профиль
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
     * База данных советов по рыбалке - аналог кода из FormFishAdvisor.cs
     */
    private fun createFishTipsDatabase(): List<FishTip> {
        return listOf(
            // Наживка на моря
            FishTip(-42, 0, "8-224", 12, "Уровень 8-12", "Наживка на моря"),
            FishTip(-42, 0, "8-384", 8, "Уровень 4-8", "Наживка на моря"),
            FishTip(-42, 0, "12-208", 15, "Уровень 15", "Наживка на моря"),
            
            // Наживка на озера
            FishTip(60, 20, "8-224", 12, "Уровень 8-12", "Наживка на озера"),
            FishTip(60, 20, "8-384", 8, "Уровень 4-8", "Наживка на озера"),
            
            // Рыба на озерах
            FishTip(177, 30, "8-224", 12, "Уровень 8-12", "Рыба на озерах"),
            FishTip(177, 30, "8-384", 8, "Уровень 4-8", "Рыба на озерах"),
            FishTip(177, 30, "7-552", 19, "Уровень 19", "Рыба на озерах"),
            
            // Щука на прудах
            FishTip(311, 40, "8-224", 12, "Уровень 8-12", "Щука на прудах"),
            FishTip(311, 40, "8-384", 8, "Уровень 4-8", "Щука на прудах"),
            FishTip(311, 40, "8-413", 8, "Уровень 4-8", "Щука на прудах"),
            FishTip(311, 40, "8-414", 8, "Уровень 4-8", "Щука на прудах"),
            FishTip(311, 40, "7-552", 19, "Уровень 19", "Щука на прудах"),
            
            // Карась на прудах
            FishTip(466, 60, "8-326", 9, "Уровень 5-9", "Карась на прудах"),
            FishTip(466, 60, "8-358", 9, "Уровень 5-9", "Карась на прудах"),
            
            // Окунь на речке
            FishTip(644, 80, "8-437", 9, "Уровень 7-9", "Окунь на речке"),
            FishTip(644, 80, "8-467", 9, "Уровень 7-9", "Окунь на речке"),
            
            // Плотва на речке
            FishTip(848, 95, "8-326", 9, "Уровень 5-9", "Плотва на речке"),
            FishTip(848, 95, "8-358", 9, "Уровень 5-9", "Плотва на речке"),
            FishTip(848, 95, "7-552", 19, "Уровень 19", "Плотва на речке"),
            
            // Судак на больших прудах
            FishTip(1084, 120, "8-326", 9, "Уровень 5-9", "Судак на больших прудах"),
            FishTip(1084, 120, "8-358", 9, "Уровень 5-9", "Судак на больших прудах"),
            
            // Лещ на больших прудах
            FishTip(1354, 140, "8-437", 9, "Уровень 7-9", "Лещ на больших прудах"),
            FishTip(1354, 140, "8-467", 9, "Уровень 7-9", "Лещ на больших прудах"),
            FishTip(1354, 140, "12-208", 15, "Уровень 15", "Лещ на больших прудах"),
            
            // Форель на речке с морем
            FishTip(1675, 180, "8-326", 9, "Уровень 5-9", "Форель на речке с морем"),
            FishTip(1675, 180, "8-358", 9, "Уровень 5-9", "Форель на речке с морем"),
            
            // Треска на море с речкой
            FishTip(2142, 210, "8-384", 8, "Уровень 4-8", "Треска на море с речкой"),
            FishTip(2142, 210, "8-413", 8, "Уровень 4-8", "Треска на море с речкой"),
            FishTip(2142, 210, "8-414", 8, "Уровень 4-8", "Треска на море с речкой"),
            FishTip(2142, 210, "8-437", 9, "Уровень 7-9", "Треска на море с речкой"),
            FishTip(2142, 210, "8-467", 9, "Уровень 7-9", "Треска на море с речкой"),
            
            // Минтай на больших прудах с морем
            FishTip(2679, 250, "8-384", 8, "Уровень 4-8", "Минтай на больших прудах с морем"),
            FishTip(2679, 250, "8-413", 8, "Уровень 4-8", "Минтай на больших прудах с морем"),
            FishTip(2679, 250, "8-414", 8, "Уровень 4-8", "Минтай на больших прудах с морем"),
            FishTip(2679, 250, "7-552", 19, "Уровень 19", "Минтай на больших прудах с морем"),
            
            // Камбала на море с прудом
            FishTip(3297, 370, "8-224", 12, "Уровень 8-12", "Камбала на море с прудом"),
            
            // Акула на море с прудами
            FishTip(4008, 450, "8-326", 9, "Уровень 5-9", "Акула на море с прудами"),
            FishTip(4008, 450, "7-552", 19, "Уровень 19", "Акула на море с прудами"),
            FishTip(4008, 450, "12-208", 15, "Уровень 15", "Акула на море с прудами"),
            
            // Морской окунь на море с прудами
            FishTip(4825, 500, "8-326", 9, "Уровень 5-9", "Морской окунь на море с прудами"),
            FishTip(4825, 500, "8-437", 9, "Уровень 7-9", "Морской окунь на море с прудами"),
            FishTip(4825, 500, "8-467", 9, "Уровень 7-9", "Морской окунь на море с прудами"),
            FishTip(4825, 500, "8-358", 9, "Уровень 5-9", "Морской окунь на море с прудами"),
            FishTip(4825, 500, "7-552", 19, "Уровень 19", "Морской окунь на море с прудами"),
            
            // Морской карась на море с прудами
            FishTip(5765, 600, "8-437", 9, "Уровень 7-9", "Морской карась на море с прудами"),
            FishTip(5765, 600, "8-467", 9, "Уровень 7-9", "Морской карась на море с прудами"),
            FishTip(5765, 600, "8-358", 9, "Уровень 5-9", "Морской карась на море с прудами"),
            FishTip(5765, 600, "12-208", 15, "Уровень 15", "Морской карась на море с прудами"),
            
            // Морская щука на море с прудами
            FishTip(6846, 700, "8-437", 9, "Уровень 7-9", "Морская щука на море с прудами"),
            FishTip(6846, 700, "8-467", 9, "Уровень 7-9", "Морская щука на море с прудами"),
            
            // Морской судак на море с прудами
            FishTip(8089, 800, "8-358", 9, "Уровень 5-9", "Морской судак на море с прудами"),
            
            // Морской лещ на море с прудами
            FishTip(9518, 1000, "8-384", 8, "Уровень 4-8", "Морской лещ на море с прудами"),
            FishTip(9518, 1000, "8-413", 8, "Уровень 4-8", "Морской лещ на море с прудами"),
            FishTip(9518, 1000, "8-414", 8, "Уровень 4-8", "Морской лещ на море с прудами"),
            FishTip(9518, 1000, "12-208", 15, "Уровень 15", "Морской лещ на море с прудами")
        )
    }
}