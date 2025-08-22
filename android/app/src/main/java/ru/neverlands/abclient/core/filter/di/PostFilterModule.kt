package ru.neverlands.abclient.core.filter.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.neverlands.abclient.core.filter.HtmlFilters
import ru.neverlands.abclient.core.filter.HttpFilter
import ru.neverlands.abclient.core.filter.JavaScriptFilters
import ru.neverlands.abclient.core.filter.PhpFilters
import ru.neverlands.abclient.data.preferences.UserPreferencesManager
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