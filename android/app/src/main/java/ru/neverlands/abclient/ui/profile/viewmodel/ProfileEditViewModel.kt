package ru.neverlands.abclient.ui.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.neverlands.abclient.data.model.UserProfile
import ru.neverlands.abclient.data.preferences.UserPreferencesManager
import ru.neverlands.abclient.ui.profile.components.ProfileEditUiState
import ru.neverlands.abclient.core.security.ProfileEncryptionManager
import javax.inject.Inject

/**
 * ViewModel для редактирования профилей пользователей
 */
@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val preferencesManager: UserPreferencesManager,
    private val encryptionManager: ProfileEncryptionManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProfileEditUiState.empty())
    val uiState: StateFlow<ProfileEditUiState> = _uiState.asStateFlow()
    
    private var originalProfile: UserProfile? = null
    
    init {
        // Автоматическая валидация при изменении данных
        uiState
            .map { it.nick.isNotBlank() && it.password.isNotBlank() }
            .distinctUntilChanged()
            .onEach { isValid ->
                _uiState.value = _uiState.value.copy(isValid = isValid)
            }
            .launchIn(viewModelScope)
    }
    
    /**
     * Загружает существующий профиль для редактирования
     */
    fun loadProfile(profileId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val profile = preferencesManager.getProfile(profileId)
                if (profile != null) {
                    originalProfile = profile
                    updateUiStateFromProfile(profile)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Профиль не найден"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Ошибка загрузки профиля: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Создает новый профиль
     */
    fun createNewProfile() {
        originalProfile = null
        _uiState.value = ProfileEditUiState.empty()
    }
    
    /**
     * Сохраняет профиль
     */
    fun saveProfile() {
        if (!_uiState.value.isValid) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val currentState = _uiState.value
                val profile = createProfileFromUiState(currentState)
                
                preferencesManager.saveProfile(profile)
                
                // Устанавливаем как текущий профиль, если это новый профиль
                if (originalProfile == null) {
                    preferencesManager.setCurrentProfileId(profile.id)
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSaved = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Ошибка сохранения: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Переключает шифрование профиля
     */
    fun toggleEncryption() {
        val currentState = _uiState.value
        if (!currentState.isValid) return
        
        if (currentState.isEncrypted) {
            // Убираем шифрование
            _uiState.value = currentState.copy(
                isEncrypted = false,
                autoLogon = false // Сбрасываем автовход при убирании шифрования
            )
        } else {
            // Добавляем шифрование
            viewModelScope.launch {
                try {
                    val encryptionPassword = encryptionManager.requestEncryptionPassword()
                    if (encryptionPassword != null) {
                        _uiState.value = currentState.copy(
                            isEncrypted = true,
                            autoLogon = false // Отключаем автовход при шифровании
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = currentState.copy(
                        errorMessage = "Ошибка настройки шифрования: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Устанавливает имя пользователя
     */
    fun setNick(nick: String) {
        _uiState.value = _uiState.value.copy(nick = nick.trim())
    }
    
    /**
     * Устанавливает пароль
     */
    fun setPassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }
    
    /**
     * Устанавливает Flash пароль
     */
    fun setFlashPassword(flashPassword: String) {
        _uiState.value = _uiState.value.copy(flashPassword = flashPassword)
    }
    
    /**
     * Устанавливает ключ пользователя
     */
    fun setUserKey(userKey: String) {
        _uiState.value = _uiState.value.copy(userKey = userKey.trim())
    }
    
    /**
     * Переключает автоматический вход
     */
    fun setAutoLogon(autoLogon: Boolean) {
        // Автовход доступен только для незашифрованных профилей
        if (!_uiState.value.isEncrypted) {
            _uiState.value = _uiState.value.copy(autoLogon = autoLogon)
        }
    }
    
    /**
     * Переключает использование прокси
     */
    fun setUseProxy(useProxy: Boolean) {
        _uiState.value = _uiState.value.copy(
            useProxy = useProxy,
            // Очищаем данные прокси при отключении
            proxyAddress = if (useProxy) _uiState.value.proxyAddress else "",
            proxyUserName = if (useProxy) _uiState.value.proxyUserName else "",
            proxyPassword = if (useProxy) _uiState.value.proxyPassword else ""
        )
    }
    
    /**
     * Устанавливает адрес прокси
     */
    fun setProxyAddress(address: String) {
        _uiState.value = _uiState.value.copy(proxyAddress = address.trim())
    }
    
    /**
     * Устанавливает имя пользователя прокси
     */
    fun setProxyUserName(userName: String) {
        _uiState.value = _uiState.value.copy(proxyUserName = userName.trim())
    }
    
    /**
     * Устанавливает пароль прокси
     */
    fun setProxyPassword(password: String) {
        _uiState.value = _uiState.value.copy(proxyPassword = password)
    }
    
    /**
     * Очищает сообщение об ошибке
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    /**
     * Обновляет UI состояние из профиля
     */
    private fun updateUiStateFromProfile(profile: UserProfile) {
        _uiState.value = ProfileEditUiState(
            isLoading = false,
            nick = profile.userNick,
            password = profile.userPassword,
            flashPassword = profile.userPasswordFlash,
            userKey = profile.userKey,
            autoLogon = profile.userAutoLogon,
            useProxy = profile.useProxy,
            proxyAddress = profile.proxyAddress,
            proxyUserName = profile.proxyUserName,
            proxyPassword = profile.proxyPassword,
            isEncrypted = profile.isPasswordProtected(),
            isValid = profile.isLoginDataComplete()
        )
    }
    
    /**
     * Создает профиль из текущего UI состояния
     */
    private fun createProfileFromUiState(state: ProfileEditUiState): UserProfile {
        val baseProfile = originalProfile ?: UserProfile.createNew(state.nick)
        
        return baseProfile.copy(
            userNick = state.nick,
            userPassword = state.password,
            userPasswordFlash = state.flashPassword,
            userKey = state.userKey,
            userAutoLogon = state.autoLogon && !state.isEncrypted,
            useProxy = state.useProxy,
            proxyAddress = if (state.useProxy) state.proxyAddress else "",
            proxyUserName = if (state.useProxy) state.proxyUserName else "",
            proxyPassword = if (state.useProxy) state.proxyPassword else "",
            isEncrypted = state.isEncrypted,
            configHash = if (state.isEncrypted) generateConfigHash() else "",
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Генерирует хеш конфигурации для шифрования
     */
    private fun generateConfigHash(): String {
        // TODO: Реализовать генерацию хеша для шифрования
        return "encrypted_${System.currentTimeMillis()}"
    }
    
    /**
     * Проверяет, есть ли несохраненные изменения
     */
    fun hasUnsavedChanges(): Boolean {
        val currentState = _uiState.value
        val original = originalProfile
        
        if (original == null) {
            // Новый профиль - есть изменения, если заполнены поля
            return currentState.nick.isNotBlank() || currentState.password.isNotBlank()
        }
        
        // Сравниваем с оригинальным профилем
        return currentState.nick != original.userNick ||
                currentState.password != original.userPassword ||
                currentState.flashPassword != original.userPasswordFlash ||
                currentState.userKey != original.userKey ||
                currentState.autoLogon != original.userAutoLogon ||
                currentState.useProxy != original.useProxy ||
                currentState.proxyAddress != original.proxyAddress ||
                currentState.proxyUserName != original.proxyUserName ||
                currentState.proxyPassword != original.proxyPassword
    }
}