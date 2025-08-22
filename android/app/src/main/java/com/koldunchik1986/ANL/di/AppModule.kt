package com.koldunchik1986.ANL.di

import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.CookieJar
import com.koldunchik1986.ANL.core.network.cookie.GameCookieManager
import javax.inject.Singleton

/**
 * Модуль Dagger Hilt для предоставления зависимостей приложения
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Предоставление контекста приложения
     */
    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context = context

    /**
     * Предоставление конфигурации WorkManager
     */
    @Provides
    @Singleton
    fun provideWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    }

    /**
     * Предоставление CookieJar для HTTP клиентов
     */
    @Provides
    @Singleton
    fun provideCookieJar(
        @ApplicationContext context: Context,
        preferencesManager: com.koldunchik1986.ANL.data.preferences.UserPreferencesManager
    ): CookieJar {
        return GameCookieManager(context, preferencesManager)
    }
}