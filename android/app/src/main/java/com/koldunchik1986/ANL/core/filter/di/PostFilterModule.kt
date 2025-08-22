package com.koldunchik1986.ANL.core.filter.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.koldunchik1986.ANL.core.filter.HtmlFilters
import com.koldunchik1986.ANL.core.filter.HttpFilter
import com.koldunchik1986.ANL.core.filter.JavaScriptFilters
import com.koldunchik1986.ANL.core.filter.PhpFilters
import com.koldunchik1986.ANL.data.preferences.UserPreferencesManager
import javax.inject.Singleton

/**
 * Модуль DI для PostFilter системы
 */
@Module
@InstallIn(SingletonComponent::class)
object PostFilterModule {

    @Provides
    @Singleton
    fun provideHtmlFilters(
        userPreferencesManager: UserPreferencesManager
    ): HtmlFilters {
        return HtmlFilters(userPreferencesManager)
    }

    @Provides
    @Singleton
    fun provideJavaScriptFilters(
        userPreferencesManager: UserPreferencesManager
    ): JavaScriptFilters {
        return JavaScriptFilters(userPreferencesManager)
    }

    @Provides
    @Singleton
    fun providePhpFilters(
        userPreferencesManager: UserPreferencesManager,
        htmlFilters: HtmlFilters
    ): PhpFilters {
        return PhpFilters(userPreferencesManager, htmlFilters)
    }

    @Provides
    @Singleton
    fun provideHttpFilter(
        userPreferencesManager: UserPreferencesManager,
        jsFilters: JavaScriptFilters,
        phpFilters: PhpFilters,
        htmlFilters: HtmlFilters
    ): HttpFilter {
        return HttpFilter(userPreferencesManager, jsFilters, phpFilters, htmlFilters)
    }
}