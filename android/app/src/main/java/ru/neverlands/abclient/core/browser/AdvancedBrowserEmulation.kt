package ru.neverlands.abclient.core.browser

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.neverlands.abclient.data.preferences.UserPreferencesManager
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Расширенная система эмуляции браузера для обхода детекции
 * Имитирует поведение реального пользователя браузера
 */
@Singleton
class AdvancedBrowserEmulation @Inject constructor(
    private val context: Context,
    private val preferencesManager: UserPreferencesManager
) {
    
    private val _sessionData = MutableStateFlow(BrowserSession.create())
    val sessionData: StateFlow<BrowserSession> = _sessionData.asStateFlow()
    
    companion object {
        // Типичные разрешения экранов Android устройств
        private val SCREEN_RESOLUTIONS = listOf(
            Pair(1080, 2400), Pair(1080, 2340), Pair(1080, 2280),
            Pair(1440, 3200), Pair(1440, 3040), Pair(720, 1600)
        )
        
        // Типичные версии Chrome
        private val CHROME_VERSIONS = listOf(
            "120.0.0.0", "119.0.0.0", "118.0.0.0", "117.0.0.0"
        )
        
        // Типичные модели Android устройств
        private val ANDROID_DEVICES = listOf(
            "SM-G998B", "SM-G991B", "SM-G981B", "SM-N981B",
            "Pixel 7", "Pixel 6", "Pixel 6a", "SM-A525F"
        )
    }
    
    /**
     * Генерирует уникальный браузерный fingerprint
     */
    fun generateBrowserFingerprint(): BrowserFingerprint {
        val random = Random()
        
        val resolution = SCREEN_RESOLUTIONS.random()
        val chromeVersion = CHROME_VERSIONS.random()
        val device = ANDROID_DEVICES.random()
        
        return BrowserFingerprint(
            screenWidth = resolution.first,
            screenHeight = resolution.second,
            colorDepth = 24,
            timezone = TimeZone.getDefault().id,
            language = "ru-RU",
            platform = "Linux armv8l",
            cookieEnabled = true,
            onlineStatus = true,
            chromeVersion = chromeVersion,
            deviceModel = device,
            androidVersion = "13",
            dnt = true,
            sessionStorage = true,
            localStorage = true,
            indexedDB = true,
            webGL = true,
            canvas = generateCanvasFingerprint()
        )
    }
    
    /**
     * Генерирует Canvas fingerprint для имитации уникального устройства
     */
    private fun generateCanvasFingerprint(): String {
        // Симулируем уникальный canvas fingerprint
        val random = Random()
        val chars = "0123456789abcdef"
        return (1..32).map { chars.random() }.joinToString("")
    }
    
    /**
     * Создает реалистичную историю посещений
     */
    fun createBrowsingHistory(): List<BrowsingHistoryEntry> {
        val history = mutableListOf<BrowsingHistoryEntry>()
        val now = System.currentTimeMillis()
        
        // Добавляем записи о посещении игрового сайта
        history.add(BrowsingHistoryEntry(
            url = "http://www.neverlands.ru/",
            title = "Бесплатная онлайн игра Neverlans",
            visitTime = now - 300000, // 5 минут назад
            referrer = "https://www.google.com/"
        ))
        
        history.add(BrowsingHistoryEntry(
            url = "http://www.neverlands.ru/main.php",
            title = "Neverlands - Главная страница",
            visitTime = now - 240000, // 4 минуты назад
            referrer = "http://www.neverlands.ru/"
        ))
        
        // Добавляем записи о "случайных" посещениях для маскировки
        history.add(BrowsingHistoryEntry(
            url = "https://www.google.com/search?q=neverlans+игра",
            title = "neverlans игра - Поиск в Google",
            visitTime = now - 600000, // 10 минут назад
            referrer = ""
        ))
        
        return history.sortedByDescending { it.visitTime }
    }
    
    /**
     * Симулирует поведение мыши и клавиатуры
     */
    fun simulateUserInteraction(): UserInteractionPattern {
        val random = Random()
        
        return UserInteractionPattern(
            mouseMovePatterns = generateMouseMovements(),
            clickPatterns = generateClickPatterns(),
            scrollPatterns = generateScrollPatterns(),
            keyboardPatterns = generateKeyboardPatterns(),
            idleTime = random.nextInt(30000) + 5000L, // 5-35 секунд
            activeTime = random.nextInt(120000) + 30000L // 30-150 секунд
        )
    }
    
    /**
     * Генерирует паттерны движения мыши
     */
    private fun generateMouseMovements(): List<MouseMovement> {
        val movements = mutableListOf<MouseMovement>()
        val random = Random()
        
        var x = random.nextInt(1080)
        var y = random.nextInt(2400)
        
        repeat(random.nextInt(20) + 5) {
            x += random.nextInt(200) - 100
            y += random.nextInt(200) - 100
            
            movements.add(MouseMovement(
                x = maxOf(0, minOf(1080, x)),
                y = maxOf(0, minOf(2400, y)),
                timestamp = System.currentTimeMillis() + it * (100 + random.nextInt(300))
            ))
        }
        
        return movements
    }
    
    /**
     * Генерирует паттерны кликов
     */
    private fun generateClickPatterns(): List<ClickEvent> {
        val clicks = mutableListOf<ClickEvent>()
        val random = Random()
        
        repeat(random.nextInt(10) + 2) {
            clicks.add(ClickEvent(
                x = random.nextInt(1080),
                y = random.nextInt(2400),
                timestamp = System.currentTimeMillis() + it * (1000 + random.nextInt(5000)),
                button = if (random.nextBoolean()) "left" else "right"
            ))
        }
        
        return clicks
    }
    
    /**
     * Генерирует паттерны прокрутки
     */
    private fun generateScrollPatterns(): List<ScrollEvent> {
        val scrolls = mutableListOf<ScrollEvent>()
        val random = Random()
        
        repeat(random.nextInt(15) + 3) {
            scrolls.add(ScrollEvent(
                deltaX = random.nextInt(20) - 10,
                deltaY = random.nextInt(200) - 100,
                timestamp = System.currentTimeMillis() + it * (500 + random.nextInt(2000))
            ))
        }
        
        return scrolls
    }
    
    /**
     * Генерирует паттерны ввода с клавиатуры
     */
    private fun generateKeyboardPatterns(): List<KeyboardEvent> {
        val keys = mutableListOf<KeyboardEvent>()
        val random = Random()
        
        // Симулируем ввод типичных игровых команд
        val gameCommands = listOf("север", "юг", "восток", "запад", "осмотреться", "инвентарь")
        
        repeat(random.nextInt(5) + 1) {
            val command = gameCommands.random()
            command.forEachIndexed { index, char ->
                keys.add(KeyboardEvent(
                    key = char.toString(),
                    timestamp = System.currentTimeMillis() + it * 2000 + index * (100 + random.nextInt(200)),
                    eventType = "keydown"
                ))
            }
        }
        
        return keys
    }
    
    /**
     * Обновляет данные сессии
     */
    fun updateSessionData(fingerprint: BrowserFingerprint) {
        val currentSession = _sessionData.value
        _sessionData.value = currentSession.copy(
            fingerprint = fingerprint,
            lastUpdate = System.currentTimeMillis(),
            pageViews = currentSession.pageViews + 1
        )
    }
    
    /**
     * Сохраняет данные сессии в настройки
     */
    suspend fun saveSessionData() {
        // TODO: Реализовать сохранение в зашифрованном виде
    }
}

/**
 * Данные браузерной сессии
 */
data class BrowserSession(
    val sessionId: String,
    val startTime: Long,
    val lastUpdate: Long,
    val fingerprint: BrowserFingerprint,
    val browsingHistory: List<BrowsingHistoryEntry>,
    val interactionPattern: UserInteractionPattern,
    val pageViews: Int,
    val tabsOpened: Int
) {
    companion object {
        fun create(): BrowserSession {
            return BrowserSession(
                sessionId = UUID.randomUUID().toString(),
                startTime = System.currentTimeMillis(),
                lastUpdate = System.currentTimeMillis(),
                fingerprint = BrowserFingerprint.default(),
                browsingHistory = emptyList(),
                interactionPattern = UserInteractionPattern.default(),
                pageViews = 0,
                tabsOpened = 1
            )
        }
    }
}

/**
 * Браузерный fingerprint
 */
data class BrowserFingerprint(
    val screenWidth: Int,
    val screenHeight: Int,
    val colorDepth: Int,
    val timezone: String,
    val language: String,
    val platform: String,
    val cookieEnabled: Boolean,
    val onlineStatus: Boolean,
    val chromeVersion: String,
    val deviceModel: String,
    val androidVersion: String,
    val dnt: Boolean,
    val sessionStorage: Boolean,
    val localStorage: Boolean,
    val indexedDB: Boolean,
    val webGL: Boolean,
    val canvas: String
) {
    companion object {
        fun default(): BrowserFingerprint {
            return BrowserFingerprint(
                screenWidth = 1080,
                screenHeight = 2400,
                colorDepth = 24,
                timezone = "Europe/Moscow",
                language = "ru-RU",
                platform = "Linux armv8l",
                cookieEnabled = true,
                onlineStatus = true,
                chromeVersion = "120.0.0.0",
                deviceModel = "SM-G998B",
                androidVersion = "13",
                dnt = true,
                sessionStorage = true,
                localStorage = true,
                indexedDB = true,
                webGL = true,
                canvas = "a1b2c3d4e5f6789012345678901234567890abcd"
            )
        }
    }
}

/**
 * Запись истории браузера
 */
data class BrowsingHistoryEntry(
    val url: String,
    val title: String,
    val visitTime: Long,
    val referrer: String
)

/**
 * Паттерн взаимодействия пользователя
 */
data class UserInteractionPattern(
    val mouseMovePatterns: List<MouseMovement>,
    val clickPatterns: List<ClickEvent>,
    val scrollPatterns: List<ScrollEvent>,
    val keyboardPatterns: List<KeyboardEvent>,
    val idleTime: Long,
    val activeTime: Long
) {
    companion object {
        fun default(): UserInteractionPattern {
            return UserInteractionPattern(
                mouseMovePatterns = emptyList(),
                clickPatterns = emptyList(),
                scrollPatterns = emptyList(),
                keyboardPatterns = emptyList(),
                idleTime = 10000L,
                activeTime = 60000L
            )
        }
    }
}

/**
 * Движение мыши
 */
data class MouseMovement(
    val x: Int,
    val y: Int,
    val timestamp: Long
)

/**
 * Событие клика
 */
data class ClickEvent(
    val x: Int,
    val y: Int,
    val timestamp: Long,
    val button: String
)

/**
 * Событие прокрутки
 */
data class ScrollEvent(
    val deltaX: Int,
    val deltaY: Int,
    val timestamp: Long
)

/**
 * Событие клавиатуры
 */
data class KeyboardEvent(
    val key: String,
    val timestamp: Long,
    val eventType: String
)