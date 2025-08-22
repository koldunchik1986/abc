package ru.neverlands.abclient.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ru.neverlands.abclient.data.model.UserProfile
import ru.neverlands.abclient.ui.tabs.model.GameTab
import javax.inject.Inject
import javax.inject.Singleton

// Extension для создания DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "abclient_settings")

/**
 * Менеджер настроек пользователя - эквивалент сохранения профилей из Windows версии
 * Использует DataStore для обычных настроек и EncryptedSharedPreferences для чувствительных данных
 */
@Singleton
class UserPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val dataStore = context.dataStore
    private val gson = Gson()
    
    // Encrypted SharedPreferences для чувствительных данных
    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        EncryptedSharedPreferences.create(
            context,
            "abclient_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    companion object {
        // Ключи для DataStore
        private val CURRENT_PROFILE_ID = stringPreferencesKey("current_profile_id")
        private val PROFILES_JSON = stringPreferencesKey("profiles_json")
        private val TABS_JSON = stringPreferencesKey("tabs_json")
        private val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        private val APP_VERSION = stringPreferencesKey("app_version")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val LANGUAGE = stringPreferencesKey("language")
        
        // Ключи для Encrypted SharedPreferences
        private const val ENCRYPTED_PASSWORDS = "encrypted_passwords"
        private const val ENCRYPTED_PROXY_CREDENTIALS = "encrypted_proxy_credentials"
        private const val COOKIES_PREFIX = "cookies_"
    }
    
    /**
     * Сохраняет профиль пользователя
     */
    suspend fun saveProfile(profile: UserProfile) {
        val currentProfiles = getProfiles().toMutableList()
        val existingIndex = currentProfiles.indexOfFirst { it.id == profile.id }
        
        val updatedProfile = profile.withUpdatedTimestamp()
        
        if (existingIndex >= 0) {
            currentProfiles[existingIndex] = updatedProfile
        } else {
            currentProfiles.add(updatedProfile)
        }
        
        // Сохраняем список профилей
        val profilesJson = gson.toJson(currentProfiles)
        dataStore.edit { preferences ->
            preferences[PROFILES_JSON] = profilesJson
        }
        
        // Сохраняем чувствительные данные отдельно
        saveEncryptedData(updatedProfile)
    }
    
    /**
     * Загружает все профили
     */
    suspend fun getProfiles(): List<UserProfile> {
        return try {
            val profilesJson = dataStore.data.first()[PROFILES_JSON] ?: "[]"
            val type = object : TypeToken<List<UserProfile>>() {}.type
            gson.fromJson(profilesJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Получает профиль по ID
     */
    suspend fun getProfile(profileId: String): UserProfile? {
        return getProfiles().find { it.id == profileId }
    }
    
    /**
     * Удаляет профиль
     */
    suspend fun deleteProfile(profileId: String) {
        val currentProfiles = getProfiles().toMutableList()
        currentProfiles.removeAll { it.id == profileId }
        
        val profilesJson = gson.toJson(currentProfiles)
        dataStore.edit { preferences ->
            preferences[PROFILES_JSON] = profilesJson
        }
        
        // Удаляем зашифрованные данные
        deleteEncryptedData(profileId)
        
        // Если удаляем текущий профиль, сбрасываем его
        val currentProfileId = getCurrentProfileId()
        if (currentProfileId == profileId) {
            setCurrentProfileId("")
        }
    }
    
    /**
     * Устанавливает текущий профиль
     */
    suspend fun setCurrentProfileId(profileId: String) {
        dataStore.edit { preferences ->
            preferences[CURRENT_PROFILE_ID] = profileId
        }
    }
    
    /**
     * Получает ID текущего профиля
     */
    suspend fun getCurrentProfileId(): String? {
        return dataStore.data.first()[CURRENT_PROFILE_ID]
    }
    
    /**
     * Получает текущий профиль
     */
    suspend fun getCurrentProfile(): UserProfile? {
        val profileId = getCurrentProfileId() ?: return null
        return getProfile(profileId)
    }
    
    /**
     * Flow текущего профиля
     */
    fun getCurrentProfileFlow(): Flow<UserProfile?> {
        return dataStore.data.map { preferences ->
            val profileId = preferences[CURRENT_PROFILE_ID]
            if (profileId != null) {
                getProfile(profileId)
            } else {
                null
            }
        }
    }
    
    /**
     * Сохраняет вкладки
     */
    suspend fun saveTabs(tabs: List<GameTab>) {
        val tabsJson = gson.toJson(tabs)
        dataStore.edit { preferences ->
            preferences[TABS_JSON] = tabsJson
        }
    }
    
    /**
     * Загружает вкладки
     */
    suspend fun loadTabs(): List<GameTab> {
        return try {
            val tabsJson = dataStore.data.first()[TABS_JSON] ?: "[]"
            val type = object : TypeToken<List<GameTab>>() {}.type
            gson.fromJson(tabsJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Сохраняет cookies для хоста
     */
    fun saveCookies(host: String, cookies: List<String>) {
        val cookiesJson = gson.toJson(cookies)
        encryptedPrefs.edit()
            .putString("$COOKIES_PREFIX$host", cookiesJson)
            .apply()
    }
    
    /**
     * Загружает cookies для хоста
     */
    fun loadCookies(host: String): List<String> {
        return try {
            val cookiesJson = encryptedPrefs.getString("$COOKIES_PREFIX$host", "[]") ?: "[]"
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(cookiesJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Очищает cookies для хоста
     */
    fun clearCookies(host: String) {
        encryptedPrefs.edit()
            .remove("$COOKIES_PREFIX$host")
            .apply()
    }
    
    /**
     * Устанавливает текущий ник пользователя
     */
    fun setCurrentUserNick(nick: String) {
        encryptedPrefs.edit()
            .putString("current_user_nick", nick)
            .apply()
    }
    
    /**
     * Получает текущий ник пользователя
     */
    fun getCurrentUserNick(): String? {
        return encryptedPrefs.getString("current_user_nick", null)
    }
    
    /**
     * Проверяет, первый ли это запуск
     */
    fun isFirstLaunch(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[FIRST_LAUNCH] ?: true
        }
    }
    
    /**
     * Отмечает, что первый запуск завершен
     */
    suspend fun setFirstLaunchCompleted() {
        dataStore.edit { preferences ->
            preferences[FIRST_LAUNCH] = false
        }
    }
    
    /**
     * Устанавливает тему приложения
     */
    suspend fun setThemeMode(themeMode: String) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = themeMode
        }
    }
    
    /**
     * Получает тему приложения
     */
    fun getThemeMode(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[THEME_MODE] ?: "system"
        }
    }
    
    /**
     * Сохраняет зашифрованные данные профиля
     */
    private fun saveEncryptedData(profile: UserProfile) {
        // Сохраняем пароли и Flash пароль для дополнительной защиты
        if (profile.userPassword.isNotBlank() || profile.userPasswordFlash.isNotBlank()) {
            val passwordData = mapOf(
                "password" to profile.userPassword,
                "flashPassword" to profile.userPasswordFlash,
                "userKey" to profile.userKey
            )
            val passwordJson = gson.toJson(passwordData)
            encryptedPrefs.edit()
                .putString("${ENCRYPTED_PASSWORDS}_${profile.id}", passwordJson)
                .apply()
        }
        
        if (profile.isProxyConfigured()) {
            val proxyData = mapOf(
                "username" to profile.proxyUserName,
                "password" to profile.proxyPassword
            )
            val proxyJson = gson.toJson(proxyData)
            encryptedPrefs.edit()
                .putString("${ENCRYPTED_PROXY_CREDENTIALS}_${profile.id}", proxyJson)
                .apply()
        }
    }
    
    /**
     * Удаляет зашифрованные данные профиля
     */
    private fun deleteEncryptedData(profileId: String) {
        encryptedPrefs.edit()
            .remove("${ENCRYPTED_PASSWORDS}_$profileId")
            .remove("${ENCRYPTED_PROXY_CREDENTIALS}_$profileId")
            .apply()
    }
    
    /**
     * Очищает все данные
     */
    suspend fun clearAllData() {
        dataStore.edit { it.clear() }
        encryptedPrefs.edit().clear().apply()
    }
}