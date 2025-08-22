package ru.neverlands.abclient.data.repository

import kotlinx.coroutines.flow.Flow
import ru.neverlands.abclient.data.model.UserProfile
import ru.neverlands.abclient.data.preferences.UserPreferencesManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository для управления профилями пользователей
 * Является оберткой над UserPreferencesManager для предоставления репозиторного интерфейса
 */
@Singleton
class ProfileRepository @Inject constructor(
    private val userPreferencesManager: UserPreferencesManager
) {
    
    /**
     * Получает все профили
     */
    suspend fun getAllProfiles(): List<UserProfile> {
        return userPreferencesManager.getProfiles()
    }
    
    /**
     * Получает профиль по ID
     */
    suspend fun getProfile(profileId: String): UserProfile? {
        return userPreferencesManager.getProfile(profileId)
    }
    
    /**
     * Получает текущий профиль
     */
    suspend fun getCurrentProfile(): UserProfile? {
        return userPreferencesManager.getCurrentProfile()
    }
    
    /**
     * Flow текущего профиля
     */
    fun getCurrentProfileFlow(): Flow<UserProfile?> {
        return userPreferencesManager.getCurrentProfileFlow()
    }
    
    /**
     * Сохраняет профиль
     */
    suspend fun saveProfile(profile: UserProfile) {
        userPreferencesManager.saveProfile(profile)
    }
    
    /**
     * Удаляет профиль
     */
    suspend fun deleteProfile(profileId: String) {
        userPreferencesManager.deleteProfile(profileId)
    }
    
    /**
     * Устанавливает текущий профиль
     */
    suspend fun setCurrentProfile(profileId: String) {
        userPreferencesManager.setCurrentProfileId(profileId)
    }
    
    /**
     * Получает ID текущего профиля
     */
    suspend fun getCurrentProfileId(): String? {
        return userPreferencesManager.getCurrentProfileId()
    }
}