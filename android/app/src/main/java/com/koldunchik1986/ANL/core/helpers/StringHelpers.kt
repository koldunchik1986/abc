package com.koldunchik1986.ANL.core.helpers

import android.util.Log

/**
 * Порт HelperStrings из Windows клиента ABClient
 * Аналог ABClient\MyHelpers\HelperStrings.cs
 */
object StringHelpers {
    
    private const val TAG = "StringHelpers"
    
    /**
     * Замена подстроки между двумя строками
     * Аналог Replace() из Windows клиента
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
     * Извлечение подстроки между двумя строками
     * Аналог SubString() из Windows клиента
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
     * Парсинг аргументов JavaScript функции
     * Аналог ParseArguments() из Windows клиента
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
     * Парсинг userinfo из JavaScript
     * Аналог ParsingUserinfo() из Windows клиента
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
     * Случайный массив из строки
     * Аналог RandomArray() из Windows клиента
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
     * Парсинг JavaScript строки в массив
     * Аналог ParseJsString() из Windows клиента
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
     * Безопасное извлечение подстроки между двумя строками
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
     * Проверка, содержится ли подстрока в HTML (без учета регистра)
     */
    fun containsIgnoreCase(html: String, search: String): Boolean {
        return html.indexOf(search, ignoreCase = true) != -1
    }
    
    /**
     * Извлечение всех подстрок между двумя строками
     */