package com.koldunchik1986.ANL.core.auth

import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Вспомогательный класс для авторизации
 * Содержит логику аналогичную IndexCgi.cs и GamePhp.cs из Windows версии
 */
@Singleton
class AuthenticationHelper @Inject constructor() {
    
    companion object {
        // Константы для авторизации (соответствие эталону)
        const val GAME_HOST = "www.neverlands.ru"
        const val BASE_URL = "http://$GAME_HOST/"
        const val GAME_URL = "http://$GAME_HOST"
        const val INDEX_CGI_URL = "$BASE_URL"
        const val GAME_PHP_URL = "$GAME_URL/game.php"
        const val MAIN_PHP_URL = "$GAME_URL/main.php" // Правильный URL для главной страницы игры
        
        // Паттерны для поиска ошибок (аналог IndexCgi.cs)
        const val ERROR_PATTERN = "show_warn\\s*\\(\\s*[\"']([^\"']+)[\"']\\s*\\)"
        const val COOKIE_ERROR_PATTERN = "Cookie\\.\\.\\."
        const val CONNECTION_ERROR_PATTERN = "Connection error"
        
        // Паттерны для Flash игры (аналог GamePhp.cs)
        const val FLASH_PLAYER_ID_PATTERN = "flashvars=\"plid=(\\d+)\""
        
        // Кодировка для игрового сервера (аналог Russian.Codepage)
        const val GAME_ENCODING = "windows-1251"
        
        // User-Agent как в эталоне (критически важно!)
        const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }
    
    /**
     * Создает HTML для автоматической отправки формы входа
     * Аналог логики из IndexCgi.cs
     */
    fun createAutoLoginHtml(userNick: String, userPassword: String): String {
        // URL encoding как в Windows версии HttpUtility.HtmlEncode
        val encodedNick = URLEncoder.encode(userNick, GAME_ENCODING)
        val encodedPassword = URLEncoder.encode(userPassword, GAME_ENCODING)
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Ввод имени и пароля...</title>
                <meta charset="UTF-8">
            </head>
            <body>
                <div style="text-align: center; margin-top: 50px; font-family: Arial;">
                    <h3>Вход в игру...</h3>
                    <p>Выполняется автоматическая авторизация...</p>
                </div>
                <form action="./game.php" method="POST" name="ff">
                    <input name="player_nick" type="hidden" value="$encodedNick">
                    <input name="player_password" type="hidden" value="$encodedPassword">
                </form>
                <script language="JavaScript">
                    console.log('Auto-submitting login form...');
                    document.ff.submit();
                </script>
            </body>
            </html>
        """.trimIndent()
    }
    
    /**
     * Создает HTML для отправки Flash пароля
     * Аналог логики из GamePhp.cs
     */
    fun createFlashPasswordHtml(flashPassword: String, playerId: String): String {
        // URL encoding как в Windows версии
        val encodedFlashPassword = URLEncoder.encode(flashPassword, GAME_ENCODING)
        val encodedPlayerId = URLEncoder.encode(playerId, GAME_ENCODING)
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Ввод флеш-пароля...</title>
                <meta charset="UTF-8">
            </head>
            <body>
                <div style="text-align: center; margin-top: 50px; font-family: Arial;">
                    <h3>Отправка пароля...</h3>
                    <p>Выполняется отправка пароля для Flash игры...</p>
                </div>
                <form action="./game.php" method="POST" name="ff">
                    <input name="flcheck" type="hidden" value="$encodedFlashPassword">
                    <input name="nid" type="hidden" value="$encodedPlayerId">
                </form>
                <script language="JavaScript">
                    console.log('Auto-submitting Flash password form...');
                    document.ff.submit();
                </script>
            </body>
            </html>
        """.trimIndent()
    }
    
    /**
     * Создает JavaScript для проверки формы авторизации
     * Аналог логики из IndexCgi.cs
     */
    fun createAuthFormCheckScript(): String {
        return """
            (function() {
                console.log('Checking for auth form...');
                
                // Ищем форму авторизации (аналог IndexCgi.cs)
                var authForm = document.querySelector('form[method="post"][id="auth_form"][action*="game.php"], form[action*="game.php"]');
                if (!authForm) {
                    console.log('Auth form not found');
                    return false;
                }
                
                console.log('Auth form found, checking for errors...');
                
                // Проверяем, нет ли ошибок на странице (аналог IndexCgi.cs)
                var pageText = document.documentElement.outerHTML;
                var errorPattern = /$ERROR_PATTERN/;
                var errorMatch = pageText.match(errorPattern);
                
                if (errorMatch) {
                    console.log('Login error detected:', errorMatch[1]);
                    return 'ERROR:' + errorMatch[1];
                }
                
                // Дополнительная проверка на общие ошибки (Cookie, соединение и т.д.)
                if (pageText.indexOf('$COOKIE_ERROR_PATTERN') !== -1) {
                    return 'ERROR:Проблема с cookies, перезагрузка страницы...';
                }
                
                if (pageText.indexOf('$CONNECTION_ERROR_PATTERN') !== -1) {
                    return 'ERROR:Ошибка соединения с сервером';
                }
                
                return true;
            })();
        """.trimIndent()
    }
    
    /**
     * Создает JavaScript для проверки Flash Player
     * Аналог логики из GamePhp.cs
     */
    fun createFlashCheckScript(): String {
        return """
            (function() {
                console.log('Checking for Flash content...');
                
                var pageText = document.documentElement.outerHTML;
                
                // Проверяем, требуется ли Flash пароль (аналог GamePhp.cs)
                var flashPattern = /$FLASH_PLAYER_ID_PATTERN/;
                var flashMatch = pageText.match(flashPattern);
                
                if (flashMatch) {
                    console.log('Flash password required for player ID:', flashMatch[1]);
                    return 'FLASH_REQUIRED:' + flashMatch[1];
                }
                
                // Проверяем, есть ли содержимое игры
                if (pageText.indexOf('<canvas') !== -1 || 
                    pageText.indexOf('game') !== -1 || 
                    pageText.length > 5000) {
                    console.log('Game content detected');
                    return 'GAME_CONTENT';
                }
                
                console.log('No Flash or game content found');
                return 'NO_CONTENT';
            })();
        """.trimIndent()
    }
    
    /**
     * Проверяет, является ли URL игровой страницей
     */
    fun isGameUrl(url: String): Boolean {
        return url.contains("neverlands.ru", ignoreCase = true)
    }
    
    /**
     * Проверяет, является ли URL страницей входа
     */
    fun isLoginUrl(url: String): Boolean {
        return url.contains("neverlands.ru", ignoreCase = true) && 
               (url.endsWith("/") || url.contains("index.cgi"))
    }
    
    /**
     * Извлечение URL капчи из HTML (аналог эталона)
     */
    fun extractCaptchaUrl(html: String): String? {
        val captchaPattern = """<img[^>]*src="([^"]*captcha[^"]*)""""".toRegex()
        val captchaMatch = captchaPattern.find(html)
        
        return if (captchaMatch != null) {
            BASE_URL + captchaMatch.groupValues[1].replace("&amp;", "&")
        } else {
            null
        }
    }
    
    /**
     * Проверка ответа на вход (аналог эталона)
     */
    fun checkLoginResponse(html: String): LoginResult {
        return when {
            html.contains("Неверный логин или пароль", ignoreCase = true) -> LoginResult.INVALID_CREDENTIALS
            html.contains("Введите код", ignoreCase = true) -> LoginResult.CAPTCHA_REQUIRED
            html.contains("error", ignoreCase = true) -> LoginResult.ERROR
            html.contains("<canvas", ignoreCase = true) || 
            html.contains("game", ignoreCase = true) || 
            html.length > 5000 -> LoginResult.SUCCESS
            else -> LoginResult.ERROR
        }
    }
}