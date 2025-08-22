package ru.neverlands.abclient.ui.profiles

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
                // Обработка ошибок загрузки профилей
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
                // Создаем новый профиль с базовыми настройками
                val newProfile = UserProfile(
                    userNick = "Новый профиль ${System.currentTimeMillis()}",
                    userPassword = "",
                    lastLogon = System.currentTimeMillis()
                )
                
                profileRepository.saveProfile(newProfile)
                loadProfiles() // Перезагружаем список
                
                // Выбираем новый профиль
                _uiState.value = _uiState.value.copy(
                    selectedProfile = newProfile
                )
            } catch (e: Exception) {
                // Обработка ошибок создания профиля
            }
        }
    }
    
    fun editProfile(profile: UserProfile) {
        viewModelScope.launch {
            try {
                // Открываем диалог редактирования профиля
                // В реальности здесь будет навигация к экрану редактирования
                // или показ диалога редактирования
            } catch (e: Exception) {
                // Обработка ошибок редактирования
            }
        }
    }
    
    fun deleteProfile(profile: UserProfile) {
        viewModelScope.launch {
            try {
                profileRepository.deleteProfile(profile.id)
                loadProfiles() // Перезагружаем список
                
                // Если удаленный профиль был выбран, сбрасываем выбор
                if (_uiState.value.selectedProfile == profile) {
                    _uiState.value = _uiState.value.copy(
                        selectedProfile = _uiState.value.profiles.firstOrNull()
                    )
                }
            } catch (e: Exception) {
                // Обработка ошибок удаления
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
                // Обработка ошибок обновления
            }
        }
    }
}

/**
 * UI состояние экрана профилей
 */
data class ProfilesUiState(
    val profiles: List<UserProfile> = emptyList(),
    val selectedProfile: UserProfile? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)