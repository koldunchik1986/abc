package ru.neverlands.abclient.ui.game

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import dagger.hilt.android.AndroidEntryPoint
import ru.neverlands.abclient.core.browser.BrowserEmulationManager
import ru.neverlands.abclient.data.preferences.UserPreferencesManager
import ru.neverlands.abclient.data.model.UserProfile
import ru.neverlands.abclient.ui.theme.ABClientTheme
import ru.neverlands.abclient.ui.game.components.AutoLoginDialog
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Activity для игрового браузера
 * Эквивалент FormMainTabs из Windows версии
 */
@AndroidEntryPoint
class GameActivity : ComponentActivity() {
    
    @Inject
    lateinit var browserEmulationManager: BrowserEmulationManager
    
    @Inject
    lateinit var preferencesManager: UserPreferencesManager
    
    companion object {
        internal const val GAME_URL = "http://www.neverlands.ru/"
        
        fun createIntent(context: Context): Intent {
            return Intent(context, GameActivity::class.java)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            ABClientTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GameScreen(
                        browserEmulationManager = browserEmulationManager,
                        preferencesManager = preferencesManager,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameScreen(
    browserEmulationManager: BrowserEmulationManager,
    preferencesManager: UserPreferencesManager,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf(GameActivity.GAME_URL) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var currentProfile by remember { mutableStateOf<UserProfile?>(null) }
    var showProfileWarning by remember { mutableStateOf(false) }
    var autoLoginStatus by remember { mutableStateOf("") }
    var showAutoLoginDialog by remember { mutableStateOf(false) }
    var autoLoginConfirmed by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    
    // Загружаем текущий профиль и проверяем автовход (аналог ConfigSelector.cs)
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                currentProfile = preferencesManager.getCurrentProfile()
                
                // Проверяем условия для автовхода (аналог ConfigSelector.cs)
                if (currentProfile?.let { profile ->
                    profile.userAutoLogon && 
                    profile.userNick.isNotBlank() && 
                    profile.userPassword.isNotBlank()
                } == true) {
                    // Показываем диалог автовхода (аналог FormAutoLogon)
                    showAutoLoginDialog = true
                } else {
                    if (currentProfile?.isLoginDataComplete() != true) {
                        showProfileWarning = true
                    }
                    // Если автовход отключен, загружаем игру сразу
                    autoLoginConfirmed = true
                }
            } catch (e: Exception) {
                // Логируем ошибку, но продолжаем работу без профиля
                showProfileWarning = true
                autoLoginConfirmed = true
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = if (isLoading) "Загрузка..." else "Neverlands",
                            maxLines = 1
                        )
                        currentProfile?.let { profile ->
                            Text(
                                text = if (profile.isLoginDataComplete()) 
                                    "Профиль: ${profile.getDisplayName()}" 
                                else 
                                    "Профиль не настроен",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    
                    IconButton(
                        onClick = { 
                            webView?.reload() 
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                    
                    IconButton(
                        onClick = { 
                            // TODO: Показать меню с дополнительными опциями
                        }
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Предупреждение о ненастроенном профиле
            if (showProfileWarning) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Автоматический вход отключен. Настройте профиль в настройках.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            // Адресная строка и статус авто-входа
            Column {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                
                // Статус авто-входа
                if (autoLoginStatus.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = autoLoginStatus,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
            
            // WebView - создаем только после подтверждения автовхода
            if (autoLoginConfirmed) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            webView = this
                            
                            // Настройка WebView для эмуляции браузера
                            browserEmulationManager.configureWebView(this)
                            
                            // Используем GameWebViewClient с текущим профилем
                            val profile = currentProfile
                            webViewClient = GameWebViewClient(
                                currentProfile = profile,
                                onPageStarted = { url ->
                                    isLoading = true
                                    currentUrl = url
                                },
                                onPageFinished = { url ->
                                    isLoading = false
                                    currentUrl = url
                                },
                                onLoadingStateChanged = { loading ->
                                    isLoading = loading
                                },
                                onAutoLoginStatus = { status ->
                                    autoLoginStatus = status
                                }
                            )
                            
                            // Загружаем игровой сайт
                            loadUrl(GameActivity.GAME_URL)
                        }
                    }
                )
            } else {
                // Показываем сообщение о жидании подтверждения автовхода
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Подготовка к запуску игры...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
    
    // Диалог автовхода (аналог FormAutoLogon)
    val profileSnapshot = currentProfile
    if (showAutoLoginDialog && profileSnapshot != null) {
        AutoLoginDialog(
            userName = profileSnapshot.getDisplayName(),
            onConfirm = {
                showAutoLoginDialog = false
                autoLoginConfirmed = true
            },
            onCancel = {
                showAutoLoginDialog = false
                autoLoginConfirmed = true // Все равно загружаем игру, но без автовхода
                // Временно отключаем автовход для этой сессии
                currentProfile = profileSnapshot.copy(userAutoLogon = false)
            }
        )
    }
}

@Composable
private fun GameLoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Подключение к игровому серверу...",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Настройка эмуляции браузера",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}