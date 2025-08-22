package ru.neverlands.abclient.di

import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.CookieJar
import ru.neverlands.abclient.core.network.cookie.GameCookieManager
import javax.inject.Singleton

/**
 * Модуль Dagger Hilt для предоставления основных зависимостей приложения
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Предоставляет Context приложения
     */
    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context = context

    /**
     * Предоставляет конфигурацию WorkManager
     */
    @Provides
    @Singleton
    fun provideWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    }

    /**
     * Предоставляет CookieJar для HTTP клиента
     */
    @Provides
    @Singleton
    fun provideCookieJar(
        @ApplicationContext context: Context,
        preferencesManager: ru.neverlands.abclient.data.preferences.UserPreferencesManager
    ): CookieJar {
        return GameCookieManager(context, preferencesManager)
    }
}