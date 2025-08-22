package com.koldunchik1986.ANL.data.repository.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.koldunchik1986.ANL.data.preferences.UserPreferencesManager
import com.koldunchik1986.ANL.data.repository.ProfileRepository
import javax.inject.Singleton

/**
 * Модуль DI для репозиториев
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideProfileRepository(
        userPreferencesManager: UserPreferencesManager
    ): ProfileRepository {
        return ProfileRepository(userPreferencesManager)
    }
}