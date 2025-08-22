package ru.neverlands.abclient.ui.game

import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.neverlands.abclient.data.model.UserProfile

/**
 * WebViewClient для игры с автоматической аутентификацией
 * Реализует логику автоматического входа аналогично IndexCgi.cs из Windows версии
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
        private const val FLASH_PASSWORD_INPUT_SELECTOR = "input[name='flcheck']"
        
        // Flash game detection
        private const val FLASH_VARS_PATTERN = "flashvars=\"plid="
        
        // JavaScript injection delay
        private const val INJECTION_DELAY_MS = 500L
    }
    
    private var isWaitingForFlash = false
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
            
            // Проверяем и обрабатываем страницу
            handlePageLoad(view, it)
        }
    }
    
    /**
     * Обработка загрузки страницы
     */
    private fun handlePageLoad(webView: WebView?, url: String) {
        webView ?: return
        
        when {
            // Главная страница или index.cgi - проверяем форму входа
            url.contains(LOGIN_URL_PATTERN) && 
            (url.endsWith("/") || url.contains(INDEX_CGI_PATTERN)) -> {
                handleLoginPage(webView, url)
            }
            
            // Страница game.php - обрабатываем flash пароль или игровой контент
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
            onAutoLoginStatus("Профиль не настроен")
            return
        }
        
        onAutoLoginStatus("Автоматический вход...")
        
        // Задержка для полной загрузки DOM
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
                    
                    // Проверяем, есть ли ошибка входа (аналог IndexCgi.cs)
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
                        // Найдена форма входа - отправляем автоматический POST запрос
                        performAutoLogin(webView)
                    }
                    result.startsWith("\"ERROR:") -> {
                        val error = result.substring(7, result.length - 1) // убираем "ERROR: и "
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
     * Выполняет автоматический вход (аналог IndexCgi.cs)
     */
    private fun performAutoLogin(webView: WebView) {
        onAutoLoginStatus("Отправка данных входа...")
        
        // Создаем и отправляем HTML с автоматической формой, точно как в IndexCgi.cs
        val autoLoginHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Автоматический вход...</title>
                <meta charset="UTF-8">
            </head>
            <body>
                <div style="text-align: center; margin-top: 50px; font-family: Arial;">
                    <h3>Ввод имени и пароля...</h3>
                    <p>Выполняется автоматический вход в игру...</p>
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
        
        // Загружаем HTML с автоматической формой
        webView.loadDataWithBaseURL(
            "http://www.neverlands.ru/",
            autoLoginHtml,
            "text/html",
            "UTF-8",
            null
        )
        
        isWaitingForFlash = true
        onAutoLoginStatus("Вход выполняется...")
        Log.i(TAG, "Auto-login form submitted successfully")
    }
    
    /**
     * Обработка игровой страницы (аналог GamePhp.cs)
     */
    private fun handleGamePage(webView: WebView, url: String) {
        Log.d(TAG, "Handling game page: $url")
        
        // Если ожидаем flash и есть flash пароль
        if (isWaitingForFlash && !currentProfile?.userPasswordFlash.isNullOrBlank()) {
            onAutoLoginStatus("Ввод Flash-пароля...")
            coroutineScope.launch {
                delay(INJECTION_DELAY_MS)
                handleFlashPassword(webView)
            }
        } else {
            // Обрабатываем обычную игровую страницу
            onAutoLoginStatus("Проверка игрового контента...")
            coroutineScope.launch {
                delay(INJECTION_DELAY_MS)
                handleGameContent(webView)
            }
        }
    }
    
    /**
     * Обработка Flash пароля (аналог GamePhp.cs)
     */
    private fun handleFlashPassword(webView: WebView) {
        val flashPassword = currentProfile?.userPasswordFlash ?: return
        
        Log.d(TAG, "Attempting to handle flash password")
        
        val flashPasswordScript = """
            (function() {
                console.log('Checking for flash password form...');
                
                // Ищем flashvars с plid
                var pageContent = document.documentElement.outerHTML;
                var flashVarsMatch = pageContent.match(/flashvars=["']plid=([^"']+)["']/);
                
                if (!flashVarsMatch) {
                    console.log('Flash vars not found');
                    return false;
                }
                
                var plid = flashVarsMatch[1];
                console.log('Found plid:', plid);
                
                // Создаем и отправляем форму с flash паролем
                var form = document.createElement('form');
                form.method = 'POST';
                form.action = './game.php';
                
                var flcheckInput = document.createElement('input');
                flcheckInput.type = 'hidden';
                flcheckInput.name = 'flcheck';
                flcheckInput.value = '$flashPassword';
                form.appendChild(flcheckInput);
                
                var nidInput = document.createElement('input');
                nidInput.type = 'hidden';
                nidInput.name = 'nid';
                nidInput.value = plid;
                form.appendChild(nidInput);
                
                document.body.appendChild(form);
                
                console.log('Submitting flash password form...');
                form.submit();
                
                return true;
            })();
        """.trimIndent()
        
        webView.evaluateJavascript(flashPasswordScript) { result ->
            Log.d(TAG, "Flash password script result: $result")
            if (result == "true") {
                isWaitingForFlash = false
                onAutoLoginStatus("Вход в игру завершен")
                Log.i(TAG, "Flash password submitted successfully")
            } else {
                onAutoLoginStatus("Ошибка ввода Flash-пароля")
            }
        }
    }
    
    /**
     * Обработка игрового контента для предотвращения белого экрана
     */
    private fun handleGameContent(webView: WebView) {
        Log.d(TAG, "Handling game content")
        
        val gameContentScript = """
            (function() {
                console.log('Checking game content...');
                
                // Проверяем, есть ли контент на странице
                var body = document.body;
                if (!body || body.innerHTML.trim().length === 0) {
                    console.log('Empty page detected, reloading...');
                    setTimeout(function() {
                        window.location.reload();
                    }, 1000);
                    return false;
                }
                
                // Проверяем наличие игрового контента или iframe
                var gameElements = document.querySelectorAll('iframe, object, embed, canvas, [class*="game"], [id*="game"]');
                console.log('Found game elements:', gameElements.length);
                
                // Проверяем наличие Flash объектов
                var flashElements = document.querySelectorAll('object[type*="flash"], embed[type*="flash"]');
                console.log('Found flash elements:', flashElements.length);
                
                // Если нет игровых элементов, проверяем ошибки
                if (gameElements.length === 0 && flashElements.length === 0) {
                    console.log('No game elements found');
                    
                    // Проверяем наличие JavaScript ошибок или проблем загрузки
                    var errorMessages = document.querySelectorAll('.error, .warning, [class*="error"], [class*="warn"]');
                    if (errorMessages.length > 0) {
                        console.log('Error messages found on page');
                        return false;
                    }
                    
                    // Проверяем, есть ли текст о необходимости Flash Player
                    var bodyText = body.textContent || body.innerText || '';
                    if (bodyText.toLowerCase().includes('flash') || 
                        bodyText.toLowerCase().includes('plugin') ||
                        bodyText.toLowerCase().includes('adobe')) {
                        console.log('Flash-related content detected');
                        return true;
                    }
                    
                    // Если страница выглядит пустой, пробуем перезагрузить
                    if (bodyText.trim().length < 100) {
                        console.log('Page appears to be mostly empty, reloading...');
                        setTimeout(function() {
                            window.location.reload();
                        }, 2000);
                        return false;
                    }
                }
                
                console.log('Game content appears to be loaded correctly');
                return true;
            })();
        """.trimIndent()
        
        webView.evaluateJavascript(gameContentScript) { result ->
            Log.d(TAG, "Game content check result: $result")
            isWaitingForFlash = false
        }
    }
    
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        // Разрешаем навигацию в пределах игрового домена
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
        
        // Уведомляем о завершении загрузки даже при ошибке
        onLoadingStateChanged(false)
    }
}