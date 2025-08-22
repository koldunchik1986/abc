package ru.neverlands.abclient.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import ru.neverlands.abclient.R
import ru.neverlands.abclient.core.network.GameHttpClient
import ru.neverlands.abclient.data.preferences.UserPreferencesManager
import ru.neverlands.abclient.core.network.cookie.GameCookieManager
import javax.inject.Inject

/**
 * Фоновая служба для поддержания онлайн статуса в игре
 * Критически важна для предотвращения автоматического выхода из игры
 */
@AndroidEntryPoint
class OnlineStatusService : Service() {
    
    @Inject
    lateinit var httpClient: GameHttpClient
    
    @Inject
    lateinit var preferencesManager: UserPreferencesManager
    
    @Inject
    lateinit var cookieManager: GameCookieManager
    
    private var serviceJob: Job? = null
    private var isRunning = false
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "online_status_channel"
        private const val CHANNEL_NAME = "Онлайн статус"
        
        // Интервалы проверки статуса
        private const val CHECK_INTERVAL_NORMAL = 30_000L // 30 секунд в обычном режиме
        private const val CHECK_INTERVAL_BATTLE = 10_000L // 10 секунд в бою
        private const val CHECK_INTERVAL_ERROR = 60_000L // 1 минута при ошибках
        
        const val ACTION_START_SERVICE = "START_SERVICE"
        const val ACTION_STOP_SERVICE = "STOP_SERVICE"
        const val ACTION_CHECK_STATUS = "CHECK_STATUS"
        
        /**
         * Запускает службу
         */
        fun start(context: Context) {
            val intent = Intent(context, OnlineStatusService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        /**
         * Останавливает службу
         */
        fun stop(context: Context) {
            val intent = Intent(context, OnlineStatusService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.stopService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVICE -> startOnlineStatusMonitoring()
            ACTION_STOP_SERVICE -> stopOnlineStatusMonitoring()
            ACTION_CHECK_STATUS -> performSingleStatusCheck()
        }
        
        return START_STICKY // Перезапускаем службу при убийстве системой
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        stopOnlineStatusMonitoring()
    }
    
    /**
     * Запускает мониторинг онлайн статуса
     */
    private fun startOnlineStatusMonitoring() {
        if (isRunning) return
        
        startForeground(NOTIFICATION_ID, createNotification("Проверка статуса..."))
        isRunning = true
        
        serviceJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            var currentInterval = CHECK_INTERVAL_NORMAL
            var consecutiveErrors = 0
            
            while (isActive && isRunning) {
                try {
                    val status = checkOnlineStatus()
                    
                    // Обновляем уведомление
                    updateNotification(status)
                    
                    // Определяем следующий интервал проверки
                    currentInterval = when {
                        status.isInBattle -> CHECK_INTERVAL_BATTLE
                        status.hasError -> CHECK_INTERVAL_ERROR
                        else -> CHECK_INTERVAL_NORMAL
                    }
                    
                    consecutiveErrors = if (status.hasError) consecutiveErrors + 1 else 0
                    
                    // Если слишком много ошибок подряд, увеличиваем интервал
                    if (consecutiveErrors > 5) {
                        currentInterval = CHECK_INTERVAL_ERROR * 2
                    }
                    
                } catch (e: Exception) {
                    updateNotification(OnlineStatus.error("Ошибка: ${e.message}"))
                    currentInterval = CHECK_INTERVAL_ERROR
                    consecutiveErrors++
                }
                
                delay(currentInterval)
            }
        }
    }
    
    /**
     * Останавливает мониторинг
     */
    private fun stopOnlineStatusMonitoring() {
        isRunning = false
        serviceJob?.cancel()
        stopForeground(true)
        stopSelf()
    }
    
    /**
     * Выполняет одиночную проверку статуса
     */
    private fun performSingleStatusCheck() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val status = checkOnlineStatus()
                updateNotification(status)
            } catch (e: Exception) {
                updateNotification(OnlineStatus.error("Ошибка проверки: ${e.message}"))
            }
        }
    }
    
    /**
     * Проверяет онлайн статус через игровой API
     */
    private suspend fun checkOnlineStatus(): OnlineStatus {
        // Проверяем, аутентифицирован ли пользователь
        if (!cookieManager.isAuthenticated()) {
            return OnlineStatus.error("Не аутентифицирован")
        }
        
        try {
            // Запрашиваем главную страницу игры для поддержания сессии
            val response = httpClient.get("http://www.neverlands.ru/main.php")
            
            if (!response.isSuccessful) {
                return OnlineStatus.error("HTTP ${response.code}")
            }
            
            val content = response.body?.string() ?: ""
            
            // Анализируем содержимое страницы
            return analyzeGamePage(content)
            
        } catch (e: Exception) {
            return OnlineStatus.error("Сетевая ошибка: ${e.message}")
        }
    }
    
    /**
     * Анализирует игровую страницу для определения статуса
     */
    private fun analyzeGamePage(content: String): OnlineStatus {
        return when {
            // Проверяем, есть ли в содержимом признаки боя
            content.contains("идет бой", ignoreCase = true) ||
            content.contains("атака", ignoreCase = true) ||
            content.contains("защита", ignoreCase = true) -> {
                OnlineStatus.online(isInBattle = true, "В бою")
            }
            
            // Проверяем признаки нормального состояния
            content.contains("Персонаж", ignoreCase = true) ||
            content.contains("Инвентарь", ignoreCase = true) -> {
                OnlineStatus.online(isInBattle = false, "Онлайн")
            }
            
            // Проверяем признаки необходимости повторного входа
            content.contains("Вход", ignoreCase = true) ||
            content.contains("Авторизация", ignoreCase = true) -> {
                OnlineStatus.error("Требуется повторный вход")
            }
            
            else -> {
                OnlineStatus.error("Неизвестное состояние")
            }
        }
    }
    
    /**
     * Создает канал уведомлений
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Уведомления о статусе подключения к игре"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Создает уведомление
     */
    private fun createNotification(statusText: String): Notification {
        val stopIntent = Intent(this, OnlineStatusService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ABClient - Онлайн статус")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_notification) // TODO: добавить иконку
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setShowWhen(false)
            .addAction(
                R.drawable.ic_stop, // TODO: добавить иконку
                "Остановить",
                stopPendingIntent
            )
            .build()
    }
    
    /**
     * Обновляет уведомление
     */
    private fun updateNotification(status: OnlineStatus) {
        val notification = createNotification(status.message)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}

/**
 * Статус онлайн подключения
 */
data class OnlineStatus(
    val isOnline: Boolean,
    val isInBattle: Boolean,
    val hasError: Boolean,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun online(isInBattle: Boolean = false, message: String = "Онлайн"): OnlineStatus {
            return OnlineStatus(
                isOnline = true,
                isInBattle = isInBattle,
                hasError = false,
                message = message
            )
        }
        
        fun error(message: String): OnlineStatus {
            return OnlineStatus(
                isOnline = false,
                isInBattle = false,
                hasError = true,
                message = message
            )
        }
    }
}