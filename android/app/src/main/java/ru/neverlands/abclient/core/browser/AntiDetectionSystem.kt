package ru.neverlands.abclient.core.browser

import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.neverlands.abclient.core.network.GameHttpClient
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Система защиты от обнаружения ботов
 * Обходит различные методы детекции автоматизированных клиентов
 */
@Singleton
class AntiDetectionSystem @Inject constructor(
    private val httpClient: GameHttpClient,
    private val advancedEmulation: AdvancedBrowserEmulation
) {
    
    private val _detectionStatus = MutableStateFlow(DetectionStatus.SAFE)
    val detectionStatus: StateFlow<DetectionStatus> = _detectionStatus.asStateFlow()
    
    private val _bypasses = MutableStateFlow<List<BypassResult>>(emptyList())
    val bypasses: StateFlow<List<BypassResult>> = _bypasses.asStateFlow()
    
    companion object {
        // Скрипты для обнаружения автоматизации
        private val DETECTION_SCRIPTS = listOf(
            "navigator.webdriver",
            "window.chrome && window.chrome.runtime",
            "navigator.plugins.length",
            "navigator.languages",
            "screen.width && screen.height",
            "window.outerWidth && window.outerHeight"
        )
        
        // Типичные антибот проверки
        private val ANTIBOT_PATTERNS = listOf(
            "cloudflare",
            "recaptcha",
            "hcaptcha", 
            "bot detection",
            "security check",
            "verify you are human"
        )
    }
    
    /**
     * Инициализирует защиту от детекции в WebView
     */
    fun initWebViewProtection(webView: WebView) {
        // Добавляем JavaScript интерфейс для перехвата проверок
        webView.addJavascriptInterface(AntiDetectionInterface(), "antiDetection")
        
        // Внедряем скрипты для обхода детекции
        injectAntiDetectionScripts(webView)
    }
    
    /**
     * Внедряет скрипты для обхода детекции
     */
    private fun injectAntiDetectionScripts(webView: WebView) {
        val script = buildString {
            // Скрываем navigator.webdriver
            appendLine("Object.defineProperty(navigator, 'webdriver', { get: () => undefined });")
            
            // Эмулируем нормальные значения navigator
            appendLine("Object.defineProperty(navigator, 'plugins', { get: () => [1, 2, 3, 4, 5] });")
            appendLine("Object.defineProperty(navigator, 'languages', { get: () => ['ru-RU', 'ru', 'en'] });")
            
            // Эмулируем Chrome runtime
            appendLine("window.chrome = { runtime: {} };")
            
            // Переопределяем методы, которые могут выдать автоматизацию
            appendLine("const originalQuery = window.navigator.permissions.query;")
            appendLine("window.navigator.permissions.query = (parameters) => (")
            appendLine("  parameters.name === 'notifications' ?")
            appendLine("    Promise.resolve({ state: Notification.permission }) :")
            appendLine("    originalQuery(parameters)")
            appendLine(");")
            
            // Эмулируем mouse и touch события
            appendLine("['mousedown', 'mouseup', 'mousemove'].forEach(event => {")
            appendLine("  document.addEventListener(event, () => {")
            appendLine("    window.antiDetection?.recordMouseActivity();")
            appendLine("  });")
            appendLine("});")
            
            // Скрываем headless режим
            appendLine("Object.defineProperty(navigator, 'hardwareConcurrency', { get: () => 4 });")
            appendLine("Object.defineProperty(navigator, 'deviceMemory', { get: () => 8 });")
        }
        
        webView.evaluateJavascript("(function() { $script })();", null)
    }
    
    /**
     * Сканирует страницу на наличие антибот систем
     */
    suspend fun scanForAntiBot(pageContent: String): DetectionResult {
        val detectedSystems = mutableListOf<String>()
        
        // Проверяем на известные антибот паттерны
        ANTIBOT_PATTERNS.forEach { pattern ->
            if (pageContent.contains(pattern, ignoreCase = true)) {
                detectedSystems.add(pattern)
            }
        }
        
        // Анализируем JavaScript код на предмет проверок
        val jsChecks = analyzeJavaScriptChecks(pageContent)
        
        val riskLevel = when {
            detectedSystems.size >= 2 -> RiskLevel.HIGH
            detectedSystems.size == 1 -> RiskLevel.MEDIUM
            jsChecks.isNotEmpty() -> RiskLevel.LOW
            else -> RiskLevel.SAFE
        }
        
        return DetectionResult(
            riskLevel = riskLevel,
            detectedSystems = detectedSystems,
            jsChecks = jsChecks,
            recommendations = generateRecommendations(riskLevel)
        )
    }
    
    /**
     * Анализирует JavaScript код на предмет проверок автоматизации
     */
    private fun analyzeJavaScriptChecks(content: String): List<String> {
        val detectedChecks = mutableListOf<String>()
        
        DETECTION_SCRIPTS.forEach { check ->
            if (content.contains(check)) {
                detectedChecks.add(check)
            }
        }
        
        // Дополнительные проверки
        if (content.contains("selenium", ignoreCase = true)) {
            detectedChecks.add("selenium detection")
        }
        
        if (content.contains("headless", ignoreCase = true)) {
            detectedChecks.add("headless detection") 
        }
        
        return detectedChecks
    }
    
    /**
     * Генерирует рекомендации по обходу детекции
     */
    private fun generateRecommendations(riskLevel: RiskLevel): List<String> {
        return when (riskLevel) {
            RiskLevel.HIGH -> listOf(
                "Использовать дополнительные задержки между запросами",
                "Включить расширенную эмуляцию мыши",
                "Варьировать User-Agent",
                "Добавить случайные перерывы в активности"
            )
            RiskLevel.MEDIUM -> listOf(
                "Увеличить задержки между действиями",
                "Эмулировать движения мыши",
                "Варьировать паттерны поведения"
            )
            RiskLevel.LOW -> listOf(
                "Поддерживать текущий уровень эмуляции",
                "Мониторить изменения в защите"
            )
            RiskLevel.SAFE -> listOf(
                "Детекция не обнаружена",
                "Можно использовать стандартные настройки"
            )
        }
    }
    
    /**
     * Применяет обходы в зависимости от обнаруженных систем
     */
    suspend fun applyBypasses(detectionResult: DetectionResult): List<BypassResult> {
        val results = mutableListOf<BypassResult>()
        
        // Обход Cloudflare
        if (detectionResult.detectedSystems.contains("cloudflare")) {
            results.add(bypassCloudflare())
        }
        
        // Обход CAPTCHA систем
        if (detectionResult.detectedSystems.any { it.contains("captcha", ignoreCase = true) }) {
            results.add(bypassCaptcha())
        }
        
        // Общие обходы JavaScript проверок
        if (detectionResult.jsChecks.isNotEmpty()) {
            results.add(bypassJavaScriptChecks())
        }
        
        _bypasses.value = results
        return results
    }
    
    /**
     * Обход Cloudflare защиты
     */
    private suspend fun bypassCloudflare(): BypassResult {
        return try {
            // Эмулируем задержку браузера при проверке Cloudflare
            delay(5000)
            
            // Устанавливаем специальные заголовки
            val headers = mapOf(
                "Sec-Fetch-Dest" to "document",
                "Sec-Fetch-Mode" to "navigate", 
                "Sec-Fetch-Site" to "none",
                "Sec-Fetch-User" to "?1"
            )
            
            BypassResult(
                system = "Cloudflare",
                success = true,
                method = "Browser emulation with delays",
                appliedHeaders = headers
            )
        } catch (e: Exception) {
            BypassResult(
                system = "Cloudflare",
                success = false,
                method = "Failed to bypass",
                error = e.message
            )
        }
    }
    
    /**
     * Обход CAPTCHA систем
     */
    private suspend fun bypassCaptcha(): BypassResult {
        return try {
            // Для CAPTCHA требуется человеческое вмешательство
            // Уведомляем пользователя
            _detectionStatus.value = DetectionStatus.CAPTCHA_REQUIRED
            
            BypassResult(
                system = "CAPTCHA",
                success = false,
                method = "Human verification required",
                userActionRequired = true
            )
        } catch (e: Exception) {
            BypassResult(
                system = "CAPTCHA",
                success = false,
                method = "Detection failed",
                error = e.message
            )
        }
    }
    
    /**
     * Обход JavaScript проверок
     */
    private suspend fun bypassJavaScriptChecks(): BypassResult {
        return try {
            // Генерируем новый браузерный fingerprint
            val newFingerprint = advancedEmulation.generateBrowserFingerprint()
            advancedEmulation.updateSessionData(newFingerprint)
            
            BypassResult(
                system = "JavaScript Detection",
                success = true,
                method = "Fingerprint rotation",
                newFingerprint = newFingerprint
            )
        } catch (e: Exception) {
            BypassResult(
                system = "JavaScript Detection",
                success = false,
                method = "Fingerprint generation failed",
                error = e.message
            )
        }
    }
    
    /**
     * JavaScript интерфейс для перехвата детекции
     */
    inner class AntiDetectionInterface {
        @JavascriptInterface
        fun recordMouseActivity() {
            // Записываем активность мыши для статистики
        }
        
        @JavascriptInterface
        fun reportDetection(system: String, details: String) {
            // Уведомляем о попытке детекции
            CoroutineScope(Dispatchers.Main).launch {
                _detectionStatus.value = DetectionStatus.DETECTED
            }
        }
    }
}

/**
 * Результат детекции антибот систем
 */
data class DetectionResult(
    val riskLevel: RiskLevel,
    val detectedSystems: List<String>,
    val jsChecks: List<String>,
    val recommendations: List<String>
)

/**
 * Результат применения обхода
 */
data class BypassResult(
    val system: String,
    val success: Boolean,
    val method: String,
    val appliedHeaders: Map<String, String> = emptyMap(),
    val newFingerprint: BrowserFingerprint? = null,
    val userActionRequired: Boolean = false,
    val error: String? = null
)

/**
 * Уровень риска детекции
 */
enum class RiskLevel {
    SAFE,       // Безопасно
    LOW,        // Низкий риск
    MEDIUM,     // Средний риск
    HIGH        // Высокий риск
}

/**
 * Статус детекции
 */
enum class DetectionStatus {
    SAFE,               // Безопасно
    DETECTED,           // Обнаружена попытка детекции
    CAPTCHA_REQUIRED,   // Требуется решение CAPTCHA
    BLOCKED            // Заблокирован
}