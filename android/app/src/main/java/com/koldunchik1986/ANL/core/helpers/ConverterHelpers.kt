package com.koldunchik1986.ANL.core.helpers

import android.util.Log
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

/**
 * Порт HelperConverters из Windows клиента ABClient
 * Аналог ABClient\MyHelpers\HelperConverters.cs
 */
object ConverterHelpers {
    
    private const val TAG = "ConverterHelpers"
    private const val CHARSET_WINDOWS_1251 = "windows-1251"
    
    // Константы из Resources (аналог Resources.cs)
    private const val ADDRESS_PINFO = "http://www.neverlands.ru/pinfo.php?name="
    private const val ADDRESS_PNAME = "http://www.neverlands.ru/pname.php?name="
    
    /**
     * Преобразование времени в строку с текущим моментом
     * Аналог TimeIntervalToNow() из Windows клиента
     */
    fun timeIntervalToNow(tick: Long): String {
        val dt = Date(tick)
        val now = Date()
        val diffMs = now.time - dt.time
        
        val days = (diffMs / (1000 * 60 * 60 * 24)).toInt()
        val hours = ((diffMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)).toInt()
        val minutes = ((diffMs % (1000 * 60 * 60)) / (1000 * 60)).toInt()
        
        val sb = StringBuilder()
        if (days > 0) {
            sb.append("${days}д ")
        }
        if (hours > 0) {
            sb.append("${hours}ч ")
        }
        sb.append("${minutes}мин")
        
        return sb.toString()
    }
    
    /**
     * Преобразование TimeSpan в строку
     * Аналог TimeSpanToString() из Windows клиента
     */
    fun timeSpanToString(totalMilliseconds: Long): String {
        val totalSeconds = totalMilliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return when {
            hours >= 1 -> String.format("(%d:%02d:%02d)", hours, minutes, seconds)
            minutes >= 1 -> String.format("(%d:%02d)", minutes, seconds)
            else -> String.format("(0:%02d)", seconds)
        }
    }
    
    /**
     * Декодирование ника из URL
     * Аналог NickDecode() из Windows клиента
     */
    fun nickDecode(nick: String?): String? {
        if (nick == null) return null
        
        return try {
            val s = nick.replace('+', ' ')
            val decodedUrl = URLDecoder.decode(s, CHARSET_WINDOWS_1251)
            val sb = StringBuilder(decodedUrl)
            sb.replace("|".toRegex(), " ")
            sb.replace("%20".toRegex(), " ")
            sb.replace("%2B".toRegex(), "+")
            sb.replace("%23".toRegex(), "#")
            sb.replace("%3D".toRegex(), "=")
            sb.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding nick: $nick", e)
            nick
        }
    }
    
    /**
     * Кодирование ника для URL
     * Аналог NickEncode() из Windows клиента
     */
    fun nickEncode(nick: String?): String? {
        if (nick == null) return null
        
        return try {
            val s1 = nick.replace('+', '|')
            val s2 = URLEncoder.encode(s1, CHARSET_WINDOWS_1251)
            val s3 = s2.replace("+", "%20")
            val s4 = s3.replace("%7C", "%2B")
            s4
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding nick: $nick", e)
            nick
        }
    }
    
    /**
     * Преобразование ника для обработки
     * Аналог NickToProc() из Windows клиента
     */
    fun nickToProc(url: String): String {
        val sb = StringBuilder(url)
        sb.replace(" ".toRegex(), "%20")
        sb.replace("\\+".toRegex(), "%2B")
        sb.replace("#".toRegex(), "%23")
        sb.replace("=".toRegex(), "%3D")
        return sb.toString()
    }
    
    /**
     * Преобразование адреса для обработки
     * Аналог AddressToProc() из Windows клиента
     */
    fun addressToProc(address: String): String {
        return when {
            address.startsWith(ADDRESS_PINFO, ignoreCase = true) -> {
                ADDRESS_PINFO + nickToProc(address.substring(ADDRESS_PINFO.length))
            }
            address.startsWith(ADDRESS_PNAME, ignoreCase = true) -> {
                ADDRESS_PNAME + nickToProc(address.substring(ADDRESS_PNAME.length))
            }
            else -> address
        }
    }
    
    /**
     * Кодирование адреса
     * Аналог AddressEncode() из Windows клиента
     */
    fun addressEncode(address: String): String {
        return try {
            URLEncoder.encode(address, CHARSET_WINDOWS_1251)
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding address: $address", e)
            address
        }
    }
    
    /**
     * Преобразование размера в строку с единицами измерения
     * Аналог LongToString() из Windows клиента
     */
    fun longToString(size: Long): String {
        var result = "$size байт"
        if (size >= 1024) {
            val kSize = size.toDouble() / 1024
            result = String.format("%.1f Кбайт", kSize)
            if (kSize >= 1024) {
                val mSize = kSize / 1024
                result = String.format("%.2f Мбайт", mSize)
            }
        }
        return result
    }
    
    /**
     * Попытка парсинга шестнадцатеричного числа
     * Аналог TryHexParse() из Windows клиента
     */
    fun tryHexParse(input: String): Int? {
        return try {
            input.toInt(16)
        } catch (e: NumberFormatException) {
            null
        }
    }
    
    /**
     * Попытка парсинга целого числа
     * Аналог TryIntParse() из Windows клиента
     */
    fun tryIntParse(input: String): Int? {
        return try {
            input.toInt()
        } catch (e: NumberFormatException) {
            null
        }
    }
    
    /**
     * Форматирование даты в строку
     */
    fun formatDate(date: Date): String {
        val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return formatter.format(date)
    }
    
    /**
     * Получение времени из строки
     */