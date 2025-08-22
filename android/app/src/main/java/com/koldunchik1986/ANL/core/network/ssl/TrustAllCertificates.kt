package com.koldunchik1986.ANL.core.network.ssl

import java.security.cert.X509Certificate
import javax.net.ssl.*
import java.security.SecureRandom

/**
 * Класс для отключения SSL проверок
 * Внимание: Отключение проверок создает уязвимость безопасности
 * В production приложениях следует использовать правильные сертификаты
 */
object TrustAllCertificates {
    
    /**
     * Установка доверительных сертификатов SSL
     * Необходимо для работы с neverlands.ru и другими серверами с самоподписанными сертификатами
     */
    fun install() {
        try {
            // Создаем TrustManager, который доверяет всем сертификатам
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                    // Пропускаем все проверки клиентских сертификатов
                }
                
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                    // Пропускаем все проверки серверных сертификатов
                }
                
                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }
            })
            
            // Инициализируем SSLContext с нашим TrustManager
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())
            
            // Устанавливаем как default
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
            
            // Отключаем проверку hostname
            HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}