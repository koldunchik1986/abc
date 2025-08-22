package ru.neverlands.abclient.data.repository.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.neverlands.abclient.data.preferences.UserPreferencesManager
import ru.neverlands.abclient.data.repository.ProfileRepository
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