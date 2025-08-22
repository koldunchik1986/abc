package com.koldunchik1986.ANL.ui.profiles

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
class ProfilesViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProfilesUiState())
    val uiState: StateFlow<ProfilesUiState> = _uiState.asStateFlow()
    
    fun loadProfiles() {
        viewModelScope.launch {
            try {
                val profiles = profileRepository.getAllProfiles()
                _uiState.value = _uiState.value.copy(
                    profiles = profiles,
                    selectedProfile = profiles.firstOrNull()
                )
            } catch (e: Exception) {
                // Обработка ошибки загрузки профилей
                _uiState.value = _uiState.value.copy(
                    profiles = emptyList(),
                    selectedProfile = null
                )
            }
        }
    }
    
    fun selectProfile(profile: UserProfile) {
        _uiState.value = _uiState.value.copy(
            selectedProfile = profile
        )
    }
    
    fun createNewProfile() {
        viewModelScope.launch {
            try {
                // Создаем новый профиль с временными данными
                val newProfile = UserProfile.createNew("Новый профиль")
                
                profileRepository.saveProfile(newProfile)
                loadProfiles() // Перезагружаем список
                
                // Выбираем новый профиль
                _uiState.value = _uiState.value.copy(
                    selectedProfile = newProfile
                )
                
                // Открываем редактор для нового профиля
                editProfile(newProfile)
            } catch (e: Exception) {
                // Обработка ошибки создания профиля
            }
        }
    }
    
    fun editProfile(profile: UserProfile) {
        viewModelScope.launch {
            try {
                // Здесь должна быть логика редактирования профиля
                // и открытие соответствующего экрана редактирования
                // Для этого потребуется открытие ProfileActivity
                _uiState.value = _uiState.value.copy(
                    editingProfile = profile
                )
            } catch (e: Exception) {
                // Обработка ошибки редактирования
            }
        }
    }
    
    fun deleteProfile(profile: UserProfile) {
        viewModelScope.launch {
            try {
                profileRepository.deleteProfile(profile.id)
                loadProfiles() // Перезагружаем список
                
                // Если удаленный профиль был выбран, выбираем другой
                if (_uiState.value.selectedProfile == profile) {
                    _uiState.value = _uiState.value.copy(
                        selectedProfile = _uiState.value.profiles.firstOrNull()
                    )
                }
            } catch (e: Exception) {
                // Обработка ошибки удаления
            }
        }
    }
    
    fun updateProfileLastUsed(profile: UserProfile) {
        viewModelScope.launch {
            try {
                val updatedProfile = profile.copy(lastLogon = System.currentTimeMillis())
                profileRepository.saveProfile(updatedProfile)
                loadProfiles()
            } catch (e: Exception) {
                // Обработка ошибки обновления
            }
        }
    }
    
    fun saveEditedProfile(profile: UserProfile) {
        viewModelScope.launch {
            try {
                val updatedProfile = profile.copy(
                    updatedAt = System.currentTimeMillis()
                )
                profileRepository.saveProfile(updatedProfile)
                loadProfiles()
                _uiState.value = _uiState.value.copy(
                    editingProfile = null
                )
            } catch (e: Exception) {
                // Обработка ошибки сохранения
            }
        }
    }
    
    fun cancelEditing() {
        _uiState.value = _uiState.value.copy(
            editingProfile = null
        )
    }
}

/**
 * UI состояние для экрана профилей
 */
data class ProfilesUiState(
    val profiles: List<UserProfile> = emptyList(),
    val selectedProfile: UserProfile? = null,
    val editingProfile: UserProfile? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)