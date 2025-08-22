package ru.neverlands.abclient.ui.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.neverlands.abclient.data.preferences.UserPreferencesManager
import ru.neverlands.abclient.data.model.UserProfile
import javax.inject.Inject

/**
 * ViewModel для экрана списка профилей пользователей
 * Эквивалент функциональности FormProfiles из Windows версии
 */
@HiltViewModel
class ProfileListViewModel @Inject constructor(
    private val preferencesManager: UserPreferencesManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProfileListUiState())
    val uiState: StateFlow<ProfileListUiState> = _uiState.asStateFlow()
    
    init {
        loadProfiles()
        observeCurrentProfile()
    }
    
    /**
     * Загружает список профилей
     */
    private fun loadProfiles() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val profiles = preferencesManager.getProfiles()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    profiles = profiles,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Ошибка загрузки профилей: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Отслеживает изменения текущего профиля
     */
    private fun observeCurrentProfile() {
        viewModelScope.launch {
            try {
                val currentProfileId = preferencesManager.getCurrentProfileId()
                _uiState.value = _uiState.value.copy(currentProfileId = currentProfileId)
            } catch (e: Exception) {
                // Игнорируем ошибки получения текущего профиля
            }
        }
    }
    
    /**
     * Устанавливает текущий профиль
     */
    fun setCurrentProfile(profileId: String) {
        viewModelScope.launch {
            try {
                preferencesManager.setCurrentProfileId(profileId)
                _uiState.value = _uiState.value.copy(
                    currentProfileId = profileId,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Ошибка установки текущего профиля: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Удаляет профиль
     */
    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            try {
                preferencesManager.deleteProfile(profileId)
                
                // Обновляем список профилей
                val updatedProfiles = _uiState.value.profiles.filter { it.id != profileId }
                
                // Если удаляли текущий профиль, сбрасываем текущий
                val currentProfileId = if (_uiState.value.currentProfileId == profileId) {
                    null
                } else {
                    _uiState.value.currentProfileId
                }
                
                _uiState.value = _uiState.value.copy(
                    profiles = updatedProfiles,
                    currentProfileId = currentProfileId,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Ошибка удаления профиля: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Обновляет список профилей
     */
    fun refreshProfiles() {
        loadProfiles()
    }
    
    /**
     * Очищает сообщение об ошибке
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

/**
 * UI состояние экрана списка профилей
 */
data class ProfileListUiState(
    val isLoading: Boolean = false,
    val profiles: List<UserProfile> = emptyList(),
    val currentProfileId: String? = null,
    val errorMessage: String? = null
) {
    /**
     * Получает текущий профиль
     */
    fun getCurrentProfile(): UserProfile? {
        return profiles.find { it.id == currentProfileId }
    }
    
    /**
     * Проверяет, есть ли профили
     */
    fun hasProfiles(): Boolean = profiles.isNotEmpty()
    
    /**
     * Получает количество профилей
     */
    fun getProfileCount(): Int = profiles.size
}