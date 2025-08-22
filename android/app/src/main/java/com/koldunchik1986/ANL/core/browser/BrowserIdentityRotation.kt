package com.koldunchik1986.ANL.core.browser

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.koldunchik1986.ANL.data.preferences.UserPreferencesManager
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Система ротации браузерной идентичности
 * Меняет User-Agent, fingerprint и другие параметры для обхода детекции
 */
@Singleton
class BrowserIdentityRotation @Inject constructor(
    private val preferencesManager: UserPreferencesManager
) {
    
    private val _currentIdentity = MutableStateFlow(BrowserIdentity.generateRandom())
    val currentIdentity: StateFlow<BrowserIdentity> = _currentIdentity.asStateFlow()
    
    companion object {
        // Различные профили браузеров
        internal val BROWSER_PROFILES = listOf(
            BrowserProfile("Chrome", "120.0.0.0", "Android", "13"),
            BrowserProfile("Chrome", "119.0.0.0", "Android", "12"),
            BrowserProfile("Chrome", "118.0.0.0", "Android", "13"),
            BrowserProfile("Samsung Internet", "21.0.0.0", "Android", "13"),
            BrowserProfile("Edge", "120.0.0.0", "Android", "13")
        )
        
        // Различные профили Android устройств
        internal val DEVICE_PROFILES = listOf(
            DeviceProfile("Samsung", "SM-G998B", "Galaxy S21 Ultra", "1440x3200"),
            DeviceProfile("Samsung", "SM-G991B", "Galaxy S21", "1080x2400"),
            DeviceProfile("Samsung", "SM-G981B", "Galaxy S20", "1080x2400"),
            DeviceProfile("Google", "Pixel 7", "Pixel 7", "1080x2400"),
            DeviceProfile("Google", "Pixel 6", "Pixel 6", "1080x2400"),
            DeviceProfile("Samsung", "SM-A525F", "Galaxy A52s", "1080x2400"),
            DeviceProfile("OnePlus", "CPH2449", "OnePlus 11", "1440x3216")
        )
        
        // Различные сетевые профили
        internal val NETWORK_PROFILES = listOf(
            NetworkProfile("WiFi", "high", listOf("ru-RU", "ru", "en")),
            NetworkProfile("4G", "medium", listOf("ru-RU", "ru")),
            NetworkProfile("WiFi", "high", listOf("ru-RU", "en-US", "en")),
            NetworkProfile("5G", "high", listOf("ru-RU", "ru", "en-US"))
        )
    }
    
    /**
     * Генерирует новую браузерную идентичность
     */
    fun rotateIdentity(): BrowserIdentity {
        val newIdentity = BrowserIdentity.generateRandom()
        _currentIdentity.value = newIdentity
        return newIdentity
    }
    
    /**
     * Генерирует User-Agent на основе текущей идентичности
     */
    fun generateUserAgent(): String {
        val identity = _currentIdentity.value
        val browser = identity.browserProfile
        val device = identity.deviceProfile
        
        return when (browser.name) {
            "Chrome" -> {
                "Mozilla/5.0 (Linux; Android ${browser.osVersion}; ${device.model}) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/${browser.version} Mobile Safari/537.36"
            }
            "Samsung Internet" -> {
                "Mozilla/5.0 (Linux; Android ${browser.osVersion}; ${device.model}) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "SamsungBrowser/${browser.version} Chrome/120.0.0.0 Mobile Safari/537.36"
            }
            "Edge" -> {
                "Mozilla/5.0 (Linux; Android ${browser.osVersion}; ${device.model}) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "EdgA/${browser.version} Chrome/120.0.0.0 Mobile Safari/537.36"
            }
            else -> {
                "Mozilla/5.0 (Linux; Android ${browser.osVersion}; ${device.model}) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/120.0.0.0 Mobile Safari/537.36"
            }
        }
    }
    
    /**
     * Генерирует HTTP заголовки на основе текущей идентичности
     */
    fun generateHttpHeaders(): Map<String, String> {
        val identity = _currentIdentity.value
        val network = identity.networkProfile
        
        val headers = mutableMapOf<String, String>()
        
        // User-Agent
        headers["User-Agent"] = generateUserAgent()
        
        // Accept заголовки
        headers["Accept"] = when (identity.browserProfile.name) {
            "Chrome" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8"
            "Samsung Internet" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8"
            "Edge" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8"
            else -> "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        }
        
        // Языковые предпочтения
        headers["Accept-Language"] = network.languages.joinToString(",") { lang ->
            when (network.languages.indexOf(lang)) {
                0 -> "$lang"
                1 -> "$lang;q=0.9"
                else -> "$lang;q=0.8"
            }
        }
        
        // Кодирование
        headers["Accept-Encoding"] = when (network.quality) {
            "high" -> "gzip, deflate, br"
            "medium" -> "gzip, deflate"
            else -> "gzip"
        }
        
        // Специфичные заголовки Chrome
        if (identity.browserProfile.name == "Chrome") {
            headers["sec-ch-ua"] = "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"${identity.browserProfile.version.split(".")[0]}\", \"Google Chrome\";v=\"${identity.browserProfile.version.split(".")[0]}\""
            headers["sec-ch-ua-mobile"] = "?1"
            headers["sec-ch-ua-platform"] = "\"Android\""
        }
        
        // Стандартные заголовки
        headers["Connection"] = "keep-alive"
        headers["DNT"] = if (Random().nextBoolean()) "1" else "0"
        
        return headers
    }
    
    /**
     * Планирует автоматическую ротацию идентичности
     */
    fun scheduleRotation(intervalMinutes: Int = 30) {
        // TODO: Реализовать автоматическую ротацию через WorkManager
    }
    
    /**
     * Сохраняет текущую идентичность
     */
    suspend fun saveCurrentIdentity() {
        // TODO: Сохранить в настройки или хранилище
    }
    
    /**
     * Загружает сохраненную идентичность
     */
    suspend fun loadSavedIdentity(): BrowserIdentity? {
        // TODO: Загрузить из хранилища
        return null
    }
    
    /**
     * Проверяет, пора ли менять идентичность
     */
    fun shouldRotateIdentity(): Boolean {
        val identity = _currentIdentity.value
        val ageHours = (System.currentTimeMillis() - identity.createdAt) / (1000 * 60 * 60)
        
        // Меняем каждые 6-12 часов для лучшей маскировки
        return ageHours >= 6 + Random().nextInt(7)
    }
    
    /**
     * Возвращает рекомендуемые настройки для текущей идентичности
     */
    fun getRecommendedSettings(): IdentitySettings {
        val identity = _currentIdentity.value
        
        return IdentitySettings(
            userAgent = generateUserAgent(),
            headers = generateHttpHeaders(),
            screenResolution = identity.deviceProfile.resolution,
            timezone = getTimezoneForDevice(identity.deviceProfile),
            language = identity.networkProfile.languages.first(),
            connectionType = identity.networkProfile.type
        )
    }
    
    /**
     * Возвращает временную зону для устройства
     */
    private fun getTimezoneForDevice(device: DeviceProfile): String {
        return when (device.manufacturer) {
            "Samsung" -> "Europe/Moscow"
            "Google" -> "Europe/Moscow" 
            "OnePlus" -> "Europe/Moscow"
            else -> "Europe/Moscow"
        }
    }
}

/**
 * Браузерная идентичность
 */
data class BrowserIdentity(
    val id: String,
    val createdAt: Long,
    val browserProfile: BrowserProfile,
    val deviceProfile: DeviceProfile,
    val networkProfile: NetworkProfile,
    val sessionId: String
) {
    companion object {
        fun generateRandom(): BrowserIdentity {
            return BrowserIdentity(
                id = UUID.randomUUID().toString(),
                createdAt = System.currentTimeMillis(),
                browserProfile = BrowserIdentityRotation.BROWSER_PROFILES.random(),
                deviceProfile = BrowserIdentityRotation.DEVICE_PROFILES.random(),
                networkProfile = BrowserIdentityRotation.NETWORK_PROFILES.random(),
                sessionId = UUID.randomUUID().toString()
            )
        }
    }
}

/**
 * Профиль браузера
 */
data class BrowserProfile(
    val name: String,
    val version: String,
    val platform: String,
    val osVersion: String
)

/**
 * Профиль устройства
 */
data class DeviceProfile(
    val manufacturer: String,
    val model: String,
    val name: String,
    val resolution: String
)

/**
 * Профиль сети
 */
data class NetworkProfile(
    val type: String,           // WiFi, 4G, 5G
    val quality: String,        // high, medium, low
    val languages: List<String>
)

/**
 * Настройки идентичности
 */
data class IdentitySettings(
    val userAgent: String,
    val headers: Map<String, String>,
    val screenResolution: String,
    val timezone: String,
    val language: String,
    val connectionType: String
)
