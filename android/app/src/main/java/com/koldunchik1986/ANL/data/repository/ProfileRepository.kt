package com.koldunchik1986.ANL.data.repository

import kotlinx.coroutines.flow.Flow
import com.koldunchik1986.ANL.data.model.UserProfile
import com.koldunchik1986.ANL.data.preferences.UserPreferencesManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository для работы с профилями пользователей
 * Обеспечивает абстракцию над UserPreferencesManager для управления профилями пользователей
 */
@Singleton
class ProfileRepository @Inject constructor(
    private val userPreferencesManager: UserPreferencesManager
) {
    
    /**
     * Получение всех профилей
     */
    suspend fun getAllProfiles(): List<UserProfile> {
        return userPreferencesManager.getProfiles()
    }
    
    /**
     * Получение профиля по ID
     */
    suspend fun getProfile(profileId: String): UserProfile? {
        return userPreferencesManager.getProfile(profileId)
    }
    
    /**
     * Получение текущего профиля
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
     * Сохранение профиля
     */
    suspend fun saveProfile(profile: UserProfile) {
        userPreferencesManager.saveProfile(profile)
    }
    
    /**
     * Удаление профиля
     */
    suspend fun deleteProfile(profileId: String) {
        userPreferencesManager.deleteProfile(profileId)
    }
    
    /**
     * Установка текущего профиля
     */
    suspend fun setCurrentProfile(profileId: String) {
        userPreferencesManager.setCurrentProfileId(profileId)
    }
    
    /**
     * Получение ID текущего профиля
     */
    suspend fun getCurrentProfileId(): String? {
        return userPreferencesManager.getCurrentProfileId()
    }
}