package com.koldunchik1986.ANL.core.filter

import android.util.Log
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import com.koldunchik1986.ANL.core.helpers.StringHelpers
import com.koldunchik1986.ANL.data.model.UserProfile
import com.koldunchik1986.ANL.data.preferences.UserPreferencesManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HTTP фильтр для обработки контента с neverlands.ru
 * Аналог ABClient\PostFilter\Filter.cs из Windows клиента
 */
@Singleton
class HttpFilter @Inject constructor(
    private val userPreferencesManager: UserPreferencesManager,
    private val jsFilters: JavaScriptFilters,
    private val phpFilters: PhpFilters,
    private val htmlFilters: HtmlFilters
) : Interceptor {

    companion object {
        private const val TAG = "HttpFilter"
        private const val CHARSET_WINDOWS_1251 = "windows-1251"
        private val MEDIA_TYPE_HTML = "text/html; charset=windows-1251".toMediaType()
        private val MEDIA_TYPE_JS = "text/javascript; charset=windows-1251".toMediaType()
    }
    
    /**
     * Получение текущего профиля пользователя
     */
    private fun getCurrentProfile(): UserProfile? {
        return runBlocking {
            userPreferencesManager.getCurrentProfile()
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        
        val url = request.url.toString()
        
        // Обрабатываем только neverlands.ru запросы
        if (!url.contains("neverlands.ru")) {
            return response
        }
        
        return try {
            val originalBody = response.body
            if (originalBody == null) {
                return response
            }
            
            val originalBytes = originalBody.bytes()
            val processedBytes = processResponse(url, originalBytes)
            
            if (processedBytes != null && !processedBytes.contentEquals(originalBytes)) {
                Log.d(TAG, "Filtered response for: $url")
                
                val mediaType = when {
                    url.contains(".js") -> MEDIA_TYPE_JS
                    else -> originalBody.contentType() ?: MEDIA_TYPE_HTML
                }
                
                val newBody = processedBytes.toResponseBody(mediaType)
                response.newBuilder()
                    .body(newBody)
                    .build()
            } else {
                response.newBuilder()
                    .body(originalBytes.toResponseBody(originalBody.contentType()))
                    .build()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error filtering response for: $url", e)
            response
        }
    }

    /**
     * Предварительная обработка данных
     * Аналог PreProcess() из Windows клиента
     */
    private fun preProcess(url: String, data: ByteArray): ByteArray? {
        // Если нет данных для предобработки, то возвращаем исходные данные
        return data
    }

    /**
     * Обработка ответа сервера
     * Аналог Process() из Windows клиента
     */
    private fun processResponse(url: String, data: ByteArray): ByteArray? {
        if (url.isEmpty()) {
            return null
        }

        val html = try {
            String(data, charset(CHARSET_WINDOWS_1251))
        } catch (e: Exception) {
            String(data, Charsets.UTF_8)
        }

        // Обработка JavaScript файлов
        if (url.contains(".js")) {
            return processJavaScript(url, data, html)
        }

        // Обработка PHP страниц
        when {
            url.startsWith("http://www.neverlands.ru/index.cgi", ignoreCase = true) ||
            url == "http://www.neverlands.ru/" -> {
                return htmlFilters.processIndexCgi(data)
            }
            
            url.startsWith("http://www.neverlands.ru/pinfo.cgi", ignoreCase = true) -> {
                return htmlFilters.processPlayerInfo(data)
            }
            
            url.startsWith("http://www.neverlands.ru/pbots.cgi", ignoreCase = true) -> {
                return htmlFilters.processPlayerBots(data)
            }
            
            url.startsWith("http://forum.neverlands.ru/", ignoreCase = true) -> {
                return htmlFilters.processForum(data)
            }
            
            url.startsWith("http://www.neverlands.ru/game.php", ignoreCase = true) -> {
                return phpFilters.processGamePhp(data)
            }
            
            url.startsWith("http://www.neverlands.ru/main.php", ignoreCase = true) -> {
                return phpFilters.processMainPhp(url, data)
            }
            
            url.startsWith("http://www.neverlands.ru/ch/msg.php", ignoreCase = true) -> {
                return phpFilters.processChatMessage(data)
            }
            
            url.startsWith("http://www.neverlands.ru/ch/but.php", ignoreCase = true) -> {
                return phpFilters.processChatButton(data)
            }
            
            url.startsWith("http://www.neverlands.ru/gameplay/trade.php", ignoreCase = true) -> {
                val currentProfile = getCurrentProfile()
                return if (currentProfile?.torgActive == true) {
                    phpFilters.processTradePhp(data)
                } else {
                    data
                }
            }
            
            url.startsWith("http://www.neverlands.ru/gameplay/ajax/map_act_ajax.php", ignoreCase = true) -> {
                return phpFilters.processMapActAjax(data)
            }
            
            url.startsWith("http://www.neverlands.ru/gameplay/ajax/fish_ajax.php", ignoreCase = true) -> {
                return phpFilters.processFishAjax(data)
            }
            
            url.startsWith("http://www.neverlands.ru/gameplay/ajax/shop_ajax.php", ignoreCase = true) -> {
                return phpFilters.processShopAjax(data)
            }
            
            url.startsWith("http://www.neverlands.ru/gameplay/ajax/roulette_ajax.php", ignoreCase = true) -> {
                return phpFilters.processRouletteAjax(data)
            }
            
            url.startsWith("http://www.neverlands.ru/ch.php?lo=", ignoreCase = true) -> {
                return phpFilters.processChatRoom(data)
            }
            
            url.contains("/ch.php?0", ignoreCase = true) -> {
                return phpFilters.processChatZero(data)
            }
        }

        return data
    }

    /**
     * Обработка JavaScript файлов
     */
    private fun processJavaScript(url: String, data: ByteArray, html: String): ByteArray? {
        return when {
            url.contains("/js/hp.js") -> {
                jsFilters.processHpJs(data)
            }
            
            url.contains("/js/map.js") -> {
                jsFilters.processMapJs(data)
            }
            
            url.contains("/arena") -> {
                jsFilters.processArenaJs(data)
            }
            
            url.endsWith("/js/game.js", ignoreCase = true) -> {
                jsFilters.processGameJs(data)
            }
            
            url.contains("pinfo_v01.js") -> {
                jsFilters.processPinfoJs(data)
            }
            
            url.contains("/js/fight_v") -> {
                jsFilters.processFightJs(data)
            }
            
            url.contains("/js/building") -> {
                jsFilters.processBuildingJs(data)
            }
            
            url.endsWith("/js/hpmp.js", ignoreCase = true) -> {
                jsFilters.processHpmpJs(data)
            }
            
            url.endsWith("/ch/ch_msg_v01.js", ignoreCase = true) -> {
                jsFilters.processChMsgJs(data)
            }
            
            url.endsWith("/js/pv.js", ignoreCase = true) -> {
                jsFilters.processPvJs(data)
            }
            
            url.endsWith("/js/ch_list.js", ignoreCase = true) -> {
                jsFilters.processChListJs(data)
            }
            
            url.endsWith("/js/svitok.js", ignoreCase = true) -> {
                jsFilters.processSvitokJs(data)
            }
            
            url.endsWith("/js/slots.js", ignoreCase = true) -> {
                jsFilters.processSlotsJs(data)
            }
            
            url.contains("/js/logs", ignoreCase = true) -> {
                jsFilters.processLogsJs(data)
            }
            
            url.contains("/js/shop", ignoreCase = true) -> {
                jsFilters.processShopJs(data)
            }
            
            url.contains("/js/forum/forum_topic.js", ignoreCase = true) -> {
                jsFilters.processForumTopicJs(data)
            }
            
            else -> data
        }
    }

    /**
     * Формирование редиректа
     * Аналог BuildRedirect() из Windows клиента
     */
    private fun buildRedirect(description: String, link: String): String {
        return buildString {
            append(htmlFilters.getHtmlHead())
            append(description)
            append("""<script language="JavaScript">""")
            append("""  window.location = """")
            append(link)
            append("""";""")
            append("""</script></body></html>""")
        }
    }
}
