package com.koldunchik1986.ANL.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import com.koldunchik1986.ANL.R
import com.koldunchik1986.ANL.core.network.GameHttpClient
import com.koldunchik1986.ANL.data.preferences.UserPreferencesManager
import com.koldunchik1986.ANL.core.network.cookie.GameCookieManager
import javax.inject.Inject

/**
 * Сервис для отслеживания онлайн статуса персонажа в игре
 * Обеспечивает непрерывную проверку статуса персонажа в игре
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
        
        // Параметры проверки статуса
        private const val CHECK_INTERVAL_NORMAL = 30_000L // 30 секунд в обычном режиме
        private const val CHECK_INTERVAL_BATTLE = 10_000L // 10 секунд в бою
        private const val CHECK_INTERVAL_ERROR = 60_000L // 1 минута при ошибке
        
        const val ACTION_START_SERVICE = "START_SERVICE"
        const val ACTION_STOP_SERVICE = "STOP_SERVICE"
        const val ACTION_CHECK_STATUS = "CHECK_STATUS"
        
        /**
         * Запуск сервиса
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
         * Остановка сервиса
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
        
        return START_STICKY // Перезапускать сервис при завершении
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        stopOnlineStatusMonitoring()
    }
    
    /**
     * Запуск мониторинга онлайн статуса
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
                    
                    // Если много ошибок подряд, увеличиваем интервал
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
     * Остановка мониторинга
     */
    private fun stopOnlineStatusMonitoring() {
        isRunning = false
        serviceJob?.cancel()
        stopForeground(true)
        stopSelf()
    }
    
    /**
     * Выполнение одиночной проверки статуса
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
     * Проверка онлайн статуса через API игры
     */
    private suspend fun checkOnlineStatus(): OnlineStatus {
        // Проверяем, авторизованы ли мы
        if (!cookieManager.isAuthenticated()) {
            return OnlineStatus.error("Не авторизован")
        }
        
        try {
            // Отправляем запрос на главную страницу для проверки статуса
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
     * Анализ содержимого страницы для определения статуса
     */
    private fun analyzeGamePage(content: String): OnlineStatus {
        return when {
            // Проверяем, находится ли персонаж в бою
            content.contains("Бой", ignoreCase = true) ||
            content.contains("Атака", ignoreCase = true) ||
            content.contains("Защита", ignoreCase = true) -> {
                OnlineStatus.online(isInBattle = true, "В бою")
            }
            
            // Проверяем обычный онлайн статус
            content.contains("Персонаж", ignoreCase = true) ||
            content.contains("Инвентарь", ignoreCase = true) -> {
                OnlineStatus.online(isInBattle = false, "Онлайн")
            }
            
            // Проверяем ошибки авторизации
            content.contains("Вход", ignoreCase = true) ||
            content.contains("Авторизация", ignoreCase = true) -> {
                OnlineStatus.error("Потеря сессии")
            }
            
            else -> {
                OnlineStatus.error("Неизвестный статус")
            }
        }
    }
    
    /**
     * Создание канала уведомлений
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Отслеживание онлайн статуса персонажа в игре"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Создание уведомления
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
            .setSmallIcon(R.drawable.ic_notification) // TODO: Добавить иконку
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setShowWhen(false)
            .addAction(
                R.drawable.ic_stop, // TODO: Добавить иконку
                "Остановить",
                stopPendingIntent
            )
            .build()
    }
    
    /**
     * Обновление уведомления
     */
    private fun updateNotification(status: OnlineStatus) {
        val notification = createNotification(status.message)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}

/**
 * Модель статуса онлайн
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