package com.koldunchik1986.ANL.ui.game

import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.koldunchik1986.ANL.data.model.UserProfile

/**
 * WebViewClient для работы с игровым браузером
 * Обрабатывает автоматическую авторизацию аналогично IndexCgi.cs из Windows клиента
 */
class GameWebViewClient(
    private val currentProfile: UserProfile?,
    private val onPageStarted: (String) -> Unit = {},
    private val onPageFinished: (String) -> Unit = {},
    private val onLoadingStateChanged: (Boolean) -> Unit = {},
    private val onAutoLoginStatus: (String) -> Unit = {}
) : WebViewClient() {
    
    companion object {
        private const val TAG = "GameWebViewClient"
        
        // URL patterns
        private const val LOGIN_URL_PATTERN = "neverlands.ru/"
        private const val INDEX_CGI_PATTERN = "index.cgi"
        private const val GAME_PHP_PATTERN = "game.php"
        
        // Form selectors
        private const val AUTH_FORM_SELECTOR = "form[id='auth_form'], form[action*='game.php']"
        private const val NICK_INPUT_SELECTOR = "input[name='player_nick']"
        private const val PASSWORD_INPUT_SELECTOR = "input[name='player_password']"
        
        // HTML5 game detection
        private const val CANVAS_GAME_PATTERN = "canvas"
        private const val HTML5_GAME_PATTERN = "game"
        
        // JavaScript injection delay
        private const val INJECTION_DELAY_MS = 500L
    }
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        url?.let { 
            Log.d(TAG, "Page started: $it")
            onPageStarted(it)
            onLoadingStateChanged(true)
        }
    }
    
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        url?.let { 
            Log.d(TAG, "Page finished: $it")
            onPageFinished(it)
            onLoadingStateChanged(false)
            
            // Обработка и анализ страницы
            handlePageLoad(view, it)
        }
    }
    
    /**
     * Обработка загрузки страницы
     */
    private fun handlePageLoad(webView: WebView?, url: String) {
        webView ?: return
        
        when {
            // Главная страница для index.cgi - обработка формы входа
            url.contains(LOGIN_URL_PATTERN) && 
            (url.endsWith("/") || url.contains(INDEX_CGI_PATTERN)) -> {
                handleLoginPage(webView, url)
            }
            
            // Страница game.php - обработка HTML5 игры внутри
            url.contains(GAME_PHP_PATTERN) -> {
                handleGamePage(webView, url)
            }
        }
    }
    
    /**
     * Обработка страницы входа (аналог IndexCgi.cs)
     */
    private fun handleLoginPage(webView: WebView, url: String) {
        Log.d(TAG, "Handling login page: $url")
        
        if (currentProfile?.isLoginDataComplete() != true) {
            Log.w(TAG, "Profile data incomplete, skipping auto-login")
            onAutoLoginStatus("Персонаж не выбран")
            return
        }
        
        // Проверяем, включен ли автологин для профиля (аналог ConfigSelector.cs)
        if (!currentProfile.userAutoLogon) {
            Log.i(TAG, "Auto-login disabled for this profile")
            onAutoLoginStatus("Автологин отключен")
            return
        }
        
        onAutoLoginStatus("Авторизация...")
        
        // Ждем немного для полной загрузки DOM
        coroutineScope.launch {
            delay(INJECTION_DELAY_MS)
            
            // Проверяем, есть ли форма авторизации на странице
            val checkFormScript = """
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
                    var errorPattern = /show_warn\s*\(\s*["']([^"']+)["']\s*\)/;
                    var errorMatch = pageText.match(errorPattern);
                    
                    if (errorMatch) {
                        console.log('Login error detected:', errorMatch[1]);
                        return 'ERROR:' + errorMatch[1];
                    }
                    
                    return true;
                })();
            """.trimIndent()
            
            webView.evaluateJavascript(checkFormScript) { result ->
                Log.d(TAG, "Form check result: $result")
                when {
                    result == "true" -> {
                        // Нашли форму входа - выполняем автоматическую авторизацию
                        performAutoLogin(webView)
                    }
                    result.startsWith("\"ERROR:") -> {
                        val error = result.substring(7, result.length - 1) // Убираем "ERROR: " и кавычки
                        onAutoLoginStatus("Ошибка входа: $error")
                        Log.w(TAG, "Login error: $error")
                    }
                    else -> {
                        onAutoLoginStatus("Форма входа не найдена")
                        Log.w(TAG, "Auth form not found")
                    }
                }
            }
        }
    }
    
    /**
     * Выполнение автоматического входа (аналог IndexCgi.cs)
     */
    private fun performAutoLogin(webView: WebView) {
        onAutoLoginStatus("Отправка данных входа...")
        
        // Генерируем HTML с автоматической отправкой формы, как это делается в IndexCgi.cs
        val autoLoginHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Авторизация...</title>
                <meta charset="UTF-8">
            </head>
            <body>
                <div style="text-align: center; margin-top: 50px; font-family: Arial;">
                    <h3>Вход в игру...</h3>
                    <p>Выполняется автоматическая авторизация...</p>
                </div>
                <form action="./game.php" method="POST" name="ff">
                    <input name="player_nick" type="hidden" value="${currentProfile?.userNick ?: ""}">
                    <input name="player_password" type="hidden" value="${currentProfile?.userPassword ?: ""}">
                </form>
                <script language="JavaScript">
                    console.log('Auto-submitting login form...');
                    document.ff.submit();
                </script>
            </body>
            </html>
        """.trimIndent()
        
        // Загружаем HTML с автоматической отправкой формы
        webView.loadDataWithBaseURL(
            "http://www.neverlands.ru/",
            autoLoginHtml,
            "text/html",
            "UTF-8",
            null
        )
        
        onAutoLoginStatus("Вход выполнен...")
        Log.i(TAG, "Auto-login form submitted successfully")
    }
    
    /**
     * Обработка страницы игры (возможно, без использования Flash игры)
     */
    private fun handleGamePage(webView: WebView, url: String) {
        Log.d(TAG, "Handling game page: $url")
        
        // Проверяем, нужен ли пароль для Flash игры
        onAutoLoginStatus("Проверка пароля для Flash игры...")
        coroutineScope.launch {
            delay(INJECTION_DELAY_MS)
            checkForFlashPasswordRequest(webView)
        }
    }
    
    /**
     * Проверка необходимости пароля для Flash игры (аналог GamePhp.cs)
     */
    private fun checkForFlashPasswordRequest(webView: WebView) {
        val checkFlashScript = """
            (function() {
                console.log('Checking for Flash password request...');
                
                // Ищем flashvars="plid= в HTML (аналог GamePhp.cs)
                var pageHtml = document.documentElement.outerHTML;
                var flashPattern = /flashvars="plid=([^"]+)"/;
                var match = pageHtml.match(flashPattern);
                
                if (match && match[1]) {
                    console.log('Flash password required, player ID:', match[1]);
                    return 'FLASH_REQUIRED:' + match[1];
                }
                
                // Проверяем наличие HTML5 игры на странице
                console.log('No Flash password required, checking game content...');
                return 'GAME_CONTENT';
            })();
        """.trimIndent()
        
        webView.evaluateJavascript(checkFlashScript) { result ->
            Log.d(TAG, "Flash check result: $result")
            when {
                result.startsWith("\"FLASH_REQUIRED:") -> {
                    val playerId = result.substring(16, result.length - 1) // Убираем "FLASH_REQUIRED: " и кавычки
                    handleFlashPasswordRequired(webView, playerId)
                }
                result == "\"GAME_CONTENT\"" -> {
                    handleHTML5GameContent(webView)
                }
                else -> {
                    onAutoLoginStatus("Неизвестная ошибка при проверке")
                }
            }
        }
    }
    
    /**
     * Обработка пароля для Flash игры (аналог GamePhp.cs)
     */
    private fun handleFlashPasswordRequired(webView: WebView, playerId: String) {
        Log.d(TAG, "Flash password required for player ID: $playerId")
        
        if (currentProfile?.userPasswordFlash?.isNotEmpty() != true) {
            onAutoLoginStatus("Пароль для Flash игры требуется, но не установлен в профиле")
            Log.w(TAG, "Flash password required but not set in profile")
            return
        }
        
        onAutoLoginStatus("Отправка пароля для Flash игры...")
        
        // Генерируем HTML с паролем для Flash игры (аналог GamePhp.cs)
        val flashPasswordHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Отправка пароля...</title>
                <meta charset="UTF-8">
            </head>
            <body>
                <div style="text-align: center; margin-top: 50px; font-family: Arial;">
                    <h3>Отправка пароля...</h3>
                    <p>Выполняется отправка пароля для Flash игры...</p>
                </div>
                <form action="./game.php" method="POST" name="ff">
                    <input name="flcheck" type="hidden" value="${currentProfile?.userPasswordFlash ?: ""}">
                    <input name="nid" type="hidden" value="$playerId">
                </form>
                <script language="JavaScript">
                    console.log('Auto-submitting Flash password form...');
                    document.ff.submit();
                </script>
            </body>
            </html>
        """.trimIndent()
        
        // Загружаем HTML с паролем для Flash игры
        webView.loadDataWithBaseURL(
            "http://www.neverlands.ru/",
            flashPasswordHtml,
            "text/html",
            "UTF-8",
            null
        )
        
        onAutoLoginStatus("Пароль для Flash игры отправлен...")
        Log.i(TAG, "Flash password form submitted for player ID: $playerId")
    }
    
    /**
     * Обработка HTML5 игры на странице
     */
    private fun handleHTML5GameContent(webView: WebView) {
        Log.d(TAG, "Handling HTML5 game content")
        
        val html5GameScript = """
            (function() {
                console.log('Checking HTML5 game content...');
                
                // Проверяем, есть ли содержимое на странице
                var body = document.body;
                if (!body || body.innerHTML.trim().length === 0) {
                    console.log('Empty page detected, reloading...');
                    setTimeout(function() {
                        window.location.reload();
                    }, 1000);
                    return false;
                }
                
                // Проверяем наличие элементов HTML5 игры
                var gameElements = document.querySelectorAll('canvas, [class*="game"], [id*="game"], iframe');
                console.log('Found HTML5 game elements:', gameElements.length);
                
                // Проверяем наличие сообщений об ошибках
                var errorMessages = document.querySelectorAll('.error, .warning, [class*="error"], [class*="warn"]');
                if (errorMessages.length > 0) {
                    console.log('Error messages found on page');
                    var errorText = Array.from(errorMessages).map(el => el.textContent).join('; ');
                    console.log('Errors:', errorText);
                    return false;
                }
                
                // Проверяем наличие скриптов игры
                var scripts = document.querySelectorAll('script[src*="game"], script[src*="main"]');
                console.log('Found game scripts:', scripts.length);
                
                // Считаем загруженные скрипты
                var scriptsLoaded = 0;
                Array.from(scripts).forEach(function(script) {
                    if (script.readyState === 'complete' || script.readyState === 'loaded') {
                        scriptsLoaded++;
                    }
                });
                
                // Проверяем, не пустая ли страница
                var bodyText = body.textContent || body.innerText || '';
                if (bodyText.trim().length < 50 && gameElements.length === 0) {
                    console.log('Page appears to be mostly empty, reloading...');
                    setTimeout(function() {
                        window.location.reload();
                    }, 2000);
                    return false;
                }
                
                console.log('HTML5 game content appears to be loaded correctly');
                return true;
            })();
        """.trimIndent()
        
        webView.evaluateJavascript(html5GameScript) { result ->
            Log.d(TAG, "HTML5 game content check result: $result")
            if (result == "true") {
                onAutoLoginStatus("Игра загружена")
            } else {
                onAutoLoginStatus("Ошибка при загрузке игры")
            }
        }
    }
    
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        // Разрешаем навигацию только на neverlands.ru
        return if (url?.contains("neverlands.ru") == true) {
            Log.d(TAG, "Allowing navigation to: $url")
            false
        } else {
            Log.d(TAG, "Blocking navigation to external URL: $url")
            true
        }
    }
    
    override fun onReceivedError(
        view: WebView?,
        errorCode: Int,
        description: String?,
        failingUrl: String?
    ) {
        super.onReceivedError(view, errorCode, description, failingUrl)
        Log.e(TAG, "WebView error: $errorCode - $description for URL: $failingUrl")
        
        // Сбрасываем состояние загрузки при ошибке
        onLoadingStateChanged(false)
    }
}
