package ru.neverlands.abclient.core.network.ssl

import java.security.cert.X509Certificate
import javax.net.ssl.*
import java.security.SecureRandom

/**
 * Класс для обхода SSL сертификатов
 * ВНИМАНИЕ: Используется только для совместимости с игровым сервером
 * В production приложениях следует использовать правильную валидацию сертификатов
 */
object TrustAllCertificates {
    
    /**
     * Устанавливает политику принятия всех SSL сертификатов
     * Необходимо для работы с neverlands.ru в случае проблем с сертификатами
     */
    fun install() {
        try {
            // Создаем TrustManager, который принимает все сертификаты
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                    // Принимаем все клиентские сертификаты
                }
                
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                    // Принимаем все серверные сертификаты
                }
                
                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }
            })
            
            // Устанавливаем SSLContext с нашим TrustManager
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