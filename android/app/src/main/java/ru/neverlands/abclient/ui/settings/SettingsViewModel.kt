package ru.neverlands.abclient.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.neverlands.abclient.data.preferences.UserPreferencesManager
import javax.inject.Inject

/**
 * ViewModel для экрана настроек
 * Управляет всеми настройками как в FormSettingsGeneral
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: UserPreferencesManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                val profile = preferencesManager.getCurrentProfile()
                if (profile != null) {
                    _uiState.value = SettingsUiState.fromProfile(profile)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun saveSettings() {
        viewModelScope.launch {
            try {
                val currentProfile = preferencesManager.getCurrentProfile()
                if (currentProfile != null) {
                    val updatedProfile = _uiState.value.toProfile(currentProfile)
                    preferencesManager.saveProfile(updatedProfile)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    // Общие настройки
    fun setPromptExit(value: Boolean) {
        _uiState.value = _uiState.value.copy(doPromptExit = value)
    }
    
    fun setUseTray(value: Boolean) {
        _uiState.value = _uiState.value.copy(doTray = value)
    }
    
    fun setShowTrayBalloons(value: Boolean) {
        _uiState.value = _uiState.value.copy(showTrayBaloons = value)
    }
    
    // Настройки чата
    fun setChatKeepMoving(value: Boolean) {
        _uiState.value = _uiState.value.copy(chatKeepMoving = value)
    }
    
    fun setChatKeepGame(value: Boolean) {
        _uiState.value = _uiState.value.copy(chatKeepGame = value)
    }
    
    fun setChatKeepLog(value: Boolean) {
        _uiState.value = _uiState.value.copy(chatKeepLog = value)
    }
    
    fun setChatSize(value: Int) {
        _uiState.value = _uiState.value.copy(chatSizeLog = value)
    }
    
    fun setChatLevels(value: Boolean) {
        _uiState.value = _uiState.value.copy(doChatLevels = value)
    }
    
    // Настройки карты
    fun setMapExtended(value: Boolean) {
        _uiState.value = _uiState.value.copy(mapShowExtended = value)
    }
    
    fun setBigMapWidth(value: Int) {
        _uiState.value = _uiState.value.copy(mapBigWidth = value)
    }
    
    fun setBigMapHeight(value: Int) {
        _uiState.value = _uiState.value.copy(mapBigHeight = value)
    }
    
    fun setBigMapScale(value: Float) {
        _uiState.value = _uiState.value.copy(mapBigScale = value)
    }
    
    fun setBigMapTransparency(value: Float) {
        _uiState.value = _uiState.value.copy(mapBigTransparency = value)
    }
    
    fun setMapBackColorWhite(value: Boolean) {
        _uiState.value = _uiState.value.copy(mapShowBackColorWhite = value)
    }
    
    fun setMapDrawRegion(value: Boolean) {
        _uiState.value = _uiState.value.copy(mapDrawRegion = value)
    }
    
    fun setShowMiniMap(value: Boolean) {
        _uiState.value = _uiState.value.copy(mapShowMiniMap = value)
    }
    
    fun setMiniMapWidth(value: Int) {
        _uiState.value = _uiState.value.copy(mapMiniWidth = value)
    }
    
    fun setMiniMapHeight(value: Int) {
        _uiState.value = _uiState.value.copy(mapMiniHeight = value)
    }
    
    fun setMiniMapScale(value: Float) {
        _uiState.value = _uiState.value.copy(mapMiniScale = value)
    }
    
    // Настройки рыбалки
    fun setFishTiedHigh(value: Int) {
        _uiState.value = _uiState.value.copy(fishTiedHigh = value)
    }
    
    fun setFishTiedZero(value: Boolean) {
        _uiState.value = _uiState.value.copy(fishTiedZero = value)
    }
    
    fun setStopOverWeight(value: Boolean) {
        _uiState.value = _uiState.value.copy(fishStopOverWeight = value)
    }
    
    fun setFishAutoWear(value: Boolean) {
        _uiState.value = _uiState.value.copy(fishAutoWear = value)
    }
    
    fun setFishHandOne(value: String) {
        _uiState.value = _uiState.value.copy(fishHandOne = value)
    }
    
    fun setFishHandTwo(value: String) {
        _uiState.value = _uiState.value.copy(fishHandTwo = value)
    }
    
    fun setFishChatReport(value: Boolean) {
        _uiState.value = _uiState.value.copy(fishChatReport = value)
    }
    
    fun setFishChatReportColor(value: Boolean) {
        _uiState.value = _uiState.value.copy(fishChatReportColor = value)
    }
    
    // Настройки звука
    fun setSoundEnabled(value: Boolean) {
        _uiState.value = _uiState.value.copy(soundEnabled = value)
    }
    
    fun setPlayDigits(value: Boolean) {
        _uiState.value = _uiState.value.copy(doPlayDigits = value)
    }
    
    fun setPlayAttack(value: Boolean) {
        _uiState.value = _uiState.value.copy(doPlayAttack = value)
    }
    
    fun setPlaySndMsg(value: Boolean) {
        _uiState.value = _uiState.value.copy(doPlaySndMsg = value)
    }
    
    fun setPlayRefresh(value: Boolean) {
        _uiState.value = _uiState.value.copy(doPlayRefresh = value)
    }
    
    fun setPlayAlarm(value: Boolean) {
        _uiState.value = _uiState.value.copy(doPlayAlarm = value)
    }
    
    fun setPlayTimer(value: Boolean) {
        _uiState.value = _uiState.value.copy(doPlayTimer = value)
    }
    
    // Настройки лечения
    fun setCureNV(index: Int, value: Int) {
        val newCureNV = _uiState.value.cureNV.toMutableList()
        if (index in 0..3) {
            newCureNV[index] = value
            _uiState.value = _uiState.value.copy(cureNV = newCureNV)
        }
    }
    
    fun setCureEnabled(index: Int, value: Boolean) {
        val newCureEnabled = _uiState.value.cureEnabled.toMutableList()
        if (index in 0..3) {
            newCureEnabled[index] = value
            _uiState.value = _uiState.value.copy(cureEnabled = newCureEnabled)
        }
    }
    
    fun setCureDisabledLowLevels(value: Boolean) {
        _uiState.value = _uiState.value.copy(cureDisabledLowLevels = value)
    }
    
    // Настройки автоответчика
    fun setAutoAnswer(value: Boolean) {
        _uiState.value = _uiState.value.copy(doAutoAnswer = value)
    }
    
    fun setAutoAnswerText(value: String) {
        _uiState.value = _uiState.value.copy(autoAnswer = value)
    }
    
    // Настройки торговли
    fun setTorgTable(value: String) {
        _uiState.value = _uiState.value.copy(torgTabl = value)
    }
    
    fun setTorgMessageAdv(value: String) {
        _uiState.value = _uiState.value.copy(torgMessageAdv = value)
    }
    
    fun setTorgAdvTime(value: Int) {
        _uiState.value = _uiState.value.copy(torgAdvTime = value)
    }
    
    fun setTorgMessageNoMoney(value: String) {
        _uiState.value = _uiState.value.copy(torgMessageNoMoney = value)
    }
    
    fun setTorgMessageTooExp(value: String) {
        _uiState.value = _uiState.value.copy(torgMessageTooExp = value)
    }
    
    fun setTorgMessageThanks(value: String) {
        _uiState.value = _uiState.value.copy(torgMessageThanks = value)
    }
    
    fun setTorgMessageLess90(value: String) {
        _uiState.value = _uiState.value.copy(torgMessageLess90 = value)
    }
    
    fun setTorgSliv(value: Boolean) {
        _uiState.value = _uiState.value.copy(torgSliv = value)
    }
    
    fun setTorgMinLevel(value: Int) {
        _uiState.value = _uiState.value.copy(torgMinLevel = value)
    }
    
    fun setTorgEx(value: String) {
        _uiState.value = _uiState.value.copy(torgEx = value)
    }
    
    fun setTorgDeny(value: String) {
        _uiState.value = _uiState.value.copy(torgDeny = value)
    }
    
    // Настройки инвентаря
    fun setInvPack(value: Boolean) {
        _uiState.value = _uiState.value.copy(doInvPack = value)
    }
    
    fun setInvPackDolg(value: Boolean) {
        _uiState.value = _uiState.value.copy(doInvPackDolg = value)
    }
    
    fun setInvSort(value: Boolean) {
        _uiState.value = _uiState.value.copy(doInvSort = value)
    }
    
    // Настройки быстрой атаки
    fun setShowFastAttack(value: Boolean) {
        _uiState.value = _uiState.value.copy(doShowFastAttack = value)
    }
    
    fun setShowFastAttackBlood(value: Boolean) {
        _uiState.value = _uiState.value.copy(doShowFastAttackBlood = value)
    }
    
    fun setShowFastAttackUltimate(value: Boolean) {
        _uiState.value = _uiState.value.copy(doShowFastAttackUltimate = value)
    }
    
    fun setShowFastAttackClosedUltimate(value: Boolean) {
        _uiState.value = _uiState.value.copy(doShowFastAttackClosedUltimate = value)
    }
    
    fun setShowFastAttackClosed(value: Boolean) {
        _uiState.value = _uiState.value.copy(doShowFastAttackClosed = value)
    }
    
    fun setShowFastAttackFist(value: Boolean) {
        _uiState.value = _uiState.value.copy(doShowFastAttackFist = value)
    }
    
    fun setShowFastAttackClosedFist(value: Boolean) {
        _uiState.value = _uiState.value.copy(doShowFastAttackClosedFist = value)
    }
    
    fun setShowFastAttackOpenNevid(value: Boolean) {
        _uiState.value = _uiState.value.copy(doShowFastAttackOpenNevid = value)
    }
    
    fun setShowFastAttackPoison(value: Boolean) {
        _uiState.value = _uiState.value.copy(doShowFastAttackPoison = value)
    }
    
    fun setShowFastAttackStrong(value: Boolean) {
        _uiState.value = _uiState.value.copy(doShowFastAttackStrong = value)
    }
    
    fun setShowFastAttackNevid(value: Boolean) {
        _uiState.value = _uiState.value.copy(doShowFastAttackNevid = value)
    }
    
    fun setShowFastAttackFog(value: Boolean) {
        _uiState.value = _uiState.value.copy(doShowFastAttackFog = value)
    }
    
    fun setShowFastAttackZas(value: Boolean) {
        _uiState.value = _uiState.value.copy(doShowFastAttackZas = value)
    }
    
    fun setShowFastAttackTotem(value: Boolean) {
        _uiState.value = _uiState.value.copy(doShowFastAttackTotem = value)
    }
}