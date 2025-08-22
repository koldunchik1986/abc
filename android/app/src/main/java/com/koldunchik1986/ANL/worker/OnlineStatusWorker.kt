package com.koldunchik1986.ANL.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.koldunchik1986.ANL.core.network.GameHttpClient
import com.koldunchik1986.ANL.data.preferences.UserPreferencesManager
import com.koldunchik1986.ANL.core.network.cookie.GameCookieManager
import com.koldunchik1986.ANL.service.OnlineStatusService
import java.util.concurrent.TimeUnit

/**
 * Worker для периодических фоновых задач
 * Дополняет OnlineStatusService для случаев, когда служба не активна
 */
@HiltWorker
class OnlineStatusWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val httpClient: GameHttpClient,
    private val preferencesManager: UserPreferencesManager,
    private val cookieManager: GameCookieManager
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        const val WORK_NAME = "online_status_worker"
        const val WORK_TAG = "background_status_check"
        
        /**
         * Планирует периодическую проверку статуса
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(false)
                .setRequiresCharging(false)
                .build()
            
            val workRequest = PeriodicWorkRequestBuilder<OnlineStatusWorker>(
                15, TimeUnit.MINUTES, // Минимальный интервал для periodic work
                5, TimeUnit.MINUTES   // Flex interval
            )
                .setConstraints(constraints)
                .addTag(WORK_TAG)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
        }
        
        /**
         * Отменяет периодическую проверку
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context)
                .cancelUniqueWork(WORK_NAME)
        }
        
        /**
         * Запускает одноразовую проверку
         */
        fun runOnce(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<OnlineStatusWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .addTag(WORK_TAG)
                .build()
            
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
    
    override suspend fun doWork(): Result {
        return try {
            // Проверяем, нужно ли поддерживать онлайн статус
            val currentProfile = preferencesManager.getCurrentProfile()
            if (currentProfile == null || !cookieManager.isAuthenticated()) {
                return Result.success()
            }
            
            // Проверяем статус через API
            val isOnline = checkGameStatus()
            
            if (isOnline) {
                // Если онлайн, запускаем основную службу для более частой проверки
                OnlineStatusService.start(context)
                Result.success()
            } else {
                // Если не онлайн, пытаемся восстановить соединение
                val restored = attemptReconnection()
                if (restored) {
                    OnlineStatusService.start(context)
                    Result.success()
                } else {
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
    
    /**
     * Проверяет статус игры
     */
    private suspend fun checkGameStatus(): Boolean {
        return try {
            val response = httpClient.get("http://www.neverlands.ru/main.php")
            response.isSuccessful && !(response.body?.string()?.contains("Вход", ignoreCase = true) ?: false)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Пытается восстановить соединение
     */
    private suspend fun attemptReconnection(): Boolean {
        return try {
            // Пытаемся получить информацию о текущем пользователе через API
            val currentProfile = preferencesManager.getCurrentProfile() ?: return false
            val userInfo = httpClient.getUserInfo(currentProfile.userNick)
            
            userInfo != null
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Менеджер фоновых задач
 */
class BackgroundTaskManager @AssistedInject constructor(
    private val context: Context,
    private val preferencesManager: UserPreferencesManager
) {
    
    /**
     * Запускает все фоновые задачи
     */
    suspend fun startBackgroundTasks() {
        val currentProfile = preferencesManager.getCurrentProfile()
        if (currentProfile != null) {
            // Запускаем периодическую проверку через WorkManager
            OnlineStatusWorker.schedule(context)
            
            // Запускаем основную службу
            OnlineStatusService.start(context)
        }
    }
    
    /**
     * Останавливает все фоновые задачи
     */
    fun stopBackgroundTasks() {
        OnlineStatusWorker.cancel(context)
        OnlineStatusService.stop(context)
    }
    
    /**
     * Перезапускает фоновые задачи
     */
    suspend fun restartBackgroundTasks() {
        stopBackgroundTasks()
        startBackgroundTasks()
    }
}
