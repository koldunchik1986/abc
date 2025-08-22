package ru.neverlands.abclient.core.helpers

import android.util.Log

/**
 * Портирование HelperStrings из Windows версии ABClient
 * Полный аналог ABClient\MyHelpers\HelperStrings.cs
 */
object StringHelpers {
    
    private const val TAG = "StringHelpers"
    
    /**
     * Заменяет содержимое между двумя строками
     * Аналог Replace() из Windows версии
     */
    fun replace(html: String, s1: String, s2: String, newStr: String): String {
        val p1 = html.indexOf(s1, ignoreCase = true)
        if (p1 == -1) {
            return html
        }
        
        val p2 = html.indexOf(s2, p1 + s1.length, ignoreCase = true)
        return if (p2 == -1) {
            html
        } else {
            html.substring(0, p1 + s1.length) +
            newStr +
            html.substring(p2)
        }
    }
    
    /**
     * Извлекает подстроку между двумя маркерами
     * Аналог SubString() из Windows версии
     */
    fun subString(html: String, s1: String, s2: String): String? {
        val p1 = html.indexOf(s1, ignoreCase = true)
        if (p1 == -1) {
            return null
        }
        
        val p2 = html.indexOf(s2, p1 + s1.length, ignoreCase = true)
        return if (p2 == -1) {
            null
        } else {
            html.substring(p1 + s1.length, p2)
        }
    }
    
    /**
     * Парсит аргументы JavaScript функций
     * Аналог ParseArguments() из Windows версии
     */
    fun parseArguments(str: String): Array<String> {
        val list = mutableListOf<String>()
        var pos = 0
        
        while (pos < str.length) {
            val pa = pos
            if (str[pa] == '\'') {
                val pb = str.indexOf('\'', pa + 1)
                if (pb == -1) break
                
                val quotedArg = str.substring(pa + 1, pb)
                list.add(quotedArg)
                pos = pb + 1
                
                if (pos < str.length) {
                    if (str[pos] != ',') break
                    pos++
                }
            } else {
                val pb = str.indexOf(',', pa + 1).let { 
                    if (it == -1) str.length else it 
                }
                
                val nonquotedArg = str.substring(pa, pb)
                list.add(nonquotedArg)
                pos = pb + 1
            }
        }
        
        return list.toTypedArray()
    }
    
    /**
     * Парсит информацию о пользователе из JavaScript
     * Аналог ParsingUserinfo() из Windows версии
     */
    fun parseUserInfo(posu: String): Array<String> {
        val list = mutableListOf<String>()
        var pos = 0
        
        while (true) {
            // Ищем первый '
            val p1 = posu.indexOf('\'', pos)
            if (p1 == -1 || p1 + 1 == posu.length) break
            
            // Ищем второй '
            val p2 = posu.indexOf('\'', p1 + 1)
            if (p2 == -1) break
            
            list.add(posu.substring(p1 + 1, p2))
            
            // Ищем ,
            if (p2 + 1 == posu.length) break
            val p3 = posu.indexOf(',', p2 + 1)
            if (p3 == -1 || p3 + 1 == posu.length) break
            
            pos = p3 + 1
        }
        
        return list.toTypedArray()
    }
    
    /**
     * Создает случайный массив из строк
     * Аналог RandomArray() из Windows версии
     */
    fun randomArray(source: String): Array<String>? {
        val lines = source.split("\n").filter { line ->
            line.isNotEmpty() && !line.startsWith(";")
        }
        
        if (lines.isEmpty()) return null
        
        val shuffled = lines.shuffled()
        return shuffled.toTypedArray()
    }
    
    /**
     * Парсит JavaScript строку с массивами
     * Аналог ParseJsString() из Windows версии
     */
    fun parseJsString(str: String): List<List<String>>? {
        if (str.isEmpty() || str.length < 2) return null
        
        val result = mutableListOf<List<String>>()
        var p1 = 0
        
        while (p1 < str.length) {
            val p2 = when {
                str[p1] != '[' -> {
                    val commaIndex = str.indexOf(',', p1 + 1)
                    if (commaIndex == -1) str.length else commaIndex
                }
                else -> {
                    val bracketIndex = str.indexOf(']', p1 + 1)
                    if (bracketIndex == -1) str.length else bracketIndex + 1
                }
            }
            
            val s = str.substring(p1, p2)
            val arg = mutableListOf<String>()
            
            if (s.isNotEmpty()) {
                if (s[0] != '[') {
                    val cleaned = s.trim(' ', '"', '\'')
                    arg.add(cleaned)
                } else {
                    val cleaned = s.trim(' ', '[', ']')
                    val parts = cleaned.split(',')
                    parts.forEach { part ->
                        val cleanedPart = part.trim(' ', '"', '\'')
                        arg.add(cleanedPart)
                    }
                }
            }
            
            result.add(arg)
            p1 = p2 + 1
        }
        
        return result
    }
    
    /**
     * Безопасное извлечение подстроки с логированием
     */
    fun safeSubString(html: String, s1: String, s2: String, logTag: String = TAG): String? {
        return try {
            subString(html, s1, s2)
        } catch (e: Exception) {
            Log.e(logTag, "Error extracting substring between '$s1' and '$s2'", e)
            null
        }
    }
    
    /**
     * Проверяет, содержится ли строка в HTML (игнорируя регистр)
     */
    fun containsIgnoreCase(html: String, search: String): Boolean {
        return html.indexOf(search, ignoreCase = true) != -1
    }
    
    /**
     * Извлекает все вхождения между маркерами
     */
    fun extractAll(html: String, s1: String, s2: String): List<String> {
        val results = mutableListOf<String>()
        var startIndex = 0
        
        while (true) {
            val p1 = html.indexOf(s1, startIndex, ignoreCase = true)
            if (p1 == -1) break
            
            val p2 = html.indexOf(s2, p1 + s1.length, ignoreCase = true)
            if (p2 == -1) break
            
            val extracted = html.substring(p1 + s1.length, p2)
            results.add(extracted)
            startIndex = p2 + s2.length
        }
        
        return results
    }
}