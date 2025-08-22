package ru.neverlands.abclient.core.helpers

import android.util.Log
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

/**
 * Портирование HelperConverters из Windows версии ABClient
 * Полный аналог ABClient\MyHelpers\HelperConverters.cs
 */
object ConverterHelpers {
    
    private const val TAG = "ConverterHelpers"
    private const val CHARSET_WINDOWS_1251 = "windows-1251"
    
    // Адреса из Resources (аналог Resources.cs)
    private const val ADDRESS_PINFO = "http://www.neverlands.ru/pinfo.php?name="
    private const val ADDRESS_PNAME = "http://www.neverlands.ru/pname.php?name="
    
    /**
     * Преобразует временной интервал с момента создания
     * Аналог TimeIntervalToNow() из Windows версии
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
     * Преобразует TimeSpan в строку
     * Аналог TimeSpanToString() из Windows версии
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
     * Декодирует ник из URL
     * Аналог NickDecode() из Windows версии
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
     * Кодирует ник для URL
     * Аналог NickEncode() из Windows версии
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
     * Преобразует ник для обработки процессов
     * Аналог NickToProc() из Windows версии
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
     * Преобразует адрес для обработки
     * Аналог AddressToProc() из Windows версии
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
     * Кодирует адрес полностью
     * Аналог AddressEncode() из Windows версии
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
     * Преобразует размер в байтах в читаемый формат
     * Аналог LongToString() из Windows версии
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
     * Парсит шестнадцатеричное число
     * Аналог TryHexParse() из Windows версии
     */
    fun tryHexParse(input: String): Int? {
        return try {
            input.toInt(16)
        } catch (e: NumberFormatException) {
            null
        }
    }
    
    /**
     * Безопасный парсинг целого числа
     * Аналог TryIntParse() из Windows версии
     */
    fun tryIntParse(input: String): Int? {
        return try {
            input.toInt()
        } catch (e: NumberFormatException) {
            null
        }
    }
    
    /**
     * Форматирует дату в стандартный формат
     */
    fun formatDate(date: Date): String {
        val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return formatter.format(date)
    }
    
    /**
     * Парсит дату из строки
     */
    fun parseDate(dateString: String): Date? {
        return try {
            val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            formatter.parse(dateString)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing date: $dateString", e)
            null
        }
    }
    
    /**
     * Преобразует массив байт в HEX представление
     */
    fun byteArrayToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Создает безопасный URL из произвольной строки
     */
    fun createSafeUrl(input: String): String {
        return try {
            input.replace("[^a-zA-Z0-9\\-_.]".toRegex(), "_")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating safe URL from: $input", e)
            "safe_url"
        }
    }
    
    /**
     * Вычисляет человекочитаемое время
     */
    fun humanReadableTime(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            days > 0 -> "${days}д ${hours % 24}ч"
            hours > 0 -> "${hours}ч ${minutes % 60}мин"
            minutes > 0 -> "${minutes}мин ${seconds % 60}сек"
            else -> "${seconds}сек"
        }
    }
    
    /**
     * Проверяет валидность email
     */
    fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"))
    }
    
    /**
     * Сокращает длинную строку с многоточием
     */
    fun truncateString(text: String, maxLength: Int): String {
        return if (text.length <= maxLength) {
            text
        } else {
            text.substring(0, maxLength - 3) + "..."
        }
    }
}