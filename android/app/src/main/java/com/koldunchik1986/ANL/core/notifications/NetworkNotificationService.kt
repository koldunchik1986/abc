package com.koldunchik1986.ANL.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.koldunchik1986.ANL.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Сервис для показа уведомлений о сетевой активности
 * Показывает всплывающие сообщения и уведомления
 */
@Singleton
class NetworkNotificationService @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val CHANNEL_ID = "network_debug_channel"
        private const val CHANNEL_NAME = "Отладка сети"
        private const val NOTIFICATION_ID = 1001
    }
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Показать Toast сообщение о запросе
     */
    fun showRequestToast(method: String, url: String) {
        coroutineScope.launch {
            val shortUrl = url.substringAfterLast("/").take(30)
            Toast.makeText(
                context,
                "🔄 $method $shortUrl",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    /**
     * Показать Toast сообщение об ответе
     */
    fun showResponseToast(url: String, responseCode: Int, duration: Long) {
        coroutineScope.launch {
            val shortUrl = url.substringAfterLast("/").take(30)
            val status = when {
                responseCode in 200..299 -> "✅"
                responseCode in 400..499 -> "❌"
                responseCode in 500..599 -> "💥"
                else -> "❓"
            }
            Toast.makeText(
                context,
                "$status $responseCode $shortUrl (${duration}ms)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    /**
     * Показать уведомление об этапе авторизации
     */
    fun showAuthStepNotification(step: String) {
        coroutineScope.launch {
            Toast.makeText(
                context,
                "🔐 $step",
                Toast.LENGTH_LONG
            ).show()
            
            showPersistentNotification("Авторизация", step)
        }
    }
    
    /**
     * Показать уведомление об ошибке
     */
    fun showErrorNotification(error: String) {
        coroutineScope.launch {
            Toast.makeText(
                context,
                "❌ $error",
                Toast.LENGTH_LONG
            ).show()
            
            showPersistentNotification("Ошибка авторизации", error)
        }
    }
    
    /**
     * Показать информационное уведомление
     */
    fun showInfoNotification(message: String) {
        coroutineScope.launch {
            Toast.makeText(
                context,
                "ℹ️ $message",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    /**
     * Показать постоянное уведомление
     */
    private fun showPersistentNotification(title: String, content: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * Скрыть все уведомления
     */
    fun clearNotifications() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
    
    /**
     * Создание канала уведомлений для Android 8+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Уведомления о сетевой активности и отладке"
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}